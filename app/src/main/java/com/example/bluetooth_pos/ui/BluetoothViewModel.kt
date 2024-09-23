package com.example.bluetooth_pos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth_pos.data.models.BluetoothDeviceDomain
import com.example.bluetooth_pos.data.models.ConnectionResult
import com.example.bluetooth_pos.domain.BluetoothController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothGattController: BluetoothController
) : ViewModel() {

    // Expose the connection state, scanned devices, paired devices, and errors
    val isConnected: StateFlow<Boolean> = bluetoothGattController.isConnected
    val scannedDevices: StateFlow<List<BluetoothDeviceDomain>> = bluetoothGattController.scannedDevices
    val pairedDevices: StateFlow<List<BluetoothDeviceDomain>> = bluetoothGattController.pairedDevices
    val logs: SharedFlow<String> = bluetoothGattController.logs


    private val _discoveredServiceUuid = MutableStateFlow<UUID?>(null)
    val discoveredServiceUuid: StateFlow<UUID?> = _discoveredServiceUuid.asStateFlow()

    private val _discoveredCharacteristicUuid = MutableStateFlow<UUID?>(null)
    val discoveredCharacteristicUuid: StateFlow<UUID?> = _discoveredCharacteristicUuid.asStateFlow()

    // MutableStateFlow to emit errors
    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    // Start scanning for devices
    fun startDiscovery() {
        bluetoothGattController.startDiscovery()
    }

    // Stop scanning for devices
    fun stopDiscovery() {
        bluetoothGattController.stopDiscovery()
    }

    fun connectToDevice(device: BluetoothDeviceDomain) {
        viewModelScope.launch {
            bluetoothGattController.connectToDevice(device)
                .collect { result ->
                    when (result) {
                        is ConnectionResult.ConnectionEstablished -> {

                        }
                        is ConnectionResult.Error -> {
                            // Handle error logic here
                            _errorState.value = result.message

                        }

                        is ConnectionResult.UuidDiscovered -> {
                            // Save the discovered service and characteristic UUIDs
                            _discoveredServiceUuid.value = result.serviceUuid
                            _discoveredCharacteristicUuid.value = result.characteristicUuid
                        }
                    }
                }
        }
    }


    // Send a message to the connected device
    fun sendMessage(message: String) {
        val serviceUuid = discoveredServiceUuid.value
        val characteristicUuid = discoveredCharacteristicUuid.value

        if (serviceUuid != null && characteristicUuid != null) {
            viewModelScope.launch {
                bluetoothGattController.trySendMessage(message, serviceUuid, characteristicUuid)
            }
        }
    }
    fun getConnectedGattDevices(): List<BluetoothDeviceDomain> {
        return bluetoothGattController.getConnectedGattDevices()
    }

    // Release resources when the ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        bluetoothGattController.release()
    }
}