package com.smartatendimentos.traccar

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.IOException

class ArduinoCommunication(private val context: Context) {

    private var usbSerialPort: UsbSerialPort? = null
    private var isConnected = false
    private var connectionListener: ((Boolean) -> Unit)? = null
    private var dataListener: ((String) -> Unit)? = null
    private var currentBaudRate = 9600

    companion object {
        private const val TAG = "ArduinoCommunication"
        private const val ACTION_USB_PERMISSION = "com.smartatendimentos.traccar.USB_PERMISSION"
    }

    fun setConnectionListener(listener: (Boolean) -> Unit) {
        connectionListener = listener
    }

    fun setDataListener(listener: (String) -> Unit) {
        dataListener = listener
    }

    fun setBaudRate(baudRate: Int) {
        currentBaudRate = baudRate
        Log.i(TAG, "Baud rate set to: $baudRate")
    }

    fun connect(): Boolean {
        return try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)

            if (availableDrivers.isEmpty()) {
                Log.w(TAG, "No USB serial drivers found")
                connectionListener?.invoke(false)
                return false
            }

            val driver = availableDrivers[0]
            val device = driver.device

            // Check for permission
            if (!usbManager.hasPermission(device)) {
                Log.w(TAG, "No USB permission, requesting...")
                val intent = Intent(ACTION_USB_PERMISSION)
                val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                usbManager.requestPermission(device, pendingIntent)
                return false
            }

            val connection = usbManager.openDevice(device)
            if (connection == null) {
                Log.e(TAG, "Failed to open USB device")
                connectionListener?.invoke(false)
                return false
            }

            usbSerialPort = driver.ports[0]
            usbSerialPort?.open(connection)
            usbSerialPort?.setParameters(currentBaudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

            isConnected = true
            connectionListener?.invoke(true)
            Log.i(TAG, "Arduino connected successfully at $currentBaudRate baud")
            
            startListening()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to Arduino: ${e.message}")
            isConnected = false
            connectionListener?.invoke(false)
            false
        }
    }

    fun disconnect() {
        try {
            if (usbSerialPort?.isOpen == true) {
                usbSerialPort?.close()
            }
            isConnected = false
            connectionListener?.invoke(false)
            Log.i(TAG, "Arduino disconnected")
        } catch (e: IOException) {
            Log.e(TAG, "Error disconnecting from Arduino: ${e.message}")
        }
    }

    fun sendCommand(command: String): Boolean {
        return if (isConnected && usbSerialPort?.isOpen == true) {
            try {
                usbSerialPort?.write(command.toByteArray(), 1000)
                Log.i(TAG, "Command sent: $command")
                true
            } catch (e: IOException) {
                Log.e(TAG, "Error sending command: ${e.message}")
                false
            }
        } else {
            Log.w(TAG, "Arduino not connected")
            false
        }
    }

    fun sendRawBytes(data: ByteArray): Boolean {
        return if (isConnected && usbSerialPort?.isOpen == true) {
            try {
                usbSerialPort?.write(data, 1000)
                Log.i(TAG, "Raw bytes sent: ${data.size} bytes")
                true
            } catch (e: IOException) {
                Log.e(TAG, "Error sending raw bytes: ${e.message}")
                false
            }
        } else {
            Log.w(TAG, "Arduino not connected")
            false
        }
    }

    fun activateEngineCut(): Boolean {
        return sendCommand("1")
    }

    fun deactivateEngineCut(): Boolean {
        return sendCommand("0")
    }

    fun isConnected(): Boolean = isConnected

    private fun startListening() {
        Thread {
            try {
                val buffer = ByteArray(1024)
                while (isConnected && usbSerialPort?.isOpen == true) {
                    val bytesRead = usbSerialPort?.read(buffer, 1000) ?: 0
                    if (bytesRead > 0) {
                        val data = String(buffer, 0, bytesRead)
                        Log.i(TAG, "Data received: $data")
                        dataListener?.invoke(data)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error in listening thread: ${e.message}")
            }
        }.start()
    }
}
