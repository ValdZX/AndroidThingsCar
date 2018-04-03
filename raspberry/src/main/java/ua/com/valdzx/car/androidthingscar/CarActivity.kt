package ua.com.valdzx.car.androidthingscar

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import com.google.android.things.pio.Pwm
import ua.com.valdzx.car.androidthingscar.bluetooth.BluetoothManagement
import java.io.IOException


class CarActivity : Activity() {
    private lateinit var bm: BluetoothManagement
    lateinit var ledPin: Gpio
    lateinit var ledPwm: Pwm
    var pin: Boolean = false
        set(value) {
            field = value
            ledPin.value = value
            Log.i(TAG, "PIN STATE CHANGED -> $value")
        }
    var pwm: Int = 0
        set(value) {
            field = value
            ledPwm.setPwmDutyCycle(value.toDouble())
            Log.i(TAG, "PWM STATE CHANGED -> $value")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bm = BluetoothManagement(this)
        bm.pinProvider = { pin }
        bm.pinChange = { pin = it }
        bm.pwmProvider = { pwm }
        bm.pwmChange = { pwm = it }

        try {
            val manager = PeripheralManager.getInstance()
            ledPin = manager.openGpio("BCM26")
            ledPin.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH)
            ledPin.value = false

            ledPwm = manager.openPwm("PWM1")
            ledPwm.setPwmFrequencyHz(120.0)
            ledPwm.setPwmDutyCycle(0.0)
            ledPwm.setEnabled(true)
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