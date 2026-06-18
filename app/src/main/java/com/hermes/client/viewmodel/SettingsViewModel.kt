package com.hermes.client.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SettingsViewModel : ViewModel() {

    private val _serverUrl = MutableLiveData<String>()
    val serverUrl: LiveData<String> = _serverUrl

    private val _apiKey = MutableLiveData<String>()
    val apiKey: LiveData<String> = _apiKey

    fun setServerUrl(url: String) {
        _serverUrl.value = url
    }

    fun setApiKey(key: String) {
        _apiKey.value = key
    }
}
