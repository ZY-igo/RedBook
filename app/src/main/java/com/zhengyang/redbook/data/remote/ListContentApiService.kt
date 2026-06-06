package com.zhengyang.redbook.data.remote

import com.zhengyang.redbook.data.remote.model.RemoteNoteDto

interface ListContentApiService {

    suspend fun getListContent(): List<RemoteNoteDto>
}
