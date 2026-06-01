package com.zhengyang.redbook.ui.home

import com.zhengyang.redbook.data.model.NoteItem

data class HomeUiState (
    val listContent: List<NoteItem> = listOf()
)