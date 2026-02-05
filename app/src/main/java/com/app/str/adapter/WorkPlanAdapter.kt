package com.app.str.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.str.R
import com.app.str.data.model.WorkPlanItem
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.*

class WorkPlanAdapter(
    private val onEditClick: (WorkPlanItem) -> Unit,
    private val onDeleteClick: (WorkPlanItem) -> Unit,
    private val onStatusClick: (WorkPlanItem) -> Unit
) : ListAdapter<WorkPlanItem, WorkPlanAdapter.WorkPlanViewHolder>(WorkPlanDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkPlanViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_work_plan, parent, false)
        return WorkPlanViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: WorkPlanViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class WorkPlanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDayName: TextView = itemView.findViewById(R.id.tvDayName)
        private val btnViewDetails: MaterialButton = itemView.findViewById(R.id.btnViewDetails)
        private val chipStatus: Chip = itemView.findViewById(R.id.chipStatus)
        private val tvCreatedBy: TextView = itemView.findViewById(R.id.tvCreatedBy)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvCreatedAt: TextView = itemView.findViewById(R.id.tvCreatedAt)
        private val recyclerViewTitles: RecyclerView = itemView.findViewById(R.id.recyclerViewTitles)
        
        fun bind(workPlan: WorkPlanItem) {
            // Set day name
            tvDayName.text = getDayName(workPlan.date)
            
            // Set up View Details button click listener
            btnViewDetails.setOnClickListener {
                showWorkPlanDetailsBottomSheet(workPlan)
            }
            
            // Set status
            chipStatus.text = workPlan.status.replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
            }
            
            // Set status color
            when (workPlan.status.lowercase()) {
                "pending" -> {
                    chipStatus.setChipBackgroundColorResource(R.color.gradient_end)
                    chipStatus.setTextColor(itemView.context.getColor(R.color.white))
                }
                "in_process" -> {
                    chipStatus.setChipBackgroundColorResource(R.color.warning_orange)
                    chipStatus.setTextColor(itemView.context.getColor(R.color.white))
                }
                "completed" -> {
                    chipStatus.setChipBackgroundColorResource(R.color.success_green)
                    chipStatus.setTextColor(itemView.context.getColor(R.color.white))
                }
            }
            
            // Set click listener on status chip
            chipStatus.setOnClickListener {
                onStatusClick(workPlan)
            }
            
            // Set created by badge
            val isUserCreated = workPlan.createdBy != "admin"
            tvCreatedBy.text = if (isUserCreated) "Your Plan" else "Allotted Plan"
            tvCreatedBy.setTextColor(
                if (isUserCreated) 
                    itemView.context.getColor(R.color.gradient_end)
                else 
                    itemView.context.getColor(R.color.text_secondary)
            )
            
            // Set description
            tvDescription.text = workPlan.description ?: "No description"
            
            // Set date
            tvDate.text = formatDate(workPlan.date)
            
            // Set created at
            tvCreatedAt.text = formatDateTime(workPlan.createdAt)
            
            // Setup titles recycler view
            val titlesAdapter = WorkPlanTitleAdapter()
            recyclerViewTitles.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = titlesAdapter
            }
            titlesAdapter.submitList(workPlan.titles)
        }
        
        private fun formatDate(dateStr: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateStr)
                date?.let { outputFormat.format(it) } ?: dateStr
            } catch (e: Exception) {
                dateStr
            }
        }
        
        private fun formatDateTime(dateTimeStr: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateTimeStr.substringBefore("+"))
                date?.let { outputFormat.format(it) } ?: dateTimeStr
            } catch (e: Exception) {
                dateTimeStr
            }
        }
        
        private fun getDayName(dateStr: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                val date = inputFormat.parse(dateStr)
                date?.let { outputFormat.format(it) } ?: ""
            } catch (e: Exception) {
                ""
            }
        }
        
        private fun showWorkPlanDetailsBottomSheet(workPlan: WorkPlanItem) {
            val bottomSheetDialog = BottomSheetDialog(itemView.context)
            val bottomSheetView = LayoutInflater.from(itemView.context)
                .inflate(R.layout.bottom_sheet_work_plan_details, null)
            
            // Setup views
            val tvDetailDayName: TextView = bottomSheetView.findViewById(R.id.tvDetailDayName)
            val chipDetailStatus: Chip = bottomSheetView.findViewById(R.id.chipDetailStatus)
            val tvDetailCreatedBy: TextView = bottomSheetView.findViewById(R.id.tvDetailCreatedBy)
            val tvDetailDescription: TextView = bottomSheetView.findViewById(R.id.tvDetailDescription)
            val tvDetailDate: TextView = bottomSheetView.findViewById(R.id.tvDetailDate)
            val tvDetailCreatedAt: TextView = bottomSheetView.findViewById(R.id.tvDetailCreatedAt)
            val recyclerViewDetailTitles: RecyclerView = bottomSheetView.findViewById(R.id.recyclerViewDetailTitles)
            val layoutCoworkers: LinearLayout = bottomSheetView.findViewById(R.id.layoutCoworkers)
            val recyclerViewCoworkers: RecyclerView = bottomSheetView.findViewById(R.id.recyclerViewCoworkers)
            
            // Set day name with date
            val dayName = getDayName(workPlan.date)
            val formattedDate = formatDate(workPlan.date)
            tvDetailDayName.text = "$dayName, $formattedDate"
            
            // Set status
            chipDetailStatus.text = workPlan.status.replaceFirstChar { 
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
            }
            
            // Set status color
            when (workPlan.status.lowercase()) {
                "pending" -> {
                    chipDetailStatus.setChipBackgroundColorResource(R.color.gradient_end)
                    chipDetailStatus.setTextColor(itemView.context.getColor(R.color.white))
                }
                "in_process" -> {
                    chipDetailStatus.setChipBackgroundColorResource(R.color.warning_orange)
                    chipDetailStatus.setTextColor(itemView.context.getColor(R.color.white))
                }
                "completed" -> {
                    chipDetailStatus.setChipBackgroundColorResource(R.color.success_green)
                    chipDetailStatus.setTextColor(itemView.context.getColor(R.color.white))
                }
            }
            
            // Set created by
            val isUserCreated = workPlan.createdBy != "admin"
            tvDetailCreatedBy.text = if (isUserCreated) "Your Plan" else "Allotted Plan"
            tvDetailCreatedBy.setTextColor(
                if (isUserCreated) 
                    itemView.context.getColor(R.color.gradient_end)
                else 
                    itemView.context.getColor(R.color.text_secondary)
            )
            
            // Set description - full text without ellipsis
            tvDetailDescription.text = workPlan.description ?: "No description available"
            
            // Set date
            tvDetailDate.text = formattedDate
            
            // Set created at
            tvDetailCreatedAt.text = formatDateTime(workPlan.createdAt)
            
            // Setup titles recycler view
            val titlesAdapter = WorkPlanTitleAdapter()
            recyclerViewDetailTitles.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = titlesAdapter
            }
            titlesAdapter.submitList(workPlan.titles)
            
            // Setup coworkers if available
            if (workPlan.coworkers.isNotEmpty()) {
                layoutCoworkers.visibility = View.VISIBLE
                val coworkersAdapter = CoworkerAdapter()
                recyclerViewCoworkers.apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = coworkersAdapter
                }
                coworkersAdapter.submitList(workPlan.coworkers)
            } else {
                layoutCoworkers.visibility = View.GONE
            }
            
            bottomSheetDialog.setContentView(bottomSheetView)
            bottomSheetDialog.show()
        }
    }
}

class WorkPlanDiffCallback : DiffUtil.ItemCallback<WorkPlanItem>() {
    override fun areItemsTheSame(oldItem: WorkPlanItem, newItem: WorkPlanItem): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: WorkPlanItem, newItem: WorkPlanItem): Boolean {
        return oldItem == newItem
    }
}
