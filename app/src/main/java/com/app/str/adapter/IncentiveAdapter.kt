package com.app.str.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.str.R
import com.app.str.data.model.IncentiveResponse
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class IncentiveAdapter : ListAdapter<IncentiveResponse, IncentiveAdapter.IncentiveViewHolder>(IncentiveDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncentiveViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_incentive, parent, false)
        return IncentiveViewHolder(view)
    }

    override fun onBindViewHolder(holder: IncentiveViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class IncentiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPlotNumber: TextView = itemView.findViewById(R.id.tvPlotNumber)
        private val tvMouza: TextView = itemView.findViewById(R.id.tvMouza)
        private val tvDealDate: TextView = itemView.findViewById(R.id.tvDealDate)
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        private val tvCustomerMobile: TextView = itemView.findViewById(R.id.tvCustomerMobile)
        private val tvTotalPrice: TextView = itemView.findViewById(R.id.tvTotalPrice)
        private val tvCommissionPrice: TextView = itemView.findViewById(R.id.tvCommissionPrice)
        private val tvAdvanceCommission: TextView = itemView.findViewById(R.id.tvAdvanceCommission)
        private val tvTotalPaid: TextView = itemView.findViewById(R.id.tvTotalPaid)
        private val tvBalanceCommission: TextView = itemView.findViewById(R.id.tvBalanceCommission)
        private val tvRemarks: TextView = itemView.findViewById(R.id.tvRemarks)
        private val llRemarks: LinearLayout = itemView.findViewById(R.id.llRemarks)

        fun bind(incentive: IncentiveResponse) {
            tvPlotNumber.text = "Plot ${incentive.plotNumber}"
            tvMouza.text = incentive.mouza
            tvDealDate.text = formatDate(incentive.dealDate)
            tvCustomerName.text = incentive.customerName
            tvCustomerMobile.text = incentive.customerMobile
            
            // Format currency values
            tvTotalPrice.text = formatCurrency(incentive.totalPrice)
            tvCommissionPrice.text = formatCurrency(incentive.commissionPrice)
            tvAdvanceCommission.text = formatCurrency(incentive.advanceCommission)
            tvTotalPaid.text = formatCurrency(incentive.totalPaidCommission)
            tvBalanceCommission.text = formatCurrency(incentive.balanceCommission)
            
            // Show/hide remarks
            if (incentive.remarks.isNotBlank()) {
                llRemarks.visibility = View.VISIBLE
                tvRemarks.text = incentive.remarks
            } else {
                llRemarks.visibility = View.GONE
            }
        }

        private fun formatDate(dateString: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                date?.let { outputFormat.format(it) } ?: dateString
            } catch (e: Exception) {
                dateString
            }
        }

        private fun formatCurrency(amount: String): String {
            return try {
                val value = amount.toDoubleOrNull() ?: 0.0
                val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                formatter.format(value)
            } catch (e: Exception) {
                "₹$amount"
            }
        }
    }

    class IncentiveDiffCallback : DiffUtil.ItemCallback<IncentiveResponse>() {
        override fun areItemsTheSame(oldItem: IncentiveResponse, newItem: IncentiveResponse): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: IncentiveResponse, newItem: IncentiveResponse): Boolean {
            return oldItem == newItem
        }
    }
}
