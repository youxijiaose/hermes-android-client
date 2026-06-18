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
import com.hermes.client.model.AssistantMessage
import com.hermes.client.model.Message
import com.hermes.client.model.ToolMessage
import com.hermes.client.model.UserMessage
import com.hermes.client.util.TimeUtils
import com.hermes.client.util.setMarkdown

class ChatAdapter : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_ASSISTANT = 2
        private const val VIEW_TYPE_TOOL = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is UserMessage -> VIEW_TYPE_USER
            is AssistantMessage -> VIEW_TYPE_ASSISTANT
            is ToolMessage -> VIEW_TYPE_TOOL
            else -> VIEW_TYPE_ASSISTANT
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
            else -> AssistantViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_message_assistant, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is UserViewHolder -> holder.bind(getItem(position) as UserMessage)
            is AssistantViewHolder -> holder.bind(getItem(position) as AssistantMessage)
            is ToolViewHolder -> holder.bind(getItem(position) as ToolMessage)
        }
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.textMessage)
        private val timeView: TextView = itemView.findViewById(R.id.timeMessage)

        init {
            // 添加时间视图到布局（需要修改布局文件）
        }

        fun bind(message: UserMessage) {
            textView.setMarkdown(message.content ?: "")
            timeView.text = TimeUtils.formatRelativeTime(itemView.context, message.timestamp)
        }
    }

    class AssistantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.textMessage)
        private val timeView: TextView = itemView.findViewById(R.id.timeMessage)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressTyping)
        private val thinkingView: TextView = itemView.findViewById(R.id.thinkingBlock)

        init {
            // 添加思考块视图
        }

        fun bind(message: AssistantMessage) {
            if (message.content.isNullOrEmpty()) {
                progressBar.visibility = View.VISIBLE
                textView.visibility = View.GONE
            } else {
                progressBar.visibility = View.GONE
                textView.visibility = View.VISIBLE
                textView.setMarkdown(message.content)
            }
            timeView.text = TimeUtils.formatRelativeTime(itemView.context, message.timestamp)
            
            // 显示思考块（如果有）
            thinkingView.visibility = if (message.thinking?.isNotEmpty() == true) View.VISIBLE else View.GONE
            thinkingView.text = message.thinking
        }
    }

    class ToolViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val toolName: TextView = itemView.findViewById(R.id.textToolName)
        private val toolOutput: TextView = itemView.findViewById(R.id.textToolOutput)
        private val timeView: TextView = itemView.findViewById(R.id.timeMessage)

        fun bind(message: ToolMessage) {
            toolName.text = message.toolName ?: "Tool"
            toolOutput.setMarkdown(message.content)
            timeView.text = TimeUtils.formatRelativeTime(itemView.context, message.timestamp)
        }
    }
}

class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
    override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem === newItem
    }

    override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
        return oldItem.content == newItem.content &&
                oldItem.role == newItem.role &&
                oldItem.timestamp == newItem.timestamp
    }
}
