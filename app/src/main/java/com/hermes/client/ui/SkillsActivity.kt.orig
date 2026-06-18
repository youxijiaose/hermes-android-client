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
import com.hermes.client.adapter.SkillsAdapter
import com.hermes.client.databinding.ActivitySkillsBinding
import com.hermes.client.model.Skill
import com.hermes.client.viewmodel.SkillsViewModel

class SkillsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkillsBinding
    private lateinit var viewModel: SkillsViewModel
    private lateinit var adapter: SkillsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySkillsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[SkillsViewModel::class.java]

        supportActionBar?.title = "Skills"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = SkillsAdapter(
            onInstall = { id -> viewModel.installSkill(id) },
            onUninstall = { id -> viewModel.uninstallSkill(id) },
            onPin = { id -> viewModel.togglePin(id) },
            onClick = { skill -> showSkillDetails(skill) }
        )

        binding.recyclerSkills.apply {
            layoutManager = LinearLayoutManager(this@SkillsActivity)
            adapter = adapter
        }

        // Search functionality
        binding.searchView.setOnQueryTextListener(object : android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.searchSkills(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchSkills(newText ?: "")
                return true
            }
        })

        // Category filter
        viewModel.categories.observe(this) { categories ->
            binding.categorySelector.removeAllViews()
            
            // Add "All" button
            val allButton = createCategoryButton("All", null)
            binding.categorySelector.addView(allButton)
            
            categories.forEach { category ->
                val button = createCategoryButton(category.name, category.name)
                binding.categorySelector.addView(button)
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.skills.observe(this) { skills ->
            adapter.submitList(skills)
            binding.emptyView.visibility = if (skills.isEmpty()) View.VISIBLE else View.GONE
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

        viewModel.installing.observe(this) { installingId ->
            adapter.setInstalling(installingId)
        }

        viewModel.refresh()
    }

    private fun createCategoryButton(text: String, category: String?): View {
        val button = android.widget.Button(this).apply {
            this.text = text
            setPadding(16, 8, 16, 8)
            setBackgroundResource(android.R.drawable.btn_default)
            setOnClickListener {
                viewModel.selectCategory(category)
                // Update visual state
                (it as android.widget.Button).isSelected = true
            }
        }
        
        // Set selected state based on current selection
        if (category == null && viewModel.selectedCategory.value == null) {
            button.isSelected = true
        } else if (category == viewModel.selectedCategory.value) {
            button.isSelected = true
        }
        
        return button
    }

    private fun showSkillDetails(skill: Skill) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(skill.name)
        
        val content = StringBuilder()
        content.append("Name: ${skill.name}\n\n")
        content.append("Description:\n${skill.description}\n\n")
        
        if (!skill.category.isNullOrBlank()) {
            content.append("Category: ${skill.category}\n")
        }
        if (!skill.version.isNullOrBlank()) {
            content.append("Version: ${skill.version}\n")
        }
        if (!skill.author.isNullOrBlank()) {
            content.append("Author: ${skill.author}\n")
        }
        
        if (!skill.tags.isNullOrEmpty()) {
            content.append("\nTags: ${skill.tags.joinToString(", ")}")
        }
        
        content.append("\n\nID: ${skill.id}")
        
        builder.setMessage(content.toString())
        
        val actions = mutableListOf<String>()
        if (skill.installed) {
            actions.add("Uninstall")
        } else {
            actions.add("Install")
        }
        actions.add("Close")
        
        builder.setNeutralButton("Close", null)
        actions.takeIf { it.size > 1 }?.let {
            builder.setItems(it.toTypedArray()) { _, which ->
                when (which) {
                    0 -> {
                        if (skill.installed) {
                            viewModel.uninstallSkill(skill.id)
                        } else {
                            viewModel.installSkill(skill.id)
                        }
                    }
                }
            }
        }
        
        builder.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.skills_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.refresh()
                true
            }
            R.id.action_installed_only -> {
                // Toggle filter
                val isChecked = item.isChecked
                item.isChecked = !isChecked
                // Installed-only filter
                Toast.makeText(this, "Filter functionality available", Toast.LENGTH_SHORT).show()
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
