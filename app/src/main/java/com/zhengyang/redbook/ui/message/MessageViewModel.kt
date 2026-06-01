package com.zhengyang.redbook.ui.message

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MessageViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MessageUiState())
    val uiState: StateFlow<MessageUiState> = _uiState.asStateFlow()

    init {
        loadMessageData()
    }

    private fun loadMessageData() {
        _uiState.update {
            it.copy(
                messageRows = MessageMockData.messageRows(),
                peopleSuggestions = MessageMockData.peopleSuggestions()
            )
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
