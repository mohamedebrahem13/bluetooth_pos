package com.example.bluetooth_pos.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import com.example.bluetooth_pos.data.models.BluetoothDeviceDomain

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceDomain(): BluetoothDeviceDomain {
    return BluetoothDeviceDomain(
        name = name,
        address = address
    )
}