package com.sunya.netchdf.hdf5

import com.sunya.cdm.api.Dimension
import com.sunya.cdm.api.TypedefKind
import com.sunya.cdm.iosp.OpenFileState
import java.io.IOException
import java.nio.ByteOrder

internal val debugGroup = false
internal val debugHardLink = false
internal val debugSoftLink = false
internal val debugBtree2 = false

@Throws(IOException::class)
internal fun H5builder.readH5Group(facade: DataObjectFacade): H5GroupBuilder? {
    val nestedObjects = mutableListOf<DataObjectFacade>()
    val dataObject = facade.dataObject!!
    val result = H5GroupBuilder(this, facade.parent, facade.name, dataObject, nestedObjects)

    // if has a "group message", then its an "old group"
    if (dataObject.groupMessage != null) {
        val groupMessage = dataObject.groupMessage!!
        // check for hard links
        if (debugHardLink) {
            println("HO look for group address = ${groupMessage.btreeAddress}")
        }
        // deja vu
        facade.group = hashGroups.get(groupMessage.btreeAddress)
        if (facade.group != null && facade.parent != null) {
            if (facade.parent.isChildOf(facade.group!!)) {
                println("Remove hard link to group that creates a loop = ${facade.group!!.name}")
                facade.group = null
                return null
            }
        }

        // read the group, and its contained data objects.
        readGroupOld(result, groupMessage.btreeAddress, groupMessage.localHeapAddress)
        
    } else if (dataObject.groupNewMessage != null) {
            // read the group, and its contained data objects.
        readGroupNew(result, dataObject.groupNewMessage!!, dataObject, nestedObjects)
        
    } else { // we dont know what it is
        throw IllegalStateException("H5Group needs group messages " + dataObject.name)
    }

    return result
}

@Throws(IOException::class)
internal fun H5builder.readGroupNew(
    parent: H5GroupBuilder,
    groupNewMessage: LinkInfoMessage,
    dobj: DataObject,
    nestedObjects : MutableList<DataObjectFacade>,
) {
    if (groupNewMessage.fractalHeapAddress >= 0) {
        val fractalHeap = FractalHeap(this, parent.name, groupNewMessage.fractalHeapAddress, memTracker)
        val btreeAddress: Long = groupNewMessage.v2BtreeAddressCreationOrder ?: groupNewMessage.v2BtreeAddress
        check(btreeAddress >= 0) { "no valid btree for GroupNew with Fractal Heap" }

        // read in btree and all entries
        val btree = BTree2(this, parent.name, btreeAddress)
        for (e in btree.entryList) {
            var heapId: ByteArray
            heapId = when (btree.btreeType) {
                5 -> (e.record as BTree2.Record5).heapId
                6 -> (e.record as BTree2.Record6).heapId
                else -> continue
            }

            // the heapId points to a Link message in the Fractal Heap
            val fractalHeapId: FractalHeap.DHeapId = fractalHeap.getFractalHeapId(heapId)
            val pos: Long = fractalHeapId.getPos()
            if (pos < 0) continue
            val state = OpenFileState(pos, ByteOrder.LITTLE_ENDIAN)
            val linkMessage = this.readLinkMessage(state)

            if (debugBtree2) {
                println("linkMessage ${linkMessage}")
            }
            nestedObjects.add(DataObjectFacade(parent, linkMessage.linkName).setAddress((linkMessage as LinkHard).linkAddress))
        }
    } else {
        // look for link messages
        for (mess in dobj.messages) {
            if (mess.mtype === MessageType.Link) {
                val linkMessage = mess as LinkMessage
                if (linkMessage.linkType == 0) { // hard link
                    val hardLink = mess as LinkHard
                    nestedObjects.add(DataObjectFacade(parent, linkMessage.linkName).setAddress(hardLink.linkAddress))
                }
            }
        }
    }
}

@Throws(IOException::class)
internal fun H5builder.readGroupOld(groupb: H5GroupBuilder, btreeAddress: Long, nameHeapAddress: Long) {
    // track by address for hard links
    hashGroups.put(btreeAddress, groupb)
    if (debugHardLink) {
        println("readGroupOld ${groupb.name}")
    }

    val nameHeap = LocalHeap(this, nameHeapAddress)
    val btree = Btree1(this, btreeAddress, groupb.name)

    // now read all the entries in the btree : Level 1C
    for (s in btree.symbolTableEntries) {
        val sname: String = nameHeap.getStringAt(s.nameOffset.toInt())
        if (debugSoftLink) {
            println(" Symbol name= ${sname}")
        }
        if (s.cacheType == 2) {
            val linkName: String = nameHeap.getStringAt(s.linkOffset!!)
            if (debugSoftLink) {
                println(" Symbol link name= ${linkName}")
            }
            groupb.nestedObjects.add(DataObjectFacade(groupb, sname).setLinkName(this, linkName))
        } else {
            groupb.nestedObjects.add(DataObjectFacade(groupb, sname).setAddress(s.objectHeaderAddress))
        }
    }
}

internal fun H5builder.replaceSymbolicLinks(groupb: H5GroupBuilder) {
    val objList = groupb.nestedObjects
    var count = 0
    while (count < objList.size) {
        val dof = objList[count]
        if (dof.group != null) { // nested group - recurse
            replaceSymbolicLinks(dof.group!!)

        } else if (dof.linkName != null) { // symbolic links
            val link: DataObjectFacade? = this.symlinkMap.get(dof.linkName)
            if (link == null) {
                println(" WARNING Didnt find symbolic link=${dof.linkName} from ${dof.name}")
                objList.removeAt(count)
                continue
            }

            // dont allow loops
            if (link.group != null) {
                if (groupb.isChildOf(link.group!!)) {
                    println(" ERROR Symbolic Link loop found =${dof.linkName}")
                    objList.removeAt(count)
                    continue
                }
            }

            // dont allow in the same group. better would be to replicate the group with the new name
            if (dof.parent == link.parent) {
                objList.remove(dof)
                count-- // negate the incr
            } else { // replace
                objList[count] = link
            }
            if (debugFlow) {
                println("  Found symbolic link=${dof.linkName}")
            }
        }
        count++
    }
}

internal class DataObjectFacade(val parent : H5GroupBuilder?, val name: String) {
    var address: Long? = null
    var dataObject: DataObject? = null
    var group: H5GroupBuilder? = null
    var linkName: String? = null

    var isGroup = false
    var isVariable = false
    var isTypedef = false

    fun setLinkName(header : H5builder, linkName : String) : DataObjectFacade {
        this.linkName = linkName
        header.symlinkMap[name] = this
        return this
    }

    fun setAddress(address : Long) : DataObjectFacade {
        this.address = address
        return this
    }

    fun setDataObject(dataObject : DataObject) : DataObjectFacade {
        this.dataObject = dataObject
        return this
    }

    fun build(header: H5builder) {
        if (dataObject == null && address != null) {
            dataObject = header.getDataObject(address!!, name)
        }
        val local = dataObject!!
        if (local.groupMessage != null || local.groupNewMessage != null) {
            // has a group message
            isGroup = true
        } else if ((local.mdt != null) and (local.mdl != null)) {
            // has a Datatype and a DataLayout message
            isVariable = true
            // if its an enum or compound, could be a non-shared typedef. Found in non-netcdf4 hdf5 files.
            if (!local.mdt!!.isShared) {
                if ((local.mdt.type == Datatype5.Enumerated) or (local.mdt.type == Datatype5.Compound)) {
                    isTypedef = true
                }
            }
        } else if (local.mdt != null) {
            // must be a  typedef
            isTypedef = true
        } else {
            // see /home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf4/tst_opaque_data.nc4 = opaque typedef
            println("WARNING Unknown DataObjectFacade = ${this}")
        }
    }

    override fun toString(): String {
        return "DataObjectFacade(parent=${parent?.name}, name='$name', address=$address, dataObject=${dataObject?.name}, " +
                "group=$group, isGroup=$isGroup, isVariable=$isVariable, isTypedef=$isTypedef, linkName=$linkName)"
    }
}

internal class H5GroupBuilder(
    val header : H5builder,
    val parent : H5GroupBuilder?,
    val name: String,
    val dataObject: DataObject,
    val nestedObjects : MutableList<DataObjectFacade> = mutableListOf()
) {
    val nestedGroupsBuilders = mutableListOf<H5GroupBuilder>()


    // is this a child of that ?
    fun isChildOf(that: H5GroupBuilder): Boolean {
        if (parent == null) return false
        return if (parent === that) true else parent.isChildOf(that)
    }

    fun build() : H5Group {
        val variables = mutableListOf<H5Variable>()
        val typedefs = mutableListOf<H5Typedef>()

        for (nested in nestedObjects) {
            nested.build(header)

            if (nested.isGroup) {
                val nestedGroup = header.readH5Group(nested)
                if (nestedGroup != null) {
                    nestedGroupsBuilders.add(nestedGroup)
                }
            }
            if (nested.isVariable) {
                variables.add(H5Variable(header, nested.dataObject!!))
            }
            // might be both a variable and a typedef
            if (nested.isTypedef) {
                val typedef = H5Typedef(nested.dataObject!!)
                typedefs.add(typedef)
            }
        }

        val nestedGroups = nestedGroupsBuilders.map { it.build() }
        val result = H5Group( name, dataObject, nestedGroups, variables, typedefs)
        nestedGroups.forEach{ it.parent = result }
        return result
    }
}

internal class H5Variable(val header : H5builder, val dataObject: DataObject) {
    // these all have to be non-null for it to be a variable
    val name = dataObject.name!!
    val mdt : DatatypeMessage = dataObject.mdt!!
    val mdl : DataLayoutMessage = dataObject.mdl!!
    val mds : DataspaceMessage = dataObject.mds!!
    // optional
    val mfp: FilterPipelineMessage? = dataObject.mfp

    // used in CdmBuilder
    var is2DCoordinate = false
    var hasNetcdfDimensions = false
    var dimList: String? = null // list of dimension names for this variable
    var isVariable = true

    fun attributes() : MutableIterable<AttributeMessage> = dataObject.attributes
    fun removeAtt(att : AttributeMessage) = dataObject.attributes.remove(att)
}

internal class H5Typedef(val dataObject: DataObject) {
    var enumMessage : DatatypeEnum? = null
    var vlenMessage : DatatypeVlen? = null
    var opaqueMessage : DatatypeOpaque? = null
    var compoundMessage : DatatypeCompound? = null

    val kind : TypedefKind
    val mdtAddress : Long
    val mdtHash : Int

    init {
        require(dataObject.mdt != null)
        mdtAddress = dataObject.mdt.address
        mdtHash = dataObject.mdt.hashCode()

        when (dataObject.mdt.type) {
            Datatype5.Enumerated -> {
                this.enumMessage = (dataObject.mdt) as DatatypeEnum
                kind = TypedefKind.Enum
            }
            Datatype5.Vlen -> {
                this.vlenMessage = (dataObject.mdt) as DatatypeVlen
                kind = TypedefKind.Vlen
            }
            Datatype5.Opaque -> {
                this.opaqueMessage = (dataObject.mdt) as DatatypeOpaque
                kind = TypedefKind.Opaque
            }
            Datatype5.Compound -> {
                this.compoundMessage = (dataObject.mdt) as DatatypeCompound
                kind = TypedefKind.Compound
            }
            else -> {
                kind = TypedefKind.Unknown
            }
        }
    }
}

internal class H5Group(
    val name: String,
    val dataObject: DataObject,
    val nestedGroups : List<H5Group>,
    val variables : List<H5Variable>,
    val typedefs : List<H5Typedef>,
) {
    val dimMap = mutableMapOf<String, Dimension>()
    val dimList = mutableListOf<Dimension>() // need to track dimension order
    var parent : H5Group? = null

    fun attributes() : Iterable<AttributeMessage> = dataObject.attributes

    fun findDimension(dimName : String) : Dimension? {
        return this.dimMap[dimName] ?: this.parent?.findDimension(dimName)
    }
}