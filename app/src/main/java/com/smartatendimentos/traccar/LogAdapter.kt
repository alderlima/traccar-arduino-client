package com.smartatendimentos.traccar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class LogAdapter : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    private val logs = mutableListOf<LogEntry>()

    inner class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewTimestamp: TextView = itemView.findViewById(R.id.textViewTimestamp)
        private val textViewLevel: TextView = itemView.findViewById(R.id.textViewLevel)
        private val textViewTag: TextView = itemView.findViewById(R.id.textViewTag)
        private val textViewMessage: TextView = itemView.findViewById(R.id.textViewMessage)

        fun bind(log: LogEntry) {
            textViewTimestamp.text = log.timestamp
            textViewTag.text = log.tag
            textViewMessage.text = log.message

            // Set color based on log level
            val textColor = when (log.level) {
                "INFO" -> ContextCompat.getColor(itemView.context, R.color.info)
                "WARN" -> ContextCompat.getColor(itemView.context, R.color.warning)
                "ERROR" -> ContextCompat.getColor(itemView.context, R.color.error)
                "DEBUG" -> ContextCompat.getColor(itemView.context, R.color.text_secondary)
                else -> ContextCompat.getColor(itemView.context, R.color.text_primary)
            }

            textViewLevel.text = log.level
            textViewLevel.setTextColor(textColor)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(logs[position])
    }

    override fun getItemCount(): Int = logs.size

    fun addLog(log: LogEntry) {
        logs.add(log)
        notifyItemInserted(logs.size - 1)
    }

    fun setLogs(newLogs: List<LogEntry>) {
        logs.clear()
        logs.addAll(newLogs)
        notifyDataSetChanged()
    }

    fun clearLogs() {
        logs.clear()
        notifyDataSetChanged()
    }
}
