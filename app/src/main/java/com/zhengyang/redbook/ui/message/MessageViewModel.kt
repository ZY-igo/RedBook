/**
 * 文件说明： MessageViewModel.kt
 * 作用： 承载消息页相关的界面状态与交互逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
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
