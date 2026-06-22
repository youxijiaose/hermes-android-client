package com.hermes.client.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.hermes.client.R
import com.hermes.client.adapter.ChatAdapter
import com.hermes.client.databinding.ActivityMainBinding
import com.hermes.client.util.CrashLogWriter
import com.hermes.client.util.ThemeManager
import com.hermes.client.util.VoiceInputHelper
import com.hermes.client.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var chatAdapter: ChatAdapter

    private var voiceInputHelper: VoiceInputHelper? = null
    private var isRecording = false

    companion object {
        private const val TAG = "MainActivity"
    }

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            CrashLogWriter.writeLog(this, "ON_CREATE_START", "MainActivity.onCreate starting")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write start log", e)
        }

        loadTheme()

        try {
            super.onCreate(savedInstanceState)
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupViewModel()
            setupUI()
            setupHelpers()
            setupObservers()

            CrashLogWriter.writeLog(this, "ON_CREATE_SUCCESS", "MainActivity created successfully")
        } catch (e: Exception) {
            CrashLogWriter.writeCrashLog(this, "ON_CREATE_EXCEPTION", e)
            throw e
        }
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }

    private fun setupUI() {
        chatAdapter = ChatAdapter()
        binding.recyclerChat.apply {
            layoutManager = LinearLayoutManager(this@MainActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }

        binding.editMessage.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                sendMessage()
                true
            } else {
                false
            }
        }

        // All clicks wrapped in safeClick
        binding.btnSend.setOnClickListener {
            safeClick("btnSend") { sendMessage() }
        }
        binding.btnSettings.setOnClickListener {
            safeClick("btnSettings") {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
        }
        binding.btnSessions.setOnClickListener {
            safeClick("btnSessions") {
                startActivity(Intent(this@MainActivity, SessionsActivity::class.java))
            }
        }
        binding.btnAttach.setOnClickListener {
            safeClick("btnAttach") {
                Toast.makeText(this@MainActivity, "Attach: coming soon", Toast.LENGTH_SHORT).show()
            }
        }
        binding.btnVoice.setOnClickListener {
            safeClick("btnVoice") { toggleVoiceInput() }
        }

        // Connection status bar - tap to reconnect
        binding.connectionStatus.setOnClickListener {
            safeClick("connectionStatus") {
                viewModel.reconnect()
                Toast.makeText(this, "Reconnecting...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun safeClick(buttonName: String, action: () -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            Log.e(TAG, "Click error on $buttonName: ${e.message}", e)
            CrashLogWriter.writeCrashLog(this, "CLICK_ERROR_$buttonName", e)
            Toast.makeText(this, "Button error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupHelpers() {
        voiceInputHelper = VoiceInputHelper(
            activity = this,
            onSpeechResult = { text ->
                binding.editMessage.append(text)
                isRecording = false
            },
            onSpeechError = { error ->
                Toast.makeText(this, "Speech error: $error", Toast.LENGTH_SHORT).show()
                isRecording = false
            }
        )
    }

    private fun setupObservers() {
        viewModel.messages.observe(this) { messages ->
            chatAdapter.submitList(messages)
            if (messages.isNotEmpty()) {
                binding.recyclerChat.smoothScrollToPosition(messages.size - 1)
            }
        }

        viewModel.connectionState.observe(this) { state ->
            updateConnectionUI(state)
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.errorShown()
            }
        }

        viewModel.pendingApproval.observe(this) { approval ->
            approval?.let {
                val intent = Intent(this, ApprovalActivity::class.java).apply {
                    putExtra(ApprovalActivity.EXTRA_APPROVAL_ID, it.id)
                    putExtra(ApprovalActivity.EXTRA_COMMAND, it.command)
                    putExtra(ApprovalActivity.EXTRA_REASON, it.reason)
                }
                startActivity(intent)
                viewModel.approvalShown()
            }
        }

        // Show/hide a stop button / change send icon during streaming
        viewModel.isStreaming.observe(this) { streaming ->
            if (streaming) {
                binding.btnSend.setImageResource(android.R.drawable.ic_media_pause)
                binding.editMessage.isEnabled = false
            } else {
                binding.btnSend.setImageResource(android.R.drawable.ic_menu_send)
                binding.editMessage.isEnabled = true
            }
        }
    }

    private fun updateConnectionUI(state: MainViewModel.ConnectionState) {
        when (state) {
            is MainViewModel.ConnectionState.Connected -> {
                binding.connectionStatus.visibility = View.GONE
            }
            is MainViewModel.ConnectionState.Disconnected -> {
                binding.connectionStatus.visibility = View.VISIBLE
                binding.connectionText.text = getString(R.string.disconnected) + " (tap to reconnect)"
            }
            is MainViewModel.ConnectionState.Connecting -> {
                binding.connectionStatus.visibility = View.VISIBLE
                binding.connectionText.text = getString(R.string.connecting)
            }
        }
    }

    private fun sendMessage() {
        val text = binding.editMessage.text.toString().trim()
        if (text.isEmpty()) return

        // if currently streaming, stop instead
        if (viewModel.isStreaming.value == true) {
            viewModel.stopStreaming()
            return
        }

        binding.editMessage.text.clear()
        viewModel.sendMessage(text)
    }

    private fun toggleVoiceInput() {
        if (isRecording) {
            voiceInputHelper?.stopListening()
            isRecording = false
        } else {
            checkVoicePermission()
        }
    }

    private fun checkVoicePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            voiceInputHelper?.startListening()
            isRecording = true
            Toast.makeText(this, "Listening...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTheme() {
        val prefs = getSharedPreferences("hermes_prefs", MODE_PRIVATE)
        val theme = prefs.getString("theme", ThemeManager.THEME_SYSTEM) ?: ThemeManager.THEME_SYSTEM
        ThemeManager.applyTheme(this, theme)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear -> {
                AlertDialog.Builder(this)
                    .setTitle(R.string.clear_chat)
                    .setMessage(R.string.clear_confirm)
                    .setPositiveButton(R.string.yes) { _, _ -> viewModel.clearChat() }
                    .setNegativeButton(R.string.no, null)
                    .show()
                true
            }
            R.id.action_memory -> {
                startActivity(Intent(this, MemoryActivity::class.java))
                true
            }
            R.id.action_cron -> {
                startActivity(Intent(this, CronActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.reconnect()
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceInputHelper?.destroy()
    }
}