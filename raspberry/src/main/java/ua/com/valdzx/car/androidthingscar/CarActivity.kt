package ua.com.valdzx.car.androidthingscar

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import ua.com.valdzx.car.androidthingscar.bluetooth.BluetoothManagement
import java.io.IOException


class CarActivity : Activity() {
    private lateinit var bm: BluetoothManagement
    lateinit var mGpio: Gpio
    var pin: Boolean = false
        set(value) {
            field = value
            mGpio.value = value
            Log.i(TAG, "PIN STATE CHANGED -> $value")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bm = BluetoothManagement(this)
        bm.statePinProvider = { pin }
        bm.statePinChange = { pin = it }

        try {
            val manager = PeripheralManager.getInstance()
            mGpio = manager.openGpio("BCM4")
            mGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
            mGpio.value = false
        } catch (e: IOException) {
            Log.w(TAG, "Unable to access GPIO", e)
        }

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
        private val TAG = CarActivity::class.java.simpleName
    }
}