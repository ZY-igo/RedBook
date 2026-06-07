package com.zhengyang.redbook.ui.home

sealed interface HomeUiEvent {
    data class ShowMessage(val message: String) : HomeUiEvent
}
