package com.example.shubham.attendancesystemstudentversion

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.google.gson.Gson
import org.jetbrains.anko.toast
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.collections.ArrayList

class ConnectionActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private val REQUEST_BT_DISCOVERABILITY = 3
    private val DURATION = 600
    private val uuid = UUID.fromString("ad50d117-8c99-497d-af7a-929214e53577")
    private lateinit var student: Student
    private val uiHandler: Handler

    init {
        uiHandler = object: Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when(msg.what) {
                    MessageConstants.MESSAGE_READ -> {
                        toast(msg.obj.toString())
                        Log.d("Handle Message", msg.obj.toString())
                    }
                }
            }
        }
    }


    /**
     * initialize the default bluetooth adapter
     */
    private fun initializeDefaultBluetoothAdapter() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            toast("The device does not support Bluetooth")
            finish()
        } else {
            enableDiscoverability()
        }
    }

    /**
     * onClick handler of the menu item `Log Out`
     * logs the user out
     */
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

    /**
     * enable discoverability
     */
    private fun enableDiscoverability() {
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DURATION)
        startActivityForResult(discoverableIntent, REQUEST_BT_DISCOVERABILITY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_BT_DISCOVERABILITY && resultCode == DURATION) {
            toast("Device discoverable for $DURATION seconds")
            acceptConnection()
            val intent = Intent(this, AttendanceActivity::class.java)
            startActivity(intent)
        } else {
            toast("Device not discoverable")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)
        initStudent()
        initializeDefaultBluetoothAdapter()
    }

    private fun acceptConnection() {
        val acceptThread = AcceptThread()
        acceptThread.start()
    }

    private inner class AcceptThread: Thread() {
        private var serverSocket: BluetoothServerSocket? = null
        init {
            try {
                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("Attendance App", uuid)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun run() {
            Log.d("mylog", "coming in run of accept thread")
            var socket: BluetoothSocket? = null

            while (true) {
                try {
                    Log.d("mylog", "server ready")
                    socket = serverSocket?.accept()
                } catch (e: Exception) {
                    e.printStackTrace()
                    break
                }

                if (socket != null) {
//                    manageMyConnectedSocket(socket)
                }
            }
        }

        /**
         * To close the serverSocket from another thread
         */
        fun cancel() {
            try {
                serverSocket?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun manageMyConnectedSocket(socket: BluetoothSocket) {
        val t = ConnectedThread(socket)
        initStudent()
        t.write(student.id.toString().toByteArray())
    }

    private inner class ConnectedThread(socket:BluetoothSocket): Thread() {
        private var mSocket: BluetoothSocket = socket
        private lateinit var inputStream: InputStream
        private lateinit var outputStream: OutputStream
        private lateinit var buffer: ByteArray

        init {

            try {
                inputStream = mSocket.inputStream
            } catch (e: Exception) {
                Log.e("BT_SERVER_CONN", "Error occurred when creating input stream ${e.message}")
            }
            try {
                outputStream = mSocket.outputStream
            } catch (e: Exception) {
                Log.e("BT_SERVER_CONN", "Error occurred when creating output stream ${e.message}")
            }
        }

        override fun run() {
            buffer = ByteArray(1024)

            // bytes returned from read()
            var numBytes: Int

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    numBytes = inputStream.read(buffer)

                    // Send the obtained bytes to the UI activity
                    val readMsg = uiHandler.obtainMessage(
                            MessageConstants.MESSAGE_READ,
                            numBytes,
                            -1,
                            buffer)
                    readMsg.sendToTarget()
                } catch (e: Exception) {
                    Log.d("BT_SERVER_CONN", "InputStream was disconnected ${e.message}")
                    break
                }
            }
        }

        /**
         * Call this from the main activity to send data to the remote device.
         */
        fun write(bytes: ByteArray) {
            try {
                outputStream.write(bytes)

                val writtenMsg = uiHandler.obtainMessage(
                        MessageConstants.MESSAGE_WRITE,
                        -1,
                        -1,
                        buffer)
                writtenMsg.sendToTarget()
            } catch (e: Exception) {
                Log.e("BT_SERVER_CONN", "Error occurred when sending data ${e.message}")

                // Send a failure message back to the activity
                val writeErrorMsg = uiHandler.obtainMessage(MessageConstants.MESSAGE_TOAST)
                val bundle = Bundle()
                bundle.putString("toast", "Couldn't send data to the other device")
                writeErrorMsg.data = bundle
                uiHandler.sendMessage(writeErrorMsg)
            }
        }

        fun cancel() {
            try {
                mSocket.close()
            } catch (e: Exception) {
                Log.e("BT_SERVER_CONN", "Could not close the connect socket ${e.message}")
            }
        }
    }

    enum class MessageConstants {
        ;
        companion object {
            val MESSAGE_READ = 0
            val MESSAGE_WRITE = 1
            val MESSAGE_TOAST = 2
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
}
