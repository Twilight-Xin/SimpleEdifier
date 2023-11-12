package com.twilight.simpleedifier

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.util.Log

object ConnectDevice {
    private const val TAG = "ConnectedDevice"

    private var connected = false
    private var gatt:BluetoothGatt? = null

    var device:BluetoothDevice? = null
        set(value){
            if(!connected){
                field = value
            }
            else{
                Log.d(TAG, ": last connect do not close")
            }
        }

    fun isConnected():Boolean{
        return connected
    }

    private fun connect(){
        if (device != null && !connected){

        }
    }

}