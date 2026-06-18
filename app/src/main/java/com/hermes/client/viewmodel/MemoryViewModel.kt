package com.hermes.client.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hermes.client.api.HermesApi
import com.hermes.client.model.CreateMemoryRequest
import com.hermes.client.model.MemoryEntry
import com.hermes.client.model.MemoryStats
import com.hermes.client.model.UpdateMemoryRequest
import kotlinx.coroutines.launch

class MemoryViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("hermes_prefs", Application.MODE_PRIVATE)
    private var api: HermesApi? = null

    private val _memoryEntries = MutableLiveData<List<MemoryEntry>>(emptyList())
    val memoryEntries: LiveData<List<MemoryEntry>> = _memoryEntries

    private val _memoryStats = MutableLiveData<MemoryStats?>()
    val memoryStats: LiveData<MemoryStats?> = _memoryStats

    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _selectedTarget = MutableLiveData<String>("memory")
    val selectedTarget: LiveData<String> = _selectedTarget

    init {
        setupApi()
        refresh()
    }

    private fun setupApi() {
        val serverUrl = prefs.getString("server_url", "") ?: return
        val apiKey = prefs.getString("api_key", "") ?: return
        if (serverUrl.isNotEmpty() && apiKey.isNotEmpty()) {
            api = HermesApi(serverUrl, apiKey)
        }
    }

    fun refresh() {
        val target = _selectedTarget.value ?: "memory"
        _loading.value = true
        viewModelScope.launch {
            val results = api?.getMemory(target)
            _loading.value = false
            results?.onSuccess { entries ->
                _memoryEntries.value = entries
                loadStats()
            }?.onFailure {
                _error.value = it.message
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            val result = api?.getMemoryStats()
            result?.onSuccess { stats ->
                _memoryStats.value = stats
            }
        }
    }

    fun createMemory(content: String, target: String = "memory", tags: List<String>? = null) {
        viewModelScope.launch {
            val request = CreateMemoryRequest(target, content, tags)
            val result = api?.createMemory(request)
            result?.onSuccess {
                refresh()
            }?.onFailure {
                _error.value = it.message
            }
        }
    }

    fun updateMemory(memoryId: String, content: String? = null, tags: List<String>? = null) {
        viewModelScope.launch {
            val request = UpdateMemoryRequest(content, tags)
            val result = api?.updateMemory(memoryId, request)
            result?.onSuccess {
                refresh()
            }?.onFailure {
                _error.value = it.message
            }
        }
    }

    fun deleteMemory(memoryId: String) {
        viewModelScope.launch {
            val result = api?.deleteMemory(memoryId)
            result?.onSuccess {
                refresh()
            }?.onFailure {
                _error.value = it.message
            }
        }
    }

    fun switchTarget(target: String) {
        _selectedTarget.value = target
        refresh()
    }

    fun searchMemory(query: String) {
        val allEntries = _memoryEntries.value ?: return
        if (query.isBlank()) {
            refresh()
            return
        }

        val filtered = allEntries.filter { entry ->
            entry.content.contains(query, ignoreCase = true) ||
                    entry.tags?.any { it.contains(query, ignoreCase = true) } == true
        }
        _memoryEntries.value = filtered
    }

    fun errorShown() {
        _error.value = null
    }
}
