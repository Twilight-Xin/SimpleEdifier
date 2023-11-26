package com.twilight.simpleedifier

import android.content.Context
import android.util.Log

abstract class ConnectDevice(var mContext: Context) {
    companion object {
        private const val TAG = "ConnectedDevice"
        const val device_mac = "device_mac"
        const val isBLE = "is_ble"


        fun makeFullCmd(s: String):ByteArray{
            val cmd: ByteArray = hexToBytes(s)
            val cmdWithHead = ByteArray(cmd.size + 2)
            cmdWithHead[0] = -86
            // should not be more than 127
            // should not be more than 127
            cmdWithHead[1] = cmd.size.toByte()
            System.arraycopy(cmd, 0, cmdWithHead, 2, cmd.size)
            val fullCmd = addCRC(cmdWithHead)
            Log.i(TAG, "makeFullCmd:"+bytesToHex(fullCmd))
            return fullCmd
        }

        fun bytesToHex(bytes: ByteArray): String {
            val sb = StringBuilder()
            for (b in bytes) {
                sb.append(String.format("%02X", b))
            }
            return sb.toString()
        }

        fun hexToBytes(hexString: String): ByteArray {
            val length = hexString.length
            val data = ByteArray(length / 2)
            var i = 0
            while (i < length) {
                val h = hexString[i].digitToIntOrNull(16) ?: 0
                val l = hexString[i+1].digitToIntOrNull(16) ?: 0
                val num = (h shl 4) + l
                data[i / 2] = num.toByte()
                i += 2
            }
            return data
        }

        fun addCRC(bArr: ByteArray): ByteArray {
            var i: Int
            i = 8217
            for (b2 in bArr) {
                i += b2.toInt() and 255
            }
            val length = bArr.size + 2
            val bArr2 = ByteArray(length)
            System.arraycopy(bArr, 0, bArr2, 0, bArr.size)
            bArr2[length - 2] = (i shr 8).toByte()
            bArr2[length - 1] = (i and 255).toByte()
            CRCCheck(bArr2)
            Log.i(
                "CRC",
                "CRC_value:" + bytesToHex(bArr2) + ",length:" + i + ",data:" + bytesToHex(bArr)
            )
            return bArr2
        }

        fun CRCCheck(bArr: ByteArray): Boolean {
            val i =
                (bArr[bArr.size - 1].toInt() and 255) + (bArr[bArr.size - 2].toInt() shl 8 and 65280)
            var i2 = 0
            for (i3 in 0 until bArr.size - 1) {
                if (i3 < bArr.size - 2) {
                    i2 += bArr[i3].toInt() and 255
                }
            }
            return i2 + 8217 == i
        }
    }

    abstract fun connect()

    abstract fun close()

    abstract fun write(s:String):Boolean



}