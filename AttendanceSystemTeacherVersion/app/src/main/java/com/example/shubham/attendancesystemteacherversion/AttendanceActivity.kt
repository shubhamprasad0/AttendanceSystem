package com.example.shubham.attendancesystemteacherversion

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListAdapter
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.nearby.connection.Connections
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_attendance2.*
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList

class AttendanceActivity: ConnectionsActivity() {

    private lateinit var SERVICE_ID: String
    var responseCode = -1

    /** A random UID used as this device's endpoint name.  */
    private lateinit var mName: String

    // list of items





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
        val semester = Integer.parseInt(intent.getStringExtra("EXTRA_SEMESTER")) as Int
        val branch = intent.getStringExtra("EXTRA_BRANCH")
        val section = intent.getStringExtra("EXTRA_SECTION")
        val courseId = intent.getIntExtra("EXTRA_COURSE_ID", -1)

        doAsync {
            Log.d("MYLOG", "coming in doAsync")
            val studentListJson = fetchStudents(semester, branch, section, courseId)
            uiThread {
                val gson = Gson()
                Log.d("MYLOG", studentListJson)
                when(courseId) {
                    1 -> {
                        try {
                            val studentsJsonArray = gson.fromJson(studentListJson, JsonArray::class.java)
                            val students = gson.fromJson(studentsJsonArray, Array<Course1Student>::class.java)
                            val studentNames = ArrayList<String>()
                            students.mapTo(studentNames) { "${it.roll} ${it.name}" }
                            val adapter = ArrayAdapter(this@AttendanceActivity, android.R.layout.simple_list_item_1, studentNames)
                            student_list.adapter = adapter
                        } catch (e: JsonSyntaxException) {
                            e.printStackTrace()
                        }
                    }
                    2 -> {
                        try {
                            val studentsJsonArray = gson.fromJson(studentListJson, JsonArray::class.java)
                            val students = gson.fromJson(studentsJsonArray, Array<Course2Student>::class.java)
                            val studentNames = ArrayList<String>()
                            students.mapTo(studentNames) { "${it.roll} ${it.name}" }
                            val adapter = ArrayAdapter(this@AttendanceActivity, android.R.layout.simple_list_item_1, studentNames)
                            student_list.adapter = adapter
                        } catch (e: JsonSyntaxException) {
                            e.printStackTrace()
                        }
                    }
                    3 -> {
                        try {
                            val studentsJsonArray = gson.fromJson(studentListJson, JsonArray::class.java)
                            val students = gson.fromJson(studentsJsonArray, Array<Course3Student>::class.java)
                            val studentNames = ArrayList<String>()
                            students.mapTo(studentNames) { "${it.roll} ${it.name}" }
                            val adapter = ArrayAdapter(this@AttendanceActivity, android.R.layout.simple_list_item_1, studentNames)
                            student_list.adapter = adapter
                        } catch (e: JsonSyntaxException) {
                            e.printStackTrace()
                        }
                    }
                    4 -> {
                        try {
                            val studentsJsonArray = gson.fromJson(studentListJson, JsonArray::class.java)
                            val students = gson.fromJson(studentsJsonArray, Array<Course4Student>::class.java)
                            val studentNames = ArrayList<String>()
                            students.mapTo(studentNames) { "${it.roll} ${it.name}" }
                            val adapter = ArrayAdapter(this@AttendanceActivity, android.R.layout.simple_list_item_1, studentNames)
                            student_list.adapter = adapter
                        } catch (e: JsonSyntaxException) {
                            e.printStackTrace()
                        }
                    }
                    5 -> {
                        try {
                            val studentsJsonArray = gson.fromJson(studentListJson, JsonArray::class.java)
                            val students = gson.fromJson(studentsJsonArray, Array<Course5Student>::class.java)
                            val studentNames = ArrayList<String>()
                            students.mapTo(studentNames) { "${it.roll} ${it.name}" }
                            val adapter = ArrayAdapter(this@AttendanceActivity, android.R.layout.simple_list_item_1, studentNames)
                            student_list.adapter = adapter
                        } catch (e: JsonSyntaxException) {
                            e.printStackTrace()
                        }
                    }
                    6 -> {
                        try {
                            val studentsJsonArray = gson.fromJson(studentListJson, JsonArray::class.java)
                            val students = gson.fromJson(studentsJsonArray, Array<Course6Student>::class.java)
                            val studentNames = ArrayList<String>()
                            students.mapTo(studentNames) { "${it.roll} ${it.name}" }
                            val adapter = ArrayAdapter(this@AttendanceActivity, android.R.layout.simple_list_item_1, studentNames)
                            student_list.adapter = adapter
                        } catch (e: JsonSyntaxException) {
                            e.printStackTrace()
                        }
                    }
                    7 -> {
                        try {
                            val studentsJsonArray = gson.fromJson(studentListJson, JsonArray::class.java)
                            val students = gson.fromJson(studentsJsonArray, Array<Course7Student>::class.java)
                            val studentNames = ArrayList<String>()
                            students.mapTo(studentNames) { "${it.roll} ${it.name}" }
                            val adapter = ArrayAdapter(this@AttendanceActivity, android.R.layout.simple_list_item_1, studentNames)
                            student_list.adapter = adapter
                        } catch (e: JsonSyntaxException) {
                            e.printStackTrace()
                        }
                    }
                    8 -> {
                        try {
                            val studentsJsonArray = gson.fromJson(studentListJson, JsonArray::class.java)
                            val students = gson.fromJson(studentsJsonArray, Array<Course8Student>::class.java)
                            val studentNames = ArrayList<String>()
                            students.mapTo(studentNames) { "${it.roll} ${it.name}" }
                            val adapter = ArrayAdapter(this@AttendanceActivity, android.R.layout.simple_list_item_1, studentNames)
                            student_list.adapter = adapter
                        } catch (e: JsonSyntaxException) {
                            e.printStackTrace()
                        }
                    }
                    9 -> {
                        try {
                            val studentsJsonArray = gson.fromJson(studentListJson, JsonArray::class.java)
                            val students = gson.fromJson(studentsJsonArray, Array<Course9Student>::class.java)
                            val studentNames = ArrayList<String>()
                            students.mapTo(studentNames) { "${it.roll} ${it.name}" }
                            val adapter = ArrayAdapter(this@AttendanceActivity, android.R.layout.simple_list_item_1, studentNames)
                            student_list.adapter = adapter
                        } catch (e: JsonSyntaxException) {
                            e.printStackTrace()
                        }
                    }
                    10 -> {
                        try {
                            val studentsJsonArray = gson.fromJson(studentListJson, JsonArray::class.java)
                            val students = gson.fromJson(studentsJsonArray, Array<Course10Student>::class.java)
                            val studentNames = ArrayList<String>()
                            students.mapTo(studentNames) { "${it.roll} ${it.name}" }
                            val adapter = ArrayAdapter(this@AttendanceActivity, android.R.layout.simple_list_item_1, studentNames)
                            student_list.adapter = adapter
                        } catch (e: JsonSyntaxException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
//        val adapter = (this, android.R.layout.simple_list_item_1, items)
//        student_list.adapter = adapter
    }

    private fun generateRandomName(): String {
        var name = ""
        val random = Random()
        for (i in 0..4) {
            name += random.nextInt(10)
        }
        return name
    }

    private fun fetchStudents(semester: Int, branch: String, section: String, courseId: Int): String {
        Log.d("MYLOG", "coming in fetchStudents")
        val serverURL = "http://archdj.pythonanywhere.com/students/"
        var response = ""
        var courseSection = CourseSection(semester, branch, section, "course$courseId")
        val gson = Gson()
        val teacherData: String = gson.toJson(courseSection)

        var httpConnection: HttpURLConnection? = null

        try {
            val targetURL = URL(serverURL)
            httpConnection = targetURL.openConnection() as HttpURLConnection
            httpConnection.setRequestProperty("Content-Type", "application/json")
            httpConnection.requestMethod = "POST"
            httpConnection.connect()

            // Sending request
            val outputStream = httpConnection.outputStream
            outputStream.write(teacherData.toByteArray())
            outputStream.flush()
            responseCode = httpConnection.responseCode

            if (responseCode != 200) {
                return "Failed: HTTP error code: ${httpConnection.responseCode}"
            }

            // Receiving response
            val reader = httpConnection.inputStream.bufferedReader()
            reader.forEachLine {
                response = it
            }
            return response

        } catch (e: MalformedURLException) {
            e.printStackTrace()
            return "MalformedURLException"
        } catch (e: IOException) {
            e.printStackTrace()
            return "IOException"
        } finally {
            httpConnection?.disconnect()
        }
    }

}
