package ua.com.valdzx.car.androidthingscar.car

import android.util.Log
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager

class DcEngine(gpioEnable: String, gpioInput1: String, gpioInput2: String) {

//    private val pwmInput1: SoftPwm
//    private val pwmInput2: SoftPwm
    private val pinEnabled: Gpio
    private val pinInput1: Gpio
    private val pinInput2: Gpio

    var state: Double = 0.0
        set(value) {
            field = value
            Log.i(TAG, "DC STATE CHANGED -> $value")
            updateState(value)
        }

    init {
        val manager = PeripheralManager.getInstance()
        pinEnabled = manager.openGpio(gpioEnable)
        pinEnabled.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH)
        pinEnabled.value = false
        pinInput1 = manager.openGpio(gpioInput1)
        pinInput1.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH)
        pinInput1.value = false
        pinInput2 = manager.openGpio(gpioInput2)
        pinInput2.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH)
        pinInput2.value = false

//        pwmInput1 = SoftPwm.openSoftPwm(gpioInput1)
//        pwmInput1.setPwmFrequencyHz(120.0)
//        pwmInput1.setPwmDutyCycle(0.0)
//        pwmInput1.setEnabled(false)
//
//        pwmInput2 = SoftPwm.openSoftPwm(gpioInput2)
//        pwmInput2.setPwmFrequencyHz(120.0)
//        pwmInput2.setPwmDutyCycle(0.0)
//        pwmInput2.setEnabled(false)

    }

    private fun updateState(value: Double) {
        pinEnabled.value = value != 0.0
//        pwmInput1.setEnabled(value != 0.0)
//        pwmInput2.setEnabled(value != 0.0)
        when {
            value == 0.0 -> {
                pinInput1.value = false
                pinInput2.value = false
            }
            value > 0.0 -> {
                pinInput1.value = true
                pinInput2.value = false
//                pwmInput1.setEnabled(true)
//                pwmInput1.setPwmDutyCycle(value * 100)
//                pwmInput2.setEnabled(false)
//                pwmInput2.setPwmDutyCycle(0.0)
            }
            value < 0.0 -> {
                pinInput2.value = true
                pinInput1.value = false
//                pwmInput2.setEnabled(true)
//                pwmInput2.setPwmDutyCycle(Math.abs(value) * 100)
//                pwmInput1.setEnabled(false)
//                pwmInput1.setPwmDutyCycle(0.0)
            }
        }
    }

    fun onDestroy() {
//        pwmInput1.close()
//        pwmInput2.close()
    }

    companion object {
        private val TAG = DcEngine::class.java.simpleName
    }
}