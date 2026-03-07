package com.nothopeless.app.ui.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.FirebaseFunctionsException
import com.nothopeless.app.data.model.EffectType
import com.nothopeless.app.data.model.KindnessType
import com.nothopeless.app.data.model.SceneType
import com.nothopeless.app.data.model.UserStateType
import com.nothopeless.app.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SubmitError { PERSONAL_INFO, SPECIFIC_NOUN, COOLDOWN, NETWORK, UNKNOWN }

data class PostUiState(
    val scene: SceneType? = null,
    val kindnessType: KindnessType? = null,
    val userState: UserStateType? = null,
    val effect: EffectType? = null,
    val body: String = "",
    val hasProperNounWarning: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitError: SubmitError? = null,
    val isSuccess: Boolean = false,
)

@HiltViewModel
class PostViewModel @Inject constructor(
    private val postRepository: PostRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PostUiState())
    val uiState = _uiState.asStateFlow()

    private var debounceJob: Job? = null

    companion object {
        private val PROPER_NOUN_SUFFIXES = listOf(
            "駅", "店", "会社", "学校", "病院", "公園", "さん", "くん", "ちゃん", "様"
        )
        private val kanjiKanaPattern = Regex(
            "[\u3040-\u309F\u30A0-\u30FF\u4E00-\u9FFF\u3400-\u4DBF]+(${
                PROPER_NOUN_SUFFIXES.joinToString("|")
            })"
        )
    }

    fun onSceneSelected(scene: SceneType) = _uiState.update { it.copy(scene = scene) }
    fun onKindnessTypeSelected(kt: KindnessType) = _uiState.update { it.copy(kindnessType = kt) }
    fun onUserStateSelected(us: UserStateType?) = _uiState.update { it.copy(userState = us) }
    fun onEffectSelected(ef: EffectType) = _uiState.update { it.copy(effect = ef) }

    fun onBodyChanged(text: String) {
        _uiState.update { it.copy(body = text, hasProperNounWarning = false, submitError = null) }
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(1_000)
            val warning = kanjiKanaPattern.containsMatchIn(text)
            _uiState.update { it.copy(hasProperNounWarning = warning) }
        }
    }

    val isSubmitEnabled: Boolean
        get() = _uiState.value.let {
            it.scene != null && it.kindnessType != null && it.effect != null
                    && it.body.isNotBlank() && it.body.length <= 140 && !it.isSubmitting
        }

    fun submit() {
        val s = _uiState.value
        if (!isSubmitEnabled) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, submitError = null) }
            runCatching {
                postRepository.createPost(
                    scene = s.scene!!.key,
                    kindnessType = s.kindnessType!!.key,
                    userState = s.userState?.key,
                    effect = s.effect!!.key,
                    body = s.body.trim(),
                )
            }.onSuccess {
                _uiState.update { it.copy(isSubmitting = false, isSuccess = true) }
            }.onFailure { e ->
                val errorCode = extractErrorCode(e)
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        submitError = when (errorCode) {
                            "PERSONAL_INFO" -> SubmitError.PERSONAL_INFO
                            "SPECIFIC_NOUN" -> SubmitError.SPECIFIC_NOUN
                            "COOLDOWN" -> SubmitError.COOLDOWN
                            else -> if (e.message?.contains("network", ignoreCase = true) == true)
                                SubmitError.NETWORK else SubmitError.UNKNOWN
                        }
                    )
                }
            }
        }
    }

    fun clearSuccess() = _uiState.update { it.copy(isSuccess = false) }

    private fun extractErrorCode(e: Throwable): String {
        return if (e is FirebaseFunctionsException) {
            e.details?.toString() ?: e.code.name
        } else {
            "NETWORK"
        }
    }
}
