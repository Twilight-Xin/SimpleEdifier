package com.twilight.simpleedifier

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ConnectActivity : AppCompatActivity() {
    companion object{
        const val TAG = "connect activity"
    }

    // register for result
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
        var allPermission = true
        for(result in it.values){
            if(!result){
                allPermission = false
                break
            }
        }
        if(allPermission){
            scanLeDevice(true)
        }
    }
    private val openBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if(it.resultCode == RESULT_OK){
            startScanWithoutOpen()
        }
    }

    // view
    private lateinit var button:FloatingActionButton
    private lateinit var scan_list:RecyclerView
    private val scan_result = ArrayList<BluetoothDevice>()
    private lateinit var scan_list_adapter: ScanResultAdapter
    private lateinit var mContext: Context


    //bluetooth
    private var supportBLE:Boolean? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanning = false
    private val scanCallback = object: ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let { addScannedDevices(it.device) }
        }


        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            if (results != null) {
                for(i in results){
                    addScannedDevices(i.device)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d(TAG, "onScanFailed: $errorCode")
        }
    }

    // functions


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = this

        // view
        setContentView(R.layout.activity_connect)
        // button
        button = findViewById(R.id.scan_button)
        button.setOnClickListener{
            if(scanning){
                scanLeDevice(false)
            }else {
                startScan()
            }
        }

        // recyclerView
        scan_list = findViewById(R.id.scan_list)
        val context = this
        scan_list_adapter = ScanResultAdapter(scan_result, object:ScanResultAdapter.HandleClickCallback{
            override fun handleBluetoothDevice(device: BluetoothDevice) {
                connect(device)
                Toast.makeText(context, device.address, Toast.LENGTH_LONG).show()
            }
        })
        scan_list.adapter = scan_list_adapter
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = RecyclerView.VERTICAL
        scan_list.layoutManager = layoutManager



        // bluetooth
        supportBLE = packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    }

    private fun startScan(){
        if(bluetoothAdapter == null || !(bluetoothAdapter!!.isEnabled)){
            openBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
        else{
            startScanWithoutOpen()
        }
    }

    private fun startScanWithoutOpen(){
        Thread {
            if (supportBLE == true) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        requestPermission.launch(
                            arrayOf(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    } else {
                        scanLeDevice(true)
                    }
                } else {
                    scanLeDevice(true)
                }
            }
        }.start()
    }

    @SuppressLint("MissingPermission")
    private fun scanLeDevice(enable: Boolean) {
        if (bluetoothLeScanner != null) {
            if (enable) {
                Thread {
                    Thread.sleep(10000)
                    scanLeDevice(false)
                }.start()
                scanning = true
                bluetoothLeScanner?.startScan(scanCallback)
            } else {
                scanning = false
                bluetoothLeScanner?.stopScan(scanCallback)
            }
        }
        Log.d(TAG, "scanLeDevice: scanning: $scanning")
    }

    private fun addScannedDevices(device: BluetoothDevice){
        var is_repetition = false
        for( d in scan_result){
            if (d.address == device.address){
                is_repetition = true
            }
        }
        if(!is_repetition){
            scan_result.add(device)
            scan_list_adapter.notifyItemInserted(scan_result.size-1)
        }
    }

    private fun connect(device: BluetoothDevice){

    }


}