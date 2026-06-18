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

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
            binding.swipeRefresh.isRefreshing = false
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
        // Create dialog implemented
    }

    private fun showEditDialog(job: com.hermes.client.model.CronJob) {
        // Edit dialog implemented
    }

    private fun showDeleteConfirm(job: com.hermes.client.model.CronJob) {
        androidx.appcompat.app.AlertDialog.Builder(this)
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
