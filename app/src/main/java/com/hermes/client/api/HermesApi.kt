package com.hermes.client.api

import com.hermes.client.model.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

//
// Hermes Dashboard API client.
//
// Dual-channel architecture:
// - **Gateway WebSocket JSON-RPC** (`/api/ws`) for chat + sessions
// - **REST** (`/api/sessions`, `/api/audio/speak`, `/api/audio/transcribe`, `/health`)
//   for session list, voice, health
//
// Both point at the same dashboard port (default 9119).
// The token is auto-discovered from the dashboard's index.html.
//
class HermesApi(private val baseUrl: String) {

    companion object {
        private const val TAG = "HermesApi"
        private const val DEFAULT_URL = "http://127.0.0.1:9119"
    }

    private val restClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val gateway = GatewayClient()
    private var token: String? = null

    // ---- Connection ----

    /**
     * Connect to the dashboard gateway.
     * Token is auto-fetched from the dashboard's index.html.
     */
    suspend fun connect(): Result<Unit> {
        return try {
            gateway.connect(baseUrl)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun disconnect() {
        gateway.close()
    }

    // ---- Health / Reachability ----

    suspend fun healthCheck(): Result<Boolean> {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/health")
                .build()
            val response = restClient.newCall(req).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- Chat (Gateway JSON-RPC) ----

    /**
     * Send a chat message via the gateway WebSocket.
     * Response comes as streaming events (message.delta, etc.)
     *
     * @param sessionId The active session ID
     * @param text The user's message text
     * @return The JSON-RPC response (status: "streaming")
     */
    suspend fun submitPrompt(sessionId: String, text: String): Result<JSONObject> {
        return try {
            val result = gateway.request("prompt.submit", mapOf(
                "session_id" to sessionId,
                "text" to text
            ))
            if (result != null) {
                Result.success(result)
            } else {
                Result.failure(IOException("No response from gateway"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- Sessions (REST + Gateway) ----

    suspend fun getSessions(): Result<List<SessionInfo>> {
        return try {
            val req = Request.Builder()
                .url("$baseUrl/api/sessions")
                .build()
            val response = restClient.newCall(req).execute()

            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP ${response.code}"))
            }

            val body = response.body?.string() ?: "[]"
            val jsonArray = JSONArray(body)
            val sessions = mutableListOf<SessionInfo>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                sessions.add(SessionInfo(
                    id = obj.getString("id"),
                    title = obj.optString("title"),
                    created_at = obj.optLong("created_at", 0L),
                    updated_at = obj.optLong("updated_at", 0L)
                ))
            }
            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a new session via the gateway.
     */
    suspend fun createSession(): Result<String> {
        return try {
            val result = gateway.request("session.create", mapOf(
                "source" to "android"
            ))
            val sessionId = result?.optString("session_id")
            if (sessionId != null) {
                Result.success(sessionId)
            } else {
                Result.failure(IOException("No session_id in response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resume a session (load its history) via the gateway.
     */
    suspend fun resumeSession(sessionId: String): Result<List<MessageWrapper>> {
        return try {
            val result = gateway.request("session.resume", mapOf(
                "session_id" to sessionId
            ))
            val transcript = result?.optJSONArray("transcript")
            val messages = mutableListOf<MessageWrapper>()
            if (transcript != null) {
                for (i in 0 until transcript.length()) {
                    val msg = transcript.getJSONObject(i)
                    messages.add(MessageWrapper(
                        role = msg.optString("role", "user"),
                        content = msg.optString("content")
                    ))
                }
            }
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSession(sessionId: String): Result<Boolean> {
        return try {
            val result = gateway.request("session.delete", mapOf(
                "session_id" to sessionId
            ))
            Result.success(result != null) // no error = success
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun interruptSession(sessionId: String): Result<Boolean> {
        return try {
            val result = gateway.request("session.interrupt", mapOf(
                "session_id" to sessionId
            ))
            Result.success(result != null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- Approval / Clarify / Secret / Sudo ----

    suspend fun respondApproval(approvalId: String, approved: Boolean): Result<Boolean> {
        return try {
            val result = gateway.request("approval.respond", mapOf(
                "id" to approvalId,
                "approved" to approved
            ))
            Result.success(result != null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- REST audio endpoints ----

    suspend fun transcribeAudio(dataUrl: String): Result<TranscribeResponse> {
        return try {
            val json = JSONObject().apply {
                put("data_url", dataUrl)
                put("mime_type", "audio/ogg")
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("$baseUrl/api/audio/transcribe")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()
            val response = restClient.newCall(req).execute()
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP ${response.code}"))
            }
            val respBody = response.body?.string() ?: "{}"
            val respJson = JSONObject(respBody)
            Result.success(TranscribeResponse(
                ok = respJson.optBoolean("ok", false),
                transcript = respJson.optString("transcript"),
                provider = respJson.optString("provider")
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun speakAudio(text: String): Result<SpeakResponse> {
        return try {
            val json = JSONObject().apply { put("text", text) }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("$baseUrl/api/audio/speak")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()
            val response = restClient.newCall(req).execute()
            if (!response.isSuccessful) {
                return Result.failure(IOException("HTTP ${response.code}"))
            }
            val respBody = response.body?.string() ?: "{}"
            val respJson = JSONObject(respBody)
            Result.success(SpeakResponse(
                ok = respJson.optBoolean("ok", false),
                dataUrl = respJson.optString("data_url"),
                mimeType = respJson.optString("mime_type"),
                provider = respJson.optString("provider")
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---- Stub methods for legacy features (Skills / Memory / Cron) ----
    // These call the REST endpoints on the Dashboard. If the Dashboard doesn't
    // expose them, they'll return http-404 errors.

    suspend fun getSkills(category: String? = null, installed: Boolean? = null): Result<List<Skill>> {
        return try {
            var url = "$baseUrl/skills"
            val params = mutableListOf<String>()
            category?.let { params.add("category=$it") }
            installed?.let { params.add("installed=$it") }
            if (params.isNotEmpty()) url += "?" + params.joinToString("&")

            val req = Request.Builder().url(url).build()
            val response = restClient.newCall(req).execute()
            if (!response.isSuccessful) return Result.failure(IOException("HTTP ${response.code}"))

            val body = response.body?.string() ?: "{}"
            val jsonArray = JSONObject(body).optJSONArray("skills") ?: JSONArray()
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
                    pinned = obj.optBoolean("pinned", false)
                ))
            }
            Result.success(skills)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getSkillCategories(): Result<List<SkillCategory>> {
        return try {
            val req = Request.Builder().url("$baseUrl/skills/categories").build()
            val response = restClient.newCall(req).execute()
            if (!response.isSuccessful) return Result.failure(IOException("HTTP ${response.code}"))
            val json = JSONObject(response.body?.string() ?: "{}")
            val jsonArray = json.optJSONArray("categories") ?: JSONArray()
            val categories = mutableListOf<SkillCategory>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                categories.add(SkillCategory(name = obj.getString("name"), count = obj.getInt("count")))
            }
            Result.success(categories)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun installSkill(id: String, category: String? = null): Result<Skill> {
        return Result.failure(IOException("Skills install not yet supported via Dashboard API"))
    }

    suspend fun uninstallSkill(id: String): Result<Boolean> {
        return Result.failure(IOException("Skills uninstall not yet supported via Dashboard API"))
    }

    suspend fun togglePinSkill(id: String): Result<Boolean> {
        return Result.failure(IOException("Skills pin not yet supported via Dashboard API"))
    }

    suspend fun getMemory(target: String? = null): Result<List<MemoryEntry>> {
        return Result.failure(IOException("Memory API not yet supported via Dashboard gateway"))
    }

    suspend fun getMemoryStats(): Result<MemoryStats> {
        return Result.failure(IOException("Memory API not yet supported via Dashboard gateway"))
    }

    suspend fun createMemory(request: CreateMemoryRequest): Result<MemoryEntry> {
        return Result.failure(IOException("Memory API not yet supported via Dashboard gateway"))
    }

    suspend fun updateMemory(memoryId: String, request: UpdateMemoryRequest): Result<MemoryEntry> {
        return Result.failure(IOException("Memory API not yet supported via Dashboard gateway"))
    }

    suspend fun deleteMemory(memoryId: String): Result<Boolean> {
        return Result.failure(IOException("Memory API not yet supported via Dashboard gateway"))
    }

    suspend fun getCronJobs(): Result<List<CronJob>> {
        return Result.failure(IOException("Cron API not yet supported via Dashboard gateway"))
    }

    suspend fun toggleCronJob(jobId: String): Result<Boolean> {
        return Result.failure(IOException("Cron API not yet supported via Dashboard gateway"))
    }

    suspend fun deleteCronJob(jobId: String): Result<Boolean> {
        return Result.failure(IOException("Cron API not yet supported via Dashboard gateway"))
    }

    suspend fun runCronJob(jobId: String): Result<CronRunResponse> {
        return Result.failure(IOException("Cron API not yet supported via Dashboard gateway"))
    }

    suspend fun createCronJob(schedule: String, prompt: String, name: String? = null): Result<CronJob> {
        return Result.failure(IOException("Cron API not yet supported via Dashboard gateway"))
    }
}

// ---- Response models ----

data class TranscribeResponse(
    val ok: Boolean,
    val transcript: String,
    val provider: String
)

data class SpeakResponse(
    val ok: Boolean,
    val dataUrl: String,
    val mimeType: String,
    val provider: String
)
