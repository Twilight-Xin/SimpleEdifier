package com.twilight.simpleedifier.ui

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.twilight.simpleedifier.device.EdifierDevice
import com.twilight.simpleedifier.device.EdifierDevice.Companion.SelectableNoiseMode.*

class EdifierViewModel: ViewModel() {
    private var edifier_device:EdifierDevice? = null

    private val not_apply_selectable_noise_modes:MutableLiveData<ArrayList<Boolean>> = MutableLiveData(
        arrayListOf(true, true, true)
    )
    private val is_edifier_device_set = MutableLiveData(false)
    private val not_apply_AS_volume = MutableLiveData(0)
    private val not_apply_prompt_volume = MutableLiveData(0)

    fun setEdifierDevice(edifierDevice: EdifierDevice){
        edifier_device = edifierDevice
        flushNotApply()

        is_edifier_device_set.postValue(true)
    }

    fun flushNotApply(){
        if(edifier_device != null) {
            not_apply_prompt_volume.postValue(edifier_device?.getPromptVolume()?.value ?: 0)
            not_apply_AS_volume.postValue(edifier_device?.getASVolume()?.value ?: 0)
            val array = when (edifier_device?.getSelectableNoiseMode()?.value) {
                all -> {
                    arrayListOf(true, true, true)
                }

                no_ambient -> {
                    arrayListOf(true, true, false)
                }

                no_normal -> {
                    arrayListOf(true, false, true)
                }

                no_reduction -> {
                    arrayListOf(false, true, true)
                }

                null -> {
                    arrayListOf(true, true, true)
                }
            }
            not_apply_selectable_noise_modes.postValue(array)
        }
    }

    fun clearEdifierDevice(){
        is_edifier_device_set.postValue(false)
        edifier_device = null
    }

    fun isEdifierDeviceSet(): LiveData<Boolean>{
        return is_edifier_device_set
    }

    fun getDevice(): LiveData<BluetoothDevice>{
        return edifier_device?.getDevice() ?: MutableLiveData()
    }

    fun setDevice(device: BluetoothDevice){
        edifier_device?.setDevice(device)
    }

    fun getNoiseMode():LiveData<EdifierDevice.Companion.NoiseMode>{
        return  edifier_device?.getNoiseMode() ?: MutableLiveData(EdifierDevice.Companion.NoiseMode.noise_reduction)
    }

    fun setNoiseMode(mode:EdifierDevice.Companion.NoiseMode){
        edifier_device?.setNoiseMode(mode)
    }

    fun  getGameMode():LiveData<Boolean>{
        return edifier_device?.getGameMode() ?: MutableLiveData(false)
    }

    fun setGameMode(mode: Boolean){
        edifier_device?.setGameMode(mode)
    }

    fun getLdacMode():LiveData<EdifierDevice.Companion.LdacMode>{
        return edifier_device?.getLdacMode() ?: MutableLiveData(EdifierDevice.Companion.LdacMode.ldac_48k)
    }

    fun getEqMode():LiveData<EdifierDevice.Companion.EqMode>{
        return edifier_device?.getEqMode() ?: MutableLiveData(EdifierDevice.Companion.EqMode.eq_normal)
    }

    fun isConnected():LiveData<Boolean>{
        return edifier_device?.isConnected() ?: MutableLiveData(false)
    }

    fun setConnected(connected:Boolean){
        edifier_device?.setConnected(connected)
    }

    fun getBattery():LiveData<Int>{
        return edifier_device?.getBattery() ?: MutableLiveData(0)
    }

    fun getMac():LiveData<String>{
        return edifier_device?.getMac() ?: MutableLiveData("")
    }

    fun getFirmware():LiveData<String>{
        return edifier_device?.getFirmware() ?: MutableLiveData("")
    }

    fun getSelectableNoiseMode():LiveData<EdifierDevice.Companion.SelectableNoiseMode>{
        return edifier_device?.getSelectableNoiseMode() ?: MutableLiveData(all)
    }


    fun getNotApplySelectableNoiseMode():LiveData<ArrayList<Boolean>>{
        return not_apply_selectable_noise_modes
    }

    fun setNotApplySelectableNoiseMode(list:ArrayList<Boolean>){
        not_apply_selectable_noise_modes.postValue(list)
    }

    fun getShutDownTime():LiveData<Int>{
        return edifier_device?.getShutDownTime() ?: MutableLiveData(0)
    }

    fun getASVolume():LiveData<Int>{
        return edifier_device?.getASVolume() ?: MutableLiveData(0)
    }

    fun getNotApplyASVolume():LiveData<Int>{
        return not_apply_AS_volume
    }


    fun setNotApplyASVolume(volume: Int){
        not_apply_AS_volume.postValue(volume)
    }

    fun getPromptVolume():LiveData<Int>{
        return edifier_device?.getPromptVolume() ?: MutableLiveData(0)
    }

    fun getNotApplyPromptVolume():LiveData<Int>{
        return not_apply_prompt_volume
    }


    fun setNotApplyPromptVolume(volume: Int){
        not_apply_prompt_volume.postValue(volume)
    }


    fun setData(byteArray: ByteArray){
        edifier_device?.setData(byteArray)
    }
}