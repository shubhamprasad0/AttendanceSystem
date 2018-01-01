package com.example.shubham.attendancesystemteacherversion

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.nearby.connection.Connections
import java.util.*

class AttendanceActivity : ConnectionsActivity() {

    private lateinit var SERVICE_ID: String

    /** A random UID used as this device's endpoint name.  */
    private lateinit var mName: String




    override fun getName(): String {
        return mName
    }

    override fun getServiceId(): String {
        return SERVICE_ID
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance2)
        mName = generateRandomName()
        SERVICE_ID = getString(R.string.service_id)
    }

    private fun generateRandomName(): String {
        var name = ""
        val random = Random()
        for (i in 0..4) {
            name += random.nextInt(10)
        }
        return name
    }

}
