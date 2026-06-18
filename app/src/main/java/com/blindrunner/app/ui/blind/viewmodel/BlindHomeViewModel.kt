package com.blindrunner.app.ui.blind.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blindrunner.app.BlindRunnerApp
import com.blindrunner.app.domain.model.RunningRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BlindHomeViewModel : ViewModel() {

    private val _demands = MutableStateFlow<List<RunningRecord>>(emptyList())
    val demands: StateFlow<List<RunningRecord>> = _demands.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadDemands(repository: com.blindrunner.app.data.repository.RunningRepository) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val all = repository.getRecords()
                _demands.value = all.filter { it.status != "completed" && it.status != "archived" }
            } catch (e: Exception) {
                _error.value = "加载失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
