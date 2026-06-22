package com.hermes.client.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.hermes.client.R
import com.hermes.client.adapter.MemoryAdapter
import com.hermes.client.databinding.ActivityMemoryBinding
import com.hermes.client.model.MemoryEntry
import com.hermes.client.viewmodel.MemoryViewModel
import androidx.appcompat.widget.SearchView

class MemoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemoryBinding
    private lateinit var viewModel: MemoryViewModel
    private lateinit var adapter: MemoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MemoryViewModel::class.java]

        supportActionBar?.title = "Memory"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = MemoryAdapter(
            onEdit = { entry -> showEditDialog(entry) },
            onDelete = { entry -> showDeleteConfirm(entry) },
            onDetails = { entry -> showDetailsDialog(entry) }
        )

        binding.recyclerMemory.apply {
            layoutManager = LinearLayoutManager(this@MemoryActivity)
            adapter = adapter
        }

        // Search functionality
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchMemory(it) }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchMemory(newText ?: "")
                return true
            }
        })

        // Target switcher
        binding.btnUserTarget.setOnClickListener {
            viewModel.switchTarget("user")
            updateTargetButtons("user")
        }

        binding.btnMemoryTarget.setOnClickListener {
            viewModel.switchTarget("memory")
            updateTargetButtons("memory")
        }

        binding.fabAdd.setOnClickListener {
            showCreateDialog()
        }

        viewModel.memoryEntries.observe(this) { entries ->
            adapter.submitList(entries)
            binding.emptyView.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.memoryStats.observe(this) { stats ->
            stats?.let {
                binding.statsView.visibility = View.VISIBLE
                binding.statsView.text = "Total: ${it.total_entries} entries | User: ${it.user_entries} | Memory: ${it.memory_entries} | Chars: ${it.total_chars}"
            } ?: run {
                binding.statsView.visibility = View.GONE
            }
        }

        viewModel.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.errorShown()
            }
        }

        viewModel.refresh()
    }

    private fun updateTargetButtons(target: String) {
        binding.btnUserTarget.isSelected = target == "user"
        binding.btnMemoryTarget.isSelected = target == "memory"
    }

    private fun showCreateDialog() {
        if (isFinishing) return
        val target = viewModel.selectedTarget.value ?: "memory"
        val input = android.widget.EditText(this)
        input.hint = "Enter content..."
        input.setPadding(50, 0, 50, 0)
        AlertDialog.Builder(this)
            .setTitle("Create Memory Entry")
            .setView(input)
            .setPositiveButton("Create") { dialog, _ ->
                val content = input.text.toString().trim()
                if (content.isNotEmpty()) {
                    viewModel.createMemory(content, target)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showEditDialog(entry: MemoryEntry) {
        if (isFinishing) return
        val input = android.widget.EditText(this)
        input.setText(entry.content)
        input.setPadding(50, 0, 50, 0)
        AlertDialog.Builder(this)
            .setTitle("Edit Memory Entry")
            .setView(input)
            .setPositiveButton("Save") { dialog, _ ->
                val content = input.text.toString().trim()
                if (content.isNotEmpty()) {
                    viewModel.updateMemory(entry.id, content = content)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showDetailsDialog(entry: MemoryEntry) {
        if (isFinishing) return
        val content = StringBuilder()
        content.append("ID: ${entry.id}\n\n")
        content.append("Target: ${entry.target}\n\n")
        content.append("Content:\n${entry.content}\n\n")
        content.append("Created: ${com.hermes.client.util.TimeUtils.formatFullTime(entry.created_at)}\n")
        content.append("Updated: ${com.hermes.client.util.TimeUtils.formatFullTime(entry.updated_at)}\n")
        if (!entry.tags.isNullOrEmpty()) {
            content.append("\nTags: ${entry.tags.joinToString(", ")}")
        }
        AlertDialog.Builder(this)
            .setTitle("Memory Entry Details")
            .setMessage(content.toString())
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showDeleteConfirm(entry: MemoryEntry) {
        if (isFinishing) return
        AlertDialog.Builder(this)
            .setTitle("Delete Memory Entry")
            .setMessage("Are you sure you want to delete this entry?\n\n${entry.content.take(100)}${if (entry.content.length > 100) "..." else ""}")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteMemory(entry.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.memory_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.refresh()
                true
            }
            R.id.action_clear_all -> {
                AlertDialog.Builder(this)
                    .setTitle("Clear All Memory")
                    .setMessage("Are you sure you want to delete ALL entries? This cannot be undone.")
                    .setPositiveButton("Clear All") { _, _ ->
                        Toast.makeText(this, "Bulk delete will be implemented in future version", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}