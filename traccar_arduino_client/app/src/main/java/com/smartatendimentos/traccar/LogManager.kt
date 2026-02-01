package com.smartatendimentos.traccar

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

data class LogEntry(
    val timestamp: String,
    val level: String,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null
) {
    val fullMessage: String
        get() = "$timestamp [$level/$tag] $message${if (throwable != null) "\n${throwable.stackTraceToString()}" else ""}"
}

class LogManager {
    private val logs = CopyOnWriteArrayList<LogEntry>()
    private val listeners = mutableListOf<(LogEntry) -> Unit>()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    companion object {
        private var instance: LogManager? = null

        fun getInstance(): LogManager {
            if (instance == null) {
                instance = LogManager()
            }
            return instance!!
        }
    }

    fun addLogListener(listener: (LogEntry) -> Unit) {
        listeners.add(listener)
    }

    fun removeLogListener(listener: (LogEntry) -> Unit) {
        listeners.remove(listener)
    }

    fun logInfo(tag: String, message: String) {
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            level = "INFO",
            tag = tag,
            message = message
        )
        addLog(entry)
        Log.i(tag, message)
    }

    fun logWarn(tag: String, message: String, throwable: Throwable? = null) {
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            level = "WARN",
            tag = tag,
            message = message,
            throwable = throwable
        )
        addLog(entry)
        Log.w(tag, message, throwable)
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            level = "ERROR",
            tag = tag,
            message = message,
            throwable = throwable
        )
        addLog(entry)
        Log.e(tag, message, throwable)
    }

    fun logDebug(tag: String, message: String) {
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            level = "DEBUG",
            tag = tag,
            message = message
        )
        addLog(entry)
        Log.d(tag, message)
    }

    private fun addLog(entry: LogEntry) {
        logs.add(entry)
        // Keep only last 500 logs to avoid memory issues
        if (logs.size > 500) {
            logs.removeAt(0)
        }
        notifyListeners(entry)
    }

    private fun notifyListeners(entry: LogEntry) {
        listeners.forEach { it.invoke(entry) }
    }

    fun getLogs(): List<LogEntry> = logs.toList()

    fun getFilteredLogs(level: String? = null): List<LogEntry> {
        return if (level == null || level == "ALL") {
            logs.toList()
        } else {
            logs.filter { it.level == level }
        }
    }

    fun clearLogs() {
        logs.clear()
        notifyListeners(LogEntry(
            timestamp = dateFormat.format(Date()),
            level = "INFO",
            tag = "LogManager",
            message = "Logs cleared"
        ))
    }

    fun exportLogs(): String {
        return logs.joinToString("\n") { it.fullMessage }
    }
}
