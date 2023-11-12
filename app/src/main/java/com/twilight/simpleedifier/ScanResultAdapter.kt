package com.twilight.simpleedifier

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScanResultAdapter(val scanResultList: ArrayList<BluetoothDevice>, val handleClickCallback: HandleClickCallback) : RecyclerView.Adapter<ScanResultAdapter.ScanResultViewHolder>() {
    class ScanResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val device_name:TextView = itemView.findViewById(R.id.bluetooth_device_name)
        val select_button:Button = itemView.findViewById(R.id.bluetooth_device_select_button)
        val device_mac:TextView = itemView.findViewById(R.id.bluetooth_device_mac)
    }

    interface HandleClickCallback{
        fun handleBluetoothDevice(device: BluetoothDevice)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.scan_result_list_item, parent, false)
        return ScanResultViewHolder(view)
    }

    override fun getItemCount(): Int {
        return scanResultList.size
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: ScanResultViewHolder, position: Int) {
        holder.device_name.text = scanResultList[position].name
        holder.device_mac.text = scanResultList[position].address
        holder.select_button.setOnClickListener {
            handleClickCallback.handleBluetoothDevice(scanResultList[position])
        }
    }
}