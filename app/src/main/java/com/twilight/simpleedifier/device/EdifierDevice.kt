package com.twilight.simpleedifier.device

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

open class EdifierDevice {
    companion object{
        enum class NoiseMode {
            noise_normal,
            noise_reduction,
            noise_ambient
        }

        enum class EqMode{
            eq_normal,
            eq_pop,
            eq_classical,
            eq_rock,
        }

        enum class LdacMode{
            ldac_off,
            ldac_48k,
            ldac_96k
        }

        enum class SelectableNoiseMode{
            all,
            no_normal,
            no_reduction,
            no_ambient
        }

        const val power_off = "cmd_power_off"
        const val disconnect = "cmd_disconnect"
        const val re_pair = "cmd_re_pair"
        const val factory_reset = "cmd_factory_reset"

    }
    private val device: MutableLiveData<BluetoothDevice> = MutableLiveData(null)
    private val device_name: MutableLiveData<String> = MutableLiveData("Unknown")

    private val noise_mode: MutableLiveData<NoiseMode> = MutableLiveData(NoiseMode.noise_reduction)
    private val game_mode: MutableLiveData<Boolean> = MutableLiveData(false)
    private val ldac_mode: MutableLiveData<LdacMode> = MutableLiveData(LdacMode.ldac_48k)
    private val eq_mode: MutableLiveData<EqMode> = MutableLiveData(EqMode.eq_normal)
    private val as_volume: MutableLiveData<Int> = MutableLiveData(0)
    private val prompt_volume: MutableLiveData<Int> = MutableLiveData(-1)

    private val selectable_noise_modes: MutableLiveData<SelectableNoiseMode> = MutableLiveData(SelectableNoiseMode.all)

    private val shutdown_time: MutableLiveData<Int> = MutableLiveData(-1)

    private val battery: MutableLiveData<Int> = MutableLiveData(-1)
    private val mac: MutableLiveData<String> = MutableLiveData("")
    private val firmware: MutableLiveData<String> = MutableLiveData("")

    private val connected: MutableLiveData<Boolean> = MutableLiveData(false)

    fun getDevice(): LiveData<BluetoothDevice> {
        return device
    }

    fun setDevice(device: BluetoothDevice){
        this.device.postValue(device)
    }

    fun getName(): LiveData<String>{
        return device_name
    }

    fun setName(name:String){
        device_name.postValue(name)
    }

    fun getNoiseMode(): LiveData<NoiseMode> {
        return  noise_mode
    }

    fun setNoiseMode(mode:NoiseMode){
        noise_mode.postValue(mode)
    }

    fun  getGameMode(): LiveData<Boolean> {
        return game_mode
    }

    fun setGameMode(mode: Boolean){
        game_mode.postValue(mode)
    }

    fun getLdacMode(): LiveData<LdacMode> {
        return ldac_mode
    }

    fun getEqMode(): LiveData<EqMode> {
        return eq_mode
    }

    fun setEqMode(mode: EqMode){
        eq_mode.postValue(mode)
    }

    fun isConnected(): LiveData<Boolean> {
        return connected
    }

    fun setConnected(connected:Boolean){
        this.connected.postValue(connected)
    }

    fun getBattery(): LiveData<Int> {
        return battery
    }

    fun getMac(): LiveData<String> {
        return mac
    }

    fun getFirmware(): LiveData<String> {
        return firmware
    }

    fun getSelectableNoiseMode(): LiveData<SelectableNoiseMode> {
        return selectable_noise_modes
    }

    fun setSelectableNoiseMode(mode:SelectableNoiseMode){
        selectable_noise_modes.postValue(mode)
    }

    fun getShutDownTime(): LiveData<Int> {
        return shutdown_time
    }

    fun getASVolume():LiveData<Int>{
        return as_volume
    }

    fun setAsVolume(volume:Int){
        as_volume.postValue(volume)
    }

    fun getPromptVolume():LiveData<Int>{
        return prompt_volume
    }

    fun setPromptVolume(volume: Int){
        prompt_volume.postValue(volume)
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
                                eq_mode.postValue(EqMode.eq_normal)
                            }
                            1u ->{
                                eq_mode.postValue(EqMode.eq_pop)
                            }
                            2u ->{
                                eq_mode.postValue(EqMode.eq_classical)
                            }
                            3u ->{
                                eq_mode.postValue(EqMode.eq_rock)
                            }
                        }
                    }
                    8u -> {
                        // game mode
                        game_mode.postValue(ch == 0x01u)
                    }
                    208u -> {
                        // battery
                        battery.postValue(ch.toInt())
                    }
                    72u -> {
                        // LDAC
                        when(ch){
                            0u ->{
                                ldac_mode.postValue(LdacMode.ldac_off)
                            }
                            1u ->{
                                ldac_mode.postValue(LdacMode.ldac_48k)
                            }
                            2u ->{
                                ldac_mode.postValue(LdacMode.ldac_96k)
                            }
                        }
                    }
                    5u -> {
                        // PV
                        prompt_volume.postValue(ch.toInt())
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
                        val address = byteArray.sliceArray(3 until byteArray.size)
                        val stringArray = ArrayList<String>()
                        for (b in address){
                            stringArray.add(String.format("%02X", b))
                        }
                        mac.postValue(stringArray.joinToString(separator = ":"))

                    }
                    cmd == 198u && len == 4u -> {
                        // 固件
                        firmware.postValue(byteArray.sliceArray(3 until byteArray.size).joinToString(separator = "."))
                    }
                    cmd == 204u && len == 3u -> {
                        // asv
                        val mode = byteArray[3].toUInt() and 0xFFu
                        val volume = ((byteArray[4].toUInt() and 0xFFu) - 6u).toInt()
                        when(mode){
                            0x01u ->{
                                noise_mode.postValue(NoiseMode.noise_normal)
                            }
                            0x02u ->{
                                noise_mode.postValue(NoiseMode.noise_reduction)
                            }
                            0x03u ->{
                                noise_mode.postValue(NoiseMode.noise_ambient)
                            }
                        }
                        as_volume.postValue(volume)
                    }
                    cmd == 201u -> {
                        // name
                        device_name.postValue(String(byteArray.sliceArray(3 until byteArray.size), Charsets.UTF_8))
                    }
                    cmd == 240u && len == 3u && (byteArray[3].toUInt() and 0xFFu ) == 10u -> {
                        // 可选降噪
                        val mode = byteArray[4].toUInt() and 0xFFu
                        val selectable_modes = arrayListOf((mode and 2u) == 2u, (mode and 1u) == 1u, (mode and 4u) == 4u)
                        val selectableNoiseMode = if(selectable_modes[0] && selectable_modes[1] && selectable_modes[2]){
                            SelectableNoiseMode.all
                        } else if(!selectable_modes[0] && selectable_modes[1] && selectable_modes[2]){
                            SelectableNoiseMode.no_reduction
                        } else if(selectable_modes[0] && !selectable_modes[1] && selectable_modes[2]){
                            SelectableNoiseMode.no_normal
                        } else{
                            SelectableNoiseMode.no_ambient
                        }
                        selectable_noise_modes.postValue(selectableNoiseMode)

                    }
                    cmd == 179u && len == 3u -> {
                        // shut down
                        shutdown_time.postValue(byteArray[4].toInt())
                    }
                }
            }
        }
    }
}