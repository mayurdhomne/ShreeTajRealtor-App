package com.app.str.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.str.R
import com.app.str.data.model.WorkPlanTitle

class WorkPlanTitleAdapter : ListAdapter<WorkPlanTitle, WorkPlanTitleAdapter.TitleViewHolder>(TitleDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TitleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_work_title, parent, false)
        return TitleViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TitleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class TitleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvTitleDescription: TextView = itemView.findViewById(R.id.tvTitleDescription)
        
        fun bind(title: WorkPlanTitle) {
            tvTitle.text = title.title
            
            if (!title.description.isNullOrEmpty()) {
                tvTitleDescription.visibility = View.VISIBLE
                tvTitleDescription.text = title.description
            } else {
                tvTitleDescription.visibility = View.GONE
            }
        }
    }
}

class TitleDiffCallback : DiffUtil.ItemCallback<WorkPlanTitle>() {
    override fun areItemsTheSame(oldItem: WorkPlanTitle, newItem: WorkPlanTitle): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: WorkPlanTitle, newItem: WorkPlanTitle): Boolean {
        return oldItem == newItem
    }
}
