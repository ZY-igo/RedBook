/**
 * 文件说明： RemoteNoteMapper.kt
 * 作用： 封装远程数据访问相关逻辑，包括接口配置、请求行为和响应解析。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.remote.mapper

import com.zhengyang.redbook.data.model.NoteItem
import com.zhengyang.redbook.data.remote.model.RemoteNoteDto
import javax.inject.Inject

class RemoteNoteMapper @Inject constructor() {

    fun mapToDomain(items: List<RemoteNoteDto>): List<NoteItem> {
        return items.mapIndexed { index, item -> item.toDomain(index) }
    }

    private fun RemoteNoteDto.toDomain(index: Int): NoteItem {
        val resolvedId = id ?: "remote_note_$index"
        val resolvedMediaType = mediaType?.lowercase().orEmpty().ifBlank { DEFAULT_MEDIA_TYPE }
        val resolvedCoverUrl = coverUrl ?: imageUrl
        return NoteItem(
            id = resolvedId,
            title = title.orEmpty().ifBlank { DEFAULT_TITLE },
            description = description.orEmpty().ifBlank { DEFAULT_DESCRIPTION },
            likeCount = likeCount ?: 0,
            author = author.orEmpty().ifBlank { DEFAULT_AUTHOR },
            mediaType = resolvedMediaType,
            imageUrl = imageUrl,
            videoUrl = videoUrl,
            coverUrl = resolvedCoverUrl,
            coverHeightDp = coverHeightDp ?: DEFAULT_COVER_HEIGHT_DP
        )
    }

    companion object {
        private const val DEFAULT_TITLE = "待补充标题"
        private const val DEFAULT_DESCRIPTION = "内容建设中"
        private const val DEFAULT_AUTHOR = "官方账号"
        private const val DEFAULT_MEDIA_TYPE = "image"
        private const val DEFAULT_COVER_HEIGHT_DP = 220
    }
}
