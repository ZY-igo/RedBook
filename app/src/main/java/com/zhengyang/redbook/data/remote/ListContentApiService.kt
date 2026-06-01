package com.zhengyang.redbook.data.remote

import com.zhengyang.redbook.data.model.NoteItem
import okhttp3.Request

interface ListContentApiService {

    suspend fun getListContent(): List<NoteItem>
}