package com.example.shubham.attendancesystemstudentversion

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class MainActivity : AppCompatActivity() {

    private val serverURL: String = "http://archdj.pythonanywhere.com/api-token-auth/"
    private var result: String = ""
    private var responseCode = -1
    private var studentId = ""
    private lateinit var student: Student

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val token = getToken()

        if (token != "NO TOKEN") {
            startConnection()
            finish()
        }
    }

    fun signIn(view: View) {
        // if student_username is not empty
        if (student_username.text.toString() == "") {
            Toast.makeText(this, "Enter your Username", Toast.LENGTH_LONG).show()
        }
        // if password is not empty
        else if (password.text.toString() == "") {
            Toast.makeText(this, "Enter your Password", Toast.LENGTH_LONG).show()
        }
        // else try to make the connection
        else {
            doAsync {
                result = authenticate()
                uiThread {
                    if (responseCode == 200) {
                        studentId = student_username.text.toString()
                        saveToken(result)
                        startConnection()
                        finish()
                    } else {
                        toast("Log In failed, username/password is wrong")
                    }
                }
                val studentDetails = getPersonDetails(studentId)
                Log.d("MYLOG", studentDetails)
                if (responseCode == 200) {
                    val gson = Gson()
                    student = gson.fromJson(studentDetails, Student::class.java)
                    saveDetails(student)
                }
            }
        }
    }

    /**
     * Saves the authorization token for future reference
     */
    private fun saveToken(result: String) {
        val preferences = getSharedPreferences("TOKEN_PREFERENCES_STUDENT", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString("TOKEN_STUDENT", result)
        editor.apply()
    }

    /**
     * Saves the username for future use
     */
    private fun saveDetails(student: Student) {
        val preferences = getSharedPreferences("STUDENT_DETAILS_PREFERENCES", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        val gson = Gson()
        val studentDetails = gson.toJson(student)
        editor.putString("STUDENT", studentDetails)
        editor.apply()
    }

    /**
     * Returns the authorization token
     */
    private fun getToken(): String {
        val preferences = getSharedPreferences("TOKEN_PREFERENCES_STUDENT", Context.MODE_PRIVATE)
        val token = preferences.getString("TOKEN_STUDENT", "NO TOKEN")
        return token
    }

    /**
     * Sends login-id and password to server as json and checks if the user is authenticated or not.
     */
    private fun authenticate(): String {
        var response = ""
        val student = User(student_username.text.toString(), password.text.toString())
        val gson = Gson()
        val studentData: String = gson.toJson(student)

        var httpConnection: HttpURLConnection? = null

        try {
            val targetURL = URL(serverURL)
            httpConnection = targetURL.openConnection() as HttpURLConnection
            httpConnection.setRequestProperty("Content-Type", "application/json")
            httpConnection.requestMethod = "POST"
            httpConnection.connect()

            // Sending request
            val outputStream = httpConnection.outputStream
            outputStream.write(studentData.toByteArray())
            outputStream.flush()
            responseCode = httpConnection.responseCode

            if (responseCode != 200) {
                return "Failed: HTTP error code: $responseCode"
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

    private fun startConnection() {
        val intent = Intent(this, ConnectionActivity::class.java)
        startActivity(intent)
    }

    private fun getPersonDetails(studentId: String): String {
        var response = ""
        val serverURL = "http://archdj.pythonanywhere.com/studentinfo/"
        val studentIdJson = """{"username": "$studentId"}"""
        Log.d("studentlog", studentIdJson)

        var httpConnection: HttpURLConnection? = null

        try {
            val targetURL = URL(serverURL)
            httpConnection = targetURL.openConnection() as HttpURLConnection
            httpConnection.setRequestProperty("Content-Type", "application/json")
            httpConnection.requestMethod = "POST"
            httpConnection.connect()

            // Sending request
            val outputStream = httpConnection.outputStream
            outputStream.write(studentIdJson.toByteArray())
            outputStream.flush()
            responseCode = httpConnection.responseCode

            if (responseCode != 200) {
                return "Failed: HTTP error code: $responseCode"
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
