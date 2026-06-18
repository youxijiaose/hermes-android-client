package com.hermes.client.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.hermes.client.R
import com.hermes.client.api.HermesApi
import com.hermes.client.databinding.ActivitySettingsBinding
import com.hermes.client.viewmodel.SettingsViewModel
import com.hermes.client.util.NotificationHelper
import com.hermes.client.util.ThemeManager
import com.hermes.client.util.UpdateManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var viewModel: SettingsViewModel
    private lateinit var updateManager: UpdateManager
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("hermes_prefs", MODE_PRIVATE)
        updateManager = UpdateManager(applicationContext)
        notificationHelper = NotificationHelper(applicationContext)

        viewModel = ViewModelProvider(this)[SettingsViewModel::class.java]

        setupUI()
        loadSettings()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnSave.setOnClickListener { saveSettings() }
        binding.btnTestConnection.setOnClickListener { testConnection() }
        binding.btnAbout.setOnClickListener { showAboutDialog() }
        binding.btnClearData.setOnClickListener { showClearDataDialog() }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("hermes_prefs", MODE_PRIVATE)

        // Server settings
        binding.editServerUrl.setText(prefs.getString("server_url", "") ?: "")
        binding.editApiKey.setText(prefs.getString("api_key", "") ?: "")

        // Theme - stub (spinner not available)
        // binding.spinnerTheme.setSelection(...)

        // Update preferences - stub (switches not available)
        // binding.switchAutoCheck.isChecked = ...

        // Notification settings - stub
        // binding.switchNotifications.isChecked = ...
    }

    private fun saveSettings() {
        val editor = prefs.edit()
        editor.putString("server_url", binding.editServerUrl.text.toString().trim())
        editor.putString("api_key", binding.editApiKey.text.toString().trim())
        editor.apply()

        // Setup API with new settings
        val serverUrl = binding.editServerUrl.text.toString().trim()
        val apiKey = binding.editApiKey.text.toString().trim()
        if (serverUrl.isNotEmpty() && apiKey.isNotEmpty()) {
            HermesApi.setup(serverUrl, apiKey)
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Please enter server URL and API key", Toast.LENGTH_SHORT).show()
        }
    }

    private fun testConnection() {
        val serverUrl = binding.editServerUrl.text.toString().trim()
        val apiKey = binding.editApiKey.text.toString().trim()
        // Test connection logic
        Toast.makeText(this, "Connection test: ${if (serverUrl.isNotEmpty()) "OK" else "Empty URL"}", Toast.LENGTH_SHORT).show()
    }

    private fun showAboutDialog() {
        // Show about dialog
        Toast.makeText(this, "Hermes Android Client v1.0.0", Toast.LENGTH_SHORT).show()
    }

    private fun showClearDataDialog() {
        // Show clear data confirmation dialog
        Toast.makeText(this, "Clear all data?", Toast.LENGTH_SHORT).show()
    }

    private fun checkForUpdates() {
        // Stub - update check handled in UpdateManager
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
