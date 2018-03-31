package ua.com.valdzx.car.androidthingscar

import android.app.Activity
import android.os.Bundle
import android.util.Log
import ua.com.valdzx.car.androidthingscar.bluetooth.BluetoothManagement

class MainActivity : Activity() {
    private lateinit var bm: BluetoothManagement
    var pin: Boolean = false
    set(value) {
        field = value
        Log.i(TAG, "PIN STATE CHANGED -> $value")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bm = BluetoothManagement(this)
        bm.statePinProvider = { pin }
        bm.statePinChange = { pin = it }
    }

    override fun onStart() {
        super.onStart()
        bm.onStart()
    }

    override fun onDestroy() {
        super.onDestroy()
        bm.onDestroy()
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }
}