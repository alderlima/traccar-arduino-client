package com.smartatendimentos.traccar

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var editTextServerAddress: EditText
    private lateinit var editTextDeviceId: EditText
    private lateinit var buttonSave: Button
    private lateinit var buttonCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initializeViews()
        loadSettings()
        setupListeners()
    }

    private fun initializeViews() {
        editTextServerAddress = findViewById(R.id.editTextServerAddress)
        editTextDeviceId = findViewById(R.id.editTextDeviceId)
        buttonSave = findViewById(R.id.buttonSave)
        buttonCancel = findViewById(R.id.buttonCancel)
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("traccar_prefs", Context.MODE_PRIVATE)
        editTextServerAddress.setText(prefs.getString("server_address", "smartatendimentos.shop"))
        editTextDeviceId.setText(prefs.getString("device_id", "123456789"))
    }

    private fun setupListeners() {
        buttonSave.setOnClickListener {
            saveSettings()
        }

        buttonCancel.setOnClickListener {
            finish()
        }
    }

    private fun saveSettings() {
        val serverAddress = editTextServerAddress.text.toString().trim()
        val deviceId = editTextDeviceId.text.toString().trim()

        if (serverAddress.isEmpty() || deviceId.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("traccar_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("server_address", serverAddress)
            putString("device_id", deviceId)
            apply()
        }

        Toast.makeText(this, "Configurações salvas", Toast.LENGTH_SHORT).show()
        finish()
    }
}
