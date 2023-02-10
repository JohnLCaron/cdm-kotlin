package com.sunya.netchdf.hdf5

import com.sunya.cdm.iosp.OpenFileState
import java.io.IOException
import java.nio.ByteOrder
import java.util.*

// "Data Object Header" Level 2A
@Throws(IOException::class)
fun H5builder.readDataObject(address: Long, name: String) : DataObject {
    println("readDataObject= $name")
    val startPos = this.getFileOffset(address)
    val state = OpenFileState( startPos, ByteOrder.LITTLE_ENDIAN)
    val messages = mutableListOf<MessageHeader>()

    var version = raf.readByte(state)
    if (version.toInt() == 1) { // IV.A.1.a. Version 1 Data Object Header Prefix
        state.pos += 1 // skip byte
        val nmess = raf.readShort(state).toInt()
        val objectReferenceCount: Int = raf.readInt(state)
        // This value specifies the number of bytes of header message data following this length field that contain
        // object header messages for this object header. This value does not include the size of object header
        // continuation blocks for this object elsewhere in the file.
        val objectHeaderSize: Int = raf.readInt(state)
        // Header messages are aligned on 8-byte boundaries for version 1 object headers.
        // LOOK not well documented
        state.pos += 4

        val count = this.readMessagesVersion1(state, nmess, objectHeaderSize, messages)
        /* if (count != nmess) {
            println("  expected $nmess, read $count messages")
        }
        if (state.pos != startPos + objectHeaderSize) {
            println("  set expected pos ${startPos + objectHeaderSize}, actual ${state.pos}")
            state.pos = startPos + objectHeaderSize
        } */
        return DataObject(address, name, messages)
        
    } else { // IV.A.1.b. Version 2 Data Object Header Prefix
        // first byte was already read
        val testForMagic = raf.readByteBuffer(state, 3).array()
        if (!testForMagic.contentEquals("HDR".toByteArray())) {
            throw IllegalStateException("DataObject doesnt start with OHDR")
        }
        version = raf.readByte(state) // better be 2
        val flags = raf.readByte(state).toInt()
        if (((flags shr 5) and 1) == 1) {
            val accessTime: Int = raf.readInt(state)
            val modTime: Int = raf.readInt(state)
            val changeTime: Int = raf.readInt(state)
            val birthTime: Int = raf.readInt(state)
        }
        if (((flags shr 4) and 1) == 1) {
            val maxCompactAttributes: Short = raf.readShort(state)
            val minDenseAttributes: Short = raf.readShort(state)
        }
        val sizeOfChunk: Long = this.readVariableSizeFactor(state,flags and 3)
        val count =  this.readMessagesVersion2(state, sizeOfChunk, ((flags shr 2) and 1) == 1, messages)

        return DataObject(address, name, messages)
    }
}


class DataObject(
    val address : Long, // aka object id : obviously unique
    var name: String?, // may be null, may not be unique
    val messages : List<MessageHeader>
) {
    var groupMessage: SymbolTableMessage? = null
    var groupNewMessage: LinkInfoMessage? = null
    var mdt: DatatypeMessage? = null
    var mds: DataspaceMessage? = null
    var mdl: DataLayoutMessage? = null
    var mfp: FilterPipelineMessage? = null
    val attributes = mutableListOf<AttributeMessage>()
    
    init {
        // look for group or a datatype/dataspace/layout message
        for (mess: MessageHeader in messages) {
            when (mess.mtype) {
                MessageType.SymbolTable -> groupMessage = mess as SymbolTableMessage
                MessageType.LinkInfo -> groupNewMessage = mess as LinkInfoMessage
                MessageType.Dataspace -> mds = mess as DataspaceMessage
                MessageType.Datatype -> mdt = mess as DatatypeMessage
                MessageType.Layout -> mdl = mess as DataLayoutMessage
                MessageType.FilterPipeline -> mfp = mess as FilterPipelineMessage
                MessageType.Attribute -> attributes.add(mess as AttributeMessage)
                MessageType.AttributeInfo -> attributes.addAll((mess as AttributeInfoMessage).attributes)
                else -> { /* noop */ }
            }
        }
    }
}
