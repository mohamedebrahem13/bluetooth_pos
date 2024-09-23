package com.example.bluetooth_pos.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.bluetooth_pos.data.models.BluetoothDeviceDomain
import com.example.bluetooth_pos.ui.BluetoothViewModel

@Composable
fun BluetoothScreen(
    modifier: Modifier = Modifier,
    navController: NavController, // Add NavController for navigation
    viewModel: BluetoothViewModel = hiltViewModel()
) {
    val isConnected = viewModel.isConnected.collectAsState()
    val scannedDevices = viewModel.scannedDevices.collectAsState()
    val pairedDevices = viewModel.pairedDevices.collectAsState()
    val serviceUuid = viewModel.discoveredServiceUuid.collectAsState()
    val characteristicUuid = viewModel.discoveredCharacteristicUuid.collectAsState()
    val connectedDevices = viewModel.getConnectedGattDevices() // Get connected GATT devices
    val error by viewModel.errorState.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        // Display connection status
        Text("Connection Status: ${if (isConnected.value) "Connected" else "Disconnected"}")

        if (error != null) {
            Text(text = "Error: $error")
        }
        // Button to start Bluetooth device discovery
        Button(onClick = { viewModel.startDiscovery() }) {
            Text("Start Discovery")
        }

        // Button to stop Bluetooth device discovery
        Button(onClick = { viewModel.stopDiscovery() }) {
            Text("Stop Discovery")
        }

        // Display connected GATT devices
        Text("Connected Devices:")
        LazyColumn {
            items(connectedDevices) { device ->
                BluetoothDeviceItem(device = device, onClick = { /* You can add any interaction here */ })
            }
        }

        // Display paired devices
        Text("Paired Devices:")
        LazyColumn {
            items(pairedDevices.value) { device ->
                BluetoothDeviceItem(device = device, onClick = {
                    viewModel.connectToDevice(device)
                })
            }
        }

        // Display scanned devices
        Text("Scanned Devices:")
        LazyColumn {
            items(scannedDevices.value) { device ->
                BluetoothDeviceItem(device = device, onClick = {
                    viewModel.connectToDevice(device)
                })
            }
        }

        // Navigate to the send data screen when connected, passing UUIDs
        LaunchedEffect(isConnected.value) {
            if (isConnected.value && serviceUuid.value != null && characteristicUuid.value != null) {
                // Pass the serviceUuid and characteristicUuid to SendDataScreen
                navController.navigate("sendData/${serviceUuid.value}/${characteristicUuid.value}")
            }
        }

    }
}

@Composable
fun BluetoothDeviceItem(device: BluetoothDeviceDomain, onClick: () -> Unit) {
    // Display the device name or address if the name is unavailable
    val deviceName = device.name ?: "Unnamed Device"
    Text(
        text = "$deviceName (${device.address})",
        modifier = Modifier.clickable(onClick = onClick) // Allow clicking on the device to connect
    )
}