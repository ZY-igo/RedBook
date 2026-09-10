package com.zhengyang.redbook.ui.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhengyang.redbook.usecase.LoadMessageUiStateParams
import com.zhengyang.redbook.usecase.LoadMessageUiStateUseCase
import com.zhengyang.redbook.utils.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val loadMessageUiState: LoadMessageUiStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageUiState())
    val uiState: StateFlow<MessageUiState> = _uiState.asStateFlow()

    init {
        loadMessageData(forceRefresh = false)
    }

    private fun loadMessageData(forceRefresh: Boolean) {
        viewModelScope.launch {
            // 后端不可用是常态（断网、人为停服、返回非 JSON 等），
            // 这里用 runCatching 兜住所有网络/解析异常，保证主线程不崩溃。
            // 失败时保留现有状态（首次加载即为默认空状态），仅记录日志，不向 UI 抛异常。
            runCatching { loadMessageUiState(LoadMessageUiStateParams(forceRefresh)) }
                .onSuccess { state -> _uiState.value = state }
                .onFailure { error ->
                    AppLogger.e(
                        tag = "MessageViewModel",
                        message = "loadMessageData failed, forceRefresh=$forceRefresh",
                        throwable = error
                    )
                }
        }
    }

    fun reload() {
        loadMessageData(forceRefresh = true)
    }

    fun dismissPerson(personId: String) {
        _uiState.update { state ->
            state.copy(
                peopleSuggestions = state.peopleSuggestions.filterNot { it.id == personId }
            )
        }
    }

    fun dismissPeopleSuggestions() {
        _uiState.update { state ->
            state.copy(peopleSuggestions = emptyList())
        }
    }
}
