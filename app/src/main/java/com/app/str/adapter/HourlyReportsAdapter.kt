package com.app.str.adapter

import android.annotation.SuppressLint
import android.location.Geocoder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.str.R
import com.app.str.data.model.HourlyReportDetail
import com.app.str.data.model.HourlyReportResponse
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class HourlyReportsAdapter(
    private var reports: List<HourlyReportResponse>,
    private val onEditDetailClick: (HourlyReportResponse, HourlyReportDetail, Int) -> Unit
) : RecyclerView.Adapter<HourlyReportsAdapter.ReportViewHolder>() {

    private val expandedPositions = mutableSetOf<Int>()
    private val addressCache = mutableMapOf<String, String>()

    class ReportViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvReportDate: TextView = itemView.findViewById(R.id.tvReportDate)
        val tvReportHour: TextView = itemView.findViewById(R.id.tvReportHour)
        val tvWorkStatus: TextView = itemView.findViewById(R.id.tvWorkStatus)
        val tvWorkTypes: TextView = itemView.findViewById(R.id.tvWorkTypes)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvReasonNotDone: TextView = itemView.findViewById(R.id.tvReasonNotDone)
        val layoutReasonNotDone: View = itemView.findViewById(R.id.layoutReasonNotDone)
        val recyclerViewDetails: RecyclerView = itemView.findViewById(R.id.recyclerViewDetails)
        val btnEdit: MaterialButton = itemView.findViewById(R.id.btnEdit)
        val tvDayNumber: TextView = itemView.findViewById(R.id.tvDayNumber)
        val tvMonthShort: TextView = itemView.findViewById(R.id.tvMonthShort)
        val headerLayout: View = itemView.findViewById(R.id.headerLayout)
        val expandableLayout: View = itemView.findViewById(R.id.expandableLayout)
        val ivExpandCollapse: ImageView = itemView.findViewById(R.id.ivExpandCollapse)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hourly_report, parent, false)
        return ReportViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        val report = reports[position]

        // Format date
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
        try {
            val date = dateFormat.parse(report.reportDate)
            holder.tvReportDate.text = date?.let { displayFormat.format(it) } ?: report.reportDate
            holder.tvDayNumber.text = date?.let { dayFormat.format(it) } ?: ""
            holder.tvMonthShort.text = date?.let { monthFormat.format(it) } ?: ""
        } catch (e: Exception) {
            holder.tvReportDate.text = report.reportDate
            holder.tvDayNumber.text = ""
            holder.tvMonthShort.text = ""
        }

        // Report hour - Convert to 12-hour format
        val hour = report.reportHour
        val period = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        holder.tvReportHour.text = "Hour: $displayHour:00 $period"

        // Work status
        if (report.workDone.equals("yes", ignoreCase = true)) {
            holder.tvWorkStatus.text = "Done"
            holder.tvWorkStatus.setBackgroundResource(R.drawable.badge_background)
            holder.tvWorkStatus.backgroundTintList = holder.itemView.context.getColorStateList(R.color.green_success)
            holder.layoutReasonNotDone.visibility = View.GONE
        } else {
            holder.tvWorkStatus.text = "Pending"
            holder.tvWorkStatus.setBackgroundResource(R.drawable.badge_background)
            holder.tvWorkStatus.backgroundTintList = holder.itemView.context.getColorStateList(R.color.red_error)
            if (!report.reasonNotDone.isNullOrEmpty()) {
                holder.layoutReasonNotDone.visibility = View.VISIBLE
                holder.tvReasonNotDone.text = report.reasonNotDone
            } else {
                holder.layoutReasonNotDone.visibility = View.GONE
            }
        }

        // Work types
        val workTypeNames = report.workTypes.joinToString(", ") { it.name }
        holder.tvWorkTypes.text = workTypeNames.ifEmpty { "No work types" }

        // Location - Convert coordinates to address
        val lat = report.locationLatitude.toDoubleOrNull()
        val lng = report.locationLongitude.toDoubleOrNull()
        val cacheKey = "${report.locationLatitude},${report.locationLongitude}"
        
        if (addressCache.containsKey(cacheKey)) {
            holder.tvLocation.text = addressCache[cacheKey]
        } else if (lat != null && lng != null) {
            holder.tvLocation.text = "Loading address..."
            getAddressFromCoordinates(holder, lat, lng, cacheKey)
        } else {
            holder.tvLocation.text = "${report.locationLatitude}, ${report.locationLongitude}"
        }

        // Expand/Collapse handling
        val isExpanded = expandedPositions.contains(position)
        holder.expandableLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.ivExpandCollapse.rotation = if (isExpanded) 180f else 0f
        
        holder.headerLayout.setOnClickListener {
            if (expandedPositions.contains(position)) {
                expandedPositions.remove(position)
                holder.expandableLayout.visibility = View.GONE
                holder.ivExpandCollapse.animate().rotation(0f).setDuration(200).start()
            } else {
                expandedPositions.add(position)
                holder.expandableLayout.visibility = View.VISIBLE
                holder.ivExpandCollapse.animate().rotation(180f).setDuration(200).start()
            }
        }

        // Setup details recycler view
        holder.recyclerViewDetails.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.recyclerViewDetails.adapter = WorkDetailEditableAdapter(report.details) { detail, detailIndex ->
            onEditDetailClick(report, detail, detailIndex)
        }

        // Edit button click
        holder.btnEdit.setOnClickListener {
            // Handle edit all action - open first detail for edit
            if (report.details.isNotEmpty()) {
                onEditDetailClick(report, report.details[0], 0)
            }
        }
    }

    override fun getItemCount(): Int = reports.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateReports(newReports: List<HourlyReportResponse>) {
        reports = newReports
        notifyDataSetChanged()
    }
    
    @Suppress("DEPRECATION")
    private fun getAddressFromCoordinates(holder: ReportViewHolder, lat: Double, lng: Double, cacheKey: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val geocoder = Geocoder(holder.itemView.context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                
                val address = if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    buildString {
                        // Get locality/sublocality for a cleaner address
                        addr.subLocality?.let { append(it) }
                        addr.locality?.let { 
                            if (isNotEmpty()) append(", ")
                            append(it) 
                        }
                        addr.subAdminArea?.let {
                            if (isNotEmpty()) append(", ")
                            append(it)
                        }
                        if (isEmpty()) {
                            // Fallback to full address
                            addr.getAddressLine(0)?.let { append(it) }
                        }
                    }.ifEmpty { "$lat, $lng" }
                } else {
                    "$lat, $lng"
                }
                
                addressCache[cacheKey] = address
                
                withContext(Dispatchers.Main) {
                    holder.tvLocation.text = address
                }
            } catch (e: Exception) {
                val fallback = "$lat, $lng"
                addressCache[cacheKey] = fallback
                withContext(Dispatchers.Main) {
                    holder.tvLocation.text = fallback
                }
            }
        }
    }
}