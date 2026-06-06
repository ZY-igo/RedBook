/**
 * 文件说明： NoteMapper.kt
 * 作用： 负责不同层级模型之间的转换，保持映射逻辑集中且可复用。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.mapper

import com.zhengyang.redbook.data.model.MediaType
import com.zhengyang.redbook.data.model.NoteItem
import com.zhengyang.redbook.ui.note.MediaUiModel
import com.zhengyang.redbook.ui.note.NoteDetailUiModel
import com.zhengyang.redbook.ui.note.UserUiModel
import javax.inject.Inject

class NoteMapper @Inject constructor() {

    fun mapEntityToUiModel(entity: NoteItem): NoteDetailUiModel {
        return NoteDetailUiModel(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            author = UserUiModel(
                id = entity.author,
                name = entity.author,
                avatarText = entity.author.take(1),
                avatarColorHex = "#DDDDDD"
            ),
            media = MediaUiModel(
                type = entity.mediaType.toMediaType(),
                imageUrls = listOfNotNull(entity.imageUrl ?: entity.coverUrl),
                videoUrl = entity.videoUrl,
                coverUrl = entity.coverUrl
            ),
            likeCount = entity.likeCount.toString(),
            commentCount = "0",
            collectCount = "0",
            isLiked = false,
            isCollected = false,
            isFollowingAuthor = false,
            createdAt = "",
            tags = emptyList()
        )
    }

    private fun String.toMediaType(): MediaType {
        return when (uppercase()) {
            MediaType.VIDEO.name -> MediaType.VIDEO
            MediaType.TEXT.name -> MediaType.TEXT
            MediaType.LONG_FORM.name -> MediaType.LONG_FORM
            else -> MediaType.IMAGE
        }
    }
}
