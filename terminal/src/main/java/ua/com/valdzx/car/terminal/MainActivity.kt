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

class MainActivity : AppCompatActivity() {

    private val accessCoarseLocationRequest = 321658

    private val bluetoothManager: BluetoothManager by lazy {
        val manager = BluetoothManager(this)
        manager.connectionListener = {
            connectionState.setText(if (it) R.string.connected else R.string.disconnected)
            if (it) loadCurrentState()
        }
        manager.pinRead = { updatePinState(it) }
        manager
    }

    private fun updatePinState(currentPinState: Boolean) {
        pinState.isChecked = currentPinState
        changePin.setText(if (currentPinState) R.string.turn_off else R.string.turn_on)
        changePin.setOnClickListener {
            bluetoothManager.setPinState(!currentPinState)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermission()
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
