package com.example.bluetooth_pos.data.models

data class BluetoothMessage(
    val message: String,
    val senderName: String,
    val isFromLocalUser: Boolean
) {
    fun toByteArray(): ByteArray {
        return message.toByteArray(Charsets.UTF_8)
    }
}