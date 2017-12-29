package com.example.shubham.attendancesystemteacherversion

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import android.support.v7.app.AppCompatActivity
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread


class MainActivity : AppCompatActivity() {

    private val serverURL: String = "http://archdj.pythonanywhere.com/api-token-auth/"
    private var result: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val token = getToken()

        if (token != "NO TOKEN") {
            selectClassAndSubject()
            finish()
        }
    }

    fun signIn(view: View) {
        // if teacher_id is not empty
        if (teacher_id.text.toString() == "") {
            Toast.makeText(this, "Enter your ID", Toast.LENGTH_LONG).show()
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
                    saveToken(result)
                    selectClassAndSubject()
                    finish()
                }
            }
        }
    }

    /**
     * Saves the authorization token for future reference
     */
    private fun saveToken(result: String) {
        val preferences = getSharedPreferences("TOKEN_PREFERENCES", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString("TOKEN", result)
        editor.apply()
    }

    /**
     * Returns the authorization token
     */
    private fun getToken(): String {
        val preferences = getSharedPreferences("TOKEN_PREFERENCES", Context.MODE_PRIVATE)
        val token = preferences.getString("TOKEN", "NO TOKEN")
        return token
    }


    /**
     * Sends login-id and password to server as json and checks if the user is authenticated or not.
     */
    private fun authenticate(): String {
        var response = ""
        val teacher = Teacher(teacher_id.text.toString(), password.text.toString())
        val gson = Gson()
        val teacherData: String = gson.toJson(teacher)

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

            if (httpConnection.responseCode != 200) {
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

    private fun selectClassAndSubject() {
        val intent = Intent(this, SelectionActivity::class.java)
        startActivity(intent)
    }
}