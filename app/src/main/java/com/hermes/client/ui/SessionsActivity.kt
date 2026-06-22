package com.hermes.client.ui

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.hermes.client.R
import com.hermes.client.adapter.SessionAdapter
import com.hermes.client.databinding.ActivitySessionsBinding
import com.hermes.client.viewmodel.SessionsViewModel
import android.widget.SearchView

class SessionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySessionsBinding
    private lateinit var viewModel: SessionsViewModel
    private lateinit var adapter: SessionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySessionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[SessionsViewModel::class.java]

        supportActionBar?.title = getString(R.string.sessions)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = SessionAdapter { session ->
            // Load session logic
            finish()
        }

        binding.recyclerSessions.apply {
            layoutManager = LinearLayoutManager(this@SessionsActivity)
            adapter = adapter
        }

        // Search functionality
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchSessions(it) }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchSessions(newText ?: "")
                return true
            }
        })

        binding.fabNewSession.setOnClickListener {
            viewModel.createNewSession()
            finish()
        }

        viewModel.sessions.observe(this) { sessions ->
            adapter.submitList(sessions)
            binding.emptyView.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.loading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.refresh()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
