package com.app.str.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.str.R
import com.app.str.data.model.Coworker

class CoworkerAdapter : ListAdapter<Coworker, CoworkerAdapter.CoworkerViewHolder>(CoworkerDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoworkerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_coworker, parent, false)
        return CoworkerViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: CoworkerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class CoworkerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCoworkerName: TextView = itemView.findViewById(R.id.tvCoworkerName)
        private val tvCoworkerEmail: TextView = itemView.findViewById(R.id.tvCoworkerEmail)
        
        fun bind(coworker: Coworker) {
            tvCoworkerName.text = coworker.username
            
            if (!coworker.email.isNullOrEmpty()) {
                tvCoworkerEmail.visibility = View.VISIBLE
                tvCoworkerEmail.text = coworker.email
            } else {
                tvCoworkerEmail.visibility = View.GONE
            }
        }
    }
}

class CoworkerDiffCallback : DiffUtil.ItemCallback<Coworker>() {
    override fun areItemsTheSame(oldItem: Coworker, newItem: Coworker): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: Coworker, newItem: Coworker): Boolean {
        return oldItem == newItem
    }
}
