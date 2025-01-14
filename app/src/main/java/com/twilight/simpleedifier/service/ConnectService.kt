package com.twilight.simpleedifier.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.twilight.simpleedifier.connect.ConnectBleDevice
import com.twilight.simpleedifier.connect.ConnectDevice
import com.twilight.simpleedifier.ui.ConnectedActivity
import com.twilight.simpleedifier.R
import com.twilight.simpleedifier.device.EdifierDevice

class ConnectService : LifecycleService() {
    companion object{
        val TAG = "ConnectService"
        val cmd_noise_mode = "cmd_noise_mode"
        val cmd_game_mode = "cmd_game_mode"
        val notificationId = 1
        val notificationChannelId = "Connect Service Channel Notification Id"
        val notificationChannelName = "Connect Service Channel Notification"
    }



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val cmd_noise =
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent?.getSerializableExtra(cmd_noise_mode, EdifierDevice.Companion.NoiseMode::class.java)
        else
            intent?.getSerializableExtra(cmd_noise_mode) as EdifierDevice.Companion.NoiseMode
        val cmd_game = intent?.getBooleanExtra(cmd_game_mode, false) ?: false
        if(cmd_noise != null) {
            setNoiseMode(cmd_noise)

        }else if(cmd_game){
            changeGameMode()
        }
        else{
            intent?.let {
                connect(it)
            }
        }
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        connect(intent)
        return ConnectServiceBinder(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
    }

    var isConnect = false

    override fun onCreate() {
        super.onCreate()

        edifierDevice.isConnected().observe(this) {
            if(it){
                isConnect = true
            }
            else{
                if(isConnect){
                    disconnect()
                }
            }
        }

        edifierDevice.getGameMode().observe(this){
            updateNotification()
        }
        edifierDevice.getNoiseMode().observe(this){
            updateNotification()
        }
    }

    var notificationManager:NotificationManager? = null

    private fun getNotification():Notification{
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(notificationChannelId, notificationChannelName, NotificationManager.IMPORTANCE_HIGH)
        notificationManager!!.createNotificationChannel(channel)
        val notification = NotificationCompat.Builder(this, notificationChannelId)
            .setSmallIcon(R.drawable.ic_sec)
            .setContentTitle(getString(R.string.edifier_service_title))
            .setOngoing(true)

        val noise_mode = edifierDevice.getNoiseMode().value ?: EdifierDevice.Companion.NoiseMode.noise_reduction

        // reduce
        if (noise_mode != EdifierDevice.Companion.NoiseMode.noise_reduction) {
            val reduce_intent = Intent(this, ConnectService::class.java)
            reduce_intent.putExtra(
                cmd_noise_mode,
                EdifierDevice.Companion.NoiseMode.noise_reduction
            )
            val pending_reduce_intent = PendingIntent.getService(
                this,
                0,
                reduce_intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            notification.addAction(
                R.drawable.ic_circle,
                getString(R.string.noise_mode),
                pending_reduce_intent
            )
        }

        // normal
        if(noise_mode != EdifierDevice.Companion.NoiseMode.noise_normal) {
            val normal_intent = Intent(this, ConnectService::class.java)
            normal_intent.putExtra(cmd_noise_mode, EdifierDevice.Companion.NoiseMode.noise_normal)
            val pending_normal_intent = PendingIntent.getService(
                this,
                1,
                normal_intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            notification.addAction(
                R.drawable.ic_circle,
                getString(R.string.standard_mode),
                pending_normal_intent
            )
        }

        // ambient
        if(noise_mode != EdifierDevice.Companion.NoiseMode.noise_ambient) {
            val ambient_intent = Intent(this, ConnectService::class.java)
            ambient_intent.putExtra(cmd_noise_mode, EdifierDevice.Companion.NoiseMode.noise_ambient)
            val pending_ambient_intent = PendingIntent.getService(
                this,
                2,
                ambient_intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            notification.addAction(
                R.drawable.ic_circle,
                getString(R.string.surround_mode),
                pending_ambient_intent
            )
        }

        // game
        val game_intent = Intent(this, ConnectService::class.java)
        game_intent.putExtra(cmd_game_mode, true)
        val pending_game_intent = PendingIntent.getService(this, 3, game_intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val gamemode_text = if (edifierDevice.getGameMode().value == true){
            getString(R.string.game_mode_on)
        }else{
            getString(R.string.game_mode_off)
        }
        notification.addAction(R.drawable.ic_circle, gamemode_text, pending_game_intent)

        // content
        val content_intent = Intent(this, ConnectedActivity::class.java)
        content_intent.putExtra(ConnectDevice.device_mac, mac)
        content_intent.putExtra(ConnectDevice.isBLE, isBle)
        notification.setContentIntent(PendingIntent.getActivity(this, 0, content_intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        return notification.build()
    }

    private fun setForeground(){
        startForeground(notificationId, getNotification())
    }

    private fun updateNotification(){
        if(notificationManager != null) {
            notificationManager?.notify(notificationId, getNotification())
        }
    }


    private val edifierDevice: EdifierDevice = EdifierDevice()
    private var connectDevice: ConnectDevice? = null
    private var mac:String? = null
    private var isBle = false

    private fun connect(intent: Intent){
        mac = intent.getStringExtra(ConnectDevice.device_mac)
        isBle = intent.getBooleanExtra(ConnectDevice.isBLE, false)
        if (mac !=null){
            isConnect = false
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val device = bluetoothManager.adapter.getRemoteDevice(mac)
            edifierDevice.setDevice(device)
            if(connectDevice == null) {
                if(isBle) {
                    connectDevice = ConnectBleDevice(this, device, edifierDevice)
                }
            }
            else{
                connectDevice?.connect(device)
            }
            setForeground()
        }else{
            Toast.makeText(this, "蓝牙MAC为空", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    fun disconnect(){
        connectDevice?.close()
        stopForeground(notificationId)
        notificationManager = null
        stopSelf()
    }

    fun getEdifierDevice(): EdifierDevice{
        return edifierDevice
    }

    fun readSettings(){
        Thread{
            connectDevice?.write(getString(R.string.cmd_read_battery))
            Thread.sleep(200)
            connectDevice?.write(getString(R.string.cmd_read_noise))
            Thread.sleep(200)
            connectDevice?.write(getString(R.string.cmd_read_name))
            Thread.sleep(200)
            connectDevice?.write(getString(R.string.cmd_read_as_settings))
            Thread.sleep(200)
            connectDevice?.write(getString(R.string.cmd_read_game_mode))
            Thread.sleep(200)
            connectDevice?.write(getString(R.string.cmd_read_mac))
            Thread.sleep(200)
            connectDevice?.write(getString(R.string.cmd_read_ldac))
            Thread.sleep(200)
            connectDevice?.write(getString(R.string.cmd_read_eq))
            Thread.sleep(200)
            connectDevice?.write(getString(R.string.cmd_read_prompt_volume))

            //
        }.start()
    }

    fun setPromptVolume(volume: Int){
        if(volume in 0..15) {
            val cmd = getString(R.string.cmd_set_prompt_volume)
            val full_cmd = cmd + String.format("%02x", volume)
            connectDevice?.write(full_cmd)
            edifierDevice.setPromptVolume(volume)
        }
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

    fun setEqMode(mode: EdifierDevice.Companion.EqMode){
        val cmd = when(mode){
            EdifierDevice.Companion.EqMode.eq_normal -> {
                getString(R.string.cmd_eq_normal)
            }
            EdifierDevice.Companion.EqMode.eq_classical -> {
                getString(R.string.cmd_eq_classical)
            }
            EdifierDevice.Companion.EqMode.eq_pop -> {
                getString(R.string.cmd_eq_pop)
            }
            EdifierDevice.Companion.EqMode.eq_rock -> {
                getString(R.string.cmd_eq_rock)
            }
        }
        val success = connectDevice?.write(cmd) ?: false
        if(success){
            edifierDevice.setEqMode(mode)
        }
    }

    fun setAmbientVolume(volume: Int){
        if (volume in -3 .. 3) {
            val cmd = getString(R.string.cmd_noise_ambient)
            val full_cmd = cmd + String.format("%02x", volume+6)
            connectDevice?.write(full_cmd)
            edifierDevice.setAsVolume(volume)
        }
    }

    fun changeGameMode(){
        val current = edifierDevice.getGameMode().value ?: false
        setGameMode(!current)
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
            edifierDevice.setSelectableNoiseMode(mode)
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

