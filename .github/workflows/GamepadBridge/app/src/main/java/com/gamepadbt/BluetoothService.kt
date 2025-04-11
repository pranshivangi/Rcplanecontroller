package com.gamepadbt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import kotlin.concurrent.thread

class BluetoothService(
    private val bluetoothAdapter: BluetoothAdapter,
    private val callback: ConnectionCallback
) {
    companion object {
        private const val TAG = "BluetoothService"
        // SPP UUID - standard for serial communication over Bluetooth
        private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var connectedDevice: BluetoothDevice? = null
    private var connectThread: ConnectThread? = null
    private var connectedThread: ConnectedThread? = null

    interface ConnectionCallback {
        fun onConnected()
        fun onDisconnected()
        fun onError(message: String)
    }

    fun getPairedDevices(): Set<BluetoothDevice> {
        return bluetoothAdapter.bondedDevices
    }

    fun connect(device: BluetoothDevice) {
        // Cancel any existing connections
        disconnect()
        
        // Store the device for reconnection
        connectedDevice = device
        
        // Start a new connection thread
        connectThread = ConnectThread(device)
        connectThread?.start()
    }

    fun reconnect() {
        connectedDevice?.let { device ->
            connect(device)
        }
    }

    fun disconnect() {
        // Cancel the connection thread
        connectThread?.cancel()
        connectThread = null
        
        // Cancel the connected thread
        connectedThread?.cancel()
        connectedThread = null
    }

    fun sendData(data: String) {
        // Send data to the connected device
        connectedThread?.write(data.toByteArray())
    }

    private inner class ConnectThread(device: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            try {
                // Create a socket with the SPP UUID
                device.createRfcommSocketToServiceRecord(SPP_UUID)
            } catch (e: IOException) {
                Log.e(TAG, "Socket creation failed", e)
                callback.onError("Failed to create socket: ${e.message}")
                null
            }
        }

        override fun run() {
            // Cancel discovery to improve performance
            bluetoothAdapter.cancelDiscovery()

            mmSocket?.let { socket ->
                try {
                    // Connect to the remote device
                    socket.connect()
                    
                    // Connection successful, start the connected thread
                    connectedThread = ConnectedThread(socket)
                    connectedThread?.start()
                    
                    // Notify successful connection
                    callback.onConnected()
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to connect", e)
                    cancel()
                    callback.onError("Failed to connect: ${e.message}")
                    callback.onDisconnected()
                }
            }
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Failed to close socket", e)
            }
        }
    }

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private var isRunning = true

        override fun run() {
            // Just keep thread alive to maintain connection
            while (isRunning) {
                try {
                    sleep(100)
                } catch (e: InterruptedException) {
                    Log.e(TAG, "Thread interrupted", e)
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
                mmOutStream.flush()
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)
                callback.onError("Failed to send data: ${e.message}")
                cancel()
                callback.onDisconnected()
            }
        }

        fun cancel() {
            isRunning = false
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Failed to close the connected socket", e)
            }
        }
    }
}
