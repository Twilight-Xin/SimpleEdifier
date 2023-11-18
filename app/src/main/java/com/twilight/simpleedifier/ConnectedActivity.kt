package com.twilight.simpleedifier

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.asFlow
import com.twilight.simpleedifier.ui.theme.SimpleEdifierTheme

class ConnectedActivity : AppCompatActivity() {
    companion object{

    }
    private var connected = false
    private lateinit var viewModel: EdifierViewModel
    private lateinit var connectDevice: ConnectDevice
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // view model
        val model:EdifierViewModel by viewModels()
        viewModel = model

        val mac = intent.getStringExtra(ConnectDevice.device_mac)
        val isBle = intent.getBooleanExtra(ConnectDevice.isBLE, false)
        if (mac !=null){
            connect(mac, isBle)
        }else{
            Toast.makeText(this, "蓝牙MAC为空", Toast.LENGTH_LONG).show()
            val back_intent = Intent(this, ScanActivity::class.java)
            startActivity(back_intent)
            finish()
        }

        waitForConnect()

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
                readSettings()
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

    private fun connect(mac:String, isBle:Boolean){
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val device = bluetoothManager.adapter.getRemoteDevice(mac)
        viewModel.setDevice(device)
        if(isBle) {
            connectDevice = ConnectBleDevice(this, device, viewModel)
        }
    }

    private fun disconnect(){
        connectDevice.close()
        val intent = Intent(this, ScanActivity::class.java)
        intent.putExtra(ScanActivity.connect_fail, true)
        startActivity(intent)
        finish()
    }

    private fun readSettings(){
        Thread{
            connectDevice.write(getString(R.string.cmd_read_battery))
            Thread.sleep(150)
            connectDevice.write(getString(R.string.cmd_read_noise))
            Thread.sleep(150)
            connectDevice.write(getString(R.string.cmd_read_as_settings))
            Thread.sleep(150)
            connectDevice.write(getString(R.string.cmd_read_game_mode))
            Thread.sleep(150)
            connectDevice.write(getString(R.string.cmd_read_mac))
            Thread.sleep(150)
            connectDevice.write(getString(R.string.cmd_read_ldac))
            Thread.sleep(150)
            connectDevice.write(getString(R.string.cmd_read_eq))
            //
        }.start()
    }

    // call back

    private val noiseModeCallback: TripleButtonCallback = object:TripleButtonCallback{
        override fun third() { // surround
            val cmd = getString(R.string.cmd_noise_ambient)
            if(connectDevice.write(cmd)){
                viewModel.setNoiseMode(EdifierViewModel.noise_ambient)
            }
        }

        override fun second() { // standard
            val cmd = getString(R.string.cmd_noise_normal)
            if(connectDevice.write(cmd)){
                viewModel.setNoiseMode(EdifierViewModel.noise_normal)
            }
        }

        override fun first() { // noise
            val cmd = getString(R.string.cmd_noise_reduction)
            if(connectDevice.write(cmd)){
                viewModel.setNoiseMode(EdifierViewModel.noise_reduction)
            }
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
            val cmd = getString(R.string.cmd_control_settings)
            var full_cmd = ""
            if(array != null && array.size == 3){
                if(array[0] && array[1] && array[2]){
                    full_cmd = cmd + "07"
                }
                else if(array[1] && array[2]){
                    full_cmd = cmd + "05"
                }
                else if(array[0] && array[2]){
                    full_cmd = cmd + "06"
                }
                else if(array[0] && array[1]){
                    full_cmd = cmd + "03"
                }
            }
            if(full_cmd != ""){
                connectDevice.write(full_cmd)
            }
        }
    }

    private fun gameModeCallback(current: Boolean) {
        val cmd = if(current){
            getString(R.string.cmd_game_on)
        }else{
            getString(R.string.cmd_game_off)
        }
        if(connectDevice.write(cmd)){
            viewModel.setGameMode(current)
        }
    }

    private val pc_control_callback = object {
        fun musicPrev(){
            connectDevice.write(getString(R.string.cmd_pc_prev))
        }

        fun musicNext(){
            connectDevice.write(getString(R.string.cmd_pc_next))
        }

        fun musicPlay(){
            connectDevice.write(getString(R.string.cmd_pc_play))
        }

        fun musicPause(){
            connectDevice.write(getString(R.string.cmd_pc_pause))
        }

        fun volumeUp(){
            connectDevice.write(getString(R.string.cmd_pc_volume_up))
        }

        fun volumeDown(){
            connectDevice.write(getString(R.string.cmd_pc_volume_down))
        }
    }

    private fun powerOffCallback(){
        connectDevice.write(getString(R.string.cmd_power_off))
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
            arrayListOf(getString(R.string.noise_mode), getString(R.string.standard_mode), getString(R.string.surround_mode))
        }
        val noise_mode = viewModel.getNoiseMode().asFlow().collectAsState(EdifierViewModel.noise_ambient)
        val booleanArray = when (noise_mode.value) {
            EdifierViewModel.noise_ambient -> {
                arrayListOf(false, false, true)
            }
            EdifierViewModel.noise_normal -> {
                arrayListOf(false, true, false)
            }
            else -> {
                arrayListOf(true, false, false)
            }
        }
        TripleButton(labelList = label, booleanArray, tripleButtonCallback = noiseModeCallback)
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
            arrayListOf(getString(R.string.noise_mode), getString(R.string.standard_mode), getString(R.string.surround_mode))
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

            Spacer(modifier = Modifier.height(50.dp))

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

            Spacer(modifier = Modifier.height(50.dp))

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
                    val text = remember {
                        getString(R.string.set)
                    }
                    Text(text = text, color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Spacer(modifier = Modifier.height(50.dp))

            Row(verticalAlignment = Alignment.CenterVertically){
                Switch(checked = false, onCheckedChange = {})
                Text(text = "game mode", color = MaterialTheme.colorScheme.primary)
            }

            Button(onClick = ::powerOffCallback) {
                Text(text = "text", color = MaterialTheme.colorScheme.onPrimary)
            }

            Button({}){
                Text(text = "disconnect", color = MaterialTheme.colorScheme.onPrimary)
            }


        }

    }

    @Composable
    fun ConnectedActivityUI(viewModel: EdifierViewModel){
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BatteryUi(viewModel = viewModel)
            NoiseModeUi(viewModel = viewModel)
            Spacer(modifier = Modifier.height(50.dp))
            PcControlUi()
            Spacer(modifier = Modifier.height(50.dp))
            SelectableNoiseModeControlUi(viewModel = viewModel)
            Spacer(modifier = Modifier.height(50.dp))
            GameModeUi(viewModel = viewModel)
            PowerOffUi()
            DisconnectUi()
        }

    }
}