package com.hermes.client.viewmodel

import android.app.Application
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
        setupApi()
    }

    private fun setupApi() {
        val serverUrl = prefs.getString("server_url", "") ?: return
        val apiKey = prefs.getString("api_key", "") ?: return
        if (serverUrl.isNotEmpty() && apiKey.isNotEmpty()) {
            api = HermesApi(serverUrl, apiKey)
        }
    }

    fun refresh() {
        _loading.value = true
        viewModelScope.launch {
            val result = api?.getSessions()
            _loading.value = false
            result?.onSuccess { sessions ->
                allSessions = sessions
                _sessions.value = sessions
            }?.onFailure {
                _error.value = it.message
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
            // Implementation depends on API
            _loading.value = false
        }
    }
}
