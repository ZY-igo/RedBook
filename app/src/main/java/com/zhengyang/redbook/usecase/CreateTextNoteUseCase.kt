package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.data.remote.model.RemoteNoteDetailDto
import com.zhengyang.redbook.data.repository.PublishRepository
import javax.inject.Inject

data class CreateTextNoteParams(
    val title: String,
    val content: String,
    val mediaType: String
)

class CreateTextNoteUseCase @Inject constructor(
    private val publishRepository: PublishRepository
) : ResourceUseCase<CreateTextNoteParams, RemoteNoteDetailDto>() {

    override suspend fun execute(input: CreateTextNoteParams): RemoteNoteDetailDto {
        return publishRepository.createTextNote(
            title = input.title,
            content = input.content,
            mediaType = input.mediaType
        )
    }
}
