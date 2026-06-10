package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.remote.model.RemoteDraftDto
import com.zhengyang.redbook.data.repository.PublishRepository
import javax.inject.Inject

/**
 * 保存草稿时使用的参数。
 *
 * @property id 草稿 id；为空时表示新建草稿。
 * @property title 草稿标题。
 * @property content 草稿正文。
 * @property mediaType 草稿媒体类型。
 * @property autoSaved 当前是否属于自动保存。
 */
data class SaveDraftParams(
    val id: String? = null,
    val title: String,
    val content: String,
    val mediaType: String,
    val autoSaved: Boolean
)

/**
 * 保存草稿的用例。
 *
 * @property publishRepository 发布模块仓库。
 */
class SaveDraftUseCase @Inject constructor(
    private val publishRepository: PublishRepository
) : ResourceUseCase<SaveDraftParams, RemoteDraftDto>() {

    /**
     * 调用仓库保存草稿。
     *
     * @param input 草稿保存参数。
     * @return 后端返回的草稿对象。
     */
    override suspend fun execute(input: SaveDraftParams): RemoteDraftDto {
        return publishRepository.saveDraft(
            id = input.id,
            title = input.title,
            content = input.content,
            mediaType = input.mediaType,
            autoSaved = input.autoSaved
        )
    }
}
