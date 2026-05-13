package com.blindrunner.app

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.blindrunner.app.data.remote.RetrofitClient
import com.blindrunner.app.ui.main.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by lazy {
        val app = application as BlindRunnerApp
        ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(app.repository) as T
                }
            }
        )[MainViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Quick verification: log API response to prove Retrofit works
        lifecycleScope.launch {
            try {
                val posts = RetrofitClient.apiService.getPosts()
                Log.d("RetrofitVerify", "API returned ${posts.size} posts. First: ${posts.first().title}")
            } catch (e: Exception) {
                Log.e("RetrofitVerify", "API call failed", e)
            }
        }

        // Observe repository-driven data from ViewModel
        lifecycleScope.launch {
            viewModel.records.collect { records ->
                Log.d("MainActivity", "Records from ViewModel: ${records.size}")
            }
        }
    }
}
