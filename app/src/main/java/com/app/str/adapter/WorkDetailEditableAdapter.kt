package com.app.str.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.str.R
import com.app.str.data.model.HourlyReportDetail
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.util.Locale

class WorkDetailEditableAdapter(
    private val details: List<HourlyReportDetail>,
    private val onEditClick: (HourlyReportDetail, Int) -> Unit
) : RecyclerView.Adapter<WorkDetailEditableAdapter.WorkDetailViewHolder>() {

    class WorkDetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        val tvMobileNumber: TextView = itemView.findViewById(R.id.tvMobileNumber)
        val tvPlotNumber: TextView = itemView.findViewById(R.id.tvPlotNumber)
        val tvCustomerResponse: TextView = itemView.findViewById(R.id.tvCustomerResponse)
        val tvArea: TextView = itemView.findViewById(R.id.tvArea)
        val tvTotalValue: TextView = itemView.findViewById(R.id.tvTotalValue)
        val tvFeedback: TextView = itemView.findViewById(R.id.tvFeedback)
        val layoutStatus: LinearLayout = itemView.findViewById(R.id.layoutStatus)
        val layoutSiteVisit: LinearLayout = itemView.findViewById(R.id.layoutSiteVisit)
        val layoutMeeting: LinearLayout = itemView.findViewById(R.id.layoutMeeting)
        val layoutBooking: LinearLayout = itemView.findViewById(R.id.layoutBooking)
        val layoutAreaValue: LinearLayout = itemView.findViewById(R.id.layoutAreaValue)
        val btnEditDetail: MaterialButton = itemView.findViewById(R.id.btnEditDetail)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkDetailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer_detail_editable, parent, false)
        return WorkDetailViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: WorkDetailViewHolder, position: Int) {
        val detail = details[position]

        // Customer name and mobile
        holder.tvCustomerName.text = detail.customerName
        holder.tvMobileNumber.text = detail.mobileNumber

        // Plot number
        if (!detail.plotNumber.isNullOrEmpty()) {
            holder.tvPlotNumber.visibility = View.VISIBLE
            holder.tvPlotNumber.text = "Plot: ${detail.plotNumber}"
        } else {
            holder.tvPlotNumber.visibility = View.GONE
        }

        // Customer response
        if (!detail.customerResponse.isNullOrEmpty()) {
            holder.tvCustomerResponse.visibility = View.VISIBLE
            val responseText = when (detail.customerResponse) {
                "interested" -> "Interested"
                "not_interested" -> "Not Interested"
                else -> detail.customerResponse
            }
            holder.tvCustomerResponse.text = "Response: $responseText"
        } else {
            holder.tvCustomerResponse.visibility = View.GONE
        }

        // Status checkboxes
        var hasStatus = false
        if (detail.siteVisitDone) {
            holder.layoutSiteVisit.visibility = View.VISIBLE
            hasStatus = true
        } else {
            holder.layoutSiteVisit.visibility = View.GONE
        }

        if (detail.meetingDone) {
            holder.layoutMeeting.visibility = View.VISIBLE
            hasStatus = true
        } else {
            holder.layoutMeeting.visibility = View.GONE
        }

        if (detail.bookingDone) {
            holder.layoutBooking.visibility = View.VISIBLE
            hasStatus = true
        } else {
            holder.layoutBooking.visibility = View.GONE
        }

        holder.layoutStatus.visibility = if (hasStatus) View.VISIBLE else View.GONE

        // Area and Total Value
        if (!detail.area.isNullOrEmpty() || !detail.totalValue.isNullOrEmpty()) {
            holder.layoutAreaValue.visibility = View.VISIBLE
            
            if (!detail.area.isNullOrEmpty()) {
                holder.tvArea.visibility = View.VISIBLE
                holder.tvArea.text = "Area: ${detail.area} sq.ft"
            } else {
                holder.tvArea.visibility = View.GONE
            }

            if (!detail.totalValue.isNullOrEmpty()) {
                holder.tvTotalValue.visibility = View.VISIBLE
                try {
                    val value = detail.totalValue.toDoubleOrNull()
                    if (value != null) {
                        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                        holder.tvTotalValue.text = formatter.format(value).replace("₹", "₹")
                    } else {
                        holder.tvTotalValue.text = "₹${detail.totalValue}"
                    }
                } catch (e: Exception) {
                    holder.tvTotalValue.text = "₹${detail.totalValue}"
                }
            } else {
                holder.tvTotalValue.visibility = View.GONE
            }
        } else {
            holder.layoutAreaValue.visibility = View.GONE
        }

        // Feedback
        if (!detail.feedback.isNullOrEmpty()) {
            holder.tvFeedback.visibility = View.VISIBLE
            holder.tvFeedback.text = "Feedback: ${detail.feedback}"
        } else {
            holder.tvFeedback.visibility = View.GONE
        }

        // Edit button click
        holder.btnEditDetail.setOnClickListener {
            onEditClick(detail, position)
        }
    }

    override fun getItemCount(): Int = details.size
}
