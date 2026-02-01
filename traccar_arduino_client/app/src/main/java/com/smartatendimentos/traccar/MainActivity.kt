package com.smartatendimentos.traccar

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.smartatendimentos.traccar.service.TraccarService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var spinnerBaudRate: Spinner
    private lateinit var buttonConnectArduino: Button
    private lateinit var buttonDisconnectArduino: Button
    private lateinit var buttonConnectTraccar: Button
    private lateinit var buttonDisconnectTraccar: Button
    private lateinit var buttonActivateCut: Button
    private lateinit var buttonDeactivateCut: Button
    private lateinit var buttonSettings: Button
    private lateinit var buttonLogs: Button

    private lateinit var textViewArduinoStatus: TextView
    private lateinit var textViewTraccarStatus: TextView
    private lateinit var textViewLatitude: TextView
    private lateinit var textViewLongitude: TextView
    private lateinit var textViewSpeed: TextView

    private lateinit var locationManager: LocationManager
    private lateinit var arduinoCommunication: ArduinoCommunication
    private lateinit var gt06Protocol: GT06Protocol
    private val logManager = LogManager.getInstance()

    private var isArduinoConnected = false
    private var isTraccarConnected = false
    private val scope = CoroutineScope(Dispatchers.Main)

    private val baudRates = intArrayOf(9600, 19200, 38400, 57600, 115200)

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        initializeCommunication()
        setupBaudRateSpinner()
        setupListeners()
        checkAndRequestPermissions()

        logManager.logInfo(TAG, "MainActivity created")
    }

    private fun initializeViews() {
        spinnerBaudRate = findViewById(R.id.spinnerBaudRate)
        buttonConnectArduino = findViewById(R.id.buttonConnectArduino)
        buttonDisconnectArduino = findViewById(R.id.buttonDisconnectArduino)
        buttonConnectTraccar = findViewById(R.id.buttonConnectTraccar)
        buttonDisconnectTraccar = findViewById(R.id.buttonDisconnectTraccar)
        buttonActivateCut = findViewById(R.id.buttonActivateCut)
        buttonDeactivateCut = findViewById(R.id.buttonDeactivateCut)
        buttonSettings = findViewById(R.id.buttonSettings)
        buttonLogs = findViewById(R.id.buttonLogs)

        textViewArduinoStatus = findViewById(R.id.textViewArduinoStatus)
        textViewTraccarStatus = findViewById(R.id.textViewTraccarStatus)
        textViewLatitude = findViewById(R.id.textViewLatitude)
        textViewLongitude = findViewById(R.id.textViewLongitude)
        textViewSpeed = findViewById(R.id.textViewSpeed)

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private fun initializeCommunication() {
        val prefs = getSharedPreferences("traccar_prefs", Context.MODE_PRIVATE)
        val serverAddress = prefs.getString("server_address", "smartatendimentos.shop") ?: "smartatendimentos.shop"
        
        gt06Protocol = GT06Protocol(serverAddress, 5023)
        arduinoCommunication = ArduinoCommunication(this)

        gt06Protocol.setConnectionListener { connected ->
            runOnUiThread {
                isTraccarConnected = connected
                updateTraccarStatus(connected)
            }
        }

        arduinoCommunication.setConnectionListener { connected ->
            runOnUiThread {
                isArduinoConnected = connected
                updateArduinoStatus(connected)
                buttonActivateCut.isEnabled = connected
                buttonDeactivateCut.isEnabled = connected
            }
        }

        gt06Protocol.setCommandListener { command ->
            runOnUiThread { handleTraccarCommand(command) }
        }
    }

    private fun setupBaudRateSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            baudRates.map { it.toString() }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBaudRate.adapter = adapter
        spinnerBaudRate.setSelection(0)
    }

    private fun setupListeners() {
        buttonConnectArduino.setOnClickListener { connectArduino() }
        buttonDisconnectArduino.setOnClickListener { disconnectArduino() }
        buttonConnectTraccar.setOnClickListener { startTraccarService() }
        buttonDisconnectTraccar.setOnClickListener { stopTraccarService() }
        
        buttonActivateCut.setOnClickListener {
            if (arduinoCommunication.activateEngineCut()) {
                Toast.makeText(this, "Corte ativado", Toast.LENGTH_SHORT).show()
            }
        }

        buttonDeactivateCut.setOnClickListener {
            if (arduinoCommunication.deactivateEngineCut()) {
                Toast.makeText(this, "Corte desativado", Toast.LENGTH_SHORT).show()
            }
        }

        buttonSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        buttonLogs.setOnClickListener {
            startActivity(Intent(this, LogsActivity::class.java))
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun startTraccarService() {
        val serviceIntent = Intent(this, TraccarService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        buttonConnectTraccar.isEnabled = false
        buttonDisconnectTraccar.isEnabled = true
        updateTraccarStatus(true)
    }

    private fun stopTraccarService() {
        stopService(Intent(this, TraccarService::class.java))
        buttonConnectTraccar.isEnabled = true
        buttonDisconnectTraccar.isEnabled = false
        updateTraccarStatus(false)
    }

    private fun connectArduino() {
        val selectedBaudRate = baudRates[spinnerBaudRate.selectedItemPosition]
        arduinoCommunication.setBaudRate(selectedBaudRate)
        if (arduinoCommunication.connect()) {
            buttonConnectArduino.isEnabled = false
            buttonDisconnectArduino.isEnabled = true
            spinnerBaudRate.isEnabled = false
        }
    }

    private fun disconnectArduino() {
        arduinoCommunication.disconnect()
        buttonConnectArduino.isEnabled = true
        buttonDisconnectArduino.isEnabled = false
        spinnerBaudRate.isEnabled = true
    }

    private fun updateArduinoStatus(connected: Boolean) {
        textViewArduinoStatus.text = if (connected) "Arduino: Conectado" else "Arduino: Desconectado"
        textViewArduinoStatus.setTextColor(if (connected) getColor(android.R.color.holo_green_dark) else getColor(android.R.color.holo_red_dark))
    }

    private fun updateTraccarStatus(connected: Boolean) {
        textViewTraccarStatus.text = if (connected) "Traccar: Ativo" else "Traccar: Inativo"
        textViewTraccarStatus.setTextColor(if (connected) getColor(android.R.color.holo_green_dark) else getColor(android.R.color.holo_red_dark))
    }

    private fun handleTraccarCommand(command: String) {
        when (command.lowercase()) {
            "engine stop", "corte" -> arduinoCommunication.activateEngineCut()
            "engine resume", "restaurar" -> arduinoCommunication.deactivateEngineCut()
        }
        Toast.makeText(this, "Comando recebido: $command", Toast.LENGTH_SHORT).show()
    }

    override fun onLocationChanged(location: Location) {
        textViewLatitude.text = "Lat: ${location.latitude}"
        textViewLongitude.text = "Lon: ${location.longitude}"
        textViewSpeed.text = "Vel: ${location.speed} km/h"
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
}
