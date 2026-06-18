package com.hermes.client.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hermes.client.R
import com.hermes.client.model.SessionInfo
import java.text.SimpleDateFormat
import java.util.*

class SessionAdapter(
    private val onItemClick: (SessionInfo) -> Unit
) : ListAdapter<SessionInfo, SessionAdapter.SessionViewHolder>(SessionDiffCallback()) {

    class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.textTitle)
        val date: TextView = itemView.findViewById(R.id.textDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = getItem(position)
        holder.title.text = session.title ?: "Session ${session.id.take(8)}"
        holder.date.text = formatDateTime(session.updated_at)
        holder.itemView.setOnClickListener { onItemClick(session) }
    }

    private fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}

class SessionDiffCallback : DiffUtil.ItemCallback<SessionInfo>() {
    override fun areItemsTheSame(oldItem: SessionInfo, newItem: SessionInfo): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SessionInfo, newItem: SessionInfo): Boolean {
        return oldItem == newItem
    }
}
