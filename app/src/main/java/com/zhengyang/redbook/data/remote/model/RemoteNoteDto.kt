/**
 * 文件说明： RemoteNoteDto.kt
 * 作用： 封装远程数据访问相关逻辑，包括接口配置、请求行为和响应解析。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.remote.model

data class RemoteNoteDto(
    val id: String?,
    val title: String?,
    val description: String?,
    val likeCount: Int?,
    val author: String?,
    val mediaType: String?,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val coverUrl: String? = null,
    val coverHeightDp: Int? = null
)
