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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.lifecycle.asFlow
import com.twilight.simpleedifier.R
import com.twilight.simpleedifier.ScanActivity
import com.twilight.simpleedifier.connect.ConnectDevice
import com.twilight.simpleedifier.device.EdifierDevice
import com.twilight.simpleedifier.service.ConnectService
import com.twilight.simpleedifier.ui.theme.SimpleEdifierTheme
import java.util.Objects
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
                Thread {
                    Thread.sleep(2000)
                    viewModel.flushNotApply()
                }.start()
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

    private val noiseModeCallback: MulButtonCallback = object: MulButtonCallback {
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

    private val EqModeCallback: MulButtonCallback = object: MulButtonCallback {
        override fun first() {
            service?.setEqMode(EdifierDevice.Companion.EqMode.eq_normal)
        }

        override fun second() {
            service?.setEqMode(EdifierDevice.Companion.EqMode.eq_classical)
        }

        override fun third() {
            service?.setEqMode(EdifierDevice.Companion.EqMode.eq_pop)
        }

        override fun fourth() {
            service?.setEqMode(EdifierDevice.Companion.EqMode.eq_rock)
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

    interface MulButtonCallback{
        fun first(){}
        fun second(){}
        fun third(){}
        fun fourth(){}
    }

    @Composable
    fun TripleButton(labelList:List<String>, status:ArrayList<Boolean>, mulButtonCallback: MulButtonCallback){
        if (labelList.size < 3 || status.size < 3){
            return
        }

        Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth(1f), verticalAlignment = Alignment.CenterVertically){
            RadioButton(status[0], onClick = mulButtonCallback::first)
            Text(text = labelList[0], color = MaterialTheme.colorScheme.primary)

            RadioButton(status[1], onClick = mulButtonCallback::second)
            Text(text = labelList[1], color = MaterialTheme.colorScheme.primary)

            RadioButton(status[2], onClick = mulButtonCallback::third)
            Text(text = labelList[2], color = MaterialTheme.colorScheme.primary)
        }
    }

    @Composable
    fun NameUi(name:State<String>){
        Text(text = name.value, color = MaterialTheme.colorScheme.primary)
    }

    @Composable
    fun NoiseModeUi(noiseMode: State<EdifierDevice.Companion.NoiseMode>){
        val label = if(!LocalInspectionMode.current) {
            remember {
                arrayListOf(
                    getString(R.string.noise_mode), getString(R.string.standard_mode), getString(
                        R.string.surround_mode
                    )
                )
            }
        } else {
            arrayListOf("Noise Mode", "Strand Mode", "Surround Mode")
        }

        val booleanArray = when (noiseMode.value) {
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
        TripleButton(labelList = label, booleanArray, mulButtonCallback = noiseModeCallback)
    }

    @Composable
    fun EqUi(eqMode: State<EdifierDevice.Companion.EqMode>){
        val labelList = if(!LocalInspectionMode.current) {
            remember {
                arrayListOf(
                    getString(R.string.standard_eq),
                    getString(R.string.classical_eq),
                    getString(R.string.pop_eq),
                    getString(R.string.rock_eq)
                )
            }
        } else {
            arrayListOf("Standard Mode", "classical", "pop", "rock")
        }
        val status = arrayListOf(false, false, false, false)
        when(eqMode.value){
            EdifierDevice.Companion.EqMode.eq_normal ->{
                status[0] = true
            }
            EdifierDevice.Companion.EqMode.eq_classical ->{
                status[1] = true
            }
            EdifierDevice.Companion.EqMode.eq_pop ->{
                status[2] = true
            }
            EdifierDevice.Companion.EqMode.eq_rock ->{
                status[3] = true
            }
        }
        Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth(1f), verticalAlignment = Alignment.CenterVertically){
            RadioButton(status[0], onClick = EqModeCallback::first)
            Text(text = labelList[0], color = MaterialTheme.colorScheme.primary)

            RadioButton(status[1], onClick = EqModeCallback::second)
            Text(text = labelList[1], color = MaterialTheme.colorScheme.primary)

            RadioButton(status[2], onClick = EqModeCallback::third)
            Text(text = labelList[2], color = MaterialTheme.colorScheme.primary)

            RadioButton(status[3], onClick = EqModeCallback::fourth)
            Text(text = labelList[3], color = MaterialTheme.colorScheme.primary)
        }

    }

    @Composable
    fun AmbientVolume(noiseMode:State<EdifierDevice.Companion.NoiseMode>, volume: State<Int>){
        if(noiseMode.value == EdifierDevice.Companion.NoiseMode.noise_ambient || LocalInspectionMode.current) {
            val text = if (!LocalInspectionMode.current) remember {
                getString(R.string.ambient_sound_volume)
            } else {
                "Ambient Sound Volume"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = (volume).value.toFloat(),
                    onValueChange = ::aSVolumeCallback,
                    steps = 5,
                    valueRange = -3f..3f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                )
                Button(onClick = ::setAsVolumeCallback, modifier = Modifier.padding(10.dp)) {
                    Text(text)
                }
            }
        }
    }
    @Composable
    fun PromptVolume(volume: State<Int>){
        val text = if(!LocalInspectionMode.current) {
            remember {
                getString(R.string.prompt_volume)
            }
        } else {
            "Prompt Volume"
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = (volume).value.toFloat(),
                onValueChange = ::promptVolumeCallback,
                steps = 14,
                valueRange = 0f..15f,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
            )
            Button(onClick = ::setPromptVolumeCallback,modifier = Modifier.padding(10.dp)) {
                Text(text)
            }
        }
    }

    @Composable
    fun GameModeUi(gameMode: State<Boolean>){
        Row(verticalAlignment = Alignment.CenterVertically){
            Switch(checked = gameMode.value, onCheckedChange = ::gameModeCallback)
            val game_mode = if(!LocalInspectionMode.current) {
                remember {
                    getString(R.string.game_mode)
                }
            } else {
                "Game Mode"
            }
            Text(text = game_mode, color = MaterialTheme.colorScheme.primary)
        }
    }
    
    @Composable
    fun BatteryUi(percent:State<Int>){
        val text = if(!LocalInspectionMode.current) {
            remember {
                getString(R.string.current_battery)
            }
        } else {
            "Battery"
        }
        val show_text = text + String.format("%d %%", percent.value)
        Text(text = show_text, color = MaterialTheme.colorScheme.primary)
    }

    @Composable
    fun PowerOffUi(){
        Button(onClick = ::powerOffCallback) {
            val text = if(!LocalInspectionMode.current) {
                remember {
                    getString(R.string.power_off)
                }
            } else{
                "Power Off"
            }
            Text(text = text, color = MaterialTheme.colorScheme.onPrimary)
        }
    }

    @Composable
    fun PcControlUi(){
        val list = if(!LocalInspectionMode.current) {
            remember {
                arrayListOf(
                    getString(R.string.pc_prev),
                    getString(R.string.pc_play),
                    getString(R.string.pc_next),
                    getString(R.string.pc_volume_up),
                    getString(R.string.pc_pause),
                    getString(R.string.pc_volume_down)
                )
            }
        } else {
            arrayListOf("Prev", "Play", "Next", "Vol Up", "Pause", "Vol down")
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
    fun SelectableNoiseModeControlUi(status:State<ArrayList<Boolean>>){

        val label = if(!LocalInspectionMode.current) {
            remember {
                arrayListOf(
                    getString(R.string.noise_mode), getString(R.string.standard_mode), getString(
                        R.string.surround_mode
                    )
                )
            }
        } else{
            arrayListOf("Noise Mode", "Stand Mode", "Surround Mode")
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
                val text = if(!LocalInspectionMode.current) {
                    remember {
                        getString(R.string.set)
                    }
                } else {
                    "Set"
                }
                Text(text = text, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }

    @Composable
    fun DisconnectUi(){
        Button(onClick = ::disconnect) {
            val text = if(!LocalInspectionMode.current) {
                remember {
                    getString(R.string.disconnect)
                }
            } else {
                "Disconnect"
            }
            Text(text = text, color = MaterialTheme.colorScheme.onPrimary)
        }
    }

    @Composable
    fun FlushNotApplyUI(){
        val text = if(!LocalInspectionMode.current) {
            remember {
                getString(R.string.flush_data)
            }
        } else {
            "Flush Data"
        }
        Button(::flushDataCallback){
            Text(text)
        }
    }

    @Preview
    @Composable
    fun PreviewActivityUI(){
        ConnectedActivityUI(viewModel = EdifierViewModel())
    }

    @Composable
    fun ConnectedActivityUI(viewModel: EdifierViewModel) {
        val edifierDevice = if (!LocalInspectionMode.current) {
            viewModel.isEdifierDeviceSet().asFlow().collectAsState(initial = false)
        } else {
            remember {
                mutableStateOf(true)
            }
        }
        if (edifierDevice.value) {
            ConstraintLayout(modifier = Modifier.fillMaxSize(1f)) {
                val (nameRef, batteryRef, noiseRef, ambientRef, pcRef, eqRef, promptRef, selectableRef, gameRef, disconnectRef, flushRef, poweroffRef) = remember {
                    createRefs()
                }
                Box(Modifier.constrainAs(nameRef){
                    top.linkTo(parent.top)
                    bottom.linkTo(batteryRef.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }){
                    val name = viewModel.getName().asFlow().collectAsState(initial = "Unknown")
                    NameUi(name)
                }

                Box(modifier = Modifier.constrainAs(batteryRef) {
                    top.linkTo(nameRef.bottom)
                    bottom.linkTo(noiseRef.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }) {
                    val battery_percent =
                        viewModel.getBattery().asFlow().collectAsState(initial = 0)
                    BatteryUi(battery_percent)
                }

                val noise_mode = viewModel.getNoiseMode().asFlow()
                    .collectAsState(EdifierDevice.Companion.NoiseMode.noise_reduction)
                Box(modifier = Modifier.constrainAs(noiseRef) {
                    top.linkTo(batteryRef.bottom)
                    bottom.linkTo(ambientRef.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }) {
                    NoiseModeUi(noise_mode)
                }

                Box(Modifier.constrainAs(ambientRef) {
                    top.linkTo(noiseRef.bottom)
                    bottom.linkTo(eqRef.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }) {
                    val ambient_volume =
                        viewModel.getNotApplyASVolume().asFlow().collectAsState(initial = 0)
                    AmbientVolume(noise_mode, ambient_volume)
                }

                Box(Modifier.constrainAs(eqRef){
                    top.linkTo(ambientRef.bottom)
                    bottom.linkTo(pcRef.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }){
                    val eq_mode = viewModel.getEqMode().asFlow().collectAsState(initial = EdifierDevice.Companion.EqMode.eq_normal)
                    EqUi(eqMode = eq_mode)
                }


                Box(Modifier.constrainAs(pcRef) {
                    top.linkTo(eqRef.bottom)
                    bottom.linkTo(promptRef.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }) { PcControlUi() }

                Box(Modifier.constrainAs(promptRef) {
                    top.linkTo(pcRef.bottom)
                    bottom.linkTo(selectableRef.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }) {
                    val prompt_volume =
                        viewModel.getNotApplyPromptVolume().asFlow().collectAsState(initial = 0)
                    PromptVolume(prompt_volume)
                }

                Box(Modifier.constrainAs(selectableRef) {
                    top.linkTo(promptRef.bottom)
                    bottom.linkTo(gameRef.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }) {
                    val noise_mode_control_status =
                        viewModel.getNotApplySelectableNoiseMode().asFlow()
                            .collectAsState(initial = arrayListOf(true, true, true))
                    SelectableNoiseModeControlUi(noise_mode_control_status)
                }

                Box(Modifier.constrainAs(gameRef) {
                    top.linkTo(selectableRef.bottom)
                    bottom.linkTo(disconnectRef.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }) {
                    val gameMode =
                        viewModel.getGameMode().asFlow().collectAsState(initial = false)
                    GameModeUi(gameMode)
                }

                Box(Modifier.constrainAs(disconnectRef) {
                    top.linkTo(gameRef.bottom)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(flushRef.start)
                }) { DisconnectUi() }

                Box(Modifier.constrainAs(flushRef) {
                    top.linkTo(gameRef.bottom)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(disconnectRef.end)
                    end.linkTo(poweroffRef.start)
                }) {
                    FlushNotApplyUI()
                }

                Box(Modifier.constrainAs(poweroffRef) {
                    top.linkTo(gameRef.bottom)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(flushRef.end)
                    end.linkTo(parent.end)
                }) {
                    PowerOffUi()
                }

            }

        }
    }
}