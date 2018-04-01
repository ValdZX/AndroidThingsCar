package ua.com.valdzx.car.androidthingscar.bluetooth

import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.ParcelUuid
import android.util.Log
import ua.com.vald_zx.car.core.Constants
import java.util.*

class BluetoothManagement(
        private val activity: Activity,
        public var statePinProvider: () -> Boolean = { false },
        public var statePinChange: (Boolean) -> Unit = {}) {
    private val TAG = BluetoothManagement::class.java.simpleName

    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothGattServer: BluetoothGattServer? = null
    private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private val mRegisteredDevices = HashSet<BluetoothDevice>()

    private val mBluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)

            when (state) {
                BluetoothAdapter.STATE_ON -> {
                    startAdvertising()
                    startServer()
                }
                BluetoothAdapter.STATE_OFF -> {
                    stopServer()
                    stopAdvertising()
                }
            }
        }
    }

    private val mAdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.i(TAG, "LE Advertise Started.")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.w(TAG, "LE Advertise Failed: $errorCode")
        }
    }

    private val mGattServerCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "BluetoothDevice CONNECTED: $device")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "BluetoothDevice DISCONNECTED: $device")
                mRegisteredDevices.remove(device)
                stopAdvertising()
                startAdvertising()
            }
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic) {
            when (characteristic.uuid) {
                Constants.PIN_STATE -> {
                    mBluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, ByteArray(1, { (if (statePinProvider.invoke()) 1 else 0).toByte() }))
                }
                else -> {
                    Log.w(TAG, "Invalid Characteristic Read: " + characteristic.uuid)
                    mBluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                }
            }
        }

        override fun onCharacteristicWriteRequest(device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray) {
            when (characteristic.uuid) {
                Constants.PIN_STATE -> {
                    statePinChange.invoke(value[0] != 0.toByte())
                    if (responseNeeded) mBluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                }
                else -> {
                    Log.w(TAG, "Invalid Characteristic Write: " + characteristic.uuid)
                    if (responseNeeded) mBluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                }
            }
        }
    }

    init {
        mBluetoothManager = activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = mBluetoothManager!!.adapter
        bluetoothAdapter.name = Constants.DeviceName
        activity.registerReceiver(mBluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        if (!bluetoothAdapter.isEnabled) {
            Log.d(TAG, "Bluetooth is currently disabled...enabling")
            bluetoothAdapter.enable()
        } else {
            Log.d(TAG, "Bluetooth enabled...starting services")
            startAdvertising()
            startServer()
        }
    }

    private fun notifyRegisteredDevices(timestamp: Long, adjustReason: Byte) {
        if (mRegisteredDevices.isEmpty()) {
            Log.i(TAG, "No subscribers registered")
            return
        }
        val exactTime = CarProfile.getExactTime(timestamp, adjustReason)

        Log.i(TAG, "Sending update to " + mRegisteredDevices.size + " subscribers")
        for (device in mRegisteredDevices) {
            val timeCharacteristic = mBluetoothGattServer?.getService(CarProfile.TIME_SERVICE)?.getCharacteristic(CarProfile.CURRENT_TIME)
            timeCharacteristic?.value = exactTime
            mBluetoothGattServer?.notifyCharacteristicChanged(device, timeCharacteristic, false)
        }
    }

    private fun startAdvertising() {
        val bluetoothAdapter = mBluetoothManager!!.adapter

        mBluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
        if (mBluetoothLeAdvertiser == null) {
            Log.w(TAG, "Failed to create advertiser")
            return
        }

        val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build()

        val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(ParcelUuid(Constants.PIN_SERVICE))
                .build()

        mBluetoothLeAdvertiser?.startAdvertising(settings, data, mAdvertiseCallback)
    }

    private fun stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return

        mBluetoothLeAdvertiser!!.stopAdvertising(mAdvertiseCallback)
    }

    private fun startServer() {
        mBluetoothGattServer = mBluetoothManager!!.openGattServer(activity, mGattServerCallback)
        if (mBluetoothGattServer == null) {
            Log.w(TAG, "Unable to create GATT server")
            return
        }
        mBluetoothGattServer?.addService(CarProfile.createPinService())
    }

    private fun stopServer() {
        if (mBluetoothGattServer == null) return

        mBluetoothGattServer!!.close()
    }

    fun onDestroy() {
        val bluetoothAdapter = mBluetoothManager!!.adapter
        if (bluetoothAdapter.isEnabled) {
            stopServer()
            stopAdvertising()
        }
        activity.unregisterReceiver(mBluetoothReceiver)
    }

    fun onStart() {
    }
}