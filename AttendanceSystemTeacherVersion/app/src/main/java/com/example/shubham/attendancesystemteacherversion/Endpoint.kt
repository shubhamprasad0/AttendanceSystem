package com.example.shubham.attendancesystemteacherversion


/**
 * Created by shubham on 29/12/17.
 */

/** Represents a device we can talk to.  */
data class Endpoint (val id: String, val name: String) {

    override fun equals(obj: Any?): Boolean {
        if (obj != null && obj is Endpoint) {
            val other = obj as Endpoint?
            return id == other!!.id
        }
        return false
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return String.format("Endpoint{id=%s, name=%s}", id, name)
    }
}