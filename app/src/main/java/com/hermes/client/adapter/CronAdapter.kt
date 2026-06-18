package com.hermes.client.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hermes.client.R
import com.hermes.client.model.CronJob
import com.hermes.client.util.TimeUtils

class CronAdapter(
    private val onToggle: (CronJob) -> Unit,
    private val onDelete: (CronJob) -> Unit,
    private val onRun: (CronJob) -> Unit,
    private val onEdit: (CronJob) -> Unit
) : ListAdapter<CronJob, CronAdapter.CronViewHolder>(CronDiffCallback()) {

    class CronViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.textName)
        val schedule: TextView = itemView.findViewById(R.id.textSchedule)
        val prompt: TextView = itemView.findViewById(R.id.textPrompt)
        val lastRun: TextView = itemView.findViewById(R.id.textLastRun)
        val nextRun: TextView = itemView.findViewById(R.id.textNextRun)
        val toggle: Switch = itemView.findViewById(R.id.switchEnabled)
        val btnRun: ImageButton = itemView.findViewById(R.id.btnRun)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CronViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cron, parent, false)
        return CronViewHolder(view)
    }

    override fun onBindViewHolder(holder: CronViewHolder, position: Int) {
        val job = getItem(position)
        
        holder.name.text = job.name ?: job.id
        holder.schedule.text = job.schedule
        holder.prompt.text = job.prompt.take(50) + if (job.prompt.length > 50) "..." else ""
        holder.lastRun.text = job.last_run?.let { TimeUtils.formatRelativeTime(itemView.context, it) } ?: "Never"
        holder.nextRun.text = job.next_run?.let { TimeUtils.formatRelativeTime(itemView.context, it) } ?: "N/A"
        
        holder.toggle.isChecked = job.enabled
        holder.toggle.setOnCheckedChangeListener { _, isChecked ->
            // Prevent recursive calls
            holder.toggle.setOnCheckedChangeListener(null)
            onToggle(job)
            holder.toggle.isChecked = job.enabled
            holder.toggle.setOnCheckedChangeListener { _, isChecked ->
                onToggle(job)
            }
        }

        holder.btnRun.setOnClickListener { onRun(job) }
        holder.btnEdit.setOnClickListener { onEdit(job) }
        holder.btnDelete.setOnClickListener { onDelete(job) }
    }

    class CronDiffCallback : DiffUtil.ItemCallback<CronJob>() {
        override fun areItemsTheSame(oldItem: CronJob, newItem: CronJob): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CronJob, newItem: CronJob): Boolean {
            return oldItem == newItem
        }
    }
}
