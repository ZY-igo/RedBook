package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.local.ListDao
import com.zhengyang.redbook.data.model.NoteItem
import com.zhengyang.redbook.data.remote.HttpService

class HomeRepositoryImpl(
    private val httpService: HttpService,
    private val localDataService: ListDao
) : HomeRepository {

    override suspend fun getListContent(): List<NoteItem> {
        val localItems = localDataService.getAll()
        return if (localItems.isNotEmpty()) {
            localItems
        } else {
            httpService.getListContent()
        }
    }
}
