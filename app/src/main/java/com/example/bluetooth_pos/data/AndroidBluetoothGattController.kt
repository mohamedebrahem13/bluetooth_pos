package com.example.bluetooth_pos.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import com.example.bluetooth_pos.data.models.BluetoothDeviceDomain
import com.example.bluetooth_pos.data.models.BluetoothMessage
import com.example.bluetooth_pos.data.models.ConnectionResult
import com.example.bluetooth_pos.domain.BluetoothController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@SuppressLint("MissingPermission")
class AndroidBluetoothGattController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter,
    private val bluetoothManager: BluetoothManager
) : BluetoothController {

    private var bluetoothGatt: BluetoothGatt? = null
    private var discoveredServiceUuid: UUID? = null
    private var discoveredCharacteristicUuid: UUID? = null
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>> = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>> = _pairedDevices.asStateFlow()
  
    // MutableSharedFlow to emit connection results
    private val _connectionResult = MutableSharedFlow<ConnectionResult>()
    override val connectionResult: SharedFlow<ConnectionResult> = _connectionResult.asSharedFlow()

    private val _logs = MutableSharedFlow<String>()
    override  val logs: SharedFlow<String> = _logs.asSharedFlow()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _isConnected.value = true
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _isConnected.value = false
                CoroutineScope(Dispatchers.IO).launch {
                    _connectionResult.emit(ConnectionResult.Error("Disconnected from GATT server."))
                }
                closeConnection()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                if (status == BluetoothGatt.GATT_SUCCESS && gatt != null) {
                    val serviceUuid = gatt.services.firstOrNull()?.uuid
                    val characteristicUuid = gatt.services.firstOrNull()?.characteristics?.firstOrNull()?.uuid

                    if (serviceUuid != null && characteristicUuid != null) {
                        _logs.emit("Discovered Service UUID: $serviceUuid")
                        _logs.emit("Discovered Characteristic UUID: $characteristicUuid")

                        // Emit the discovered UUIDs
                        _connectionResult.emit(ConnectionResult.UuidDiscovered(serviceUuid, characteristicUuid))
                    } else {
                        _connectionResult.emit(ConnectionResult.Error("Failed to discover UUIDs"))
                    }
                } else {
                    _connectionResult.emit(ConnectionResult.Error("Failed to discover GATT services."))
                }
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val value = characteristic?.value
                value?.let {
                    CoroutineScope(Dispatchers.IO).launch {
                        _connectionResult.emit(ConnectionResult.Error("Data read from characteristic: ${it.toString(Charsets.UTF_8)}"))
                    }
                }
            }
        }


        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    _logs.emit("Data successfully written to characteristic.")
                } else {
                    _connectionResult.emit(ConnectionResult.Error("Failed to write data to characteristic."))
                }
            }
        }
    }


    override fun startDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            CoroutineScope(Dispatchers.IO).launch {
                _connectionResult.emit(ConnectionResult.Error("Missing permission: BLUETOOTH_SCAN"))
            }
            return
        }

        if (bluetoothAdapter.isEnabled) {
            updatePairedDevices()
            bluetoothAdapter.startDiscovery()
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                _connectionResult.emit(ConnectionResult.Error("Bluetooth is disabled. Please enable Bluetooth."))
            }
        }
    }

    override fun stopDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            CoroutineScope(Dispatchers.IO).launch {
                _connectionResult.emit(ConnectionResult.Error("Missing permission: BLUETOOTH_SCAN"))
            }
            return
        }
        bluetoothAdapter.cancelDiscovery()
    }

    override fun connectToDevice(device: BluetoothDeviceDomain): Flow<ConnectionResult> {
        return flow {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                throw SecurityException("No BLUETOOTH_CONNECT permission")
            }

            val bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.address)
            bluetoothGatt = bluetoothDevice.connectGatt(context, false, gattCallback)

            if (bluetoothGatt != null) {
                emit(ConnectionResult.ConnectionEstablished)
            } else {
                emit(ConnectionResult.Error("Failed to connect to GATT server."))
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun trySendMessage(message: String, serviceUuid: UUID?, characteristicUuid: UUID?): BluetoothMessage? {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            return null
        }

        val targetServiceUuid = serviceUuid ?: discoveredServiceUuid
        val targetCharacteristicUuid = characteristicUuid ?: discoveredCharacteristicUuid

        if (targetServiceUuid != null && targetCharacteristicUuid != null) {
            val service = bluetoothGatt?.getService(targetServiceUuid)
            val characteristic = service?.getCharacteristic(targetCharacteristicUuid)

            if (characteristic != null) {
                val bluetoothMessage = BluetoothMessage(
                    message = message,
                    senderName = bluetoothAdapter.name ?: "Unknown",
                    isFromLocalUser = true
                )

                characteristic.value = bluetoothMessage.toByteArray()
                bluetoothGatt?.writeCharacteristic(characteristic)
                return bluetoothMessage
            }
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                _connectionResult.emit(ConnectionResult.Error("Service or Characteristic not discovered yet."))
            }
        }
        return null
    }


    override fun closeConnection() {
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    override fun release() {
        closeConnection()
    }

    private fun updatePairedDevices() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        bluetoothAdapter
            .bondedDevices
            .map { it.toBluetoothDeviceDomain() }
            .also { devices ->
                _pairedDevices.update { devices }
            }
    }

    override fun getConnectedGattDevices(): List<BluetoothDeviceDomain> {
        val connectedDevices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
        return connectedDevices.map { it.toBluetoothDeviceDomain() }
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

}