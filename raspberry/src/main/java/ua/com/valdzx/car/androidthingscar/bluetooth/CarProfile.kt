package ua.com.valdzx.car.androidthingscar.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.*
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY
import ua.com.vald_zx.car.core.Constants.CAR_SERVICE
import ua.com.vald_zx.car.core.Constants.LEFT_ENGINE_STATE
import ua.com.vald_zx.car.core.Constants.RIGHT_ENGINE_STATE

object CarProfile {
    fun createPinService(): BluetoothGattService {
        val service = BluetoothGattService(CAR_SERVICE, SERVICE_TYPE_PRIMARY)
        val leftEngine = BluetoothGattCharacteristic(LEFT_ENGINE_STATE, PROPERTY_READ or PROPERTY_NOTIFY or PROPERTY_WRITE, PERMISSION_READ or PERMISSION_WRITE)
        val rightEngine = BluetoothGattCharacteristic(RIGHT_ENGINE_STATE, PROPERTY_READ or PROPERTY_NOTIFY or PROPERTY_WRITE, PERMISSION_READ or PERMISSION_WRITE)
        service.addCharacteristic(leftEngine)
        service.addCharacteristic(rightEngine)
        return service
    }
}