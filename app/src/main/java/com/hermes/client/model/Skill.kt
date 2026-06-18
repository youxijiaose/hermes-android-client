package com.hermes.client.model

import com.google.gson.annotations.SerializedName

data class Skill(
    val id: String,
    val name: String,
    val description: String,
    val category: String? = null,
    val version: String? = null,
    val author: String? = null,
    val installed: Boolean = false,
    val pinned: Boolean = false,
    val created_at: Long? = null,
    val updated_at: Long? = null,
    val tags: List<String>? = null,
    val hub_id: String? = null,
    val is_hub_skill: Boolean = false
)

data class SkillCategory(
    val name: String,
    val count: Int,
    val icon: String? = null
)

data class InstallSkillRequest(
    val id: String,
    val category: String? = null
)

data class SkillHub(
    val id: String,
    val name: String,
    val url: String,
    val skills_count: Int,
    val installed: Boolean = false
)
