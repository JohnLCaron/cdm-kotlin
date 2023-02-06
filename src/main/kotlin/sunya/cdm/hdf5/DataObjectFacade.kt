package sunya.cdm.hdf5

/**
 * A DataObjectFacade can be:
 * 1) a DataObject with a specific group/name.
 * 2) a SymbolicLink to a DataObject.
 * DataObjects can be pointed to from multiple places.
 * A DataObjectFacade is in a specific group and has a name specific to that group.
 * A DataObject's name is one of its names.
 */
internal class DataObjectFacade {
    val parent: H5Group?
    val name: String
    var displayName: String? = null
    var dataObject: DataObject? = null
    var isGroup = false
    var isVariable = false
    var isTypedef = false
    var is2DCoordinate = false
    var hasNetcdfDimensions = false

    // is a group
    var group: H5Group? = null

    // or a variable
    var dimList: String? = null // list of dimension names for this variable

    // or a link
    var linkName: String? = null

    constructor(parent: H5Group?, name: String, linkName: String?) {
        this.parent = parent
        this.name = name
        this.linkName = linkName
    }

    constructor(parent: H5Group?, name: String, dobj: DataObject) {
        this.parent = parent
        this.name = name
        displayName = if (name.isEmpty()) "root" else name
        this.dataObject = dobj

        // hash for soft link lookup
        //header.addSymlinkMap(getName(), this) // TODO does getName() match whats stored in soft link ??

        // if has a "group message", then its a group
        if (dobj.groupMessage != null || dobj.groupNewMessage != null) { // if has a "groupNewMessage", then its a
            // groupNew
            isGroup = true

            // if it has a Datatype and a StorageLayout, then its a Variable
        } else if (dobj.mdt != null && dobj.mdl != null) {
            isVariable = true

            // if it has only a Datatype, its a Typedef
        } else if (dobj.mdt != null) {
            isTypedef = true
        }
    }

    /* fun getName(): String {
        return if (parent == null) name else parent.name + "/" + name
    }

    override fun toString(): String {
        val sbuff = StringBuilder()
        sbuff.append(getName())
        if (dobj == null) {
            sbuff.append(" dobj is NULL! ")
        } else {
            sbuff.append(" id= ").append(dobj.address)
            sbuff.append(" messages= ")
            for (message in dobj.messages) sbuff.append("\n  ").append(message)
        }
        return sbuff.toString()
    }

     */
}