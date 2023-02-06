package sunya.cdm.hdf5

import sunya.cdm.dsl.structdsl
import sunya.cdm.hdf5.FilterType.Companion.fromId
import sunya.cdm.iosp.OpenFileState
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*

private val debugContinuation = false
internal val debugMessage = false

// type safe enum
enum class MessageType(val uname: String, val num: Int) {
        NIL("NIL", 0),
        Dataspace("SimpleDataspace", 1),
        LinkInfo("GroupNew", 2),
        Datatype("Datatype", 3),
        FillValueOld("FillValueOld", 4),
        FillValue("FillValue", 5),
        Link("Link", 6),
        ExternalDataFiles("ExternalDataFiles", 7), // not supported
        Layout("Layout", 8),
        GroupInfo("GroupInfo", 10),
        FilterPipeline("FilterPipeline", 11),
        Attribute("Attribute", 12),
        Comment("Comment", 13),
        LastModifiedOld("LastModifiedOld", 14),
        SharedObject("SharedObject", 15),
        ObjectHeaderContinuation("Continuation", 16),
        SymbolTable("Group", 17),
        LastModified("LastModified", 18),
        AttributeInfo("AttributeInfo", 21),
        ObjectReferenceCount("ObjectReferenceCount", 22),
    ;

    override fun toString(): String {
        return "$uname($num)"
    }

    companion object {
        private val messbyNum = values().associateBy { it.num }

        fun byNumber(num: Int): MessageType? {
            return messbyNum[num]
        }
    }
}

// Level 2A1
@Throws(IOException::class)
fun H5builder.readMessagesVersion1(
    state: OpenFileState,
    nmess: Int,
    dataSize: Int,
    messages: MutableList<HeaderMessage>
): Int {
    val posLimit = state.pos + dataSize
    var count = 0
    while (count < nmess && state.pos < posLimit) {
        val mess = this.readHeaderMessage(state, 1, false)
        count++
        if (mess == null) {
            continue
        }

        // if we hit a continuation, then we go into nested reading
        if (mess.mtype == MessageType.ObjectHeaderContinuation) {
            val c: ContinueMessage = mess as ContinueMessage // LOOK does this count towards nmess?
            count += readMessagesVersion1(state.copy(pos = c.offset), nmess - count, c.length.toInt(), messages)

        } else {
            messages.add(mess)
        }
    }
    return count
}

@Throws(IOException::class)
fun H5builder.readMessagesVersion2(
    state: OpenFileState,
    dataSize: Long,
    creationOrderPresent: Boolean,
    messages: MutableList<HeaderMessage>,
): Int {
    val startPos = state.pos
    val posLimit = state.pos + dataSize - 3

    while (state.pos < posLimit) {
        // maxBytes is number of bytes of messages to be read. however, a message is at least 4 bytes long, so
        // we are done if we have read > maxBytes - 4. There appears to be an "off by one" possibility
        val mess = this.readHeaderMessage(state, 2, creationOrderPresent) ?: continue

        // if we hit a continuation, then we go into nested reading
        if (mess.mtype == MessageType.ObjectHeaderContinuation) {
            val c: ContinueMessage = mess as ContinueMessage
            val continuationBlockFilePos: Long = this.getFileOffset(c.offset)
            val cstate = state.copy(pos = continuationBlockFilePos)
            val sign = raf.readString(cstate, 4)
            // make sure were not jumping into nowhere
            if (sign != "OCHK") throw RuntimeException(" ObjectHeaderContinuation Missing signature")
            if (debugContinuation) println("***ObjectHeaderContinuation start $continuationBlockFilePos")
            readMessagesVersion2(
                cstate,
                (c.length.toInt() - 8).toLong(),
                creationOrderPresent,
                messages
            )
            if (debugContinuation) println("***ObjectHeaderContinuation end")
        } else {
            messages.add(mess)
        }
    }
    return messages.size
}

@Throws(IOException::class)
fun H5builder.readHeaderMessage(state: OpenFileState, version: Int, hasCreationOrder: Boolean): HeaderMessage? {
    val startPos = state.pos
    val mtype: MessageType?
    val flags: Int
    val headerSize : Int
    val messageSize : Int
    if (version == 1) {
        val rawdata1 =
            structdsl("HeaderMessage1", raf, state) {
                fld("type", 2)
                fld("size", 2)
                fld("flags", 1)
                skip(3)
            }
        if (debugMessage) rawdata1.show()

        mtype = MessageType.byNumber(rawdata1.getShort("type").toInt())
        flags = rawdata1.getByte("flags").toInt()
        messageSize = rawdata1.getShort("size").toInt()
        headerSize = rawdata1.dataSize()

        /* if (flags and 2 != 0) { // shared
            messData = getSharedDataObject(mtype).mdt // a shared datatype, eg enums
            return header_length + size // can we return a MessageDataType?
        }

         */

    } else {
        val rawdata2 =
            structdsl("HeaderMessage2", raf, state) {
                fld("type", 1)
                fld("size", 2)
                fld("flags", 1)
                if (hasCreationOrder) {
                    fld("creationOrder", 2)
                }
            }
        if (debugMessage) rawdata2.show()
        mtype = MessageType.byNumber(rawdata2.getByte("type").toInt())
        flags = rawdata2.getByte("flags").toInt()
        messageSize = rawdata2.getShort("size").toInt()
        headerSize = rawdata2.dataSize()

        // ignoring the gap and the checksum
    }

    val result = when (mtype) {
        MessageType.NIL -> {
            null
        }
        MessageType.Dataspace -> this.readDataspaceMessage(state) // 1
        MessageType.LinkInfo -> this.readLinkInfoMessage(state) // 2
        MessageType.Datatype -> this.readDatatypeMessage(state) // 3
        MessageType.FillValueOld -> this.readFillValueOldMessage(state) // 4
        MessageType.FillValue-> this.readFillValueMessage(state) // 5
        MessageType.Link -> this.readLinkMessage(state) // 5
        MessageType.Layout -> this.readDataLayoutMessage(state) // 8
        MessageType.GroupInfo -> this.readGroupInfoMessage(state) // 10
        MessageType.FilterPipeline -> this.readFilterPipelineMessage(state) // 11
        MessageType.Attribute -> this.readAttributeMessage(state) // 11
        MessageType.ObjectHeaderContinuation -> this.readContinueMessage(state) // 16
        MessageType.SymbolTable -> this.readSymbolTableMessage(state) // 17
        MessageType.AttributeInfo -> this.readAttributeInfoMessage(state) // 21
        else -> throw RuntimeException("Unimplemented message type = $mtype")
    }

    println(" done Message ${mtype} pos = ${state.pos} expect ${startPos + messageSize + headerSize}")
    // heres where we get the position right, no matter what
    state.pos = startPos + messageSize + headerSize
    return result
}


// Header Message: Level 2A1 and 2A2 (part of Data Object)
open class HeaderMessage(val mtype: MessageType) : Comparable<HeaderMessage> {
    override operator fun compareTo(other: HeaderMessage): Int =
        compareValuesBy(this, other, { it.mtype.num }, { other.mtype.num })
}

////////////////////////////////////////// 1 IV.A.2.b. The Dataspace Message
@Throws(IOException::class)
fun H5builder.readDataspaceMessage(state: OpenFileState): DataspaceMessage {
    val version = raf.readByte(state).toInt()
    val flags = raf.readByte(state.incr(1)).toInt()
    state.incr(-3)
    val rawdata =
        structdsl("DataspaceMessage", raf, state) {
            fld("version", 1)
            fld("rank", 1)
            fld("flags", 1)
            if (version == 1) {
                skip(4)
            }
            fld("type", 1)
            array("dims", sizeLengths, "rank")
            if (flags and 1 != 0) {
              array("maxsize", sizeLengths, "rank")
            }
            if (version == 1 && (flags and 2 != 0)) {
                array("permute", sizeLengths, "rank")
            }
        }
    if (debugMessage) rawdata.show()

    val type = if (version == 1) {
        if (rawdata.getByte("rank").toInt() == 0) 0 else 1
    } else {
        rawdata.getByte("type").toInt()
    }

    return DataspaceMessage(
        DataspaceType.of(type),
        rawdata.getIntArray("dims"),
    )
}

enum class DataspaceType(val num : Int) {
    Scalar(0), Simple(1), Null(2);

    companion object {
        fun of(num: Int) : DataspaceType {
            return when (num) {
                0 -> Scalar
                1 -> Simple
                2 -> Null
                else -> throw RuntimeException("Unknown DataspaceType $num")
            }
        }
    }
}

data class DataspaceMessage(val type: DataspaceType, val dims: IntArray) : HeaderMessage(MessageType.Dataspace) {
    fun rank() : Int = dims.size
}

////////////////////////////////////////// 2 IV.A.2.c. The Link Info Message
@Throws(IOException::class)
fun H5builder.readLinkInfoMessage(state: OpenFileState): LinkInfoMessage {
    val flags = raf.readByte(state.copy().incr(1)).toInt()
    val rawdata =
        structdsl("LinkInfoMessage", raf, state) {
            fld("version", 1)
            fld("flags", 1)
            if ((flags and 1) != 0) {
                fld("maxCreationIndex", 8)
            }
            fld("fractalHeapAddress", sizeOffsets)
            fld("v2BtreeAddress", sizeOffsets)
            if ((flags and 2) != 0) {
                fld("v2BtreeAddressCreationOrder", 8)
            }
        }
    if (debugGroup) rawdata.show()

    return LinkInfoMessage(
        rawdata.getLong("fractalHeapAddress"),
        rawdata.getLong("v2BtreeAddress"),
    )
}

data class LinkInfoMessage(val fractalHeapAddress: Long, val v2BtreeAddress: Long) : HeaderMessage(MessageType.LinkInfo)

////////////////////////////////////////// 4
@Throws(IOException::class)
fun H5builder.readFillValueOldMessage(state: OpenFileState): FillValueOldMessage {
    val rawdata =
        structdsl("FillValueOldMessage", raf, state) {
            fld("size", 4)
            array("value", 1, "size")
        }
    if (debugMessage) rawdata.show()

    return FillValueOldMessage(
        rawdata.getInt("size"),
        rawdata.getByteBuffer("value"),
    )
}

data class FillValueOldMessage(val size: Int, val value: ByteBuffer) : HeaderMessage(MessageType.FillValueOld)

////////////////////////////////////////// 5
@Throws(IOException::class)
fun H5builder.readFillValueMessage(state: OpenFileState): FillValueMessage {
    val version = raf.readByte(state).toInt()
    val hasFillValue: Boolean
    if (version < 3) {
        val spaceAllocateTime = raf.readByte(state)
        val fillWriteTime = raf.readByte(state)
        hasFillValue = raf.readByte(state).toInt() != 0
    } else {
        val flags = raf.readByte(state)
        val spaceAllocateTime = (flags.toInt() and 3).toByte()
        val fillWriteTime = (flags.toInt() shr 2 and 3).toByte()
        hasFillValue = flags.toInt() and 32 != 0
    }

    if (hasFillValue) {
        val size = raf.readInt(state)
        if (size > 0) {
            val value = raf.readByteBuffer(state, size)
            return FillValueMessage(
                true,
                size,
                value,
            )
        }
    }

    return FillValueMessage(
        false,
        null,
        null,
    )
}

data class FillValueMessage(val hasFillVakue: Boolean, val size: Int?, val value: ByteBuffer?) :
    HeaderMessage(MessageType.FillValue)

////////////////////////////////////////// 6
@Throws(IOException::class)
fun H5builder.readLinkMessage(state: OpenFileState): LinkMessage {
    val version = raf.readByte(state)
    val flags = raf.readByte(state).toInt()
    val vsize =  variableSizeFactor(flags and 3)
    state.incr(-2)

    val rawdata =
        structdsl("LinkMessage", raf, state) {
            fld("version", 1)
            fld("flags", 1)
            if (flags and 8 != 0) {
                fld("linkType", 1)
            }
            if (flags and 4 != 0) {
                fld("creationOrder", 8)
            }
            if (flags and 0x10 != 0) {
                fld("encoding", 1)
            }
            fld("linkNameLength", vsize)
            array("linkName", 1, "linkNameLength")
        }
    if (debugGroup) rawdata.show()

    val linkName = rawdata.getString("linkName")
    val linkType = if (flags and 8 != 0) {
        rawdata.getByte("linkType").toInt() // 0 = hard, 1 = soft, 64 = external
    } else 0

    if (linkType == 0) {
        // "A hard link (should never be stored in the file)" wtf?
        val linkAddress = this.readOffset(state)
        if (debugGroup) println(" LinkHard $linkName $linkAddress")
        return LinkHard(linkType, linkName, linkAddress)
    }

    val linkInfo =
        when (linkType) {
            1 -> {
                val len = raf.readShort(state)
                raf.readString(state, len.toInt())
            }
            64 -> {
                val len = raf.readShort(state)
                raf.readString(state, len.toInt()) // actually 2 strings - see docs
            }
            else -> {
                throw RuntimeException("Unknown link type")
            }
        }

    if (debugGroup) println(" LinkSoft $linkName $linkInfo")
    return LinkSoft(
        linkType,
        linkName,
        linkInfo
    )
}

open class LinkMessage(val linkType: Int, val linkName: String) : HeaderMessage(MessageType.Link)
open class LinkSoft(linkType: Int, linkName: String, val linkInfo: String) : LinkMessage(linkType, linkName)
open class LinkHard(linkType: Int, linkName: String, val linkAddress: Long) : LinkMessage(linkType, linkName)

////////////////////////////////////////// 10

@Throws(IOException::class)
fun H5builder.readGroupInfoMessage(state: OpenFileState): GroupInfoMessage {
    val version = raf.readByte(state).toInt()
    val flags = raf.readByte(state).toInt()
    state.incr(-2)

    val rawdata =
        structdsl("GroupInfoMessage", raf, state) {
            fld("version", 1)
            fld("flags", 1)
            if (flags and 1 != 0) {
                fld("maxCompactValue", 2)
                fld("minDenseValue", 2)
            }
            if (flags and 2 != 0) {
                fld("estNumEntries", 2)
                fld("estLengthEntryName", 2)
            }
        }
    if (debugGroup) rawdata.show()

    return GroupInfoMessage(
        if (flags and 2 != 0) rawdata.getShort("estNumEntries") else null,
        if (flags and 2 != 0) rawdata.getShort("estLengthEntryName") else null,
    )
}

data class GroupInfoMessage(val estNumEntries: Short?, val estLengthEntryName: Short?) : HeaderMessage(MessageType.GroupInfo)

////////////////////////////////////////// 11
// Message Type 11/0xB "Data Storage - Filter Pipeline" : apply a filter to the "data stream"
@Throws(IOException::class)
fun H5builder.readFilterPipelineMessage(state: OpenFileState): FilterPipelineMessage {
    val version: Byte = raf.readByte(state)
    val nfilters: Byte = raf.readByte(state)
    if (version.toInt() == 1) {
        state.pos += 6
    }
    val filters = mutableListOf<Filter>()
    for (i in 0 until nfilters) {
        val rawdata =
            structdsl("Filter", raf, state) {
                fld("filterId", 2)
                fld("nameLength", 2)
                fld("flags", 2)
                fld("nclientValues", 2)
                array("name", 1, "nameLength")
                array("clientValues", 4, "nclientValues")
                // padding for version 1
            }
        if (debugMessage) rawdata.show()

        val filterType = fromId(rawdata.getByte("filterId").toInt())
        filters.add(Filter(filterType, rawdata.getString("name"), rawdata.getIntArray("clientValues")))
    }

    return FilterPipelineMessage(
        filters,
    )
}

enum class FilterType(val id: Int) {
    none(0), deflate(1), shuffle(2), fletcher32(3), szip(4), nbit(5), scaleoffset(6), zstandard(32015), unknown(Int.MAX_VALUE);

    companion object {
        fun fromId(id: Int): FilterType {
            for (type in FilterType.values()) {
                if (type.id == id) {
                    return type
                }
            }
            return unknown
        }

        fun nameFromId(id: Int): String {
            for (type in FilterType.values()) {
                if (type.id == id) {
                    return type.name
                }
            }
            return "UnknownFilter$id"
        }
    }
}

data class Filter(val filterType: FilterType, val name : String, val clientValues :IntArray)

data class FilterPipelineMessage(val filters: List<Filter>) : HeaderMessage(MessageType.FilterPipeline)

///////////////////////////////////////////// 12/0xC "Attribute" : define an Attribute

@Throws(IOException::class)
fun H5builder.readAttributeMessage(state: OpenFileState): AttributeMessage {
    val version = raf.readByte(state).toInt()
    val flags = raf.readByte(state).toInt()
    state.incr(-2)

    val rawdata =
        structdsl("AttributeMessage", raf, state) {
            fld("version", 1)
            fld("flags", 1)
            fld("nameLength", 2)
            fld("datatypeSize", 2)
            fld("dataspaceSize", 2)
            array("name", 1, "nameLength")
            if (version == 3) {
                fld("encoding", 1)
            }
        }
    if (debugMessage) rawdata.show()

    val name = rawdata.getString("name")
    if (version == 1) {
        state.pos += padding(name.length, 8) // LOOK ??
    }

    // read the datatype
    val mdt : DatatypeMessage
    val isShared = flags and 1 != 0
    if (isShared) {
        throw java.lang.RuntimeException("getSharedDataObject not implemented")
        // mdt = getSharedDataObject(MessageType.Datatype).mdt
    } else {
        val startPos = state.pos
        mdt = this.readDatatypeMessage(state)
        if (version == 1) {
            val datatypeSize = rawdata.getShort("datatypeSize").toInt()
            state.pos = startPos + datatypeSize + padding(datatypeSize, 8)
        }
    }

    // read the dataspace
    val startPos = state.pos
    val mds = this.readDataspaceMessage(state)
    if (version == 1) {
        val dataspaceSize = rawdata.getShort("dataspaceSize").toInt()
        state.pos = startPos + dataspaceSize + padding(dataspaceSize, 8)
    }

    val dataPos = state.pos // note this is absolute position (no offset needed)
    val size : Int = 0 // LOOK where does this come from
    state.pos += size

    return AttributeMessage(
        name,
        mdt,
        mds,
        dataPos,
    )
}

data class AttributeMessage(val name: String, val mdt: DatatypeMessage, val mds : DataspaceMessage, val dataPos : Long)
    : HeaderMessage(MessageType.Attribute) {
        fun show() {
            println("${mdt.type} $name ${mds.dims.contentToString()} ${mds.type}")
        }
    }


////////////////////////////////////////// 16
@Throws(IOException::class)
fun H5builder.readContinueMessage(state: OpenFileState): ContinueMessage {
    val rawdata =
        structdsl("ContinueMessage", raf, state) {
            fld("offset", sizeOffsets)
            fld("length", sizeLengths)
        }
    if (debugMessage) rawdata.show()

    return ContinueMessage(
        rawdata.getLong("offset"),
        rawdata.getLong("length"),
    )
}

data class ContinueMessage(val offset: Long, val length: Long) : HeaderMessage(MessageType.ObjectHeaderContinuation)

//////

////////////////////////////////////////// 17
@Throws(IOException::class)
fun H5builder.readSymbolTableMessage(state: OpenFileState): SymbolTableMessage {
    val rawdata =
        structdsl("SymbolTableMessage", raf, state) {
            fld("btreeAddress", sizeOffsets)
            fld("localHeapAddress", sizeOffsets)
        }
    if (debugMessage) rawdata.show()

    return SymbolTableMessage(
        rawdata.getLong("btreeAddress"),
        rawdata.getLong("localHeapAddress"),
    )
}

data class SymbolTableMessage(val btreeAddress: Long, val localHeapAddress: Long) : HeaderMessage(MessageType.SymbolTable)

////////////////////////////////////////// 10

@Throws(IOException::class)
fun H5builder.readAttributeInfoMessage(state: OpenFileState): AttributeInfoMessage {
    val version = raf.readByte(state).toInt()
    val flags = raf.readByte(state).toInt()
    state.incr(-2)

    val rawdata =
        structdsl("AttributeInfoMessage", raf, state) {
            fld("version", 1)
            fld("flags", 1)
            if (flags and 1 != 0) {
                fld("maxCreationIndex", 2)
            }
            fld("fractalHeapAddress", sizeOffsets)
            fld("attributeNameBtreeAddress", sizeOffsets)
            if (flags and 2 != 0) {
                fld("attributeOrderBtreeAddress", sizeOffsets)
            }
        }
    if (debugMessage) rawdata.show()

    return AttributeInfoMessage(
        rawdata.getLong("fractalHeapAddress"),
    )
}

data class AttributeInfoMessage(val fractalHeapAddress: Long) : HeaderMessage(MessageType.GroupInfo)




