package com.twilight.simpleedifier

import com.twilight.simpleedifier.connect.ConnectDevice
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun text_change_hex(){
        val cmd = "C101"
        val target = byteArrayOf(0xAA.toByte(), 0x02.toByte(), 0xC1.toByte(), 0x01.toByte(), 0x21.toByte(), 0x87.toByte())
        val full = ConnectDevice.makeFullCmd(cmd)
        assertArrayEquals(target, full)
    }
}