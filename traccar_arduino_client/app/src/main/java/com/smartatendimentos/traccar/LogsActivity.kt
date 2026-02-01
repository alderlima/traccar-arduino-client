package com.smartatendimentos.traccar

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LogsActivity : AppCompatActivity() {

    private lateinit var recyclerViewLogs: RecyclerView
    private lateinit var buttonClearLogs: Button
    private lateinit var buttonFilterAll: Button
    private lateinit var buttonFilterInfo: Button
    private lateinit var buttonFilterWarn: Button
    private lateinit var buttonFilterError: Button

    private lateinit var logAdapter: LogAdapter
    private val logManager = LogManager.getInstance()
    private var currentFilter = "ALL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)

        initializeViews()
        setupRecyclerView()
        setupListeners()
        loadLogs()
    }

    private fun initializeViews() {
        recyclerViewLogs = findViewById(R.id.recyclerViewLogs)
        buttonClearLogs = findViewById(R.id.buttonClearLogs)
        buttonFilterAll = findViewById(R.id.buttonFilterAll)
        buttonFilterInfo = findViewById(R.id.buttonFilterInfo)
        buttonFilterWarn = findViewById(R.id.buttonFilterWarn)
        buttonFilterError = findViewById(R.id.buttonFilterError)
    }

    private fun setupRecyclerView() {
        logAdapter = LogAdapter()
        recyclerViewLogs.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // Start from bottom
        }
        recyclerViewLogs.adapter = logAdapter
    }

    private fun setupListeners() {
        buttonClearLogs.setOnClickListener {
            logManager.clearLogs()
            logAdapter.clearLogs()
        }

        buttonFilterAll.setOnClickListener {
            currentFilter = "ALL"
            loadLogs()
            updateFilterButtonStates()
        }

        buttonFilterInfo.setOnClickListener {
            currentFilter = "INFO"
            loadLogs()
            updateFilterButtonStates()
        }

        buttonFilterWarn.setOnClickListener {
            currentFilter = "WARN"
            loadLogs()
            updateFilterButtonStates()
        }

        buttonFilterError.setOnClickListener {
            currentFilter = "ERROR"
            loadLogs()
            updateFilterButtonStates()
        }

        // Listen for new logs
        logManager.addLogListener { log ->
            if (shouldShowLog(log)) {
                logAdapter.addLog(log)
                recyclerViewLogs.scrollToPosition(logAdapter.itemCount - 1)
            }
        }
    }

    private fun loadLogs() {
        val filteredLogs = logManager.getFilteredLogs(
            if (currentFilter == "ALL") null else currentFilter
        )
        logAdapter.setLogs(filteredLogs)
        if (filteredLogs.isNotEmpty()) {
            recyclerViewLogs.scrollToPosition(filteredLogs.size - 1)
        }
    }

    private fun shouldShowLog(log: LogEntry): Boolean {
        return currentFilter == "ALL" || log.level == currentFilter
    }

    private fun updateFilterButtonStates() {
        val selectedColor = getColor(R.color.primary)
        val defaultColor = getColor(R.color.surface)
        val selectedTextColor = getColor(R.color.white)
        val defaultTextColor = getColor(R.color.text_primary)

        buttonFilterAll.setBackgroundColor(if (currentFilter == "ALL") selectedColor else defaultColor)
        buttonFilterAll.setTextColor(if (currentFilter == "ALL") selectedTextColor else defaultTextColor)

        buttonFilterInfo.setBackgroundColor(if (currentFilter == "INFO") selectedColor else defaultColor)
        buttonFilterInfo.setTextColor(if (currentFilter == "INFO") selectedTextColor else defaultTextColor)

        buttonFilterWarn.setBackgroundColor(if (currentFilter == "WARN") selectedColor else defaultColor)
        buttonFilterWarn.setTextColor(if (currentFilter == "WARN") selectedTextColor else defaultTextColor)

        buttonFilterError.setBackgroundColor(if (currentFilter == "ERROR") selectedColor else defaultColor)
        buttonFilterError.setTextColor(if (currentFilter == "ERROR") selectedTextColor else defaultTextColor)
    }

    override fun onResume() {
        super.onResume()
        loadLogs()
        updateFilterButtonStates()
    }
}
