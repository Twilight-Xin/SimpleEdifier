package com.twilight.simpleedifier

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class EdifierViewModel: ViewModel() {
    companion object{
        const val noise_normal = "cmd_noise_normal"
        const val noise_reduction = "cmd_noise_reduction"
        const val noise_ambient = "cmd_noise_ambient"

        const val eq_normal = "cmd_eq_normal"
        const val eq_pop = "cmd_eq_pop"
        const val eq_classical = "cmd_eq_classical"
        const val eq_rock = "cmd_eq_rock"

        const val game_on = "cmd_game_on"
        const val game_off = "cmd_game_off"

        const val ldac_off = "cmd_ldac_off"
        const val ldac_48k = "cmd_ldac_48k"
        const val ldac_96k = "cmd_ldac_96k"

        const val power_off = "cmd_power_off"
        const val disconnect = "cmd_disconnect"
        const val re_pair = "cmd_re_pair"
        const val factory_reset = "cmd_factory_reset"

    }
    private val device:MutableLiveData<BluetoothDevice> = MutableLiveData(null)
    private val device_name:MutableLiveData<String> = MutableLiveData(null)

    private val noise_mode:MutableLiveData<String> = MutableLiveData(null)
    private val game_mode:MutableLiveData<Boolean> = MutableLiveData(false)
    private val ldac_mode:MutableLiveData<String> = MutableLiveData(null)
    private val eq_mode:MutableLiveData<String> = MutableLiveData(null)
    private val as_volume:MutableLiveData<Int> = MutableLiveData(0)

    private val selectable_noise_modes:MutableLiveData<ArrayList<Boolean>> = MutableLiveData(
        ArrayList()
    )

    private val shutdown_time:MutableLiveData<UInt> = MutableLiveData(0u)

    private val battery:MutableLiveData<UInt> = MutableLiveData(0u)
    private val mac:MutableLiveData<String> = MutableLiveData(null)
    private val firmware:MutableLiveData<String> = MutableLiveData(null)

    private val connected:MutableLiveData<Boolean> = MutableLiveData(false)

    fun getDevice(): LiveData<BluetoothDevice>{
        return device
    }

    fun setDevice(device: BluetoothDevice){
        this.device.postValue(device)
    }

    fun getNoiseMode():LiveData<String>{
        return  noise_mode
    }

    fun setNoiseMode(mode:String){
        if(mode == noise_ambient || mode == noise_normal || mode == noise_reduction) {
            noise_mode.postValue(mode)
        }
    }

    fun  getGameMode():LiveData<Boolean>{
        return game_mode
    }

    fun setGameMode(mode: Boolean){
        game_mode.postValue(mode)
    }

    fun getLdacMode():LiveData<String>{
        return ldac_mode
    }

    fun getEqMode():LiveData<String>{
        return eq_mode
    }

    fun isConnected():LiveData<Boolean>{
        return connected
    }

    fun setConnected(connected:Boolean){
        this.connected.postValue(connected)
    }

    fun getBattery():LiveData<UInt>{
        return battery
    }

    fun getMac():LiveData<String>{
        return mac
    }

    fun getFirmware():LiveData<String>{
        return firmware
    }

    fun getSelectableNoiseMode():LiveData<ArrayList<Boolean>>{
        return selectable_noise_modes
    }

    fun getShutDownTime():LiveData<UInt>{
        return shutdown_time
    }


    fun setData(byteArray: ByteArray){
        val head = byteArray[0].toUInt() and 0xFFu
        val len = byteArray[1].toUInt() and 0xFFu
        if (head == 187u) {
            if (len == 2u) {
                val cmd = byteArray[2].toUInt() and 0xFFu
                val ch = byteArray[3].toUInt() and 0xFFu
                when (cmd) {
                    149u -> {
                        // eq mode
                        when (ch){
                            0u -> {
                                eq_mode.postValue(eq_normal)
                            }
                            1u ->{
                                eq_mode.postValue(eq_pop)
                            }
                            2u ->{
                                eq_mode.postValue(eq_classical)
                            }
                            3u ->{
                                eq_mode.postValue(eq_rock)
                            }
                        }
                    }
                    8u -> {
                        // game mode
                        game_mode.postValue(ch == 0x01u)
                    }
                    160u -> {
                        // battery
                        battery.postValue(ch)
                    }
                    72u -> {
                        // LDAC
                        when(ch){
                            0u ->{
                                ldac_mode.postValue(ldac_off)
                            }
                            1u ->{
                                ldac_mode.postValue(ldac_48k)
                            }
                            2u ->{
                                ldac_mode.postValue(ldac_96k)
                            }
                        }
                    }
                    5u -> {
                        // PV ?
                    }
                    179u -> {
                        // auto power off

                    }
                }
            } else if (len > 2u) {
                val cmd = byteArray[2].toUInt() and 0xFFu
                when {
                    cmd == 200u && len == 7u -> {
                        // （mac）
                        val address = byteArray.sliceArray(3..byteArray.size)
                        val stringArray = ArrayList<String>()
                        for (b in address){
                            stringArray.add(String.format("%02X", b))
                        }
                        mac.postValue(stringArray.joinToString(separator = ":"))

                    }
                    cmd == 198u && len == 4u -> {
                        // 固件
                        firmware.postValue(byteArray.sliceArray(3..byteArray.size).joinToString(separator = "."))
                    }
                    cmd == 204u && len == 3u -> {
                        // asv
                        val mode = byteArray[3].toUInt() and 0xFFu
                        val volume = ((byteArray[4].toUInt() and 0xFFu) - 6u).toInt()
                        when(mode){
                            0x01u ->{
                                noise_mode.postValue(noise_normal)
                            }
                            0x02u ->{
                                noise_mode.postValue(noise_reduction)
                            }
                            0x03u ->{
                                noise_mode.postValue(noise_ambient)
                            }
                        }
                        as_volume.postValue(volume)
                    }
                    cmd == 201u -> {
                        // name
                        device_name.postValue(String(byteArray, Charsets.UTF_8))
                    }
                    cmd == 240u && len == 3u && (byteArray[3].toUInt() and 0xFFu ) == 10u -> {
                        // 可选降噪
                        val mode = byteArray[4].toUInt() and 0xFFu
                        val selectable_modes = arrayListOf((mode and 1u) == 1u, (mode and 2u) == 2u, (mode and 4u) == 4u)

                    }
                    cmd == 179u && len == 3u -> {
                        // shut down
                        shutdown_time.postValue(byteArray[4].toUInt() and 0xFFu)
                    }
                }
            }
        }
    }
}