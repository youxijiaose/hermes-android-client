package com.hermes.client.api

import com.hermes.client.model.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

// Update info data class
data class UpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String? = null,
    val mandatory: Boolean = false,
    val publishedAt: Long
)

class HermesApi(private val baseUrl: String, private val apiKey: String) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private val jsonMediaType = "application/json".toMediaType()
    
    private val websocketClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .build()

    // Chat endpoint
    suspend fun chat(request: ChatRequest): Result<ChatResponse> {
        return try {
            val body = GsonSerializer.toJson(request).toRequestBody(jsonMediaType)
            val req = Request.Builder()
                .url("$baseUrl/chat")
                .post(body)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(req).execute()
            
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
            }
            
            val responseBody = response.body?.string() ?: ""
            val chatResponse = GsonSerializer.parse(responseBody, ChatResponse::class.java)
            Result.success(chatResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // WebSocket streaming
    fun streamChat(
        messages: List<MessageWrapper>,
        model: String = "default",
        listener: WebSocketListener
    ) {
        val request = ChatRequest(messages, model, stream = true)
        val body = GsonSerializer.toJson(request).toRequestBody(jsonMediaType)
        
        val webSocketRequest = Request.Builder()
            .url("$baseUrl/chat/stream")
            .addHeader("Authorization", "Bearer $apiKey")
            .build()
        
        websocketClient.newWebSocket(webSocketRequest, object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(body.toString())
                listener.onOpen()
            }
            
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val data = GsonSerializer.parse(text, ChatResponse::class.java)
                    listener.onMessage(data)
                } catch (e: Exception) {
                    listener.onError(e)
                }
            }
            
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                listener.onClosed()
            }
            
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                listener.onClosed()
            }
            
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listener.onError(t)
            }
        })
        
        listener.onSocketReady(websocketClient.newWebSocket(webSocketRequest, object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(body.toString())
                listener.onOpen()
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val data = GsonSerializer.parse(text, ChatResponse::class.java)
                    listener.onMessage(data)
                } catch (e: Exception) {
                    listener.onError(e)
                }
            }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                listener.onClosed()
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                listener.onClosed()
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                listener.onError(t)
            }
        }))
    }

    // Get sessions
    suspend fun getSessions(): Result<List<SessionInfo>> {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/sessions")
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            
            val response = client.newCall(req).execute()
            
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
            }
            
            val responseBody = response.body?.string() ?: "[]"
            val jsonArray = JSONObject(responseBody).getJSONArray("sessions")
            val sessions = mutableListOf<SessionInfo>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                sessions.add(SessionInfo(
                    id = obj.getString("id"),
                    title = obj.optString("title"),
                    created_at = obj.getLong("created_at"),
                    updated_at = obj.getLong("updated_at")
                ))
            }
            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get models
    suspend fun getModels(): Result<List<ModelInfo>> {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/models")
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            
            val response = client.newCall(req).execute()
            
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
            }
            
            val responseBody = response.body?.string() ?: "[]"
            val jsonArray = JSONObject(responseBody).getJSONArray("models")
            val models = mutableListOf<ModelInfo>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                models.add(ModelInfo(
                    id = obj.getString("id"),
                    name = obj.optString("name", obj.getString("id")),
                    provider = obj.optString("provider", "")
                ))
            }
            Result.success(models)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Approval endpoints
    suspend fun submitApproval(request: ApprovalResponse): Result<Boolean> {
        return try {
            val body = GsonSerializer.toJson(request).toRequestBody(jsonMediaType)
            val req = Request.Builder()
                .url("$baseUrl/approvals/${request.id}")
                .post(body)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(req).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Health check
    suspend fun healthCheck(): Result<Boolean> {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/health")
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            
            val response = client.newCall(req).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Cron Jobs
    suspend fun getCronJobs(): Result<List<CronJob>> {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/cron")
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            
            val response = client.newCall(req).execute()
            
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
            }
            
            val responseBody = response.body?.string() ?: "[]"
            val jsonArray = JSONObject(responseBody).getJSONArray("jobs")
            val jobs = mutableListOf<CronJob>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                jobs.add(CronJob(
                    id = obj.getString("id"),
                    name = obj.optString("name"),
                    schedule = obj.getString("schedule"),
                    prompt = obj.getString("prompt"),
                    enabled = obj.getBoolean("enabled"),
                    last_run = obj.optLong("last_run"),
                    next_run = obj.optLong("next_run"),
                    created_at = obj.getLong("created_at"),
                    updated_at = obj.getLong("updated_at")
                ))
            }
            Result.success(jobs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCronJob(schedule: String, prompt: String, name: String? = null): Result<CronJob> {
        return try {
            val request = CreateCronRequest(schedule, prompt, name)
            val body = GsonSerializer.toJson(request).toRequestBody(jsonMediaType)
            val req = Request.Builder()
                .url("$baseUrl/cron")
                .post(body)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(req).execute()
            
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
            }
            
            val responseBody = response.body?.string() ?: ""
            val job = GsonSerializer.parse(responseBody, CronJob::class.java)
            Result.success(job)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleCronJob(jobId: String): Result<Boolean> {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/cron/$jobId/toggle")
                .post(RequestBody.create(jsonMediaType, ""))
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            
            val response = client.newCall(req).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCronJob(jobId: String): Result<Boolean> {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/cron/$jobId")
                .delete()
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            
            val response = client.newCall(req).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun runCronJob(jobId: String): Result<CronRunResponse> {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/cron/$jobId/run")
                .post(RequestBody.create(jsonMediaType, ""))
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            
            val response = client.newCall(req).execute()
            
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
            }
            
            val responseBody = response.body?.string() ?: ""
            val result = GsonSerializer.parse(responseBody, CronRunResponse::class.java)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Memory endpoints
    suspend fun getMemory(target: String? = null): Result<List<MemoryEntry>> {
        return try {
            val url = if (target != null) "$baseUrl/memory?target=$target" else "$baseUrl/memory"
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            
            val response = client.newCall(req).execute()
            
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
            }
            
            val responseBody = response.body?.string() ?: "[]"
            val jsonArray = JSONObject(responseBody).getJSONArray("entries")
            val entries = mutableListOf<MemoryEntry>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                entries.add(MemoryEntry(
                    id = obj.getString("id"),
                    target = obj.getString("target"),
                    content = obj.getString("content"),
                    created_at = obj.getLong("created_at"),
                    updated_at = obj.getLong("updated_at"),
                    tags = obj.optJSONArray("tags")?.let { tagsArray ->
                        (0 until tagsArray.length()).map { tagsArray.getString(it) }
                    },
                    metadata = obj.optJSONObject("metadata")?.let { json ->
                        val map = mutableMapOf<String, Any>()
                        json.keys().forEach { key ->
                            map[key] = when (val value = json.get(key)) {
                                is String, is Int, is Long, is Double, is Boolean -> value
                                else -> value.toString()
                            }
                        }
                        map
                    }
                ))
            }
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createMemory(request: CreateMemoryRequest): Result<MemoryEntry> {
        return try {
            val body = GsonSerializer.toJson(request).toRequestBody(jsonMediaType)
            val req = Request.Builder()
                .url("$baseUrl/memory")
                .post(body)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(req).execute()
            
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
            }
            
            val responseBody = response.body?.string() ?: ""
            val entry = GsonSerializer.parse(responseBody, MemoryEntry::class.java)
            Result.success(entry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMemory(memoryId: String, request: UpdateMemoryRequest): Result<MemoryEntry> {
        return try {
            val body = GsonSerializer.toJson(request).toRequestBody(jsonMediaType)
            val req = Request.Builder()
                .url("$baseUrl/memory/$memoryId")
                .patch(body)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(req).execute()
            
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
            }
            
            val responseBody = response.body?.string() ?: ""
            val entry = GsonSerializer.parse(responseBody, MemoryEntry::class.java)
            Result.success(entry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMemory(memoryId: String): Result<Boolean> {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/memory/$memoryId")
                .delete()
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            
            val response = client.newCall(req).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMemoryStats(): Result<MemoryStats> {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/memory/stats")
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            
            val response = client.newCall(req).execute()
            
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
            }
            
            val responseBody = response.body?.string() ?: ""
            val stats = GsonSerializer.parse(responseBody, MemoryStats::class.java)
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Skills endpoints
    suspend fun getSkills(category: String? = null, installed: Boolean? = null): Result<List<Skill>> {
        return try {
            var url = "$baseUrl/skills"
            val params = mutableListOf<String>()
            category?.let { params.add("category=$it") }
            installed?.let { params.add("installed=$it") }
            if (params.isNotEmpty()) {
                url += "?" + params.joinToString("&")
            }
            
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            
            val response = client.newCall(req).execute()
            
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
            }
            
            val responseBody = response.body?.string() ?: "[]"
            val jsonArray = JSONObject(responseBody).getJSONArray("skills")
            val skills = mutableListOf<Skill>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                skills.add(Skill(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    description = obj.optString("description", ""),
                    category = obj.optString("category"),
                    version = obj.optString("version"),
                    author = obj.optString("author"),
                    installed = obj.getBoolean("installed"),
                    pinned = obj.optBoolean("pinned", false),
                    created_at = obj.optLong("created_at"),
                    updated_at = obj.optLong("updated_at"),
                    tags = obj.optJSONArray("tags")?.let { tagsArray ->
                        (0 until tagsArray.length()).map { tagsArray.getString(it) }
                    },
                    hub_id = obj.optString("hub_id"),
                    is_hub_skill = obj.optBoolean("is_hub_skill", false)
                ))
            }
            Result.success(skills)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSkillCategories(): Result<List<SkillCategory>> {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/skills/categories")
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            
            val response = client.newCall(req).execute()
            
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
            }
            
            val responseBody = response.body?.string() ?: "[]"
            val jsonArray = JSONObject(responseBody).getJSONArray("categories")
            val categories = mutableListOf<SkillCategory>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                categories.add(SkillCategory(
                    name = obj.getString("name"),
                    count = obj.getInt("count"),
                    icon = obj.optString("icon")
                ))
            }
            Result.success(categories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun installSkill(id: String, category: String? = null): Result<Skill> {
        return try {
            val request = InstallSkillRequest(id, category)
            val body = GsonSerializer.toJson(request).toRequestBody(jsonMediaType)
            val req = Request.Builder()
                .url("$baseUrl/skills/install")
                .post(body)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()
            
            val response = client.newCall(req).execute()
            
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
            }
            
            val responseBody = response.body?.string() ?: ""
            val skill = GsonSerializer.parse(responseBody, Skill::class.java)
            Result.success(skill)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uninstallSkill(id: String): Result<Boolean> {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/skills/$id/uninstall")
                .post(RequestBody.create(jsonMediaType, ""))
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            
            val response = client.newCall(req).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun togglePinSkill(id: String): Result<Boolean> {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/skills/$id/pin")
                .post(RequestBody.create(jsonMediaType, ""))
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            
            val response = client.newCall(req).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSkillDetails(id: String): Result<Skill> {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/skills/$id")
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            
            val response = client.newCall(req).execute()
            
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
            }
            
            val responseBody = response.body?.string() ?: ""
            val skill = GsonSerializer.parse(responseBody, Skill::class.java)
            Result.success(skill)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSkillHubs(): Result<List<SkillHub>> {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/skills/hubs")
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            
            val response = client.newCall(req).execute()
            
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
            }
            
            val responseBody = response.body?.string() ?: "[]"
            val jsonArray = JSONObject(responseBody).getJSONArray("hubs")
            val hubs = mutableListOf<SkillHub>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                hubs.add(SkillHub(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    url = obj.getString("url"),
                    skills_count = obj.getInt("skills_count"),
                    installed = obj.optBoolean("installed", false)
                ))
            }
            Result.success(hubs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update endpoints
    suspend fun getUpdateInfo(): Result<UpdateInfo> {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/update")
                .addHeader("Authorization", "Bearer $apiKey")
                .build()
            
            val response = client.newCall(req).execute()
            
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
            }
            
            val responseBody = response.body?.string() ?: ""
            val json = JSONObject(responseBody)
            val updateInfo = UpdateInfo(
                versionCode = json.getLong("version_code"),
                versionName = json.getString("version_name"),
                apkUrl = json.getString("apk_url"),
                releaseNotes = json.optString("release_notes"),
                mandatory = json.optBoolean("mandatory", false),
                publishedAt = json.optLong("published_at", System.currentTimeMillis())
            )
            Result.success(updateInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        @Volatile private var serverUrl: String = "http://192.168.1.100:8080"
        @Volatile private var apiKey: String = ""
        
        fun setup(url: String, key: String) {
            serverUrl = url
            apiKey = key
        }
        
        fun getServerUrl(): String = serverUrl
        fun getApiKey(): String = apiKey
    }
}

// WebSocket listener interface
interface WebSocketListener {
    fun onOpen()
    fun onMessage(response: ChatResponse)
    fun onError(error: Throwable)
    fun onClosed()
    fun onSocketReady(socket: WebSocket)
}

// Simple JSON serializer
object GsonSerializer {
    private val gson = com.google.gson.GsonBuilder().create()
    
    fun <T> parse(json: String, clazz: Class<T>): T {
        return gson.fromJson(json, clazz)
    }
    
    fun <T> toJson(obj: T): String {
        return gson.toJson(obj)
    }



}
