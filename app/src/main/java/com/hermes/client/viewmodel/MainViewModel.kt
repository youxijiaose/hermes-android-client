package com.hermes.client.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hermes.client.api.GatewayClient
import com.hermes.client.api.GatewayEvent
import com.hermes.client.api.HermesApi
import com.hermes.client.model.*
import com.hermes.client.util.CrashLogWriter
import kotlinx.coroutines.launch
import org.json.JSONObject

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("hermes_prefs", Application.MODE_PRIVATE)

    private var api: HermesApi? = null
    private var activeSessionId: String? = null
    private var streamingAssistantIndex = -1

    // ── Observables ──

    private val _messages = MutableLiveData<MutableList<Message>>(mutableListOf())
    val messages: LiveData<MutableList<Message>> = _messages

    private val _connectionState = MutableLiveData<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: LiveData<ConnectionState> = _connectionState

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _pendingApproval = MutableLiveData<ApprovalRequest?>()
    val pendingApproval: LiveData<ApprovalRequest?> = _pendingApproval

    private val _isStreaming = MutableLiveData(false)
    val isStreaming: LiveData<Boolean> = _isStreaming

    companion object {
        private const val TAG = "MainViewModel"
    }

    sealed class ConnectionState {
        object Connected : ConnectionState()
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
    }

    init {
        try {
            CrashLogWriter.writeLog(application, "VIEWMODEL_INIT", "MainViewModel init")
            val serverUrl = prefs.getString("server_url", "http://127.0.0.1:9119")
                ?: "http://127.0.0.1:9119"
            Log.d(TAG, "Initializing with server URL: $serverUrl")
            connect(serverUrl)
        } catch (e: Exception) {
            CrashLogWriter.writeCrashLog(application, "VIEWMODEL_INIT_ERROR", e)
            _error.value = "Init error: ${e.message}"
        }
    }

    // ── Connection ──

    fun connect(serverUrl: String = "http://127.0.0.1:9119") {
        _connectionState.value = ConnectionState.Connecting

        val newApi = HermesApi(serverUrl)
        api = newApi

        // Register gateway event listeners
        newApi.gateway.on("message.delta", ::onMessageDelta)
        newApi.gateway.on("message.complete", ::onMessageComplete)
        newApi.gateway.on("thinking.delta", ::onThinkingDelta)
        newApi.gateway.on("tool.start", ::onToolStart)
        newApi.gateway.on("tool.complete", ::onToolComplete)
        newApi.gateway.on("error", ::onGatewayError)
        newApi.gateway.on("approval.request", ::onApprovalRequest)
        newApi.gateway.on("clarify.request", ::onClarifyRequest)
        newApi.gateway.on("session.info", ::onSessionInfo)

        newApi.gateway.onState { state ->
            when (state) {
                GatewayClient.State.Open -> {
                    _connectionState.postValue(ConnectionState.Connected)
                    // Create a session once connected
                    createNewSession()
                }
                GatewayClient.State.Closed -> {
                    _connectionState.postValue(ConnectionState.Disconnected)
                }
                GatewayClient.State.Error -> {
                    _connectionState.postValue(ConnectionState.Disconnected)
                    _error.postValue("Connection failed - is Hermes Dashboard running?")
                }
                else -> {}
            }
        }

        viewModelScope.launch {
            val result = newApi.connect()
            if (result.isFailure) {
                _connectionState.value = ConnectionState.Disconnected
                _error.value = "Connect failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    private fun createNewSession() {
        viewModelScope.launch {
            val api = api ?: return@launch
            val result = api.createSession()
            result.onSuccess { sessionId ->
                activeSessionId = sessionId
                addSystemMessage("Session $sessionId ready")
                CrashLogWriter.writeLog(getApplication(), "SESSION_CREATED", sessionId)
            }.onFailure { e ->
                Log.e(TAG, "Failed to create session", e)
                _error.value = "Session create failed: ${e.message}"
            }
        }
    }

    // ── Sending Messages ──

    fun sendMessage(content: String) {
        val sessionId = activeSessionId ?: return
        if (_connectionState.value != ConnectionState.Connected) {
            _error.value = "Not connected - tap to reconnect"
            return
        }

        if (_isStreaming.value == true) {
            _error.value = "Still streaming previous response"
            return
        }

        // Add user message
        val messages = _messages.value ?: mutableListOf()
        messages.add(Message.UserMessage(content = content))
        _messages.value = messages

        // Add placeholder for streaming assistant message
        streamingAssistantIndex = messages.size
        val assistantPlaceholder = Message.AssistantMessage(content = "")
        messages.add(assistantPlaceholder)
        _messages.value = messages

        _isStreaming.value = true

        viewModelScope.launch {
            val api = api ?: return@launch
            val result = api.submitPrompt(sessionId, content)
            result.onFailure { e ->
                Log.e(TAG, "Failed to submit prompt", e)
                _error.value = "Send failed: ${e.message}"
                _isStreaming.value = false
            }
            // If success, events will handle the streaming
        }
    }

    // ── Event Handlers ──

    private fun onMessageDelta(event: GatewayEvent) {
        val payload = event.payload ?: return
        try {
            val json = JSONObject(payload)
            val text = json.optString("text", "")
            if (text.isNotEmpty()) {
                updateAssistantContent(text)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message.delta", e)
        }
    }

    private fun onMessageComplete(event: GatewayEvent) {
        _isStreaming.postValue(false)
        streamingAssistantIndex = -1
    }

    private fun onThinkingDelta(event: GatewayEvent) {
        val payload = event.payload ?: return
        try {
            val json = JSONObject(payload)
            val text = json.optString("text", "")
            if (text.isNotEmpty()) {
                // Append thinking to the current assistant message
                val messages = _messages.value ?: return
                val index = streamingAssistantIndex
                if (index >= 0 && index < messages.size) {
                    val msg = messages[index]
                    if (msg is Message.AssistantMessage) {
                        val currentThinking = msg.thinking ?: ""
                        val updatedMsg = msg.copy(thinking = currentThinking + text)
                        messages[index] = updatedMsg
                        _messages.postValue(messages)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse thinking.delta", e)
        }
    }

    private fun onToolStart(event: GatewayEvent) {
        val payload = event.payload ?: return
        try {
            val json = JSONObject(payload)
            val toolName = json.optString("name", "tool")
            val messages = _messages.value ?: return
            messages.add(Message.ToolMessage(
                content = "Running: $toolName...",
                toolName = toolName
            ))
            _messages.postValue(messages)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse tool.start", e)
        }
    }

    private fun onToolComplete(event: GatewayEvent) {
        val payload = event.payload ?: return
        try {
            val json = JSONObject(payload)
            val toolName = json.optString("name", "tool")
            val result = json.optString("result", "")
            // Last tool message - replace with completed status
            val messages = _messages.value ?: return
            val toolMsgIndex = messages.indexOfLast { it is Message.ToolMessage }
            if (toolMsgIndex >= 0) {
                messages[toolMsgIndex] = Message.ToolMessage(
                    content = "✅ $toolName: ${result.take(200)}${if (result.length > 200) "…" else ""}",
                    toolName = toolName
                )
                _messages.postValue(messages)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse tool.complete", e)
        }
    }

    private fun onApprovalRequest(event: GatewayEvent) {
        val payload = event.payload ?: return
        try {
            val json = JSONObject(payload)
            val approvalRequest = ApprovalRequest(
                id = json.optString("id"),
                command = json.optString("command", ""),
                reason = json.optString("reason"),
                timestamp = System.currentTimeMillis()
            )
            _pendingApproval.postValue(approvalRequest)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse approval.request", e)
        }
    }

    private fun onClarifyRequest(event: GatewayEvent) {
        val payload = event.payload ?: return
        try {
            val json = JSONObject(payload)
            _error.postValue("Clarify: ${json.optString("question", "")}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse clarify.request", e)
        }
    }

    private fun onGatewayError(event: GatewayEvent) {
        val payload = event.payload ?: return
        try {
            val json = JSONObject(payload)
            val message = json.optString("message", "Gateway error")
            _error.postValue(message)
            _isStreaming.postValue(false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse gateway error", e)
        }
    }

    private fun onSessionInfo(event: GatewayEvent) {
        val payload = event.payload ?: return
        try {
            val json = JSONObject(payload)
            val sessionId = json.optString("session_id")
            if (!sessionId.isNullOrEmpty()) {
                activeSessionId = sessionId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse session.info", e)
        }
    }

    // ── Helpers ──

    private fun updateAssistantContent(delta: String) {
        val messages = _messages.value ?: return
        val index = streamingAssistantIndex
        if (index >= 0 && index < messages.size) {
            val msg = messages[index]
            if (msg is Message.AssistantMessage) {
                val currentContent = msg.content ?: ""
                val updatedMsg = msg.copy(content = currentContent + delta)
                messages[index] = updatedMsg
                _messages.postValue(messages)
            }
        }
    }

    private fun addSystemMessage(content: String) {
        val messages = _messages.value ?: mutableListOf()
        messages.add(Message.SystemMessage(content = content))
        _messages.value = messages
    }

    // ── Actions ──

    fun clearChat() {
        _messages.value = mutableListOf()
    }

    fun stopStreaming() {
        val sessionId = activeSessionId ?: return
        viewModelScope.launch {
            api?.interruptSession(sessionId)
            _isStreaming.value = false
        }
    }

    fun errorShown() {
        _error.value = null
    }

    fun approvalShown() {
        _pendingApproval.value = null
    }

    fun submitApproval(approved: Boolean, approvalId: String) {
        viewModelScope.launch {
            api?.respondApproval(approvalId, approved)
        }
    }

    fun reconnect() {
        val serverUrl = prefs.getString("server_url", "http://127.0.0.1:9119")
            ?: "http://127.0.0.1:9119"
        connect(serverUrl)
    }

    override fun onCleared() {
        super.onCleared()
        api?.disconnect()
    }
}