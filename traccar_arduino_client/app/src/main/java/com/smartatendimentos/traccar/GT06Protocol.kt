package com.smartatendimentos.traccar

import android.util.Log
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

class GT06Protocol(private val serverAddress: String, private val serverPort: Int = 5023) {

    private var socket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    private val isConnected = AtomicBoolean(false)
    private var connectionListener: ((Boolean) -> Unit)? = null
    private var commandListener: ((String) -> Unit)? = null
    private var listeningThread: Thread? = null

    companion object {
        private const val TAG = "GT06Protocol"
    }

    fun setConnectionListener(listener: (Boolean) -> Unit) {
        connectionListener = listener
    }

    fun setCommandListener(listener: (String) -> Unit) {
        commandListener = listener
    }

    fun connect(deviceId: String): Boolean {
        if (isConnected.get()) return true
        
        return try {
            socket = Socket()
            socket?.connect(InetSocketAddress(serverAddress, serverPort), 10000)
            outputStream = DataOutputStream(socket!!.getOutputStream())
            inputStream = DataInputStream(socket!!.getInputStream())
            isConnected.set(true)

            sendLoginPacket(deviceId)
            startListening()

            connectionListener?.invoke(true)
            Log.i(TAG, "Connected to Traccar server at $serverAddress:$serverPort")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to Traccar: ${e.message}")
            cleanup()
            false
        }
    }

    fun disconnect() {
        cleanup()
        Log.i(TAG, "Disconnected from Traccar server")
    }

    private fun cleanup() {
        isConnected.set(false)
        try {
            outputStream?.close()
            inputStream?.close()
            socket?.close()
        } catch (e: Exception) {}
        outputStream = null
        inputStream = null
        socket = null
        connectionListener?.invoke(false)
    }

    fun sendLocationData(
        latitude: Double,
        longitude: Double,
        speed: Float,
        accuracy: Float,
        altitude: Float,
        deviceId: String
    ): Boolean {
        if (!isConnected.get()) return false
        
        return try {
            val packet = buildLocationPacket(latitude, longitude, speed, accuracy, altitude, deviceId)
            outputStream?.write(packet)
            outputStream?.flush()
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error sending location data: ${e.message}")
            cleanup()
            false
        }
    }

    private fun sendLoginPacket(deviceId: String) {
        try {
            val imei = deviceId.padEnd(15, '0').take(15)
            val buffer = ByteBuffer.allocate(22).order(ByteOrder.BIG_ENDIAN)

            buffer.putShort(0x7878.toShort()) // Header
            buffer.put(0x11.toByte())        // Length
            buffer.put(0x01.toByte())        // Protocol ID (Login)

            // IMEI as BCD (8 bytes)
            val imeiBcd = ByteArray(8)
            val paddedImei = "0$imei"
            for (i in 0 until 8) {
                val high = paddedImei[i * 2].digitToInt()
                val low = paddedImei[i * 2 + 1].digitToInt()
                imeiBcd[i] = ((high shl 4) or low).toByte()
            }
            buffer.put(imeiBcd)

            buffer.putShort(0x0001.toShort()) // Serial number
            
            val dataForCrc = buffer.array().copyOfRange(2, 18)
            val crc = calculateCRC16(dataForCrc)
            buffer.putShort(crc.toShort())
            buffer.putShort(0x0D0A.toShort()) // Footer

            outputStream?.write(buffer.array())
            outputStream?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending login packet: ${e.message}")
        }
    }

    private fun buildLocationPacket(
        latitude: Double,
        longitude: Double,
        speed: Float,
        accuracy: Float,
        altitude: Float,
        deviceId: String
    ): ByteArray {
        val buffer = ByteBuffer.allocate(45).order(ByteOrder.BIG_ENDIAN)

        buffer.putShort(0x7878.toShort()) // Header
        val lengthPos = buffer.position()
        buffer.put(0x00.toByte())        // Placeholder Length
        buffer.put(0x12.toByte())        // Protocol ID (Location)

        // Date/Time (6 bytes: YY MM DD HH MM SS)
        val now = java.util.Calendar.getInstance()
        buffer.put((now.get(java.util.Calendar.YEAR) - 2000).toByte())
        buffer.put((now.get(java.util.Calendar.MONTH) + 1).toByte())
        buffer.put(now.get(java.util.Calendar.DAY_OF_MONTH).toByte())
        buffer.put(now.get(java.util.Calendar.HOUR_OF_DAY).toByte())
        buffer.put(now.get(java.util.Calendar.MINUTE).toByte())
        buffer.put(now.get(java.util.Calendar.SECOND).toByte())

        // Quantity of GPS satellites (high 4 bits) and GPS length (low 4 bits)
        buffer.put(0xCC.toByte()) 

        // Latitude (4 bytes)
        val latVal = (latitude * 1800000).toInt()
        buffer.putInt(latVal)

        // Longitude (4 bytes)
        val lonVal = (longitude * 1800000).toInt()
        buffer.putInt(lonVal)

        // Speed (1 byte)
        buffer.put(speed.toInt().toByte())

        // Course/Status (2 bytes)
        buffer.putShort(0x0000.toShort())

        buffer.putShort(0x0001.toShort()) // Serial number

        // Update length
        val currentPos = buffer.position()
        buffer.put(lengthPos, (currentPos - 5).toByte())

        val dataForCrc = buffer.array().copyOfRange(2, currentPos)
        val crc = calculateCRC16(dataForCrc)
        buffer.putShort(crc.toShort())
        buffer.putShort(0x0D0A.toShort()) // Footer

        return buffer.array().copyOfRange(0, buffer.position())
    }

    private fun startListening() {
        listeningThread = Thread {
            try {
                val buffer = ByteArray(1024)
                while (isConnected.get()) {
                    val bytesRead = inputStream?.read(buffer) ?: -1
                    if (bytesRead == -1) break
                    
                    if (bytesRead >= 5) {
                        // Simple command parsing for GT06
                        // Look for Protocol 0x80 (Command)
                        for (i in 0 until bytesRead - 4) {
                            if (buffer[i] == 0x78.toByte() && buffer[i+1] == 0x78.toByte()) {
                                val proto = buffer[i+3].toInt() and 0xFF
                                if (proto == 0x80) {
                                    val len = buffer[i+2].toInt() and 0xFF
                                    val cmd = String(buffer, i + 4, len - 5)
                                    commandListener?.invoke(cmd)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (isConnected.get()) cleanup()
            }
        }
        listeningThread?.start()
    }

    private fun calculateCRC16(data: ByteArray): Int {
        var crc = 0xFFFF
        for (b in data) {
            crc = crc xor (b.toInt() and 0xFF)
            for (i in 0 until 8) {
                crc = if (crc and 0x0001 != 0) (crc shr 1) xor 0xA001 else crc shr 1
            }
        }
        return crc and 0xFFFF
    }

    fun isConnected(): Boolean = isConnected.get()
}
