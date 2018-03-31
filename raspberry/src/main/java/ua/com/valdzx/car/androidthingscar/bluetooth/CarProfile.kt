package ua.com.valdzx.car.androidthingscar.bluetooth

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.*
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY
import ua.com.vald_zx.car.core.Constants.PIN_SERVICE
import ua.com.vald_zx.car.core.Constants.PIN_STATE
import java.util.*

object CarProfile {


    val TIME_SERVICE = UUID.fromString("d72a9f56-86ec-4843-8d50-6c69d68b7007")
    val CURRENT_TIME = UUID.fromString("75d57f73-bb55-4f38-b0cf-40bf04c6e4e9")
    val LOCAL_TIME_INFO = UUID.fromString("25570166-dc85-47fa-a81d-5c41e367bf36")
    val CLIENT_CONFIG = UUID.fromString("cfd3adfe-0287-493d-8bc8-53e9eab710b9")

    val ADJUST_NONE: Byte = 0x0
    val ADJUST_MANUAL: Byte = 0x1
    val ADJUST_EXTERNAL: Byte = 0x2
    val ADJUST_TIMEZONE: Byte = 0x4
    val ADJUST_DST: Byte = 0x8

    private val FIFTEEN_MINUTE_MILLIS = 900000
    private val HALF_HOUR_MILLIS = 1800000

    private val DAY_UNKNOWN: Byte = 0
    private val DAY_MONDAY: Byte = 1
    private val DAY_TUESDAY: Byte = 2
    private val DAY_WEDNESDAY: Byte = 3
    private val DAY_THURSDAY: Byte = 4
    private val DAY_FRIDAY: Byte = 5
    private val DAY_SATURDAY: Byte = 6
    private val DAY_SUNDAY: Byte = 7

    private val DST_STANDARD: Byte = 0x0
    private val DST_HALF: Byte = 0x2
    private val DST_SINGLE: Byte = 0x4
    private val DST_DOUBLE: Byte = 0x8
    private val DST_UNKNOWN = 0xFF.toByte()

    fun createPinService(): BluetoothGattService {
        val service = BluetoothGattService(PIN_SERVICE, SERVICE_TYPE_PRIMARY)
        val pin = BluetoothGattCharacteristic(PIN_STATE, PROPERTY_READ or PROPERTY_NOTIFY or PROPERTY_WRITE, PERMISSION_READ or PERMISSION_WRITE)
        service.addCharacteristic(pin)
        return service
    }

    fun createTimeService(): BluetoothGattService {
        val service = BluetoothGattService(TIME_SERVICE, SERVICE_TYPE_PRIMARY)
        val currentTime = BluetoothGattCharacteristic(CURRENT_TIME, PROPERTY_READ or PROPERTY_NOTIFY, PERMISSION_READ)
        val configDescriptor = BluetoothGattDescriptor(CLIENT_CONFIG, BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE)
        currentTime.addDescriptor(configDescriptor)
        val localTime = BluetoothGattCharacteristic(LOCAL_TIME_INFO, PROPERTY_READ, PERMISSION_READ)
        service.addCharacteristic(currentTime)
        service.addCharacteristic(localTime)
        return service
    }

    fun getExactTime(timestamp: Long, adjustReason: Byte): ByteArray {
        val time = Calendar.getInstance()
        time.timeInMillis = timestamp

        val field = ByteArray(10)

        val year = time.get(Calendar.YEAR)
        field[0] = (year and 0xFF).toByte()
        field[1] = (year shr 8 and 0xFF).toByte()
        field[2] = (time.get(Calendar.MONTH) + 1).toByte()
        field[3] = time.get(Calendar.DATE).toByte()
        field[4] = time.get(Calendar.HOUR_OF_DAY).toByte()
        field[5] = time.get(Calendar.MINUTE).toByte()
        field[6] = time.get(Calendar.SECOND).toByte()
        field[7] = getDayOfWeekCode(time.get(Calendar.DAY_OF_WEEK))
        field[8] = (time.get(Calendar.MILLISECOND) / 256).toByte()

        field[9] = adjustReason

        return field
    }

    fun getLocalTimeInfo(timestamp: Long): ByteArray {
        val time = Calendar.getInstance()
        time.timeInMillis = timestamp
        val field = ByteArray(2)
        val zoneOffset = time.get(Calendar.ZONE_OFFSET) / FIFTEEN_MINUTE_MILLIS // 15 minute intervals
        field[0] = zoneOffset.toByte()
        val dstOffset = time.get(Calendar.DST_OFFSET) / HALF_HOUR_MILLIS // 30 minute intervals
        field[1] = getDstOffsetCode(dstOffset)
        return field
    }

    private fun getDayOfWeekCode(dayOfWeek: Int): Byte {
        return when (dayOfWeek) {
            Calendar.MONDAY -> DAY_MONDAY
            Calendar.TUESDAY -> DAY_TUESDAY
            Calendar.WEDNESDAY -> DAY_WEDNESDAY
            Calendar.THURSDAY -> DAY_THURSDAY
            Calendar.FRIDAY -> DAY_FRIDAY
            Calendar.SATURDAY -> DAY_SATURDAY
            Calendar.SUNDAY -> DAY_SUNDAY
            else -> DAY_UNKNOWN
        }
    }

    private fun getDstOffsetCode(rawOffset: Int): Byte {
        return when (rawOffset) {
            0 -> DST_STANDARD
            1 -> DST_HALF
            2 -> DST_SINGLE
            4 -> DST_DOUBLE
            else -> DST_UNKNOWN
        }
    }
}