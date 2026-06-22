package com.hermes.client.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hermes.client.api.HermesApi
import com.hermes.client.model.CronJob
import kotlinx.coroutines.launch

class CronViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("hermes_prefs", Application.MODE_PRIVATE)
    private var api: HermesApi? = null

    private val _cronJobs = MutableLiveData<List<CronJob>>(emptyList())
    val cronJobs: LiveData<List<CronJob>> = _cronJobs

    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    init {
        setupApi()
        refresh()
    }

    private fun setupApi() {
        val serverUrl = prefs.getString("server_url", "") ?: return
        if (serverUrl.isNotEmpty()) {
            api = HermesApi(serverUrl)
        }
    }

    fun refresh() {
        _loading.value = true
        viewModelScope.launch {
            val result = api?.getCronJobs()
            _loading.value = false
            result?.onSuccess { jobs ->
                _cronJobs.value = jobs
            }?.onFailure {
                _error.value = it.message
            }
        }
    }

    fun toggleJob(jobId: String) {
        viewModelScope.launch {
            val result = api?.toggleCronJob(jobId)
            result?.onFailure {
                _error.value = it.message
            }
            refresh()
        }
    }

    fun deleteJob(jobId: String) {
        viewModelScope.launch {
            val result = api?.deleteCronJob(jobId)
            result?.onFailure {
                _error.value = it.message
            }
            refresh()
        }
    }

    fun runJob(jobId: String) {
        viewModelScope.launch {
            val result = api?.runCronJob(jobId)
            result?.onFailure {
                _error.value = it.message
            }
        }
    }

    fun createJob(schedule: String, prompt: String, name: String? = null) {
        viewModelScope.launch {
            val result = api?.createCronJob(schedule, prompt, name)
            result?.onFailure {
                _error.value = it.message
            }
            refresh()
        }
    }

    fun errorShown() {
        _error.value = null
    }
}