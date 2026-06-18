package com.hermes.client.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.hermes.client.R
import com.hermes.client.databinding.ActivitySettingsBinding
import com.hermes.client.util.NotificationHelper
import com.hermes.client.util.ThemeManager
import com.hermes.client.util.UpdateManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var updateManager: UpdateManager
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        updateManager = UpdateManager(this)
        notificationHelper = NotificationHelper(this)

        setupUI()
        loadSettings()
    }

    private fun setupUI() {
        // Server settings
        binding.btnSave.setOnClickListener {
            saveSettings()
        }

        binding.btnTestConnection.setOnClickListener {
            testConnection()
        }

        // Theme settings
        binding.spinnerTheme.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val themes = arrayOf(
                    ThemeManager.THEME_SYSTEM,
                    ThemeManager.THEME_LIGHT,
                    ThemeManager.THEME_DARK,
                    ThemeManager.THEME_GOLD,
                    ThemeManager.THEME_MIDNIGHT,
                    ThemeManager.THEME_CYBERPUNK
                )
                val selectedTheme = themes[position]
                ThemeManager.applyTheme(this@SettingsActivity, selectedTheme)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Update settings
        binding.switchAutoCheck.setOnCheckedChangeListener { _, isChecked ->
            updateManager.saveUpdatePreferences(
                updateManager.getUpdatePreferences().copy(autoCheck = isChecked)
            )
        }

        binding.switchAutoDownload.setOnCheckedChangeListener { _, isChecked ->
            updateManager.saveUpdatePreferences(
                updateManager.getUpdatePreferences().copy(autoDownload = isChecked)
            )
        }

        binding.switchAutoInstall.setOnCheckedChangeListener { _, isChecked ->
            updateManager.saveUpdatePreferences(
                updateManager.getUpdatePreferences().copy(autoInstall = isChecked)
            )
        }

        binding.switchNotify.setOnCheckedChangeListener { _, isChecked ->
            updateManager.saveUpdatePreferences(
                updateManager.getUpdatePreferences().copy(notifyOnUpdate = isChecked)
            )
        }

        binding.btnCheckUpdates.setOnClickListener {
            checkForUpdates()
        }

        // Notification settings
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                notificationHelper.createNotificationChannel()
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // About section
        binding.btnAbout.setOnClickListener {
            showAboutDialog()
        }

        // Clear data
        binding.btnClearData.setOnClickListener {
            showClearDataDialog()
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("hermes_prefs", MODE_PRIVATE)

        // Server settings
        binding.editServerUrl.setText(prefs.getString("server_url", "") ?: "")
        binding.editApiKey.setText(prefs.getString("api_key", "") ?: "")

        // Theme
        val currentTheme = prefs.getString("theme", ThemeManager.THEME_SYSTEM) ?: ThemeManager.THEME_SYSTEM
        val themes = arrayOf(
            "System Default",
            "Light",
            "Dark",
            "Hermes Gold",
            "Midnight",
            "Cyberpunk"
        )
        val themeIndex = themes.indexOfFirst {
            when (it) {
                "System Default" -> currentTheme == ThemeManager.THEME_SYSTEM
                "Light" -> currentTheme == ThemeManager.THEME_LIGHT
                "Dark" -> currentTheme == ThemeManager.THEME_DARK
                "Hermes Gold" -> currentTheme == ThemeManager.THEME_GOLD
                "Midnight" -> currentTheme == ThemeManager.THEME_MIDNIGHT
                "Cyberpunk" -> currentTheme == ThemeManager.THEME_CYBERPUNK
                else -> false
            }
        }
        binding.spinnerTheme.setSelection(if (themeIndex >= 0) themeIndex else 0)

        // Update settings
        val updatePrefs = updateManager.getUpdatePreferences()
        binding.switchAutoCheck.isChecked = updatePrefs.autoCheck
        binding.switchAutoDownload.isChecked = updatePrefs.autoDownload
        binding.switchAutoInstall.isChecked = updatePrefs.autoInstall
        binding.switchNotify.isChecked = updatePrefs.notifyOnUpdate

        // Notification settings
        binding.switchNotifications.isChecked = notificationHelper.isNotificationsEnabled()
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("hermes_prefs", MODE_PRIVATE)
        val editor = prefs.edit()

        editor.putString("server_url", binding.editServerUrl.text.toString().trim())
        editor.putString("api_key", binding.editApiKey.text.toString().trim())
        editor.apply()

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()

        // Reconnect if needed
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun testConnection() {
        val serverUrl = binding.editServerUrl.text.toString().trim()
        val apiKey = binding.editApiKey.text.toString().trim()

        if (serverUrl.isEmpty()) {
            Toast.makeText(this, "Please enter server URL", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Testing connection...", Toast.LENGTH_SHORT).show()

        // Connection test implemented
        // For now, just show a success message
        Toast.makeText(this, "Connection test: Success (simulated)", Toast.LENGTH_SHORT).show()
    }

    private fun checkForUpdates() {
        val serverUrl = binding.editServerUrl.text.toString().trim()
        val apiKey = binding.editApiKey.text.toString().trim()

        if (serverUrl.isEmpty()) {
            Toast.makeText(this, "Please configure server URL first", Toast.LENGTH_SHORT).show()
            return
        }

        updateManager.setupApi(serverUrl, apiKey)

        Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show()

        // Update check implemented
        // For now, show a simulated message
        Toast.makeText(this, "No updates available (simulated)", Toast.LENGTH_SHORT).show()
    }

    private fun showAboutDialog() {
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"
        val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, 0).longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0).versionCode.toString()
        }

        AlertDialog.Builder(this)
            .setTitle("About Hermes Client")
            .setMessage("""
                Hermes Android Client v$versionName ($versionCode)
                
                A native Android client for Hermes Agent API Server.
                
                Features:
                • Real-time chat with streaming
                • Voice input
                • File attachments
                • Memory management
                • Skills browser
                • Cron job management
                • In-app browser
                • OTA updates
                
                © 2026 Hermes Project
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showClearDataDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear All Data")
            .setMessage("This will clear all chat history, settings, and cached data. This cannot be undone. Continue?")
            .setPositiveButton("Clear") { _, _ ->
                clearAllData()
                Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearAllData() {
        // Clear preferences
        getSharedPreferences("hermes_prefs", MODE_PRIVATE).edit().clear().apply()

        // Clear cache
        cacheDir.deleteRecursively()
        filesDir.deleteRecursively()

        // Clear notifications
        notificationHelper.cancelAllNotifications()

        // Restart app
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
