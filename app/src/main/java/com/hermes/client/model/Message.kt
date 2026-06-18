package com.hermes.client.model

import com.google.gson.annotations.SerializedName

sealed class Message {
    abstract val role: String
    abstract val content: String?
    abstract val timestamp: Long
    
    data class UserMessage(
        override val role: String = "user",
        override val content: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : Message()
    
    data class AssistantMessage(
        override val role: String = "assistant",
        override val content: String? = null,
        override val timestamp: Long = System.currentTimeMillis(),
        val toolCalls: List<ToolCall>? = null,
        val thinking: String? = null
    ) : Message()
    
    data class ToolMessage(
        override val role: String = "tool",
        override val content: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val toolName: String? = null
    ) : Message()
    
    data class SystemMessage(
        override val role: String = "system",
        override val content: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : Message()
}

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: Map<String, Any>? = null
)

data class ChatRequest(
    val messages: List<MessageWrapper>,
    val model: String = "default",
    val stream: Boolean = true
)

data class MessageWrapper(
    val role: String,
    val content: String? = null,
    val tool_calls: List<ToolCallWrapper>? = null,
    val tool_call_id: String? = null
)

data class ToolCallWrapper(
    val id: String,
    val type: String = "function",
    val function: FunctionWrapper
)

data class FunctionWrapper(
    val name: String,
    val arguments: String
)

data class ChatResponse(
    val id: String? = null,
    val object_: String? = null,
    val created: Long? = null,
    val model: String? = null,
    val choices: List<Choice>? = null,
    val usage: Usage? = null
)

data class Choice(
    val index: Int,
    val message: ResponseMessage,
    val finish_reason: String? = null
)

data class ResponseMessage(
    val role: String,
    val content: String? = null,
    val tool_calls: List<ToolCallWrapper>? = null
)

data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

data class ApprovalRequest(
    val id: String,
    val command: String,
    val reason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ApprovalResponse(
    val id: String,
    val approved: Boolean
)

data class SessionInfo(
    val id: String,
    val title: String? = null,
    val created_at: Long,
    val updated_at: Long
)

data class ModelInfo(
    val id: String,
    val name: String,
    val provider: String
)
