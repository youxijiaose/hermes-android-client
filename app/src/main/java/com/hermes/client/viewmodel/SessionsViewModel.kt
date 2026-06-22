package com.hermes.client.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hermes.client.api.HermesApi
import com.hermes.client.model.SessionInfo
import kotlinx.coroutines.launch

class SessionsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("hermes_prefs", Application.MODE_PRIVATE)
    private var api: HermesApi? = null

    private val _sessions = MutableLiveData<List<SessionInfo>>(emptyList())
    val sessions: LiveData<List<SessionInfo>> = _sessions

    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private var allSessions: List<SessionInfo> = emptyList()

    init {
        val serverUrl = prefs.getString("server_url", "http://127.0.0.1:9119")
            ?: "http://127.0.0.1:9119"
        api = HermesApi(serverUrl)
    }

    fun refresh() {
        _loading.value = true
        viewModelScope.launch {
            val result = api?.getSessions()
            _loading.value = false
            result?.onSuccess { sessions ->
                allSessions = sessions.sortedByDescending { it.updated_at }
                _sessions.value = allSessions
            }?.onFailure {
                _error.value = it.message ?: "Failed to load sessions"
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            val result = api?.deleteSession(sessionId)
            result?.onSuccess {
                refresh()
            }?.onFailure {
                _error.value = "Delete failed: ${it.message}"
            }
        }
    }

    fun searchSessions(query: String) {
        if (query.isBlank()) {
            _sessions.value = allSessions
            return
        }

        val filtered = allSessions.filter { session ->
            session.title?.contains(query, ignoreCase = true) == true ||
                    session.id.contains(query, ignoreCase = true)
        }
        _sessions.value = filtered
    }

    fun createNewSession() {
        _loading.value = true
        viewModelScope.launch {
            val result = api?.createSession()
            _loading.value = false
            result?.onSuccess {
                refresh()
            }?.onFailure {
                _error.value = "Create failed: ${it.message}"
            }
        }
    }
}