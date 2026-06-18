package com.hermes.client.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hermes.client.api.HermesApi
import com.hermes.client.model.Skill
import com.hermes.client.model.SkillCategory
import kotlinx.coroutines.launch

class SkillsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("hermes_prefs", Application.MODE_PRIVATE)
    private var api: HermesApi? = null

    private val _skills = MutableLiveData<List<Skill>>(emptyList())
    val skills: LiveData<List<Skill>> = _skills

    private val _categories = MutableLiveData<List<SkillCategory>>(emptyList())
    val categories: LiveData<List<SkillCategory>> = _categories

    private val _selectedCategory = MutableLiveData<String?>(null)
    val selectedCategory: LiveData<String?> = _selectedCategory

    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _installing = MutableLiveData<String?>()
    val installing: LiveData<String?> = _installing

    init {
        setupApi()
        refresh()
        loadCategories()
    }

    private fun setupApi() {
        val serverUrl = prefs.getString("server_url", "") ?: return
        val apiKey = prefs.getString("api_key", "") ?: return
        if (serverUrl.isNotEmpty() && apiKey.isNotEmpty()) {
            api = HermesApi(serverUrl, apiKey)
        }
    }

    fun refresh() {
        val category = _selectedCategory.value
        _loading.value = true
        viewModelScope.launch {
            val result = api?.getSkills(category = category)
            _loading.value = false
            result?.onSuccess { skills: List<Skill> ->
                _skills.value = skills
            }?.onFailure { error: Throwable ->
                _error.value = error.message
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            val result = api?.getSkillCategories()
            result?.onSuccess { categories: List<SkillCategory> ->
                _categories.value = categories
            }
        }
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
        refresh()
    }

    fun installSkill(id: String) {
        _installing.value = id
        viewModelScope.launch {
            val result = api?.installSkill(id)
            _installing.value = null
            result?.onSuccess { skill: Skill ->
                refresh()
            }?.onFailure { error: Throwable ->
                _error.value = error.message
            }
        }
    }

    fun uninstallSkill(id: String) {
        _installing.value = id
        viewModelScope.launch {
            val result = api?.uninstallSkill(id)
            _installing.value = null
            result?.onSuccess { success: Boolean ->
                refresh()
            }?.onFailure { error: Throwable ->
                _error.value = error.message
            }
        }
    }

    fun togglePin(id: String) {
        viewModelScope.launch {
            val result = api?.togglePinSkill(id)
            result?.onFailure { error: Throwable ->
                _error.value = error.message
            }
            refresh()
        }
    }

    fun searchSkills(query: String) {
        val allSkills = _skills.value ?: return
        if (query.isBlank()) {
            refresh()
            return
        }

        val filtered = allSkills.filter { skill: Skill ->
            skill.name.contains(query, ignoreCase = true) ||
                    skill.description.contains(query, ignoreCase = true) ||
                    skill.tags?.any { tag: String -> tag.contains(query, ignoreCase = true) } == true
        }
        _skills.value = filtered
    }

    fun errorShown() {
        _error.value = null
    }
}
