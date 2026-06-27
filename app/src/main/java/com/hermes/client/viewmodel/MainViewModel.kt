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
            Log.d(TAG, "MainViewModel init")
            CrashLogWriter.writeLog(application, "VIEWMODEL_INIT", "MainViewModel init")
        } catch (e: Exception) {
            Log.e(TAG, "Init log failed", e)
        }
    }

    fun connect(serverUrl: String = "http://127.0.0.1:9119") {
        try {
            _connectionState.value = ConnectionState.Connecting
            Log.d(TAG, "Connecting to $serverUrl")

            val newApi = HermesApi(serverUrl)
            api = newApi

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
                try {
                    when (state) {
                        GatewayClient.State.Open -> {
                            Log.d(TAG, "Gateway opened, creating session")
                            _connectionState.postValue(ConnectionState.Connected)
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
                } catch (e: Exception) {
                    Log.e(TAG, "State callback error", e)
                    _error.postValue("State error: ${e.message}")
                }
            }

            viewModelScope.launch {
                try {
                    val result = newApi.connect()
                    if (result.isFailure) {
                        val msg = result.exceptionOrNull()?.message ?: "unknown error"
                        Log.e(TAG, "Connect failed: $msg")
                        _connectionState.value = ConnectionState.Disconnected
                        _error.value = "Connect failed: $msg"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Connect exception", e)
                    _connectionState.value = ConnectionState.Disconnected
                    _error.value = "Connect error: ${e.message}"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "connect() threw", e)
            _connectionState.value = ConnectionState.Disconnected
            _error.value = "Init error: ${e.message}"
        }
    }

    private fun createNewSession() {
        viewModelScope.launch {
            try {
                val a = api ?: return@launch
                val result = a.createSession()
                result.onSuccess { sessionId ->
                    activeSessionId = sessionId
                    addSystemMessage("Session ready")
                    Log.d(TAG, "Session created: $sessionId")
                }.onFailure { e ->
                    Log.e(TAG, "Session create failed", e)
                    _error.value = "Session create failed: ${e.message}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "createNewSession exception", e)
            }
        }
    }

    fun sendMessage(content: String) {
        val sessionId = activeSessionId ?: return
        if (_connectionState.value != ConnectionState.Connected) {
            _error.value = "Not connected"
            return
        }
        if (_isStreaming.value == true) {
            _error.value = "Still streaming"
            return
        }

        val messages = _messages.value ?: mutableListOf()
        messages.add(Message.UserMessage(content = content))
        streamingAssistantIndex = messages.size
        messages.add(Message.AssistantMessage(content = ""))
        _messages.value = messages
        _isStreaming.value = true

        viewModelScope.launch {
            try {
                val a = api ?: return@launch
                val result = a.submitPrompt(sessionId, content)
                result.onFailure { e ->
                    Log.e(TAG, "Submit prompt failed", e)
                    _error.value = "Send failed: ${e.message}"
                    _isStreaming.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "sendMessage exception", e)
                _error.value = "Send error: ${e.message}"
                _isStreaming.value = false
            }
        }
    }

    private fun onMessageDelta(event: GatewayEvent) {
        val payload = event.payload ?: return
        try {
            val json = JSONObject(payload)
            val text = json.optString("text", "")
            if (text.isNotEmpty()) updateAssistantContent(text)
        } catch (e: Exception) {
            Log.e(TAG, "parse message.delta", e)
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
                val messages = _messages.value ?: return
                val index = streamingAssistantIndex
                if (index >= 0 && index < messages.size && messages[index] is Message.AssistantMessage) {
                    val msg = messages[index] as Message.AssistantMessage
                    val newThinking = (msg.thinking ?: "") + text
                    // Only update if thinking content actually changed
                    if (newThinking != msg.thinking) {
                        val updatedMsg = msg.copy(thinking = newThinking)
                        messages[index] = updatedMsg
                        _messages.postValue(messages)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "parse thinking.delta", e)
        }
    }

    private fun onToolStart(event: GatewayEvent) {
        val payload = event.payload ?: return
        try {
            val json = JSONObject(payload)
            val toolName = json.optString("name", "tool")
            val messages = _messages.value ?: return
            messages.add(Message.ToolMessage(content = "Running: $toolName...", toolName = toolName))
            _messages.postValue(messages)
        } catch (e: Exception) {
            Log.e(TAG, "parse tool.start", e)
        }
    }

    private fun onToolComplete(event: GatewayEvent) {
        val payload = event.payload ?: return
        try {
            val json = JSONObject(payload)
            val toolName = json.optString("name", "tool")
            val result = json.optString("result", "")
            val messages = _messages.value ?: return
            val toolMsgIndex = messages.indexOfLast { it is Message.ToolMessage }
            if (toolMsgIndex >= 0) {
                messages[toolMsgIndex] = Message.ToolMessage(
                    content = "Done: $toolName: ${result.take(200)}${if (result.length > 200) "..." else ""}",
                    toolName = toolName
                )
                _messages.postValue(messages)
            }
        } catch (e: Exception) {
            Log.e(TAG, "parse tool.complete", e)
        }
    }

    private fun onApprovalRequest(event: GatewayEvent) {
        val payload = event.payload ?: return
        try {
            val json = JSONObject(payload)
            _pendingApproval.postValue(ApprovalRequest(
                id = json.optString("id"),
                command = json.optString("command", ""),
                reason = json.optString("reason"),
                timestamp = System.currentTimeMillis()
            ))
        } catch (e: Exception) {
            Log.e(TAG, "parse approval.request", e)
        }
    }

    private fun onClarifyRequest(event: GatewayEvent) {
        val payload = event.payload ?: return
        try {
            val json = JSONObject(payload)
            _error.postValue("Clarify: ${json.optString("question", "")}")
        } catch (e: Exception) {
            Log.e(TAG, "parse clarify.request", e)
        }
    }

    private fun onGatewayError(event: GatewayEvent) {
        val payload = event.payload ?: return
        try {
            val json = JSONObject(payload)
            _error.postValue(json.optString("message", "Gateway error"))
            _isStreaming.postValue(false)
        } catch (e: Exception) {
            Log.e(TAG, "parse gateway error", e)
        }
    }

    private fun onSessionInfo(event: GatewayEvent) {
        val payload = event.payload ?: return
        try {
            val json = JSONObject(payload)
            val sid = json.optString("session_id")
            if (sid.isNotEmpty()) activeSessionId = sid
        } catch (e: Exception) {
            Log.e(TAG, "parse session.info", e)
        }
    }

    private fun updateAssistantContent(delta: String) {
        val messages = _messages.value ?: return
        val index = streamingAssistantIndex
        if (index >= 0 && index < messages.size && messages[index] is Message.AssistantMessage) {
            val msg = messages[index] as Message.AssistantMessage
            val newContent = (msg.content ?: "") + delta
            // Only update if content actually changed to avoid unnecessary LiveData emissions
            if (newContent != msg.content) {
                messages[index] = msg.copy(content = newContent)
                _messages.postValue(messages)
            }
        }
    }

    private fun addSystemMessage(content: String) {
        val messages = _messages.value ?: mutableListOf()
        messages.add(Message.SystemMessage(content = content))
        _messages.value = messages
    }

    fun clearChat() {
        _messages.value = mutableListOf()
    }

    fun stopStreaming() {
        val sessionId = activeSessionId ?: return
        viewModelScope.launch {
            try { api?.interruptSession(sessionId) } catch (e: Exception) {}
            _isStreaming.value = false
        }
    }

    fun errorShown() { _error.value = null }
    fun approvalShown() { _pendingApproval.value = null }

    fun submitApproval(approved: Boolean, approvalId: String) {
        viewModelScope.launch {
            try { api?.respondApproval(approvalId, approved) } catch (e: Exception) {}
        }
    }

    fun reconnect() {
        val serverUrl = prefs.getString("server_url", "http://127.0.0.1:9119")
            ?: "http://127.0.0.1:9119"
        api?.disconnect()
        api = null
        connect(serverUrl)
    }

    override fun onCleared() {
        super.onCleared()
        try { api?.disconnect() } catch (e: Exception) {}
    }
}