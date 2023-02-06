package sunya.cdm.hdf5

import sunya.cdm.api.Dimension
import java.io.IOException

internal val debugGroup = false


// facade must have a dataObject and a groupMessage or groupNewMessage
@Throws(IOException::class)
internal fun H5builder.readH5Group(dataObject: DataObject): H5GroupBuilder {
    require(dataObject.groupMessage != null || dataObject.groupNewMessage != null)

    val nestedObjects = mutableListOf<DataObject>()
    var groupDataObject : DataObject? = null

    /* if has a "group message", then its an "old group"
    if (fdataObject.groupMessage != null) {
        val groupMessage = fdataObject.groupMessage!!
        if (null != hashGroups[groupMessage.btreeAddress].also { facade.group = it }) {
            if (parent.isChildOf(facade.group)) {
                H5objects.log.debug("Remove hard link to group that creates a loop = {}", facade.group.getName())
                facade.group = null
                return
            }
        }

        // read the group, and its contained data objects.
        readGroupOld(this, groupMessage.btreeAddress, groupMessage.nameHeapAddress)
        
    } else */
    if (dataObject.groupNewMessage != null) {
        val groupNewMessage = dataObject.groupNewMessage!!
            // read the group, and its contained data objects.
        groupDataObject = readGroupNew(groupNewMessage, dataObject, nestedObjects)
        
    } else { // we dont know what it is
        throw IllegalStateException("H5Group needs group messages " + dataObject.name)
    }

    return H5GroupBuilder(this, "dunno", groupDataObject!!, nestedObjects)
}

@Throws(IOException::class)
internal fun H5builder.readGroupNew(
    groupNewMessage: LinkInfoMessage,
    dobj: DataObject,
    nestedObjects : MutableList<DataObject>,
) : DataObject? {
    var name = ""

    /* if (groupNewMessage.fractalHeapAddress >= 0) {
        val fractalHeap = FractalHeap(header, group.displayName, groupNewMessage.fractalHeapAddress, memTracker)
        val btreeAddress: Long =
            if (groupNewMessage.v2BtreeAddressCreationOrder >= 0) groupNewMessage.v2BtreeAddressCreationOrder else groupNewMessage.v2BtreeAddress
        check(btreeAddress >= 0) { "no valid btree for GroupNew with Fractal Heap" }

        // read in btree and all entries
        val btree = BTree2(header, group.displayName, btreeAddress)
        for (e in btree.entryList) {
            var heapId: ByteArray
            heapId = when (btree.btreeType) {
                5 -> (e.record as BTree2.Record5).getHeapId()
                6 -> (e.record as BTree2.Record6).getHeapId()
                else -> continue
            }

            // the heapId points to a Link message in the Fractal Heap
            val fractalHeapId: FractalHeap.DHeapId = fractalHeap.getFractalHeapId(heapId)
            val pos: Long = fractalHeapId.getPos()
            if (pos < 0) continue
            raf.seek(pos)
            val linkMessage = readMessageLink()
            linkMessage.read()
            group.nestedObjects.add(DataObjectFacade(group, linkMessage.linkName, linkMessage.linkAddress))
        }
    } else { */
        // look for link messages
        for (mess in dobj.messages) {
            if (mess.mtype === MessageType.Link) {
                val linkMessage = mess as LinkMessage
                if (linkMessage.linkType == 0) { // hard link
                    val hardLink = mess as LinkHard
                    return this.getDataObject(hardLink.linkAddress, null)
                    //nestedObjects.add(dobj)
                    //name = hardLink.linkName
                }
            }
        }
    // }
    return null
}


/*
@Throws(IOException::class)
internal fun H5builder.readGroupOld(group: H5Group, btreeAddress: Long, nameHeapAddress: Long) {
    // track by address for hard links
    hashGroups.put(btreeAddress, group)

    val nameHeap = LocalHeap(group, nameHeapAddress)
    val btree = GroupBTree(group.displayName, btreeAddress)

    // now read all the entries in the btree : Level 1C
    for (s in btree.getSymbolTableEntries()) {
        val sname: String = nameHeap.getString(s.getNameOffset().toInt())
        if (s.cacheType == 2) {
            val linkName: String = nameHeap.getString(s.linkOffset)
            group.nestedObjects.add(DataObjectFacade(group, sname, linkName))
        } else {
            group.nestedObjects.add(DataObjectFacade(group, sname, s.getObjectAddress()))
        }
    }
}

 */

internal class H5GroupBuilder(
    val h5builder : H5builder,
    val name: String,
    val dataObject: DataObject,
    val nestedObjects : MutableList<DataObject> = mutableListOf()
) {
    val dimMap = mutableMapOf<String, Dimension>()
    val dimList = mutableListOf<Dimension>() // need to track dimension order
    val nestedGroupsBuilders = mutableListOf<H5GroupBuilder>()
    val variables = mutableListOf<H5Variable>()
    val typedefs = mutableListOf<H5Typedef>()

    init {

        for (nested in nestedObjects) {
            // if has a "group message", then its a group
            if (dataObject.groupMessage != null || dataObject.groupNewMessage != null) {
                nestedGroupsBuilders.add(h5builder.readH5Group(nested))

            // if it has a Datatype and a StorageLayout, then its a Variable
            } else if (dataObject.mdt != null && dataObject.mdl != null) {
                variables.add(H5Variable(nested))

            // if it has only a Datatype, its a Typedef
            } else if (dataObject.mdt != null) {
                typedefs.add(H5Typedef(nested))

            } else {
                println("unknown nestedObject $nested")
            }
        }
    }

    fun build() : H5Group {
        val nestedGroups = nestedGroupsBuilders.map { it.build() }
        return H5Group( name, dataObject, nestedGroups, variables, typedefs)
    }
}

internal class H5Variable(val dataObject: DataObject) {
    val mdt : DatatypeMessage = dataObject.mdt!!
    val mdl : DataLayoutMessage = dataObject.mdl!!
}

internal class H5Typedef(val dataObject: DataObject) {
    val enumMessage : DatatypeEnum

    init {
        require(dataObject.mdt != null && dataObject.mdl == null)
        require(dataObject.mdt!!.type == Datatype5.Enumerated)
        this.enumMessage = (dataObject.mdt!!) as DatatypeEnum
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

    fun attributes() : Iterable<AttributeMessage> = dataObject.attributes
}