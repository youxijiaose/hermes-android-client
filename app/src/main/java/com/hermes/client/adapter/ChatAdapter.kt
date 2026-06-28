package com.hermes.client.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hermes.client.R
import com.hermes.client.model.Message

import com.hermes.client.util.TimeUtils
import com.hermes.client.util.setMarkdown

class ChatAdapter : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_ASSISTANT = 2
        private const val VIEW_TYPE_TOOL = 3
        private const val VIEW_TYPE_SYSTEM = 4
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Message.UserMessage -> VIEW_TYPE_USER
            is Message.AssistantMessage -> VIEW_TYPE_ASSISTANT
            is Message.ToolMessage -> VIEW_TYPE_TOOL
            is Message.SystemMessage -> VIEW_TYPE_SYSTEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> UserViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_message_user, parent, false)
            )
            VIEW_TYPE_TOOL -> ToolViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_message_tool, parent, false)
            )
            VIEW_TYPE_SYSTEM -> SystemViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_message_system, parent, false)
            )
            else -> AssistantViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_message_assistant, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is UserViewHolder -> holder.bind(getItem(position) as Message.UserMessage)
            is AssistantViewHolder -> holder.bind(getItem(position) as Message.AssistantMessage)
            is ToolViewHolder -> holder.bind(getItem(position) as Message.ToolMessage)
            is SystemViewHolder -> holder.bind(getItem(position) as Message.SystemMessage)
        }
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.textMessage)
        private val timeView: TextView = itemView.findViewById(R.id.timeMessage)

        fun bind(message: Message.UserMessage) {
            textView.setMarkdown(message.content ?: "")
            timeView.text = TimeUtils.formatRelativeTime(itemView.context, message.timestamp)
        }
    }

    class AssistantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.textMessage)
        private val timeView: TextView = itemView.findViewById(R.id.timeMessage)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressTyping)
        private val thinkingBlock: android.widget.LinearLayout = itemView.findViewById(R.id.thinkingBlock)

        // Cache last rendered content to skip redundant renders
        private var lastContentHash: Int = 0

        fun bind(message: Message.AssistantMessage) {
            val contentHash = message.content?.hashCode() ?: 0

            if (message.content.isNullOrEmpty()) {
                progressBar.visibility = View.VISIBLE
                textView.visibility = View.GONE
            } else {
                progressBar.visibility = View.GONE
                textView.visibility = View.VISIBLE
                // Only re-render if content actually changed
                if (contentHash != lastContentHash) {
                    textView.setMarkdown(message.content)
                    lastContentHash = contentHash
                }
            }
            timeView.text = TimeUtils.formatRelativeTime(itemView.context, message.timestamp)

            // 显示思考块（如果有）
            thinkingBlock.visibility = if (message.thinking?.isNotEmpty() == true) View.VISIBLE else View.GONE
            val thinkingText = itemView.findViewById<android.widget.TextView>(R.id.textThinking)
            thinkingText?.text = message.thinking
        }
    }

    class ToolViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val toolName: TextView = itemView.findViewById(R.id.textToolName)
        private val toolOutput: TextView = itemView.findViewById(R.id.textToolOutput)
        private val timeView: TextView = itemView.findViewById(R.id.timeMessage)

        fun bind(message: Message.ToolMessage) {
            toolName.text = message.toolName ?: "Tool"
            toolOutput.setMarkdown(message.content)
            timeView.text = TimeUtils.formatRelativeTime(itemView.context, message.timestamp)
        }
    }

    class SystemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.textSystem)

        fun bind(message: Message.SystemMessage) {
            textView.text = message.content
        }
    }
}

class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem::class == newItem::class &&
                oldItem.content == newItem.content &&
                oldItem.timestamp == newItem.timestamp
    }

    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.content == newItem.content &&
                oldItem.role == newItem.role &&
                oldItem.timestamp == newItem.timestamp
    }
}