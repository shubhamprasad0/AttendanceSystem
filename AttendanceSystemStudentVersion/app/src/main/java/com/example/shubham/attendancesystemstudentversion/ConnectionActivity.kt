package com.example.shubham.attendancesystemstudentversion

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import org.jetbrains.anko.toast

class ConnectionActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private val REQUEST_BT_DISCOVERABILITY = 1
    private val DURATION = 600


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
        preferences = getSharedPreferences("STUDENT_USERNAME_PREFERENCES", Context.MODE_PRIVATE)
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
        if (requestCode == REQUEST_BT_DISCOVERABILITY && resultCode == Activity.RESULT_OK) {
            toast("Device discoverable for $DURATION seconds")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)
        initializeDefaultBluetoothAdapter()
    }
}
