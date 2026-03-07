package com.nothopeless.app.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nothopeless.app.data.model.ReportReason
import com.nothopeless.app.data.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportUiState(
    val postId: String = "",
    val selectedReason: ReportReason? = null,
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val hasError: Boolean = false,
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState = _uiState.asStateFlow()

    fun init(postId: String) {
        _uiState.update { it.copy(postId = postId) }
    }

    fun selectReason(reason: ReportReason) {
        _uiState.update { it.copy(selectedReason = reason) }
    }

    fun submit() {
        val s = _uiState.value
        if (s.selectedReason == null || s.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, hasError = false) }
            runCatching { reportRepository.report(s.postId, s.selectedReason.key) }
                .onSuccess { _uiState.update { it.copy(isSubmitting = false, isSuccess = true) } }
                .onFailure { _uiState.update { it.copy(isSubmitting = false, hasError = true) } }
        }
    }
}
