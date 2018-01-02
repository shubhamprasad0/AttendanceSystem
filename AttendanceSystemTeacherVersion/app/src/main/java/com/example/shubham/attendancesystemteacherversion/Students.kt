package com.example.shubham.attendancesystemteacherversion

import com.google.gson.annotations.Expose

/**
 * Created by shubham on 1/1/18.
 */
open class Student(val id: Int, val roll: String, val name: String)

class Course1Student(
        id: Int,
        roll: String,
        name: String,
        val course1_attended: Int,
        val course1_total: Int
): Student(id, roll, name)

class Course2Student(
        id: Int,
        roll: String,
        name: String,
        val course2_attended: Int,
        val course2_total: Int
): Student(id, roll, name)

class Course3Student(
        id: Int,
        roll: String,
        name: String,
        val course3_attended: Int,
        val course3_total: Int
): Student(id, roll, name)

class Course4Student(
        id: Int,
        roll: String,
        name: String,
        val course4_attended: Int,
        val course4_total: Int
): Student(id, roll, name)

class Course5Student(
        id: Int,
        roll: String,
        name: String,
        val course5_attended: Int,
        val course5_total: Int
): Student(id, roll, name)

class Course6Student(
        id: Int,
        roll: String,
        name: String,
        val course6_attended: Int,
        val course6_total: Int
): Student(id, roll, name)

class Course7Student(
        id: Int,
        roll: String,
        name: String,
        val course7_attended: Int,
        val course7_total: Int
): Student(id, roll, name)

class Course8Student(
        id: Int,
        roll: String,
        name: String,
        val course8_attended: Int,
        val course8_total: Int
): Student(id, roll, name)

class Course9Student(
        id: Int,
        roll: String,
        name: String,
        val course9_attended: Int,
        val course9_total: Int
): Student(id, roll, name)

class Course10Student(
        id: Int,
        roll: String,
        name: String,
        val course10_attended: Int,
        val course10_total: Int
): Student(id, roll, name)