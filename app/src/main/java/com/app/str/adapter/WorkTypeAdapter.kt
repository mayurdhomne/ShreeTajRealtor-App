package com.app.str.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.str.R
import com.app.str.data.api.WorkType
import com.app.str.databinding.ItemWorkTypeBinding

class WorkTypeAdapter(
    private val workTypes: List<WorkType>,
    private val onItemClick: (WorkType, Int) -> Unit
) : RecyclerView.Adapter<WorkTypeAdapter.WorkTypeViewHolder>() {

    private var selectedPosition = -1

    inner class WorkTypeViewHolder(private val binding: ItemWorkTypeBinding) : 
        RecyclerView.ViewHolder(binding.root) {

        fun bind(workType: WorkType, position: Int) {
            binding.tvWorkTypeName.text = workType.name
            
            // Set selection state
            val isSelected = position == selectedPosition
            binding.cardWorkType.setBackgroundResource(
                if (isSelected) R.drawable.card_selected_background 
                else R.drawable.card_unselected_background
            )
            
            binding.ivSelectionIndicator.visibility = 
                if (isSelected) android.view.View.VISIBLE 
                else android.view.View.GONE

            // Set work type icon based on type
            val iconRes = when (workType.name.lowercase()) {
                "site visit" -> R.drawable.ic_visits
                "booking closed" -> R.drawable.ic_home
                "meeting" -> R.drawable.ic_person
                "calling" -> R.drawable.ic_phonecard
                "field marketing" -> R.drawable.ic_location
                "canopy activity" -> R.drawable.ic_work_outline
                "government office" -> R.drawable.ic_work_outline
                "digital marketing" -> R.drawable.ic_reports
                "personal contact" -> R.drawable.ic_person
                "old customer reference" -> R.drawable.ic_person
                "broker coordination" -> R.drawable.ic_person
                "local area marketing" -> R.drawable.ic_location
                "token collection" -> R.drawable.ic_home
                "outstation visit" -> R.drawable.ic_visits
                "telecalling" -> R.drawable.ic_phonecard
                else -> R.drawable.ic_work_outline
            }
            binding.ivWorkTypeIcon.setImageResource(iconRes)

            // Click listener
            binding.root.setOnClickListener {
                val previousSelectedPosition = selectedPosition
                selectedPosition = position
                
                // Notify changes for better animation
                if (previousSelectedPosition != -1) {
                    notifyItemChanged(previousSelectedPosition)
                }
                notifyItemChanged(selectedPosition)
                
                onItemClick(workType, position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkTypeViewHolder {
        val binding = ItemWorkTypeBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return WorkTypeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WorkTypeViewHolder, position: Int) {
        holder.bind(workTypes[position], position)
    }

    override fun getItemCount(): Int = workTypes.size

    fun getSelectedWorkType(): WorkType? {
        return if (selectedPosition != -1) workTypes[selectedPosition] else null
    }

    fun getSelectedPosition(): Int = selectedPosition

    fun setSelectedPosition(position: Int) {
        val previousSelectedPosition = selectedPosition
        selectedPosition = position
        
        if (previousSelectedPosition != -1) {
            notifyItemChanged(previousSelectedPosition)
        }
        if (selectedPosition != -1) {
            notifyItemChanged(selectedPosition)
        }
    }
}