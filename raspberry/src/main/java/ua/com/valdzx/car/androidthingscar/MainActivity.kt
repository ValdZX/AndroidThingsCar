package ua.com.valdzx.car.androidthingscar

import android.app.Activity
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import ua.com.valdzx.car.androidthingscar.bluetooth.BluetoothManagement
import java.util.*

class MainActivity : Activity() {
    private lateinit var bm : BluetoothManagement

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bm = BluetoothManagement(this)
    }

    override fun onStart() {
        super.onStart()
        bm.onStart()
    }

    override fun onStop() {
        super.onStop()
        bm.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        bm.onDestroy()
    }

    private fun updateLocalUi(timestamp: Long) {
        val date = Date(timestamp)
        val displayDate = (DateFormat.getMediumDateFormat(this).format(date)
                + "\n"
                + DateFormat.getTimeFormat(this).format(date))
        Log.i(TAG, "DisplayDate $displayDate")
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}