package com.example.shubham.attendancesystemstudentversion

import android.app.Activity
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.*
import android.support.v7.app.AppCompatActivity
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.google.gson.Gson
import com.microsoft.projectoxford.face.FaceServiceClient
import com.microsoft.projectoxford.face.FaceServiceRestClient
import com.microsoft.projectoxford.face.contract.Face
import com.microsoft.projectoxford.face.contract.FaceRectangle
import com.microsoft.projectoxford.face.contract.VerifyResult
import kotlinx.android.synthetic.main.activity_attendance.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import kotlin.math.max
import kotlin.math.min

class AttendanceActivity : AppCompatActivity() {
    private val REQUEST_IMAGE_CAPTURE = 1
    private lateinit var faceServiceClient: FaceServiceClient
    private lateinit var progressDialog: ProgressDialog
    private lateinit var imageBitmap: Bitmap
    private val FACE_RECT_SCALE_RATIO = 1.3
    private var studentId = ""
    private lateinit var student: Student
    private val uiHandler: Handler
    var responseCode = -1

    init {
        uiHandler = object: Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    ConnectionActivity.MessageConstants.MESSAGE_WRITE -> {
                        Log.d("Handle Student Msgs", msg.obj.toString())
                    }
                }
            }
        }
    }

    fun logOut(menuItem: MenuItem) {
        var preferences = getSharedPreferences("TOKEN_PREFERENCES_STUDENT", Context.MODE_PRIVATE)
        var editor = preferences.edit()
        editor.clear()
        editor.apply()
        preferences = getSharedPreferences("STUDENT_DETAILS_PREFERENCES", Context.MODE_PRIVATE)
        editor = preferences.edit()
        editor.clear()
        editor.apply()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)
//        studentId = intent.getStringExtra("STUDENT_ID")
        initStudent()
//        Log.d("Mylog", studentId)
        button.setOnClickListener {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }

            progressDialog = ProgressDialog(this)
            progressDialog.setTitle("Please wait")

            faceServiceClient = FaceServiceRestClient(getString(R.string.endpoint), getString(R.string.subscription_key))
        }
    }

    private fun initStudent() {
        val preferences = getSharedPreferences("STUDENT_DETAILS_PREFERENCES", Context.MODE_PRIVATE)
        val json = preferences.getString("STUDENT", "no value")
        if (json != "no value") {
            val gson = Gson()
            student = gson.fromJson(json, Student::class.java)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val extras: Bundle? = data?.extras
            imageBitmap = extras?.get("data") as Bitmap
            imageView.setImageBitmap(imageBitmap)
            val output = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
            val inputStream = ByteArrayInputStream(output.toByteArray())
            val detectionTask = DetectionTask()
            detectionTask.execute(inputStream)
        }
    }

    private inner class DetectionTask: AsyncTask<InputStream, String, Array<Face>?>() {

        private var isSuccess = true

        override fun doInBackground(vararg params: InputStream?): Array<Face>? {
            try {
                publishProgress("Detecting...")

                return faceServiceClient.detect(
                        params[0],
                        true,
                        false,
                        null
                )
            } catch (e: Exception) {
                isSuccess = false
                publishProgress(e.message)
                return null
            }
        }

        override fun onPreExecute() {
            progressDialog.show()
        }

        override fun onProgressUpdate(vararg progress: String?) {
            progressDialog.setMessage(progress[0])
        }

        override fun onPostExecute(faces: Array<Face>?) {
            if (faces != null) {
                setUiAfterDetection(faces, isSuccess)
                verifyStudent(faces[0], studentId)
            }
        }
    }

    private inner class VerificationTask(private var faceId: UUID, private var studentId: String) : AsyncTask<Void, String, VerifyResult?>() {

        override fun doInBackground(vararg params: Void?): VerifyResult? {
            try {
                publishProgress("Verifying...")

                return faceServiceClient.verify(
                        faceId,
                        student.person_group_id,
                        student.person_id)
            } catch (e: Exception) {
                publishProgress(e.message)
                return null
            }
        }

        override fun onPreExecute() {
            progressDialog.show()
        }

        override fun onProgressUpdate(vararg progress: String?) {
            progressDialog.setMessage(progress[0])
        }

        override fun onPostExecute(result: VerifyResult?) {
            if (result != null) {
                setUiAfterVerification(result)
            }
        }
    }

    private fun setUiAfterVerification(result: VerifyResult) {
        progressDialog.dismiss()
        if (result.isIdentical) {
            doAsync {
                val courseAttendanceJson = getCourseAttendance()
                Log.d("mylog", courseAttendanceJson)
                val gson = Gson()
                val courseAttendance = gson.fromJson(courseAttendanceJson, CourseAttendance::class.java)
                val response = updateAttendance(courseAttendance)
                Log.d("mylog", response)
                toast("Face matched, attendance successful")

            }

        } else {
            toast("Face does not match with database, attendance failed!!")
        }

    }

    private fun verifyStudent(face: Face, studentId: String) {
        val verificationTask = VerificationTask(face.faceId, studentId)
        verificationTask.execute()
    }



    private fun setUiAfterDetection(result: Array<Face>?, isSuccess: Boolean) {
        progressDialog.dismiss()

        if (isSuccess) {
            if (result != null) {
                imageView.setImageBitmap(drawFaceRectanglesOnBitmap(imageBitmap, result))
            }
        }
    }

    private fun drawFaceRectanglesOnBitmap(imageBitmap: Bitmap, faces: Array<Face>?): Bitmap {
        val bitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.isAntiAlias = true
        paint.style = Paint.Style.STROKE
        paint.color = Color.GREEN
        var strokeWidth = Math.max(imageBitmap.width, imageBitmap.height) / 100
        if (strokeWidth == 0) {
            strokeWidth = 1
        }
        paint.strokeWidth = strokeWidth.toFloat()
        faces?.map { calculateFaceRectangle(bitmap, it.faceRectangle, FACE_RECT_SCALE_RATIO) }?.forEach {
            canvas.drawRect(
                    it.left.toFloat(),
                    it.top.toFloat(),
                    it.left.toFloat() + it.width,
                    it.top.toFloat() + it.height,
                    paint
            )
        }
        return bitmap
    }

    private fun calculateFaceRectangle(bitmap: Bitmap, faceRectangle: FaceRectangle, faceRectangleEnlargeRatio: Double): FaceRectangle {
        var sideLength = faceRectangle.width * faceRectangleEnlargeRatio
        sideLength = min(sideLength, bitmap.width.toDouble())
        sideLength = min(sideLength, bitmap.height.toDouble())

        var left = faceRectangle.left - faceRectangle.width * (faceRectangleEnlargeRatio - 1.0) * 0.5
        left = max(left, 0.0)
        left = min(left, bitmap.width - sideLength)

        var top = faceRectangle.top - faceRectangle.height * (faceRectangleEnlargeRatio - 1.0) * 0.5
        top = max(top, 0.0)
        top = min(top, bitmap.height - sideLength)

        var shiftTop = faceRectangleEnlargeRatio - 1.0
        shiftTop = max(shiftTop, 0.0)
        shiftTop = min(shiftTop, 1.0)
        top -= 0.15 * shiftTop * faceRectangle.height
        top = max(top, 0.0)

        val result = FaceRectangle()
        result.left = left.toInt()
        result.top = top.toInt()
        result.width = sideLength.toInt()
        result.height = sideLength.toInt()

        return result
    }

    private fun getCourseAttendance(): String {
        var response = ""
        val serverURL = "http://archdj.pythonanywhere.com/getattendance/"
        val studentIdJson = """{"name": "${student.name}"}"""
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

    fun updateAttendance(courseAttendance: CourseAttendance): String {
        responseCode = -1
        var response = ""
        val serverURL = "http://archdj.pythonanywhere.com/students/${student.id}/"
        val courseAttendanceNew = CourseAttendance(courseAttendance.course1_attended + 1, courseAttendance.course1_total + 1)
        val gson = Gson()
        val courseAttendanceNewString = gson.toJson(courseAttendanceNew)
        Log.d("studentlog", courseAttendanceNewString)

        var httpConnection: HttpURLConnection? = null

        try {
            val targetURL = URL(serverURL)
            httpConnection = targetURL.openConnection() as HttpURLConnection
            httpConnection.setRequestProperty("Content-Type", "application/json")
            httpConnection.requestMethod = "PUT"
            httpConnection.connect()

            // Sending request
            val outputStream = httpConnection.outputStream
            outputStream.write(courseAttendanceNewString.toByteArray())
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
