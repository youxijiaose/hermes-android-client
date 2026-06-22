package com.hermes.client.ui

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.hermes.client.R
import com.hermes.client.databinding.ActivitySettingsBinding
import com.hermes.client.util.ThemeManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("hermes_prefs", MODE_PRIVATE)

        setupUI()
        loadSettings()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnSave.setOnClickListener { saveSettings() }
        binding.btnTestConnection.setOnClickListener { testConnection() }
        binding.btnAbout.setOnClickListener { showAboutDialog() }
        binding.btnResetDefault.setOnClickListener { resetToDefault() }
    }

    private fun loadSettings() {
        binding.editServerUrl.setText(prefs.getString("server_url", "") ?: "")
    }

    private fun saveSettings() {
        val editor = prefs.edit()
        editor.putString("server_url", binding.editServerUrl.text.toString().trim())
        editor.apply()
        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
    }

    private fun testConnection() {
        val serverUrl = binding.editServerUrl.text.toString().trim()
        if (serverUrl.isEmpty()) {
            Toast.makeText(this, "Please enter server URL first", Toast.LENGTH_SHORT).show()
            return
        }
        // Run test in background thread
        Thread {
            try {
                val url = java.net.URL("$serverUrl/health")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val code = conn.responseCode
                runOnUiThread {
                    if (code == 200) {
                        Toast.makeText(this, "✅ Dashboard reachable (HTTP $code)", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "⚠️ Server responded HTTP $code", Toast.LENGTH_SHORT).show()
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "❌ Connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun resetToDefault() {
        binding.editServerUrl.setText("http://127.0.0.1:9119")
        Toast.makeText(this, "Default URL set", Toast.LENGTH_SHORT).show()
    }

    private fun showAboutDialog() {
        Toast.makeText(this, "Hermes Android Client v2.0\nDirect Dashboard Gateway", Toast.LENGTH_LONG).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}