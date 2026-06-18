package com.hermes.client.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hermes.client.api.HermesApi
import com.hermes.client.model.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("hermes_prefs", Application.MODE_PRIVATE)
    
    private var api: HermesApi? = null
    private val _messages = MutableLiveData<MutableList<Message>>(mutableListOf())
    val messages: LiveData<MutableList<Message>> = _messages
    
    private val _connectionState = MutableLiveData<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: LiveData<ConnectionState> = _connectionState
    
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error
    
    private val _pendingApproval = MutableLiveData<ApprovalRequest?>()
    val pendingApproval: LiveData<ApprovalRequest?> = _pendingApproval
    
    private var currentModel = prefs.getString("model", "default") ?: "default"
    private var isStreaming = false

    init {
        checkConnection()
    }

    sealed class ConnectionState {
        object Connected : ConnectionState()
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
    }

    fun checkConnection() {
        val serverUrl = prefs.getString("server_url", "http://192.168.1.100:8080") ?: return
        val apiKey = prefs.getString("api_key", "") ?: return

        if (serverUrl.isEmpty() || apiKey.isEmpty()) {
            _connectionState.value = ConnectionState.Disconnected
            return
        }

        _connectionState.value = ConnectionState.Connecting
        api = HermesApi(serverUrl, apiKey)

        viewModelScope.launch {
            val result = api?.healthCheck()
            _connectionState.value = if (result?.isSuccess == true) {
                ConnectionState.Connected
            } else {
                ConnectionState.Disconnected
            }
        }
    }

    fun sendMessage(content: String) {
        if (_connectionState.value != ConnectionState.Connected) {
            _error.value = "Not connected to server"
            return
        }

        val messages = _messages.value ?: return
        messages.add(Message.UserMessage(content = content))
        _messages.value = messages

        viewModelScope.launch {
            streamResponse()
        }
    }

    private suspend fun streamResponse() {
        val messagesList = _messages.value?.map { msg ->
            MessageWrapper(
                role = msg.role,
                content = msg.content
            )
        } ?: return

        val request = ChatRequest(
            messages = messagesList,
            model = currentModel,
            stream = true
        )
        
        val assistantMsg = Message.AssistantMessage()
        _messages.value?.add(assistantMsg)
        _messages.value = _messages.value

        api?.streamChat(messagesList, currentModel, object : com.hermes.client.api.WebSocketListener {
            override fun onOpen() {}
            override fun onMessage(response: ChatResponse) {
                response.choices?.firstOrNull()?.message?.content?.let { content ->
                    // Update the content by replacing the message in the list
                    val index = _messages.value?.indexOf(assistantMsg) ?: -1
                    if (index >= 0) {
                        val updatedMsg = assistantMsg.copy(content = (assistantMsg.content ?: "") + content)
                        _messages.value?.set(index, updatedMsg)
                        _messages.value = _messages.value
                    }
                }
            }
            override fun onError(error: Throwable) {
                _error.value = error.message
                isStreaming = false
            }
            override fun onClosed() { isStreaming = false }
            override fun onSocketReady(socket: okhttp3.WebSocket) {}
        })
    }

    fun clearChat() { _messages.value = mutableListOf() }
    fun refreshChat() {}
    fun errorShown() { _error.value = null }
    fun approvalShown() { _pendingApproval.value = null }

    fun submitApproval(approved: Boolean, approvalId: String) {
        viewModelScope.launch {
            val response = ApprovalResponse(id = approvalId, approved = approved)
            api?.submitApproval(response)
        }
    }

    fun updateModel(model: String) {
        currentModel = model
        prefs.edit().putString("model", model).apply()
    }

    fun updateServerConfig(serverUrl: String, apiKey: String) {
        prefs.edit()
            .putString("server_url", serverUrl)
            .putString("api_key", apiKey)
            .apply()
        checkConnection()
    }
}
