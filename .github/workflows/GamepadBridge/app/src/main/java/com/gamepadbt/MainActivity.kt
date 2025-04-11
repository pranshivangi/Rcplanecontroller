package com.gamepadbt

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

class MainActivity : AppCompatActivity() {
    private lateinit var connectButton: Button
    private lateinit var statusText: TextView
    private lateinit var leftXText: TextView
    private lateinit var rightXText: TextView
    private lateinit var rightYText: TextView
    private lateinit var rightXYText: TextView
    private lateinit var transmitText: TextView
    
    // Joystick visualization
    private lateinit var leftJoystickIndicator: View
    private lateinit var rightJoystickIndicator: View
    private lateinit var leftJoystickContainer: View
    private lateinit var rightJoystickContainer: View

    private lateinit var bluetoothService: BluetoothService
    private lateinit var gamepadHandler: GamepadHandler

    private var isConnected = false
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 50L // 20 times per second (1000ms / 20 = 50ms)

    // Bluetooth permission request launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var allGranted = true
            permissions.entries.forEach {
                if (!it.value) {
                    allGranted = false
                }
            }
            if (allGranted) {
                initializeBluetooth()
            } else {
                Toast.makeText(this, "Bluetooth permissions denied", Toast.LENGTH_SHORT).show()
            }
        }

    // Bluetooth enable request launcher
    private val enableBluetoothLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                setupDeviceList()
            } else {
                Toast.makeText(this, "Bluetooth must be enabled to use this app", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        connectButton = findViewById(R.id.connectButton)
        statusText = findViewById(R.id.statusText)
        leftXText = findViewById(R.id.leftXValue)
        rightXText = findViewById(R.id.rightXValue)
        rightYText = findViewById(R.id.rightYValue)
        rightXYText = findViewById(R.id.rightXYValues)
        transmitText = findViewById(R.id.transmitValue)
        
        // Initialize joystick visualization
        leftJoystickIndicator = findViewById(R.id.leftJoystickIndicator)
        rightJoystickIndicator = findViewById(R.id.rightJoystickIndicator)
        leftJoystickContainer = findViewById(R.id.leftJoystickContainer)
        rightJoystickContainer = findViewById(R.id.rightJoystickContainer)

        // Initialize gamepad handler
        gamepadHandler = GamepadHandler()
        
        // Check Bluetooth permissions
        checkPermissions()

        // Connect button action
        connectButton.setOnClickListener {
            if (isConnected) {
                bluetoothService.disconnect()
                updateConnectionStatus(false)
            } else {
                setupDeviceList()
            }
        }

        // Start data transmission scheduler
        startDataTransmission()
    }

    override fun onResume() {
        super.onResume()
        if (checkGamepadConnected()) {
            statusText.text = "Gamepad connected, waiting for Bluetooth connection"
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
        } else {
            statusText.text = "Please connect a gamepad"
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
        }
    }

    override fun onDestroy() {
        if (::bluetoothService.isInitialized) {
            bluetoothService.disconnect()
        }
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        // Process gamepad input events
        if (event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
            && event.action == MotionEvent.ACTION_MOVE) {
            
            gamepadHandler.processJoystickInput(event)
            
            // Update UI with joystick values
            updateJoystickUI()
            
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }
    
    private fun updateJoystickUI() {
        // Update text values
        leftXText.text = gamepadHandler.leftX.toString()
        rightXText.text = gamepadHandler.rightX.toString()
        rightYText.text = gamepadHandler.rightY.toString()
        rightXYText.text = "X: ${gamepadHandler.rightX}, Y: ${gamepadHandler.rightY}"
        
        // Update joystick visualizations
        updateJoystickPosition(leftJoystickIndicator, leftJoystickContainer, 
                              gamepadHandler.leftX.toFloat() / 100f, 0f)
        updateJoystickPosition(rightJoystickIndicator, rightJoystickContainer, 
                              gamepadHandler.rightX.toFloat() / 100f, gamepadHandler.rightY.toFloat() / 100f)
    }
    
    private fun updateJoystickPosition(joystickIndicator: View, joystickContainer: View, normalizedX: Float, normalizedY: Float) {
        // Get container dimensions
        val containerWidth = joystickContainer.width
        val containerHeight = joystickContainer.height
        
        // Calculate center position
        val centerX = containerWidth / 2f
        val centerY = containerHeight / 2f
        
        // Calculate offset based on normalized joystick values (-1 to 1)
        val offsetX = normalizedX * (containerWidth / 2f - joystickIndicator.width / 2f)
        val offsetY = normalizedY * (containerHeight / 2f - joystickIndicator.height / 2f)
        
        // Set the position
        val params = joystickIndicator.layoutParams as ConstraintLayout.LayoutParams
        
        // Create constraint set to modify the position
        val set = ConstraintSet()
        set.clone(joystickContainer.parent as ConstraintLayout)
        
        // Clear existing constraints for the indicator
        set.clear(joystickIndicator.id, ConstraintSet.START)
        set.clear(joystickIndicator.id, ConstraintSet.END)
        set.clear(joystickIndicator.id, ConstraintSet.TOP)
        set.clear(joystickIndicator.id, ConstraintSet.BOTTOM)
        
        // Set new bias values (0.5f is center, range is 0-1)
        set.setHorizontalBias(joystickIndicator.id, 0.5f + normalizedX * 0.5f)
        set.setVerticalBias(joystickIndicator.id, 0.5f + normalizedY * 0.5f)
        
        // Connect the indicator to the container edges
        set.connect(joystickIndicator.id, ConstraintSet.START, joystickContainer.id, ConstraintSet.START)
        set.connect(joystickIndicator.id, ConstraintSet.END, joystickContainer.id, ConstraintSet.END)
        set.connect(joystickIndicator.id, ConstraintSet.TOP, joystickContainer.id, ConstraintSet.TOP)
        set.connect(joystickIndicator.id, ConstraintSet.BOTTOM, joystickContainer.id, ConstraintSet.BOTTOM)
        
        // Apply the constraints
        set.applyTo(joystickContainer.parent as ConstraintLayout)
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissionsToRequest = mutableListOf<String>()
            
            // Bluetooth permissions for Android 12+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            
            if (permissionsToRequest.isNotEmpty()) {
                requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            } else {
                initializeBluetooth()
            }
        } else {
            // For older Android versions
            val permissionsToRequest = mutableListOf<String>()
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH)
            }
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            
            if (permissionsToRequest.isNotEmpty()) {
                requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            } else {
                initializeBluetooth()
            }
        }
    }

    private fun initializeBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show()
            return
        }
        
        bluetoothService = BluetoothService(bluetoothAdapter, object : BluetoothService.ConnectionCallback {
            override fun onConnected() {
                runOnUiThread {
                    updateConnectionStatus(true)
                }
            }

            override fun onDisconnected() {
                runOnUiThread {
                    updateConnectionStatus(false)
                    // Auto-reconnect after a short delay
                    handler.postDelayed({
                        if (!isConnected && ::bluetoothService.isInitialized) {
                            bluetoothService.reconnect()
                        }
                    }, 2000)
                }
            }

            override fun onError(message: String) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    updateConnectionStatus(false)
                }
            }
        })

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            setupDeviceList()
        }
    }

    private fun setupDeviceList() {
        try {
            val devices = bluetoothService.getPairedDevices()
            
            // For simplicity, we'll connect to the first ESP32 device found
            // In a real app, you'd show a list and let the user choose
            var esp32Device: BluetoothDevice? = null
            
            for (device in devices) {
                val deviceName = if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    "Unknown" // Permission not granted case
                } else {
                    device.name ?: "Unknown"
                }
                
                // Check if it's an ESP32 device - in real app, users would select from a list
                if (deviceName.contains("ESP32", ignoreCase = true)) {
                    esp32Device = device
                    break
                }
            }
            
            if (esp32Device != null) {
                statusText.text = "Connecting to ESP32..."
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                bluetoothService.connect(esp32Device)
            } else {
                Toast.makeText(this, "No paired ESP32 device found. Please pair an ESP32 device in Bluetooth settings.", Toast.LENGTH_LONG).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Bluetooth permissions missing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateConnectionStatus(connected: Boolean) {
        isConnected = connected
        if (connected) {
            statusText.text = "Connected to ESP32"
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            connectButton.text = "Disconnect"
            connectButton.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_red_light)
        } else {
            statusText.text = "Disconnected"
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            connectButton.text = "Connect"
            connectButton.backgroundTintList = ContextCompat.getColorStateList(this, android.R.color.holo_green_light)
        }
    }

    private fun startDataTransmission() {
        // Schedule periodic data transmission
        handler.post(object : Runnable {
            override fun run() {
                if (isConnected && checkGamepadConnected()) {
                    // Format data string
                    val dataString = "R:${gamepadHandler.leftX} A:${gamepadHandler.rightX} E:${-gamepadHandler.rightY}"
                    
                    // Update UI
                    transmitText.text = dataString
                    
                    // Send data
                    bluetoothService.sendData(dataString)
                }
                
                // Schedule next update
                handler.postDelayed(this, updateInterval)
            }
        })
    }

    private fun checkGamepadConnected(): Boolean {
        val inputDevices = InputDevice.getDeviceIds()
        for (deviceId in inputDevices) {
            val dev = InputDevice.getDevice(deviceId)
            if (dev.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                dev.sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) {
                return true
            }
        }
        return false
    }
}
