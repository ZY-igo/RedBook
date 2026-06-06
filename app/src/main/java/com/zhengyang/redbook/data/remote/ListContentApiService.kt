/**
 * 文件说明： ListContentApiService.kt
 * 作用： 封装远程数据访问相关逻辑，包括接口配置、请求行为和响应解析。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.remote

import com.zhengyang.redbook.data.remote.model.RemoteNoteDto

interface ListContentApiService {

    suspend fun getListContent(): List<RemoteNoteDto>
}
