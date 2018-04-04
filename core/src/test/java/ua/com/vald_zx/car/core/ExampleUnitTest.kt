package ua.com.vald_zx.car.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.*

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        println(UUID.randomUUID())
        assertEquals(4, (2 + 2).toLong())
    }

    @Test
    fun double() {
        println()
        val range = -100..100
        range.forEach { print((it.toDouble() / 100.0).toString() + "\t") }
    }

    @Test
    fun doubleByteArray() {
        println()
        val range = arrayListOf(0.0, 10.0, 12.1, 0.3)
        range.forEach {  print((it.toBytes()).toString() + "\t")  }
        println()
        range.forEach {  print((it.toBytes().toDouble()).toString() + "\t")  }
    }
}