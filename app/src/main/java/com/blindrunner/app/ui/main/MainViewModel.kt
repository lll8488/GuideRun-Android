package com.blindrunner.app.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blindrunner.app.data.repository.RunningRepository
import com.blindrunner.app.domain.model.RunningRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: RunningRepository
) : ViewModel() {

    private val _records = MutableStateFlow<List<RunningRecord>>(emptyList())
    val records: StateFlow<List<RunningRecord>> = _records.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadRecords()
    }

    fun loadRecords() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = repository.getRecords()
                _records.value = data
                Log.d("MainViewModel", "Loaded ${data.size} records")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to load records", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = repository.getRecords(forceRefresh = true)
                _records.value = data
                Log.d("MainViewModel", "Refreshed ${data.size} records")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to refresh", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
