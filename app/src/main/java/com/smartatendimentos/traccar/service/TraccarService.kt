package com.smartatendimentos.traccar.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.smartatendimentos.traccar.ArduinoCommunication
import com.smartatendimentos.traccar.GT06Protocol
import com.smartatendimentos.traccar.MainActivity
import com.smartatendimentos.traccar.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TraccarService : Service(), LocationListener {

    private lateinit var locationManager: LocationManager
    private var gt06Protocol: GT06Protocol? = null
    private var arduinoCommunication: ArduinoCommunication? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isTracking = false
    private var deviceId: String = "123456789"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Service created")
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service started")

        val prefs = getSharedPreferences("traccar_prefs", Context.MODE_PRIVATE)
        val serverAddress = intent?.getStringExtra("server_address") 
            ?: prefs.getString("server_address", "smartatendimentos.shop") 
            ?: "smartatendimentos.shop"
        deviceId = intent?.getStringExtra("device_id") 
            ?: prefs.getString("device_id", "123456789") 
            ?: "123456789"

        if (gt06Protocol == null) {
            gt06Protocol = GT06Protocol(serverAddress, 5023)
            setupCommandListener()
        }
        
        if (arduinoCommunication == null) {
            arduinoCommunication = ArduinoCommunication(this)
        }

        startTracking()

        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private fun startTracking() {
        if (isTracking) return
        
        scope.launch {
            try {
                // Connect to Traccar
                gt06Protocol?.connect(deviceId)

                // Connect to Arduino
                arduinoCommunication?.connect()

                // Request location updates
                launch(Dispatchers.Main) {
                    if (ActivityCompat.checkSelfPermission(
                            this@TraccarService,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            10000, // 10 seconds to save battery
                            5f,    // 5 meters
                            this@TraccarService
                        )
                        isTracking = true
                        Log.i(TAG, "Tracking started")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting tracking: ${e.message}")
            }
        }
    }

    private fun stopTracking() {
        try {
            locationManager.removeUpdates(this)
            gt06Protocol?.disconnect()
            arduinoCommunication?.disconnect()
            isTracking = false
            Log.i(TAG, "Tracking stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping tracking: ${e.message}")
        }
    }

    override fun onLocationChanged(location: Location) {
        Log.i(TAG, "Location updated: ${location.latitude}, ${location.longitude}")
        
        scope.launch {
            // Send location to Traccar
            gt06Protocol?.sendLocationData(
                location.latitude,
                location.longitude,
                location.speed,
                location.accuracy,
                location.altitude.toFloat(),
                deviceId
            )
        }
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    private fun setupCommandListener() {
        gt06Protocol?.setCommandListener { command ->
            Log.i(TAG, "Command from Traccar: $command")
            when (command.lowercase()) {
                "engine stop", "corte" -> {
                    arduinoCommunication?.activateEngineCut()
                    Log.i(TAG, "Engine cut activated")
                }
                "engine resume", "restaurar" -> {
                    arduinoCommunication?.deactivateEngineCut()
                    Log.i(TAG, "Engine cut deactivated")
                }
            }
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Traccar Arduino Client")
            .setContentText("Rastreamento ativo em segundo plano")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Traccar Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopTracking()
        scope.cancel()
        super.onDestroy()
        Log.i(TAG, "Service destroyed")
    }

    companion object {
        private const val TAG = "TraccarService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "traccar_channel"
    }
}
