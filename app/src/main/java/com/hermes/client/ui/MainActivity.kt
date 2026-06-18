package com.hermes.client.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.hermes.client.R
import com.hermes.client.adapter.ChatAdapter
import com.hermes.client.api.WebSocketListener
import com.hermes.client.databinding.ActivityMainBinding
import com.hermes.client.util.FileAttachmentHelper
import com.hermes.client.util.NotificationHelper
import com.hermes.client.util.ThemeManager
import com.hermes.client.util.VoiceInputHelper
import com.hermes.client.viewmodel.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var chatAdapter: ChatAdapter
    
    private var voiceInputHelper: VoiceInputHelper? = null
    private var fileAttachmentHelper: FileAttachmentHelper? = null
    private var notificationHelper: NotificationHelper? = null
    private var isRecording = false

    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Activity result launcher for file/image pick
    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        fileAttachmentHelper?.handleActivityResult(
            result.requestCode,
            result.resultCode,
            result.data
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupUI()
        setupHelpers()
        setupObservers()
        loadTheme()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)

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

        binding.btnSend.setOnClickListener { sendMessage() }
        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnSessions.setOnClickListener {
            startActivity(Intent(this, SessionsActivity::class.java))
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshChat()
            binding.swipeRefresh.isRefreshing = false
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
                Toast.makeText(this, "Speech recognition error: $error", Toast.LENGTH_SHORT).show()
                isRecording = false
            }
        )

        fileAttachmentHelper = FileAttachmentHelper(
            activity = this,
            onFileSelected = { uri, type ->
                Toast.makeText(this, "File selected: $uri", Toast.LENGTH_SHORT).show()
            },
            onImageCaptured = { uri ->
                Toast.makeText(this, "Image captured: $uri", Toast.LENGTH_SHORT).show()
            }
        )

        notificationHelper = NotificationHelper(this)
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
    }

    private fun updateConnectionUI(state: MainViewModel.ConnectionState) {
        when (state) {
            is MainViewModel.ConnectionState.Connected -> {
                binding.connectionStatus.visibility = View.GONE
            }
            is MainViewModel.ConnectionState.Disconnected -> {
                binding.connectionStatus.visibility = View.VISIBLE
                binding.connectionText.text = getString(R.string.disconnected)
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
        viewModel.checkConnection()
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceInputHelper?.destroy()
        notificationHelper?.cancelNotification()
    }
}
