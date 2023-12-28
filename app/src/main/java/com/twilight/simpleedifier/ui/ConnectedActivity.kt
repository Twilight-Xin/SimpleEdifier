package com.twilight.simpleedifier.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.asFlow
import com.twilight.simpleedifier.R
import com.twilight.simpleedifier.ScanActivity
import com.twilight.simpleedifier.connect.ConnectDevice
import com.twilight.simpleedifier.device.EdifierDevice
import com.twilight.simpleedifier.service.ConnectService
import com.twilight.simpleedifier.ui.theme.SimpleEdifierTheme
import kotlin.math.roundToInt

class ConnectedActivity : AppCompatActivity() {
    companion object{

    }
    private var connected = false
    private lateinit var viewModel: EdifierViewModel
    private var service: ConnectService? = null
    private val requsetPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()){

    }

    val serviceCallback: ServiceConnection = object: ServiceConnection{
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as ConnectService.ConnectServiceBinder
            service = binder.getService()
            service?.let { viewModel.setEdifierDevice(it.getEdifierDevice()) }
            waitForConnect()
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            service = null
            viewModel.clearEdifierDevice()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // view model
        val model: EdifierViewModel by viewModels()
        viewModel = model

        val mac = intent.getStringExtra(ConnectDevice.device_mac)
        val supportBle = intent.getBooleanExtra(ConnectDevice.isBLE, false)

        // bind
        if(mac != null) {
            val service_intent = Intent(this, ConnectService::class.java)
            service_intent.putExtra(ConnectDevice.device_mac, mac)
            service_intent.putExtra(ConnectDevice.isBLE, supportBle)
            if(Build.VERSION.SDK_INT >= 33 && (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
                        )) {
                requsetPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }else{
                startForegroundService(service_intent)
            }
            bindService(service_intent, serviceCallback, Context.BIND_AUTO_CREATE)
        }

        // UI
        setContent {
            SimpleEdifierTheme {
                ConnectedActivityUI(viewModel)
            }
        }
    }

    private fun waitForConnect(){
        viewModel.isConnected().observe(this){
            if(it){
                Toast.makeText(this, getString(R.string.connect_success), Toast.LENGTH_LONG).show()
                connected = true
                service?.readSettings()
            }else{
                if(connected) {
                    Toast.makeText(this, getString(R.string.disconnect), Toast.LENGTH_LONG).show()
                    disconnect()
                }
            }
        }
        Thread{
            Thread.sleep(5000)
            if(!connected){
                disconnect()
            }
        }.start()
    }

    private fun disconnect(){
        service?.disconnect()
        val intent = Intent(this, ScanActivity::class.java)
        intent.putExtra(ScanActivity.connect_fail, true)
        startActivity(intent)
        finish()
    }


    // call back

    private val noiseModeCallback: TripleButtonCallback = object: TripleButtonCallback {
        override fun third() { // surround
            service?.setNoiseMode(EdifierDevice.Companion.NoiseMode.noise_ambient)
        }

        override fun second() { // standard
            service?.setNoiseMode(EdifierDevice.Companion.NoiseMode.noise_normal)
        }

        override fun first() { // noise
            service?.setNoiseMode(EdifierDevice.Companion.NoiseMode.noise_reduction)
        }

    }

    private val pc_control_callback = object {
        fun musicPrev(){
            service?.musicPrev()
        }

        fun musicNext(){
            service?.musicNext()
        }

        fun musicPlay(){
            service?.musicPlay()
        }

        fun musicPause(){
            service?.musicPause()
        }

        fun volumeUp(){
            service?.volumeUp()
        }

        fun volumeDown(){
            service?.volumeDown()
        }
    }

    private val selectable_noise_modes_callback = object {
        fun changeStatus(index:Int, checked:Boolean){
            val array = viewModel.getNotApplySelectableNoiseMode().value
            if(array != null) {
                val new_array = ArrayList<Boolean>()
                new_array.addAll(array)
                new_array[index] = checked
                viewModel.setNotApplySelectableNoiseMode(new_array)
            }
        }

        fun first(checked:Boolean){
            changeStatus(0, checked)
        }

        fun second(checked:Boolean){
            changeStatus(1, checked)
        }

        fun third(checked:Boolean){
            changeStatus(2, checked)
        }

        fun set(){
            val array = viewModel.getNotApplySelectableNoiseMode().value
            var mode:EdifierDevice.Companion.SelectableNoiseMode? = null
            if(array != null && array.size == 3){
                if(array[0] && array[1] && array[2]){
                    mode = EdifierDevice.Companion.SelectableNoiseMode.all
                }
                else if(array[1] && array[2]){
                    mode = EdifierDevice.Companion.SelectableNoiseMode.no_reduction
                }
                else if(array[0] && array[2]){
                    mode = EdifierDevice.Companion.SelectableNoiseMode.no_normal
                }
                else if(array[0] && array[1]){
                    mode = EdifierDevice.Companion.SelectableNoiseMode.no_ambient
                }
            }
            if(mode != null){
                service?.setSelectableNoiseMode(mode)
            }
        }
    }

    private fun gameModeCallback(current: Boolean) {
        service?.setGameMode(current)
    }

    private fun powerOffCallback(){
        service?.powerOff()
    }

    private fun aSVolumeCallback(volume: Float) {
        viewModel.setNotApplyASVolume(volume.roundToInt())
    }

    private fun setAsVolumeCallback(){
        val volume = viewModel.getNotApplyASVolume().value ?: 0
        service?.setAmbientVolume(volume)
    }

    private fun promptVolumeCallback(volume: Float){
        viewModel.setNotApplyPromptVolume(volume.roundToInt())
    }

    private fun setPromptVolumeCallback(){
        val volume = viewModel.getNotApplyPromptVolume().value ?: 0
        service?.setPromptVolume(volume)
    }

    private fun flushDataCallback(){
        service?.readSettings()
        viewModel.flushNotApply()
    }

    // UI

    interface TripleButtonCallback{
        fun first()
        fun second()
        fun third()
    }

    @Composable
    fun TripleButton(labelList:List<String>,status:ArrayList<Boolean> , tripleButtonCallback: TripleButtonCallback){
        if (labelList.size < 3 || status.size < 3){
            return
        }

        Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth(1f), verticalAlignment = Alignment.CenterVertically){
            RadioButton(status[0], onClick = tripleButtonCallback::first)
            Text(text = labelList[0], color = MaterialTheme.colorScheme.primary)

            RadioButton(status[1], onClick = tripleButtonCallback::second)
            Text(text = labelList[1], color = MaterialTheme.colorScheme.primary)

            RadioButton(status[2], onClick = tripleButtonCallback::third)
            Text(text = labelList[2], color = MaterialTheme.colorScheme.primary)
        }
    }

    @Composable
    fun NoiseModeUi(viewModel: EdifierViewModel){
        val label = remember {
            arrayListOf(getString(R.string.noise_mode), getString(R.string.standard_mode), getString(
                R.string.surround_mode
            ))
        }
        val noise_mode = viewModel.getNoiseMode().asFlow().collectAsState(EdifierDevice.Companion.NoiseMode.noise_reduction)
        val booleanArray = when (noise_mode.value) {
            EdifierDevice.Companion.NoiseMode.noise_ambient -> {
                arrayListOf(false, false, true)
            }
            EdifierDevice.Companion.NoiseMode.noise_normal -> {
                arrayListOf(false, true, false)
            }
            else -> {
                arrayListOf(true, false, false)
            }
        }
        TripleButton(labelList = label, booleanArray, tripleButtonCallback = noiseModeCallback)
        if(noise_mode.value == EdifierDevice.Companion.NoiseMode.noise_ambient){
            AmbientVolume(viewModel = viewModel)
        }
    }

    @Composable
    fun AmbientVolume(viewModel: EdifierViewModel){
        val text = remember {
            getString(R.string.ambient_sound_volume)
        }
        val volume = viewModel.getNotApplyASVolume().asFlow().collectAsState(initial = 0)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = (volume).value.toFloat(),
                onValueChange = ::aSVolumeCallback,
                steps = 5,
                valueRange = -3f..3f,
                modifier = Modifier.weight(1f).padding(start = 10.dp)
            )
            Button(onClick = ::setAsVolumeCallback, modifier = Modifier.padding(10.dp)) {
                Text(text)
            }
        }
    }
    @Composable
    fun PromptVolume(viewModel: EdifierViewModel){
        val text = remember {
            getString(R.string.prompt_volume)
        }
        val volume = viewModel.getNotApplyPromptVolume().asFlow().collectAsState(initial = 0)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = (volume).value.toFloat(),
                onValueChange = ::promptVolumeCallback,
                steps = 14,
                valueRange = 0f..15f,
                modifier = Modifier.weight(1f).padding(start = 10.dp)
            )
            Button(onClick = ::setPromptVolumeCallback,modifier = Modifier.padding(10.dp)) {
                Text(text)
            }
        }
    }

    @Composable
    fun GameModeUi(viewModel: EdifierViewModel){
        val gameMode = viewModel.getGameMode().asFlow().collectAsState(initial = false)
        Row(verticalAlignment = Alignment.CenterVertically){
            Switch(checked = gameMode.value, onCheckedChange = ::gameModeCallback)
            val game_mode = remember {
                getString(R.string.game_mode)
            }
            Text(text = game_mode, color = MaterialTheme.colorScheme.primary)
        }

    }
    
    @Composable
    fun BatteryUi(viewModel: EdifierViewModel){
        val percent = viewModel.getBattery().asFlow().collectAsState(initial = 0)
        val text = remember {
            getString(R.string.current_battery)
        }
        val show_text = text + String.format("%d %%", percent.value)
        Text(text = show_text, color = MaterialTheme.colorScheme.primary)
    }

    @Composable
    fun PowerOffUi(){
        Button(onClick = ::powerOffCallback) {
            val text = remember {
                getString(R.string.power_off)
            }
            Text(text = text, color = MaterialTheme.colorScheme.onPrimary)
        }
    }

    @Composable
    fun PcControlUi(){
        val list = remember {
            arrayListOf(getString(R.string.pc_prev), getString(R.string.pc_play), getString(R.string.pc_next),
                getString(R.string.pc_volume_up), getString(R.string.pc_pause), getString(R.string.pc_volume_down)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally){
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = pc_control_callback::musicPrev) {
                    Text(text = list[0], color = MaterialTheme.colorScheme.onPrimary)
                }
                Button(onClick = pc_control_callback::musicPlay) {
                    Text(text = list[1], color = MaterialTheme.colorScheme.onPrimary)
                }
                Button(onClick = pc_control_callback::musicNext) {
                    Text(text = list[2], color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = pc_control_callback::volumeUp) {
                    Text(text = list[3], color = MaterialTheme.colorScheme.onPrimary)
                }
                Button(onClick = pc_control_callback::musicPause) {
                    Text(text = list[4], color = MaterialTheme.colorScheme.onPrimary)
                }
                Button(onClick = pc_control_callback::volumeDown) {
                    Text(text = list[5], color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

    }

    @Composable
    fun SelectableNoiseModeControlUi(viewModel: EdifierViewModel){
        val status = viewModel.getNotApplySelectableNoiseMode().asFlow().collectAsState(initial = arrayListOf(true, true, true))
        val label = remember {
            arrayListOf(getString(R.string.noise_mode), getString(R.string.standard_mode), getString(
                R.string.surround_mode
            ))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = status.value[0],
                    onCheckedChange = selectable_noise_modes_callback::first
                )
                Text(text = label[0], color = MaterialTheme.colorScheme.primary)
                Checkbox(
                    checked = status.value[1],
                    onCheckedChange = selectable_noise_modes_callback::second
                )
                Text(text = label[1], color = MaterialTheme.colorScheme.primary)
                Checkbox(
                    checked = status.value[2],
                    onCheckedChange = selectable_noise_modes_callback::third
                )
                Text(text = label[2], color = MaterialTheme.colorScheme.primary)
            }
            Button(onClick = selectable_noise_modes_callback::set) {
                val text = remember {
                    getString(R.string.set)
                }
                Text(text = text, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }

    @Composable
    fun DisconnectUi(){
        Button(onClick = ::disconnect) {
            val text = remember {
                getString(R.string.disconnect)
            }
            Text(text = text, color = MaterialTheme.colorScheme.onPrimary)
        }
    }

    @Composable
    fun FlushNotApplyUI(viewModel: EdifierViewModel){
        val text = remember{
            getString(R.string.flush_data)
        }
        Button(::flushDataCallback){
            Text(text)
        }
    }

    @Preview
    @Composable
    fun Preview(){
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("23%")
            
            TripleButton(
                labelList = arrayListOf("noise mode", "standard mode", "surround mode"),
                arrayListOf(true, false, false),
                tripleButtonCallback = noiseModeCallback
            )

            Row{
                Slider(
                    value = 2f,
                    onValueChange = ::aSVolumeCallback,
                    steps = 7,
                    valueRange = -3f..3f,
                    modifier = Modifier.width(250.dp)
                )
                Button(onClick = ::setAsVolumeCallback) {
                    Text("text")
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally){
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = pc_control_callback::musicPrev) {
                        Text(text = "list[0]", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Button(onClick = pc_control_callback::musicPlay) {
                        Text(text = "list[1]", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Button(onClick = pc_control_callback::musicNext) {
                        Text(text = "list[2]", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = pc_control_callback::volumeUp) {
                        Text(text = "list[3]", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Button(onClick = pc_control_callback::musicPause) {
                        Text(text = "list[4]", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Button(onClick = pc_control_callback::volumeDown) {
                        Text(text = "list[5]", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            Row {
                Slider(
                    value = 0f,
                    onValueChange = ::promptVolumeCallback,
                    steps = 14,
                    valueRange = 0f..15f,
                    modifier = Modifier.weight(1f).padding(start = 10.dp)
                )
                Button(onClick = ::setPromptVolumeCallback, modifier = Modifier.padding(end = 10.dp)) {
                    Text("text")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = true,
                        onCheckedChange = selectable_noise_modes_callback::first
                    )
                    Text(text = "label[0]")
                    Checkbox(
                        checked = true,
                        onCheckedChange = selectable_noise_modes_callback::second
                    )
                    Text(text = "label[1]")
                    Checkbox(
                        checked = true,
                        onCheckedChange = selectable_noise_modes_callback::third
                    )
                    Text(text = "label[2]")
                }
                Button(onClick = selectable_noise_modes_callback::set) {

                    Text(text = "set", color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Spacer(modifier = Modifier.height(50.dp))

            Row(verticalAlignment = Alignment.CenterVertically){
                Switch(checked = false, onCheckedChange = {})
                Text(text = "game mode", color = MaterialTheme.colorScheme.primary)
            }


            Row(verticalAlignment = Alignment.CenterVertically) {
                Button({}) {
                    Text(text = "disconnect", color = MaterialTheme.colorScheme.onPrimary)
                }
                Spacer(modifier = Modifier.width(20.dp))
                Button({}) {
                    Text(text = "disconnect", color = MaterialTheme.colorScheme.onPrimary)
                }

            }

            Button(onClick = ::powerOffCallback) {
                Text(text = "text", color = MaterialTheme.colorScheme.onPrimary)
            }

        }

    }

    @Composable
    fun ConnectedActivityUI(viewModel: EdifierViewModel){
        val edifierDevice = viewModel.isEdifierDeviceSet().asFlow().collectAsState(initial = false)
        if(edifierDevice.value) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BatteryUi(viewModel = viewModel)
                NoiseModeUi(viewModel = viewModel)
                Spacer(modifier = Modifier.height(20.dp))
                PcControlUi()
                PromptVolume(viewModel = viewModel)
                Spacer(modifier = Modifier.height(20.dp))
                SelectableNoiseModeControlUi(viewModel = viewModel)
                Spacer(modifier = Modifier.height(20.dp))
                GameModeUi(viewModel = viewModel)
                Row(verticalAlignment = Alignment.CenterVertically){
                    DisconnectUi()
                    Spacer(modifier = Modifier.width(10.dp))
                    FlushNotApplyUI(viewModel)
                }
                PowerOffUi()
            }
        }
    }
}