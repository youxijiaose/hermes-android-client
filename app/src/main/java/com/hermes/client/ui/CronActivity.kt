package com.hermes.client.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.hermes.client.adapter.CronAdapter
import com.hermes.client.databinding.ActivityCronBinding
import com.hermes.client.viewmodel.CronViewModel

class CronActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCronBinding
    private lateinit var viewModel: CronViewModel
    private lateinit var adapter: CronAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCronBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[CronViewModel::class.java]

        supportActionBar?.title = "Cron Jobs"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = CronAdapter(
            onToggle = { job -> viewModel.toggleJob(job.id) },
            onDelete = { job -> showDeleteConfirm(job) },
            onRun = { job -> viewModel.runJob(job.id) },
            onEdit = { job -> showEditDialog(job) }
        )

        binding.recyclerCron.apply {
            layoutManager = LinearLayoutManager(this@CronActivity)
            adapter = adapter
        }

        binding.fabAdd.setOnClickListener {
            showCreateDialog()
        }


        viewModel.cronJobs.observe(this) { jobs ->
            adapter.submitList(jobs)
            binding.emptyView.visibility = if (jobs.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                android.widget.Toast.makeText(this, it, android.widget.Toast.LENGTH_SHORT).show()
                viewModel.errorShown()
            }
        }

        viewModel.refresh()
    }

    private fun showCreateDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Create Cron Job")

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }

        val nameInput = android.widget.EditText(this).apply {
            hint = "Job name (optional)"
        }
        layout.addView(nameInput)

        val scheduleInput = android.widget.EditText(this).apply {
            hint = "Schedule (e.g. every 30m, 0 9 * * *)"
        }
        layout.addView(scheduleInput)

        val promptInput = android.widget.EditText(this).apply {
            hint = "Prompt"
            minLines = 3
        }
        layout.addView(promptInput)

        builder.setView(layout)
        builder.setPositiveButton("Create") { dialog, _ ->
            val schedule = scheduleInput.text.toString().trim()
            val prompt = promptInput.text.toString().trim()
            if (schedule.isNotEmpty() && prompt.isNotEmpty()) {
                viewModel.createJob(schedule, prompt, nameInput.text.toString().trim().takeIf { it.isNotEmpty() })
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        if (!isFinishing) builder.show()
    }

    private fun showEditDialog(job: com.hermes.client.model.CronJob) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Cron Job")

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }

        val nameInput = android.widget.EditText(this).apply {
            hint = "Job name"
            setText(job.name ?: "")
        }
        layout.addView(nameInput)

        val scheduleInput = android.widget.EditText(this).apply {
            hint = "Schedule"
            setText(job.schedule)
        }
        layout.addView(scheduleInput)

        val promptInput = android.widget.EditText(this).apply {
            hint = "Prompt"
            setText(job.prompt)
            minLines = 3
        }
        layout.addView(promptInput)

        builder.setView(layout)
        builder.setPositiveButton("Save") { dialog, _ ->
            dialog.dismiss()
            // For now, create a new job with updated params and delete old one
            val schedule = scheduleInput.text.toString().trim()
            val prompt = promptInput.text.toString().trim()
            if (schedule.isNotEmpty() && prompt.isNotEmpty()) {
                viewModel.createJob(schedule, prompt, nameInput.text.toString().trim().takeIf { it.isNotEmpty() })
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        if (!isFinishing) builder.show()
    }

    private fun showDeleteConfirm(job: com.hermes.client.model.CronJob) {
        if (isFinishing) return
        AlertDialog.Builder(this)
            .setTitle("Delete Cron Job")
            .setMessage("Are you sure you want to delete \"${job.name ?: job.id}\"?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteJob(job.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
