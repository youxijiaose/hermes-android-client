package com.hermes.client.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hermes.client.R
import com.hermes.client.model.Skill
import com.hermes.client.util.setMarkdown

class SkillsAdapter(
    private val onInstall: (String) -> Unit,
    private val onUninstall: (String) -> Unit,
    private val onPin: (String) -> Unit,
    private val onClick: (Skill) -> Unit
) : ListAdapter<Skill, SkillsAdapter.SkillViewHolder>(SkillDiffCallback()) {

    class SkillViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.textName)
        val description: TextView = itemView.findViewById(R.id.textContent)
        val category: TextView = itemView.findViewById(R.id.textCategory)
        val version: TextView = itemView.findViewById(R.id.textVersion)
        val author: TextView = itemView.findViewById(R.id.textAuthor)
        val pinButton: ImageButton = itemView.findViewById(R.id.btnPin)
        val installButton: Button = itemView.findViewById(R.id.btnInstall)
        val card: View = itemView.findViewById(R.id.skillCard)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkillViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_skill, parent, false)
        return SkillViewHolder(view)
    }

    override fun onBindViewHolder(holder: SkillViewHolder, position: Int) {
        val skill = getItem(position)
        
        holder.name.text = skill.name
        holder.description.setMarkdown(skill.description)
        holder.category.text = skill.category ?: "Uncategorized"
        holder.version.text = skill.version ?: "N/A"
        holder.author.text = skill.author ?: "Unknown"
        
        // Update button state based on installation status
        if (skill.installed) {
            holder.installButton.text = "Uninstall"
            holder.installButton.setBackgroundResource(R.drawable.btn_uninstall)
        } else {
            holder.installButton.text = "Install"
            holder.installButton.setBackgroundResource(R.drawable.btn_install)
        }

        // Pin button state
        holder.pinButton.isSelected = skill.pinned
        holder.pinButton.setImageDrawable(
            if (skill.pinned) {
                itemView.context.getDrawable(android.R.drawable.ic_menu_share)
            } else {
                itemView.context.getDrawable(android.R.drawable.ic_menu_agenda)
            }
        )

        holder.pinButton.setOnClickListener { onPin(skill.id) }
        holder.installButton.setOnClickListener {
            if (skill.installed) {
                onUninstall(skill.id)
            } else {
                onInstall(skill.id)
            }
        }
        holder.card.setOnClickListener { onClick(skill) }

        // Visual feedback for installing
        holder.installButton.isEnabled = _installingId != skill.id
    }

    private var _installingId: String? = null

    fun setInstalling(id: String?) {
        _installingId = id
        notifyItemRangeChanged(0, itemCount)
    }

    class SkillDiffCallback : DiffUtil.ItemCallback<Skill>() {
        override fun areItemsTheSame(oldItem: Skill, newItem: Skill): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Skill, newItem: Skill): Boolean {
            return oldItem == newItem
        }
    }
}
