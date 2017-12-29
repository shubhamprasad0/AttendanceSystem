package com.example.shubham.attendancesystemteacherversion

import android.os.Bundle
import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner

import kotlinx.android.synthetic.main.activity_selection.*

class SelectionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selection)
        displaySpinnerChoices(spinner_semester, R.array.semester)
        displaySpinnerChoices(spinner_branches, R.array.branches)
        displaySpinnerChoices(spinner_section, R.array.sections)
        displaySpinnerChoices(spinner_subject, R.array.subjects)
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
