package com.example.shubham.attendancesystemteacherversion

import android.os.Bundle
import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner

import kotlinx.android.synthetic.main.activity_selection.*
import org.jetbrains.anko.toast

class SelectionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selection)
        displaySpinnerChoices(spinner_semester, R.array.semester)
        displaySpinnerChoices(spinner_branches, R.array.branches)
        displaySpinnerChoices(spinner_section, R.array.sections)
        displaySpinnerChoices(spinner_subject, R.array.subjects)

        spinner_semester.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val semester = parent?.getItemAtPosition(position).toString()
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
                val branch = parent?.getItemAtPosition(position).toString()
                if (branch != "Select Branch") {
                    // do database call
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

    fun takeAttendance(view: View) {
        val intent = Intent(this, AttendanceActivity::class.java)
        startActivity(intent)
    }

}
