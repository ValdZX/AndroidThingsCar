package ua.com.valdzx.car.androidthingscar

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
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.ParcelUuid
import android.text.format.DateFormat
import android.util.Log
import android.view.WindowManager
import ua.com.vald_zx.car.core.Constants.DeviceName
import java.util.*

class MainActivity : Activity() {

    private var mBluetoothManager: BluetoothManager? = null
    private var mBluetoothGattServer: BluetoothGattServer? = null
    private var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private val mRegisteredDevices = HashSet<BluetoothDevice>()

    private val mTimeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val adjustReason: Byte = when (intent.action) {
                Intent.ACTION_TIME_CHANGED -> TimeProfile.ADJUST_MANUAL
                Intent.ACTION_TIMEZONE_CHANGED -> TimeProfile.ADJUST_TIMEZONE
                Intent.ACTION_TIME_TICK -> TimeProfile.ADJUST_NONE
                else -> TimeProfile.ADJUST_NONE
            }
            val now = System.currentTimeMillis()
            notifyRegisteredDevices(now, adjustReason)
            updateLocalUi(now)
        }
    }

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
            }
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int,
                                                 characteristic: BluetoothGattCharacteristic) {
            val now = System.currentTimeMillis()
            when {
                TimeProfile.CURRENT_TIME == characteristic.uuid -> {
                    Log.i(TAG, "Read CurrentTime")
                    mBluetoothGattServer!!.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            TimeProfile.getExactTime(now, TimeProfile.ADJUST_NONE))
                }
                TimeProfile.LOCAL_TIME_INFO == characteristic.uuid -> {
                    Log.i(TAG, "Read LocalTimeInfo")
                    mBluetoothGattServer!!.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            TimeProfile.getLocalTimeInfo(now))
                }
                else -> {
                    Log.w(TAG, "Invalid Characteristic Read: " + characteristic.uuid)
                    mBluetoothGattServer!!.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null)
                }
            }
        }

        override fun onDescriptorReadRequest(device: BluetoothDevice, requestId: Int, offset: Int, descriptor: BluetoothGattDescriptor) {
            if (TimeProfile.CLIENT_CONFIG == descriptor.uuid) {
                Log.d(TAG, "Config descriptor read")
                val returnValue: ByteArray = if (mRegisteredDevices.contains(device)) {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else {
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                }
                mBluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, returnValue)
            } else {
                Log.w(TAG, "Unknown descriptor read request")
                mBluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
            }
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice,
                                              requestId: Int,
                                              descriptor: BluetoothGattDescriptor,
                                              preparedWrite: Boolean,
                                              responseNeeded: Boolean,
                                              offset: Int,
                                              value: ByteArray) {
            if (TimeProfile.CLIENT_CONFIG == descriptor.uuid) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Subscribe device to notifications: $device")
                    mRegisteredDevices.add(device)
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Unsubscribe device from notifications: $device")
                    mRegisteredDevices.remove(device)
                }

                if (responseNeeded) {
                    mBluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                }
            } else {
                Log.w(TAG, "Unknown descriptor write request")
                if (responseNeeded) {
                    mBluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = mBluetoothManager!!.adapter
        if (!checkBluetoothSupport(bluetoothAdapter)) {
            finish()
        }
        bluetoothAdapter.name = DeviceName
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(mBluetoothReceiver, filter)
        if (!bluetoothAdapter.isEnabled) {
            Log.d(TAG, "Bluetooth is currently disabled...enabling")
            bluetoothAdapter.enable()
        } else {
            Log.d(TAG, "Bluetooth enabled...starting services")
            startAdvertising()
            startServer()
        }
    }

    override fun onStart() {
        super.onStart()
        // Register for system clock events
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_TIME_TICK)
        filter.addAction(Intent.ACTION_TIME_CHANGED)
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
        registerReceiver(mTimeReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(mTimeReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()

        val bluetoothAdapter = mBluetoothManager!!.adapter
        if (bluetoothAdapter.isEnabled) {
            stopServer()
            stopAdvertising()
        }

        unregisterReceiver(mBluetoothReceiver)
    }

    private fun checkBluetoothSupport(bluetoothAdapter: BluetoothAdapter?): Boolean {

        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported")
            return false
        }

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported")
            return false
        }

        return true
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
                .addServiceUuid(ParcelUuid(TimeProfile.TIME_SERVICE))
                .build()

        mBluetoothLeAdvertiser!!
                .startAdvertising(settings, data, mAdvertiseCallback)
    }

    private fun stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return

        mBluetoothLeAdvertiser!!.stopAdvertising(mAdvertiseCallback)
    }

    private fun startServer() {
        mBluetoothGattServer = mBluetoothManager!!.openGattServer(this, mGattServerCallback)
        if (mBluetoothGattServer == null) {
            Log.w(TAG, "Unable to create GATT server")
            return
        }
        mBluetoothGattServer?.addService(TimeProfile.createTimeService())
        updateLocalUi(System.currentTimeMillis())
    }

    private fun stopServer() {
        if (mBluetoothGattServer == null) return

        mBluetoothGattServer!!.close()
    }

    private fun notifyRegisteredDevices(timestamp: Long, adjustReason: Byte) {
        if (mRegisteredDevices.isEmpty()) {
            Log.i(TAG, "No subscribers registered")
            return
        }
        val exactTime = TimeProfile.getExactTime(timestamp, adjustReason)

        Log.i(TAG, "Sending update to " + mRegisteredDevices.size + " subscribers")
        for (device in mRegisteredDevices) {
            val timeCharacteristic = mBluetoothGattServer!!
                    .getService(TimeProfile.TIME_SERVICE)
                    .getCharacteristic(TimeProfile.CURRENT_TIME)
            timeCharacteristic.value = exactTime
            mBluetoothGattServer!!.notifyCharacteristicChanged(device, timeCharacteristic, false)
        }
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