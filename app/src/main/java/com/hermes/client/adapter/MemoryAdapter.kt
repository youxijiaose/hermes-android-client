package com.hermes.client.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hermes.client.R
import com.hermes.client.model.MemoryEntry
import com.hermes.client.util.TimeUtils
import com.hermes.client.util.setMarkdown

class MemoryAdapter(
    private val onEdit: (MemoryEntry) -> Unit,
    private val onDelete: (MemoryEntry) -> Unit,
    private val onDetails: (MemoryEntry) -> Unit
) : ListAdapter<MemoryEntry, MemoryAdapter.MemoryViewHolder>(MemoryDiffCallback()) {

    class MemoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val target: TextView = itemView.findViewById(R.id.textTarget)
        val content: TextView = itemView.findViewById(R.id.textContent)
        val createdAt: TextView = itemView.findViewById(R.id.textCreatedAt)
        val tagsContainer: ViewGroup = itemView.findViewById(R.id.tagsContainer)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        val btnDetails: ImageButton = itemView.findViewById(R.id.btnDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_memory, parent, false)
        return MemoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemoryViewHolder, position: Int) {
        val entry = getItem(position)
        
        holder.target.text = entry.target.uppercase()
        holder.content.setMarkdown(entry.content)
        holder.createdAt.text = TimeUtils.formatRelativeTime(holder.itemView.context, entry.updated_at)
        
        // Display tags
        holder.tagsContainer.removeAllViews()
        entry.tags?.forEach { tag ->
            val tagView = TextView(holder.itemView.context).apply {
                text = "#$tag"
                setPadding(8, 4, 8, 4)
                setBackgroundResource(R.drawable.tag_background)
            }
            holder.tagsContainer.addView(tagView)
        }

        holder.btnEdit.setOnClickListener { onEdit(entry) }
        holder.btnDelete.setOnClickListener { onDelete(entry) }
        holder.btnDetails.setOnClickListener { onDetails(entry) }
    }

    class MemoryDiffCallback : DiffUtil.ItemCallback<MemoryEntry>() {
        override fun areItemsTheSame(oldItem: MemoryEntry, newItem: MemoryEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MemoryEntry, newItem: MemoryEntry): Boolean {
            return oldItem == newItem
        }
    }
}
