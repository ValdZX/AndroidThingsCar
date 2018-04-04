package ua.com.valdzx.car.terminal

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.PermissionChecker.PERMISSION_GRANTED
import android.support.v7.app.AppCompatActivity
import android.view.View.GONE
import android.view.View.VISIBLE
import com.marcoscg.easypermissions.EasyPermissions
import kotlinx.android.synthetic.main.activity_main.*
import ua.com.vald_zx.car.core.Constants.DeviceName
import ua.com.valdzx.car.terminal.bluetooth.BluetoothManager
import ua.com.valdzx.car.terminal.utils.EmptyOnSeekBarChangeListener

class TerminalActivity : AppCompatActivity() {

    private val accessCoarseLocationRequest = 321658

    private val bluetoothManager: BluetoothManager by lazy {
        val manager = BluetoothManager(this)
        manager.connectionListener = {
            connectionState.setText(if (it) R.string.connected else R.string.disconnected)
            if (it) loadCurrentState()
            if (!it) findDevice()
        }
        manager.leftEngineRead = { updateLeftState(it) }
        manager.rightEngineRead = { updateRightState(it) }
        manager
    }

    private fun updateLeftState(state: Double) {
        leftState.text = state.toString()
    }

    private fun updateRightState(state: Double) {
        rightState.text = state.toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermission()

        stopButton.setOnClickListener {
            rightSeek.progress = 100
            leftSeek.progress = 100
            bluetoothManager.setEnginesState(0.0, 0.0)
        }
        stopRight.setOnClickListener {
            rightSeek.progress = 100
            bluetoothManager.setRightEngineState(0.0)
        }
        stopLeft.setOnClickListener {
            leftSeek.progress = 100
            bluetoothManager.setLeftEngineState(0.0)
        }

        leftSeek.setOnSeekBarChangeListener(EmptyOnSeekBarChangeListener { bluetoothManager.setLeftEngineState((it - 100).toDouble() / 100.0) })
        rightSeek.setOnSeekBarChangeListener(EmptyOnSeekBarChangeListener { bluetoothManager.setRightEngineState((it - 100).toDouble() / 100.0) })
        joyStick.setOnMoveListener({ a, s ->
            //TODO
        })
    }

    override fun onResume() {
        super.onResume()
        if (!bluetoothManager.isConnected) {
            findDevice()
        }
        bluetoothManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        bluetoothManager.onPause()

    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == bluetoothManager.requestEnableBt && resultCode == Activity.RESULT_CANCELED) {
            bluetoothManager.enableIfNeed()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (!grantResults.contains(PERMISSION_GRANTED)) {
            requestPermission()
        } else {
            findDevice()
        }
    }

    private fun findDevice() {
        scanIndicator.visibility = VISIBLE
        bluetoothManager.findDevice(DeviceName) {
            scanIndicator.visibility = GONE
            bluetoothManager.connect(it)
        }
    }


    private fun requestPermission() {
        EasyPermissions.requestPermissions(this, arrayOf(EasyPermissions.ACCESS_FINE_LOCATION), accessCoarseLocationRequest)
    }

    private fun loadCurrentState() {
        bluetoothManager.loadState()
    }
}
