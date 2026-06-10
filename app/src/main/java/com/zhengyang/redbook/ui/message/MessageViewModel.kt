package com.zhengyang.redbook.ui.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhengyang.redbook.usecase.LoadMessageUiStateParams
import com.zhengyang.redbook.usecase.LoadMessageUiStateUseCase
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
            _uiState.value = loadMessageUiState(LoadMessageUiStateParams(forceRefresh))
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
