package com.example.shubham.attendancesystemteacherversion

import android.os.Bundle
import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*

import kotlinx.android.synthetic.main.activity_selection.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL

class SelectionActivity : Activity() {

    private var semester: String = ""
    private var branch: String = ""
    private var section: String = ""
    private var sections = arrayListOf<String>()
    private var course: String = ""
    private var responseCode = -1
    private var sectionsAndCourses = ""
    private var numCourses = -1
    private var courses = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selection)
        displaySpinnerChoices(spinner_semester, R.array.semester)
        displaySpinnerChoices(spinner_branches, R.array.branches)

        spinner_semester.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                semester = parent?.getItemAtPosition(position).toString()
                if (semester != "Select Semester") {

                    // enable the next spinner
                    spinner_branches.isClickable = true
                } else {
                    // disable all spinners after this one
                    spinner_branches.isClickable = false
                    spinner_section.isClickable = false
                    spinner_subject.isClickable = false
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // do nothing
            }
        }

        spinner_branches.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                branch = parent?.getItemAtPosition(position).toString()
                if (branch != "Select Branch") {

                    doAsync {
                        sectionsAndCourses = getSectionsAndCourses(semester, branch)
                        uiThread {
                            Log.d("MYLOG", sectionsAndCourses)
                            val gson = Gson()
                            val sectionsAndCoursesObject = gson.fromJson(sectionsAndCourses, SectionsAndCourses::class.java)
                            Log.d("MYLOG", sectionsAndCoursesObject.toString())
                            extractSectionsAndCourses(sectionsAndCoursesObject)
                            displaySpinnerChoices(spinner_section, sections)
                            displaySpinnerChoices(spinner_subject, courses)
                            spinner_section.isClickable = true
                        }
                    }
                    // populate the next spinners
                    // enable the next spinner
                } else {
                    spinner_section.isClickable = false
                    spinner_subject.isClickable = false
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // do nothing
            }
        }

        spinner_section.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val section = parent?.getItemAtPosition(position).toString()
                if (section != "Select Section") {
                    spinner_subject.isClickable = true
                } else {
                    spinner_subject.isClickable = false
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // do nothing
            }
        }
    }

    private fun displaySpinnerChoices(spinner: Spinner, array: Int) {
        val adapter = ArrayAdapter.createFromResource(this, array, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun displaySpinnerChoices(spinner: Spinner, array: ArrayList<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, array)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    fun takeAttendance(view: View) {
        val intent = Intent(this, AttendanceActivity::class.java)
        startActivity(intent)
    }

    private fun getSectionsAndCourses(semester: String, branch: String): String {
        val serverURL = "http://archdj.pythonanywhere.com/deptinfo/"
        var response = StringBuilder("")
        val semesterAndBranch = SemesterAndBranch(semester, branch)
        val gson = Gson()
        val semesterAndBranchData: String = gson.toJson(semesterAndBranch)
        Log.d("MYLOG", semesterAndBranchData)

        var httpConnection: HttpURLConnection? = null

        try {
            val targetURL = URL(serverURL)
            httpConnection = targetURL.openConnection() as HttpURLConnection
            httpConnection.setRequestProperty("Content-Type", "application/json")
            httpConnection.requestMethod = "POST"
            httpConnection.connect()

            // Sending request
            val outputStream = httpConnection.outputStream
            outputStream.write(semesterAndBranchData.toByteArray())
            outputStream.flush()
            responseCode = httpConnection.responseCode

            if (responseCode != 200) {
                return "Failed: HTTP error code: ${httpConnection.responseCode}"
            }

            // Receiving response
            val reader = httpConnection.inputStream.bufferedReader()
            reader.forEachLine {
                response.append(it)
                response.append("\r\n")
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

    private fun extractSectionsAndCourses(sectionsAndCourses: SectionsAndCourses) {
        var sectionChar = 'A'
        sections.clear()
        sections.add("Select Section")
        for (i in 1 .. sectionsAndCourses.sections_count) {
            sections.add(sectionChar.toString())
            sectionChar++
        }
        courses = sectionsAndCourses.courses.split(",") as ArrayList<String>
        courses.add(0, "Select Course")
    }

}
