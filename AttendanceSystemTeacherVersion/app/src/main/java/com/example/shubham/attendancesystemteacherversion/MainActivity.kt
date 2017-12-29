package com.example.shubham.attendancesystemteacherversion

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class MainActivity : AppCompatActivity() {

    val serverURL = "localhost:8000"
    var response = StringBuilder("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun signIn(view: View) {
        // if teacher_id is not empty
        if (teacher_id.text.toString() == "") {
            Toast.makeText(this, "Enter your ID", Toast.LENGTH_LONG).show()
        }
        // if password is not empty
        else if (password.text.toString() == "") {
            Toast.makeText(this, "Enter your password", Toast.LENGTH_LONG).show()
        }
        // else try to make the connection
        else {
            val data = authenticate()
            Log.d("MYLOG", data)
        }
    }

    /**
     * Sends login-id and password to server as json and checks if the user is authenticated or not.
     */
    private fun authenticate(): String {
        val teacher = Teacher(teacher_id.text.toString(), password.text.toString())
        val gson = Gson()
        val teacherData: String = gson.toJson(teacher)

        var httpConnection: HttpURLConnection? = null

        try {
            val targetURL = URL(serverURL)
            httpConnection = targetURL.openConnection() as HttpURLConnection
            httpConnection.setRequestProperty("Content-Type", "application/json")
            httpConnection.requestMethod = "GET"
            httpConnection.connect()

            // Sending request
            val outputStream = httpConnection.outputStream
            outputStream.write(teacherData.toByteArray())
            outputStream.flush()

            if (httpConnection.responseCode != 200) {
                return "Failed: HTTP error code: ${httpConnection.responseCode}"
            }

            // Receiving response
            val inputStream = httpConnection.inputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            reader.useLines {
                it.map { line ->
                    response.append(line)
                    response.append('\r')
                }
            }
            return response.toString()
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

    fun selectClassAndSubject(view: View) {
        val intent = Intent(this, SelectionActivity::class.java)
        startActivity(intent)
    }
}