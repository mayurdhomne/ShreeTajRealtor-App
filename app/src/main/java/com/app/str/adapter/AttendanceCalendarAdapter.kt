package com.app.str.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.app.str.R

class AttendanceCalendarAdapter(
    private val context: Context,
    private val attendanceData: List<String>
) : RecyclerView.Adapter<AttendanceCalendarAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayText: TextView = itemView.findViewById(R.id.tvDay)
        val statusView: View = itemView.findViewById(R.id.statusIndicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_attendance_day, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dayNumber = position + 1
        val status = if (position < attendanceData.size) attendanceData[position] else "-"
        
        holder.dayText.text = dayNumber.toString()
        
        when (status) {
            "P" -> {
                holder.statusView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_green_light))
                holder.dayText.setTextColor(Color.WHITE)
            }
            "A" -> {
                holder.statusView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_red_light))
                holder.dayText.setTextColor(Color.WHITE)
            }
            "H" -> { // Half day
                holder.statusView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.holo_orange_light))
                holder.dayText.setTextColor(Color.WHITE)
            }
            "-" -> { // Future day
                holder.statusView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                holder.dayText.setTextColor(Color.WHITE)
            }
            else -> {
                holder.statusView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
                holder.dayText.setTextColor(Color.WHITE)
            }
        }
    }

    override fun getItemCount(): Int = 31 // Maximum days in a month
}