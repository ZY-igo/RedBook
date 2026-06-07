/**
 * 文件说明：MessageViewModel.kt
 * 作用：负责 Message View Model 相关界面状态组织、数据加载与事件响应。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.ui.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhengyang.redbook.usecase.LoadMessageUiStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val loadMessageUiState: LoadMessageUiStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageUiState())
    val uiState: StateFlow<MessageUiState> = _uiState.asStateFlow()

    init {
        loadMessageData()
    }

    private fun loadMessageData() {
        viewModelScope.launch {
            _uiState.value = loadMessageUiState()
        }
    }

    fun reload() {
        loadMessageData()
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
