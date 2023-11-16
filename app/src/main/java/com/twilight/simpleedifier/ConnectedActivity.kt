package com.twilight.simpleedifier

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.asFlow
import com.twilight.simpleedifier.ui.theme.SimpleEdifierTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.toCollection

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
                ConnectedActivityUI()
            }
        }
    }

    private fun waitForConnect(){
        viewModel.isConnected().observe(this){
            if(it){
                Toast.makeText(this, getString(R.string.connect_success), Toast.LENGTH_LONG).show()
                connected = true
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
        val intent = Intent(this, ScanActivity::class.java)
        intent.putExtra(ScanActivity.connect_fail, true)
        startActivity(intent)
        finish()
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
            Text(text = labelList[0])

            RadioButton(status[1], onClick = tripleButtonCallback::second)
            Text(text = labelList[1])

            RadioButton(status[2], onClick = tripleButtonCallback::third)
            Text(text = labelList[2])
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

    @Preview
    @Composable
    fun PreviewTripleButton(){
        TripleButton(
            labelList = arrayListOf("noise mode", "standard mode", "surround mode"),
            arrayListOf(true, false, false),
            tripleButtonCallback = noiseModeCallback
        )
    }

    @Composable
    fun ConnectedActivityUI(){
        NoiseModeUi(viewModel = viewModel)
    }
}