package com.twilight.simpleedifier.connect

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import android.util.Log
import com.twilight.simpleedifier.device.EdifierDevice
import java.util.UUID


class ConnectBleDevice(private val context: Context, private var device:BluetoothDevice, val edifierDevice: EdifierDevice): ConnectDevice(context) {
    companion object{
        private const val TAG = "ConnectBleDevice"

        const val service_uuid = "00001000-0000-1000-8991-00805f9b34fb"
        const val contain_uuid = "-1a48-11e9-ab14-d663bd873d93"

        val receive_uuid_list = arrayListOf("00001000-0000-1000-8992-00805f9b34fb","48090001-1a48-11e9-ab14-d663bd873d93")
        val transmit_uuid_list = arrayListOf("00001000-0000-1000-8993-00805f9b34fb", "48090002-1a48-11e9-ab14-d663bd873d93")
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Thread{
                Log.d(TAG, "onConnectionStateChange: 连接状态改变")
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        gatt?.discoverServices()
                        Log.d(TAG, "onConnectionStateChange: 连接")
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        edifierDevice.setConnected(false)
                        Log.d(TAG, "onConnectionStateChange: 断开")
                    }
                }
            }.start()
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            Thread {
                if (checkCRC(value)) {
                    val data = value.sliceArray(0 until  value.size - 2)
                    edifierDevice.setData(data)
                }
                Log.d(TAG, "onCharacteristicChanged: 接受数据")
            }.start()
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            Log.d(TAG, "onDescriptorWrite: 监听成功")
            edifierDevice.setConnected(true)
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Thread {
                Log.d(TAG, "onServicesDiscovered: 发现服务")
                serviceList = gatt?.services
                service = gatt?.getService(UUID.fromString(service_uuid))
                if (service == null && serviceList != null) {
                    for (i in 0 until serviceList!!.size) {
                        if (serviceList?.get(i)?.uuid?.toString()?.contains(contain_uuid) == true) {
                            service = serviceList?.get(i)
                            break
                        }
                    }
                }
                if (service != null) {
                    characteristicList = service?.characteristics
                    if (characteristicList != null) {
                        for (c in characteristicList!!) {
                            if (receive_uuid_list.contains(c.uuid.toString())) {
                                notify_characteristic = c
                            }
                            if (transmit_uuid_list.contains(c.uuid.toString())) {
                                write_characteristic = c
                            }
                        }
                    }
                }
                if (notify_characteristic != null && write_characteristic != null) {
                    gatt?.setCharacteristicNotification(notify_characteristic, true)
                    // val descriptors = notify_characteristic?.descriptors
                    val descriptor =
                        notify_characteristic?.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    descriptor?.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    gatt?.writeDescriptor(descriptor)
                }
            }.start()
        }
    }

    @SuppressLint("MissingPermission")
    var gatt: BluetoothGatt? = device.connectGatt(context,false, bluetoothGattCallback)
    var serviceList: List<BluetoothGattService>? = null
    var service:BluetoothGattService? = null
    var characteristicList:List<BluetoothGattCharacteristic>? = null
    var notify_characteristic: BluetoothGattCharacteristic? = null
    var write_characteristic: BluetoothGattCharacteristic? = null



    @SuppressLint("MissingPermission")
    override fun write(s: String):Boolean {
        var send_success = false
        if(write_characteristic != null){
            val cmd = makeFullCmd(s)
            write_characteristic?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            write_characteristic?.value = cmd
            send_success = gatt?.writeCharacteristic(write_characteristic) == true
            if(!send_success){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    send_success = gatt?.writeCharacteristic(write_characteristic!!, cmd, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothStatusCodes.SUCCESS
                }
            }
        }
        return send_success
    }

    @SuppressLint("MissingPermission")
    override fun connect() {
        if(gatt == null) {
            gatt = device.connectGatt(context, false, bluetoothGattCallback)
        }
        else{
            gatt?.connect()
        }

    }

    @SuppressLint("MissingPermission")
    override fun connect(device: BluetoothDevice) {
        if(device.address == this.device.address){
            connect()
        }
        else{
            close()
            this.device = device
            gatt = device.connectGatt(context, false, bluetoothGattCallback)
        }
    }

    @SuppressLint("MissingPermission")
    override fun close() {
        notify_characteristic = null
        serviceList = null
        service = null
        write_characteristic = null
        gatt?.disconnect()
        gatt = null
    }



}