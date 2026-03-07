package com.nothopeless.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nothopeless.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(val currentPage: Int = 0, val isCompleted: Boolean = false)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    fun nextPage() {
        _uiState.update { state ->
            if (state.currentPage < 2) {
                state.copy(currentPage = state.currentPage + 1)
            } else {
                complete()
                state
            }
        }
    }

    private fun complete() {
        viewModelScope.launch {
            settingsRepository.completeOnboarding()
            _uiState.update { it.copy(isCompleted = true) }
        }
    }
}
