package com.example.bluetooth_pos.data.models

import java.util.UUID

sealed class ConnectionResult {
    data object ConnectionEstablished : ConnectionResult()
    data class Error(val message: String) : ConnectionResult()
    data class UuidDiscovered(val serviceUuid: UUID, val characteristicUuid: UUID) : ConnectionResult()

}
