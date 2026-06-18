package com.hermes.client.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hermes.client.R
import com.hermes.client.databinding.ActivityApprovalBinding
import com.hermes.client.viewmodel.MainViewModel
import androidx.lifecycle.ViewModelProvider

class ApprovalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApprovalBinding
    private lateinit var viewModel: MainViewModel

    companion object {
        const val EXTRA_APPROVAL_ID = "approval_id"
        const val EXTRA_COMMAND = "command"
        const val EXTRA_REASON = "reason"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApprovalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        val approvalId = intent.getStringExtra(EXTRA_APPROVAL_ID) ?: ""
        val command = intent.getStringExtra(EXTRA_COMMAND) ?: ""
        val reason = intent.getStringExtra(EXTRA_REASON)

        supportActionBar?.title = getString(R.string.command_approval_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.commandText.text = command
        binding.reasonText.text = reason ?: ""

        binding.btnApprove.setOnClickListener {
            viewModel.submitApproval(true, approvalId)
            finish()
        }

        binding.btnDeny.setOnClickListener {
            viewModel.submitApproval(false, approvalId)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
