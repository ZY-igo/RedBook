package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.model.NoteItem

interface HomeRepository {
    suspend fun getListContent(): List<NoteItem>
}
