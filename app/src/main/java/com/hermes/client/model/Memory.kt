package com.hermes.client.model

import com.google.gson.annotations.SerializedName

data class MemoryEntry(
    val id: String,
    val target: String,  // "user" or "memory"
    val content: String,
    val created_at: Long,
    val updated_at: Long,
    val tags: List<String>? = null,
    val metadata: Map<String, Any>? = null
)

data class CreateMemoryRequest(
    val target: String,
    val content: String,
    val tags: List<String>? = null
)

data class UpdateMemoryRequest(
    val content: String? = null,
    val tags: List<String>? = null
)

data class MemoryStats(
    val total_entries: Int,
    val user_entries: Int,
    val memory_entries: Int,
    val total_chars: Int
)
