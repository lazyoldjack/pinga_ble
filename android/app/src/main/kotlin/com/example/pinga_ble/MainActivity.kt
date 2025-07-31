package com.example.pinga_ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.util.UUID

class MainActivity : FlutterActivity() {
    //private val CHANNEL = "com.example.ble_advertiser/ble"
    private val CHANNEL = "com.example.pinga_ble/ble"
    private lateinit var channel: MethodChannel

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var advertisingCallback: AdvertiseCallback? = null

    private val REQUEST_BLUETOOTH_PERMISSIONS = 1

    // A dummy service UUID for advertising. Replace with your actual service UUID if needed.
    // This is often required for advertising to be discoverable by some scanners.
    private val SERVICE_UUID = ParcelUuid(UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB")) // Example: Heart Rate Service UUID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bluetoothManager: BluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Log.e("BLEAdvertiser", "Bluetooth not supported on this device.")
            // Handle gracefully, e.g., show a message to the user
        } else if (!bluetoothAdapter!!.isEnabled) {
            Log.w("BLEAdvertiser", "Bluetooth is not enabled. Please enable it.")
            // You might want to prompt the user to enable Bluetooth
        }
    }

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
        channel.setMethodCallHandler { call, result ->
            when (call.method) {
                "startAdvertising" -> {
                    val txPowerLevel = call.argument<Int>("txPowerLevel") ?: AdvertiseSettings.ADVERTISE_TX_POWER_LOW
                    startAdvertising(txPowerLevel)
                    result.success(null)
                }
                "stopAdvertising" -> {
                    stopAdvertising()
                    result.success(null)
                }
                "updateAdvertisingPowerLevel" -> {
                    val txPowerLevel = call.argument<Int>("txPowerLevel") ?: AdvertiseSettings.ADVERTISE_TX_POWER_LOW
                    updateAdvertisingPowerLevel(txPowerLevel)
                    result.success(null)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_ADVERTISE)
            }
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_SCAN)
            }
        } else {
            // For older Android versions, ACCESS_FINE_LOCATION is required for BLE
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            // Check if all requested permissions were granted
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("BLEAdvertiser", "All BLE permissions granted.")
                // Optionally restart advertising if it was trying to start
                // You might want to store a flag if advertising was pending permissions
            } else {
                Log.e("BLEAdvertiser", "Not all BLE permissions granted. Cannot advertise.")
                channel.invokeMethod("onAdvertisingStatusChanged", mapOf(
                    "isAdvertising" to false,
                    "message" to "Permissions denied. Cannot advertise."
                ))
            }
        }
    }

    private fun startAdvertising(txPowerLevel: Int) {
        if (!checkAndRequestPermissions()) {
            Log.w("BLEAdvertiser", "Permissions not granted, cannot start advertising.")
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e("BLEAdvertiser", "Bluetooth not available or not enabled.")
            channel.invokeMethod("onAdvertisingStatusChanged", mapOf(
                "isAdvertising" to false,
                "message" to "Bluetooth is off or not supported."
            ))
            return
        }

        bluetoothLeAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        if (bluetoothLeAdvertiser == null) {
            Log.e("BLEAdvertiser", "Bluetooth LE Advertiser not available.")
            channel.invokeMethod("onAdvertisingStatusChanged", mapOf(
                "isAdvertising" to false,
                "message" to "BLE advertising not supported."
            ))
            return
        }

        // Stop any existing advertising before starting a new one
        stopAdvertisingInternal()

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER) // Optimize for low power
            .setTxPowerLevel(txPowerLevel) // User-controlled power level
            .setConnectable(true) // Allow other devices to connect (optional, but common for advertising local name)
            .setTimeout(0) // Advertise indefinitely
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true) // Include the local device name
            .addServiceUuid(SERVICE_UUID) // Add a service UUID (optional, but helps discovery)
            .build()

        advertisingCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                Log.d("BLEAdvertiser", "BLE advertising started successfully with power level: $txPowerLevel")
                channel.invokeMethod("onAdvertisingStatusChanged", mapOf(
                    "isAdvertising" to true,
                    "message" to "Advertising local name (Power: ${getPowerLevelName(txPowerLevel)})"
                ))
            }

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                val errorMessage = when (errorCode) {
                    AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "Already started"
                    AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                    AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal error"
                    AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers"
                    AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "Data too large"
                    else -> "Unknown error: $errorCode"
                }
                Log.e("BLEAdvertiser", "BLE advertising failed: $errorMessage")
                channel.invokeMethod("onAdvertisingStatusChanged", mapOf(
                    "isAdvertising" to false,
                    "message" to "Advertising failed: $errorMessage"
                ))
            }
        }

        try {
            bluetoothLeAdvertiser?.startAdvertising(settings, data, advertisingCallback)
        } catch (e: SecurityException) {
            Log.e("BLEAdvertiser", "SecurityException during startAdvertising: ${e.message}")
            channel.invokeMethod("onAdvertisingStatusChanged", mapOf(
                "isAdvertising" to false,
                "message" to "Permission error during advertising. Please grant all Bluetooth permissions."
            ))
        }
    }

    private fun stopAdvertising() {
        stopAdvertisingInternal()
        channel.invokeMethod("onAdvertisingStatusChanged", mapOf(
            "isAdvertising" to false,
            "message" to "Advertising stopped."
        ))
        Log.d("BLEAdvertiser", "BLE advertising stopped.")
    }

    private fun stopAdvertisingInternal() {
        if (bluetoothLeAdvertiser != null && advertisingCallback != null) {
            try {
                bluetoothLeAdvertiser?.stopAdvertising(advertisingCallback)
            } catch (e: SecurityException) {
                Log.e("BLEAdvertiser", "SecurityException during stopAdvertising: ${e.message}")
                // This might happen if permissions are revoked while advertising
            }
            advertisingCallback = null
        }
    }

    private fun updateAdvertisingPowerLevel(txPowerLevel: Int) {
        if (bluetoothLeAdvertiser != null && advertisingCallback != null) {
            // Stop current advertising and start with new power level
            stopAdvertisingInternal()
            startAdvertising(txPowerLevel)
        } else {
            Log.w("BLEAdvertiser", "Not advertising, cannot update power level.")
            channel.invokeMethod("onAdvertisingStatusChanged", mapOf(
                "isAdvertising" to false,
                "message" to "Not advertising. Start advertising first to set power level."
            ))
        }
    }

    private fun getPowerLevelName(txPowerLevel: Int): String {
        return when (txPowerLevel) {
            AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW -> "Ultra Low"
            AdvertiseSettings.ADVERTISE_TX_POWER_LOW -> "Low"
            AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM -> "Medium"
            AdvertiseSettings.ADVERTISE_TX_POWER_HIGH -> "High"
            else -> "Unknown"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAdvertisingInternal() // Ensure advertising is stopped when activity is destroyed
    }
}
/*************************************************** JRE
```

**Android Manifest (`android/app/src/main/AndroidManifest.xml`):**

Ensure your `AndroidManifest.xml` includes the necessary permissions. Add these inside the `<manifest>` tag, usually before the `<application>` tag.

```xml
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>

<!-- Required for Android 12 (API level 31) and above -->
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" android:maxSdkVersion="33"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" android:maxSdkVersion="33"/>
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:maxSdkVersion="33"/>

<!-- If your app targets Android 12 (API level 31) or higher,
     and you use BLUETOOTH_SCAN, BLUETOOTH_ADVERTISE, or BLUETOOTH_CONNECT,
     you must declare that your app doesn't require the ACCESS_FINE_LOCATION permission.
     However, for BLE advertising, ACCESS_FINE_LOCATION is still often needed for older devices.
     So, keep ACCESS_FINE_LOCATION for broader compatibility.
-->
<uses-feature android:name="android.hardware.bluetooth_le" android:required="true"/>
```

**To run this application:**

1.  **Create a new Flutter project:**
    `flutter create ble_advertiser`
    `cd ble_advertiser`
2.  **Replace `lib/main.dart`** with the Dart code provided above.
3.  **Replace `android/app/src/main/kotlin/com/example/ble_advertiser/MainActivity.kt`** with the Kotlin code provided above.
4.  **Update `android/app/src/main/AndroidManifest.xml`** by adding the permissions.
5.  **Run the app on an Android device:**
    `flutter run`

**Important Considerations and Next Steps:**

* **Permissions:** The app will request Bluetooth and location permissions upon launch. You *must* grant these for BLE advertising to work.
* **Bluetooth Enablement:** The app checks if Bluetooth is enabled. If not, it logs a warning. You might want to add UI to prompt the user to enable Bluetooth.
* **Foreground Service (for truly continuous background advertising):** For advertising to reliably continue when the app is in the background or the screen is off for extended periods, you would typically need to implement an Android Foreground Service. This is a more complex topic involving notifications and managing the service lifecycle. This current implementation might be stopped by the OS under certain power-saving scenarios when the app is in the background. If truly continuous advertising is a strict requirement, a foreground service is the next step.
* **Device Name:** The `setIncludeDeviceName(true)` in `AdvertiseData` automatically picks up the device's name as set in Android's system settings (e.g., "Galaxy S23", "Pixel 7"). You cannot dynamically change this name directly through the BLE advertising API without changing the system setting itself (which is generally not allowed for apps).
* **Service UUID:** I've included a dummy service UUID (`0000180D-0000-1000-8000-00805F9B34FB`, which is the Heart Rate Service UUID). While `setIncludeDeviceName(true)` advertises the name, some BLE scanners might only show devices advertising specific service UUIDs. You can remove `addServiceUuid` if not needed, or replace it with your own custom UUID.
* **Testing:** You'll need a BLE scanner app on another device (e.g., "nRF Connect for Mobile" by Nordic Semiconductor) to verify that your phone is advertising and to see its local name and the changes in TX power level (though TX power is an internal setting and might not be directly reported by all scanners, its effect on range will be noticeabl

*****************************/