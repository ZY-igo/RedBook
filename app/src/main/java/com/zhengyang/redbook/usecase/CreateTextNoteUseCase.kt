package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.remote.model.RemoteNoteDetailDto
import com.zhengyang.redbook.data.repository.PublishRepository
import javax.inject.Inject

/**
 * 创建文本笔记时使用的参数。
 *
 * @property title 笔记标题。
 * @property content 笔记正文。
 * @property mediaType 笔记媒体类型。
 */
data class CreateTextNoteParams(
    val title: String,
    val content: String,
    val mediaType: String
)

/**
 * 创建文本笔记的用例。
 *
 * @property publishRepository 发布模块仓库。
 */
class CreateTextNoteUseCase @Inject constructor(
    private val publishRepository: PublishRepository
) : ResourceUseCase<CreateTextNoteParams, RemoteNoteDetailDto>() {

    /**
     * 调用仓库创建文本笔记。
     *
     * @param input 创建笔记参数。
     * @return 后端返回的笔记详情。
     */
    override suspend fun execute(input: CreateTextNoteParams): RemoteNoteDetailDto {
        return publishRepository.createTextNote(
            title = input.title,
            content = input.content,
            mediaType = input.mediaType
        )
    }
}
