package ua.com.valdzx.car.terminal

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.os.IBinder
import androidx.content.systemService

class BluetoothManager(private val activity: Activity) {
    val requestEnableBt = 1
    private val adapter: BluetoothAdapter
    private val scanner: BluetoothLeScanner
    private var bluetoothLeService: BluetoothLeService? = null
    private var isScanning: Boolean = false
    private lateinit var deviceName: String
    private lateinit var findCallable: (BluetoothDevice) -> Unit
    private lateinit var serviceConnection: ServiceConnection
    private var device: BluetoothDevice? = null
    var connectionListener: (Boolean) -> Unit = {}

    var isConnected: Boolean = false
        private set(value) {
            field = value
            connectionListener.invoke(value)
        }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result?.device?.name == deviceName) {
                findCallable.invoke(result.device)
            }
        }
    }

    init {
        val manager = activity.systemService<BluetoothManager>()
        adapter = manager.adapter
        scanner = adapter.bluetoothLeScanner
    }

    fun enableIfNeed() {
        if (!adapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivityForResult(enableBtIntent, requestEnableBt)
        }
    }

    fun findDevice(deviceName: String, findCallable: (BluetoothDevice) -> Unit) {
        isScanning = true
        this.deviceName = deviceName
        this.findCallable = findCallable
        scanner.startScan(scanCallback)
    }

    fun stopFinding() {
        isScanning = false
        scanner.stopScan(scanCallback)
    }

    fun connect(device: BluetoothDevice) {
        this.device = device
        if (bluetoothLeService == null) {
            val gattServiceIntent = Intent(activity, BluetoothLeService::class.java)
            activity.bindService(gattServiceIntent, createServiceConnection(device), Context.BIND_AUTO_CREATE)
        } else {
            bluetoothLeService?.connect(device.address)
        }
    }

    private fun createServiceConnection(device: BluetoothDevice): ServiceConnection {
        this.serviceConnection = object : ServiceConnection {

            override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
                bluetoothLeService = (service as BluetoothLeService.LocalBinder).service
                bluetoothLeService?.initialize()
                bluetoothLeService?.connect(device.address)
            }

            override fun onServiceDisconnected(componentName: ComponentName) {
                bluetoothLeService = null
            }
        }
        return serviceConnection
    }

    fun onDestroy() {
        activity.unbindService(serviceConnection)
    }

    private val mGattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> isConnected = true
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> isConnected = false
//                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> showCurrentState(mBluetoothLeService.getSupportedGattServices())
//                BluetoothLeService.ACTION_DATA_AVAILABLE -> displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))
            }
        }
    }

    fun onPause() {
        if (device == null) {
            stopFinding()
        } else {
            activity.unregisterReceiver(mGattUpdateReceiver)
        }
    }

    fun onResume() {
        enableIfNeed()
        if (device != null) {
            device?.let { connect(it) }
            val intentFilter = IntentFilter()
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
            activity.registerReceiver(mGattUpdateReceiver, intentFilter)
        }
    }
}
