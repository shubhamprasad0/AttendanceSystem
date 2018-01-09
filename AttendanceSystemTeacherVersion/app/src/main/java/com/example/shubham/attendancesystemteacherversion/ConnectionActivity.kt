package com.example.shubham.attendancesystemteacherversion

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_connection.*
import org.jetbrains.anko.toast
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class ConnectionActivity : AppCompatActivity() {

    private val REQUEST_ENABLE_BT = 2
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var pairedDevices = setOf<BluetoothDevice>()
    private var pairedDevicesNames = ArrayList<String>()
    private var discoveredDevices = ArrayList<BluetoothDevice>()
    private var discoveredDevicesNames = ArrayList<String>()
    private lateinit var discoveredDevicesAdapter: ArrayAdapter<*>
    private lateinit var pairedDevicesAdatpter: ArrayAdapter<*>
    private val uuid = UUID.fromString("ad50d117-8c99-497d-af7a-929214e53577")
    private val uiHandler: Handler
    private var attendedStudentIds = ""

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
            checkAndEnableBluetooth()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)
        discoveredDevicesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, discoveredDevicesNames)
        pairedDevicesAdatpter = ArrayAdapter(this, android.R.layout.simple_list_item_1, pairedDevicesNames)
        initializeDefaultBluetoothAdapter()
        val actionFoundFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        val discoveryCompletedFilter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        val discoveryStartedFilter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        registerReceiver(receiver, actionFoundFilter)
        registerReceiver(receiver, discoveryCompletedFilter)
        registerReceiver(receiver, discoveryStartedFilter)
        paired_devices_list.adapter = pairedDevicesAdatpter
        discovered_devices_list.adapter = discoveredDevicesAdapter
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
        preferences = getSharedPreferences("STUDENT_USERNAME_PREFERENCES", Context.MODE_PRIVATE)
        editor = preferences.edit()
        editor.clear()
        editor.apply()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    /**
     * checks and enables Bluetooth
     */
    private fun checkAndEnableBluetooth() {
        if (!bluetoothAdapter?.isEnabled!!) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent, REQUEST_ENABLE_BT)
        } else {
            queryPairedDevices()
            bluetoothAdapter?.startDiscovery()
        }
    }

    /**
     * Queries the paired devices
     */
    private fun queryPairedDevices() {
        pairedDevices = bluetoothAdapter?.bondedDevices!!
        if (!pairedDevices.isEmpty()) {
            for (device in pairedDevices) {
                pairedDevicesNames.add(device.name)
                Log.d("Bluetooth", "${device.name} ${device.address}")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
            toast("Bluetooth turned on")
            queryPairedDevices()
            pairedDevicesAdatpter.notifyDataSetChanged()
            bluetoothAdapter?.startDiscovery()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.options_menu, menu)
        return true
    }

    private val receiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    discoveredDevices.add(device)
                    val deviceName = device.name
                    val deviceHardwareAddress = device.address
                    Log.d("Bluetooth discovery", "$deviceName $deviceHardwareAddress")
                    discoveredDevicesNames.add(deviceName)
                    discoveredDevicesAdapter.notifyDataSetChanged()
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    toast("Discovery Finished")
                    connectToDiscoveredDevices()
                }

                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> toast("Discovery Started")
            }
        }
    }

    private fun connectToDiscoveredDevices() {
        for (device in discoveredDevices) {
            val connectThread = ConnectThread(device)
            connectThread.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    private inner class ConnectThread(device:BluetoothDevice): Thread() {
        private lateinit var mSocket: BluetoothSocket
        private var mDevice = device

        init {
            try {
                mSocket = mDevice.createRfcommSocketToServiceRecord(uuid)
            } catch (e: Exception) {
                Log.d("BT_CLIENT_CONN", e.message)
            }
        }

        override fun run() {
            bluetoothAdapter?.cancelDiscovery()
            try {
                mSocket.connect()
                Log.d("teacher log", "connected to ${mDevice.name}")
            } catch (e: Exception) {
                try {
                    mSocket.close()
                } catch (e: Exception) {
                    Log.e("BT_CLIENT_CONN", e.message)
                }
                return
            }

            // The connection attempt succeeded
            manageMyConnectedSocket(mSocket)
        }

        fun cancel() {
            try {
                mSocket.close()
            } catch (e: Exception) {
                Log.e("BT_CLIENT_CONN", e.message)
            }
        }
    }

    private fun manageMyConnectedSocket(socket: BluetoothSocket) {
        Log.d("teacher log", "in manage connection")
        val connectedThread = ConnectedThread(socket)
        connectedThread.start()
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
                Log.e("BT_CLIENT_CONN", "Error occurred when creating input stream ${e.message}")
            }
            try {
                outputStream = mSocket.outputStream
            } catch (e: Exception) {
                Log.e("BT_CLIENT_CONN", "Error occurred when creating output stream ${e.message}")
            }
        }

        override fun run() {
            buffer = ByteArray(1024)

            // bytes returned from read()
            var numBytes: Int

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    Log.d("teacher log", "trying to read data")
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
                    Log.d("BT_CLIENT_CONN", "InputStream was disconnected ${e.message}")
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
                Log.e("BT_CLIENT_CONN", "Error occurred when sending data ${e.message}")

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
                Log.e("BT_CLIENT_CONN", "Could not close the connect socket ${e.message}")
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
}