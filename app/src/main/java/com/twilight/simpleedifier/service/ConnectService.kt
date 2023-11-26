package com.twilight.simpleedifier.service

import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.widget.Toast
import com.twilight.simpleedifier.ConnectBleDevice
import com.twilight.simpleedifier.ConnectDevice
import com.twilight.simpleedifier.R
import com.twilight.simpleedifier.ScanActivity
import com.twilight.simpleedifier.device.EdifierDevice

class ConnectService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { connect(it) }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        connect(intent)
        return ConnectServiceBinder(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
    }

    override fun onCreate() {
        super.onCreate()
    }


    private val edifierDevice: EdifierDevice = EdifierDevice()
    private var connectDevice: ConnectDevice? = null
    private fun connect(intent: Intent){
        val mac = intent.getStringExtra(ConnectDevice.device_mac)
        val isBle = intent.getBooleanExtra(ConnectDevice.isBLE, false)
        if (mac !=null){
            if(connectDevice == null) {
                val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val device = bluetoothManager.adapter.getRemoteDevice(mac)
                edifierDevice.setDevice(device)
                if(isBle) {
                    connectDevice = ConnectBleDevice(this, device, edifierDevice)
                }
            }
            else{
                connectDevice?.connect()
            }
        }else{
            Toast.makeText(this, "蓝牙MAC为空", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    fun disconnect(){
        connectDevice?.close()
        stopSelf()
    }

    fun getEdifierDevice(): EdifierDevice{
        return edifierDevice
    }

    fun readSettings(){
        Thread{
            connectDevice?.write(getString(R.string.cmd_read_battery))
            Thread.sleep(150)
            connectDevice?.write(getString(R.string.cmd_read_noise))
            Thread.sleep(150)
            connectDevice?.write(getString(R.string.cmd_read_as_settings))
            Thread.sleep(150)
            connectDevice?.write(getString(R.string.cmd_read_game_mode))
            Thread.sleep(150)
            connectDevice?.write(getString(R.string.cmd_read_mac))
            Thread.sleep(150)
            connectDevice?.write(getString(R.string.cmd_read_ldac))
            Thread.sleep(150)
            connectDevice?.write(getString(R.string.cmd_read_eq))
            //
        }.start()
    }

    fun setNoiseMode(mode: EdifierDevice.Companion.NoiseMode){
        val cmd = when(mode){
            EdifierDevice.Companion.NoiseMode.noise_ambient -> {
                getString(R.string.cmd_noise_ambient)
            }
            EdifierDevice.Companion.NoiseMode.noise_normal -> {
                getString(R.string.cmd_noise_normal)
            }
            EdifierDevice.Companion.NoiseMode.noise_reduction -> {
                getString(R.string.cmd_noise_reduction)
            }
        }
        val success = connectDevice?.write(cmd) ?: false
        if(success){
            edifierDevice.setNoiseMode(mode)
        }
    }

    fun setGameMode(mode: Boolean){
        val cmd = if(mode){
            getString(R.string.cmd_game_on)
        }else{
            getString(R.string.cmd_game_off)
        }
        val success = connectDevice?.write(cmd) ?: false
        if(success){
            edifierDevice.setGameMode(mode)
        }
    }

    fun setSelectableNoiseMode(mode: EdifierDevice.Companion.SelectableNoiseMode){
        val cmd = getString(R.string.cmd_control_settings)
        val full_cmd = when (mode) {
            EdifierDevice.Companion.SelectableNoiseMode.all -> {
                cmd + "07"
            }

            EdifierDevice.Companion.SelectableNoiseMode.no_reduction -> {
                cmd + "05"
            }

            EdifierDevice.Companion.SelectableNoiseMode.no_normal -> {
                cmd + "06"
            }

            EdifierDevice.Companion.SelectableNoiseMode.no_ambient -> {
                cmd + "03"
            }
        }
        if(full_cmd != ""){
            connectDevice?.write(full_cmd)
        }
    }

    fun musicPrev(){
        connectDevice?.write(getString(R.string.cmd_pc_prev))
    }

    fun musicNext(){
        connectDevice?.write(getString(R.string.cmd_pc_next))
    }

    fun musicPlay(){
        connectDevice?.write(getString(R.string.cmd_pc_play))
    }

    fun musicPause(){
        connectDevice?.write(getString(R.string.cmd_pc_pause))
    }

    fun volumeUp(){
        connectDevice?.write(getString(R.string.cmd_pc_volume_up))
    }

    fun volumeDown(){
        connectDevice?.write(getString(R.string.cmd_pc_volume_down))
    }

    fun powerOff(){
        connectDevice?.write(getString(R.string.cmd_power_off))
    }

    class ConnectServiceBinder(private val service: ConnectService): Binder(){
        fun getService(): ConnectService{
            return service
        }
    }
}

