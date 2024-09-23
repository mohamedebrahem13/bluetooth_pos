package com.example.bluetooth_pos.domain

import com.example.bluetooth_pos.data.models.BluetoothDeviceDomain
import com.example.bluetooth_pos.data.models.BluetoothMessage
import com.example.bluetooth_pos.data.models.ConnectionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

interface BluetoothController {

    // State flow to indicate whether the device is connected to a Bluetooth GATT server
    val isConnected: StateFlow<Boolean>

    // State flow to hold a list of discovered devices during scanning
    val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>

    // State flow to hold a list of paired devices
    val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>


    // Shared flow for connectionResult
    val connectionResult: SharedFlow<ConnectionResult>

    // Shared flow for logs
    val logs: SharedFlow<String>

    // Start discovering Bluetooth devices
    fun startDiscovery()

    // Stop discovering Bluetooth devices
    fun stopDiscovery()

    // Connect to a Bluetooth device using GATT
    fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult>

    // Send a message via Bluetooth GATT service and characteristic
    suspend fun trySendMessage(message: String, serviceUuid: UUID?, characteristicUuid: UUID?): BluetoothMessage?
    // Close the GATT connection
    fun closeConnection()

    // Clean up any resources, unregister receivers, etc.
    fun release()
   fun getConnectedGattDevices(): List<BluetoothDeviceDomain>
}