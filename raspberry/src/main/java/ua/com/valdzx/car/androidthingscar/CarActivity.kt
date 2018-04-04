package ua.com.valdzx.car.androidthingscar

import android.app.Activity
import android.os.Bundle
import android.util.Log
import ua.com.valdzx.car.androidthingscar.bluetooth.BluetoothManagement
import ua.com.valdzx.car.androidthingscar.car.DcEngine
import java.io.IOException


class CarActivity : Activity() {
    private lateinit var bm: BluetoothManagement
    lateinit var leftEngine: DcEngine
    lateinit var rightEngine: DcEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bm = BluetoothManagement(this)
        bm.leftEngineProvider = { leftEngine.state }
        bm.leftEngineChange = { leftEngine.state = it }
        bm.rightEngineProvider = { rightEngine.state }
        bm.rightEngineChange = { rightEngine.state = it }

        try {
            leftEngine = DcEngine("BCM25", "BCM16", "BCM12")
            rightEngine = DcEngine("BCM5", "BCM26", "BCM6")
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
        leftEngine.onDestroy()
        rightEngine.onDestroy()
    }

    companion object {
        private val TAG = CarActivity::class.java.simpleName
    }
}