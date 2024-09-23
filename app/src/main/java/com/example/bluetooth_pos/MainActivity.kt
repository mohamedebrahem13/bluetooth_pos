package com.example.bluetooth_pos

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bluetooth_pos.ui.screen.BluetoothScreen
import com.example.bluetooth_pos.ui.screen.SendDataScreen
import com.example.bluetooth_pos.ui.theme.Bluetooth_posTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var bluetoothAdapter: BluetoothAdapter

    private val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter.isEnabled

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val enableBluetoothLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { /* Handle Bluetooth enable result if needed */ }

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val canEnableBluetooth = permissions[android.Manifest.permission.BLUETOOTH_CONNECT] == true
            val canScanBluetooth = permissions[android.Manifest.permission.BLUETOOTH_SCAN] == true
            val canAccessFineLocation = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
            val canAccessCoarseLocation = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true

            // Check if the essential permissions were granted
            if (canEnableBluetooth && canScanBluetooth && canAccessFineLocation && canAccessCoarseLocation) {
                if (!isBluetoothEnabled) {
                    enableBluetoothLauncher.launch(
                        Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    )
                }
            } else {
                Toast.makeText(this, "Permissions required for Bluetooth functionality", Toast.LENGTH_LONG).show()
            }
        }

        // Request permissions depending on the Android version
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            arrayOf(
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_ADMIN,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

        // Launch the permission request
        permissionLauncher.launch(permissionsToRequest)

        // Enable edge-to-edge UI and set up the content
        enableEdgeToEdge()
        setContent {
            Bluetooth_posTheme {
                val navController: NavHostController = rememberNavController()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    content = { innerPadding ->
                        // Navigation between BluetoothScreen and SendDataScreen
                        NavHost(
                            navController = navController,
                            startDestination = "bluetooth",
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            // Bluetooth scanning and connecting screen
                            composable("bluetooth") {
                                BluetoothScreen(navController = navController)
                            }

                            // Screen for sending data after connecting
                            composable("sendData") {
                                SendDataScreen()
                            }
                        }
                    }
                )
            }
        }
    }
}