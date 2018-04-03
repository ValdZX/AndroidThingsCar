package ua.com.vald_zx.car.core

import java.util.*

fun Boolean.toByte() = (if(this) 1 else 0).toByte()
fun Byte.toBool() = this != 0.toByte()

object Constants {
    const val DeviceName = "RPI3 CAR"

    val CAR_SERVICE = UUID.fromString("a7ad9c7d-8913-4f48-937d-5312dcfa5a83")
    val PIN_STATE = UUID.fromString("6655785f-1c36-4e72-9d62-1d3626734caa")
    val PWM_STATE = UUID.fromString("8442e09d-ab2e-43a3-a6ba-c1d0e5c18f03")
    //    val PWM_SERVICE = UUID.fromString("8520ed6c-b92b-438d-9437-5b7dde107bcb")



    val UUID_CHANGED = "ua.com.vald_zx.car.core.UUID_CHANGED"
}