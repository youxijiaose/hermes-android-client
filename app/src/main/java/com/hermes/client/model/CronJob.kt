package com.hermes.client.model

import com.google.gson.annotations.SerializedName

data class CronJob(
    val id: String,
    val name: String? = null,
    val schedule: String,
    val prompt: String,
    val enabled: Boolean = true,
    val last_run: Long? = null,
    val next_run: Long? = null,
    val created_at: Long,
    val updated_at: Long,
    val skills: List<String>? = null,
    val deliver: String? = null
)

data class CreateCronRequest(
    val schedule: String,
    val prompt: String,
    val name: String? = null,
    val skills: List<String>? = null,
    val deliver: String? = null
)

data class UpdateCronRequest(
    val schedule: String? = null,
    val prompt: String? = null,
    val name: String? = null,
    val enabled: Boolean? = null,
    val skills: List<String>? = null,
    val deliver: String? = null
)

data class CronRunResponse(
    val id: String,
    val status: String,
    val message: String? = null
)
