package com.example.shubham.attendancesystemteacherversion

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_connection.*
import org.jetbrains.anko.toast

class ConnectionActivity : AppCompatActivity() {

    private val REQUEST_ENABLE_BT = 2
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var pairedDevices = setOf<BluetoothDevice>()
    private var pairedDevicesNames = ArrayList<String>()
    private var discoveredDevices = ArrayList<BluetoothDevice>()
    private var discoveredDevicesNames = ArrayList<String>()
    private lateinit var discoveredDevicesAdapter: ArrayAdapter<*>
    private lateinit var pairedDevicesAdatpter: ArrayAdapter<*>

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
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> toast("Discovery Finished")
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> toast("Discovery Started")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

}
