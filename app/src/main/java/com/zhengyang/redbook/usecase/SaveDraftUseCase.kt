package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.remote.model.RemoteDraftDto
import com.zhengyang.redbook.data.repository.PublishRepository
import javax.inject.Inject

data class SaveDraftParams(
    val id: String? = null,
    val title: String,
    val content: String,
    val mediaType: String,
    val autoSaved: Boolean
)

class SaveDraftUseCase @Inject constructor(
    private val publishRepository: PublishRepository
) : ResourceUseCase<SaveDraftParams, RemoteDraftDto>() {

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
