package com.example.bluetooth_pos.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.example.bluetooth_pos.data.AndroidBluetoothGattController
import com.example.bluetooth_pos.domain.BluetoothController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideBluetoothManager(
        @ApplicationContext context: Context
    ): BluetoothManager {
        return context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    @Provides
    fun provideBluetoothAdapter(
        bluetoothManager: BluetoothManager
    ): BluetoothAdapter {
        return bluetoothManager.adapter
    }
    @Provides
    @Singleton
    fun provideBluetoothController(
        @ApplicationContext context: Context,
        bluetoothAdapter: BluetoothAdapter,
        bluetoothManager: BluetoothManager
    ): BluetoothController {
        return AndroidBluetoothGattController(
            context = context,
            bluetoothAdapter = bluetoothAdapter,
            bluetoothManager = bluetoothManager
        )
    }
}