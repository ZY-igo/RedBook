/**
 * 文件说明：PublishRepositoryImpl.kt
 * 作用：实现发布场景仓储接口，负责创建笔记与草稿相关远端请求。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.remote.RedBookApiService
import com.zhengyang.redbook.data.remote.model.RemoteCreateNoteRequestDto
import com.zhengyang.redbook.data.remote.model.RemoteDraftDto
import com.zhengyang.redbook.data.remote.model.RemoteNoteDetailDto
import com.zhengyang.redbook.data.remote.model.RemoteSaveDraftRequestDto
import com.zhengyang.redbook.data.remote.requireData
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 发布页仓储实现类
 *
 * 负责将发布流程中的用户输入转换为远端请求 DTO，
 * 并处理创建笔记、保存草稿和拉取草稿列表等能力。
 */
class PublishRepositoryImpl @Inject constructor(
    /** 发布相关远端接口服务。 */
    private val apiService: RedBookApiService
) : PublishRepository {

    /**
     * 创建文本笔记。
     *
     * @param title 笔记标题。
     * @param content 笔记正文内容。
     * @param mediaType 发布内容媒体类型。
     * @return 创建成功后的笔记详情 DTO。
     */
    override suspend fun createTextNote(title: String, content: String, mediaType: String): RemoteNoteDetailDto = withContext(Dispatchers.IO) {
        apiService.createNote(
            RemoteCreateNoteRequestDto(
                title = title,
                description = content,
                mediaType = mediaType
            )
        ).requireData()
    }

    /**
     * 保存草稿。
     *
     * @param id 草稿 ID。
     * @param title 草稿标题。
     * @param content 草稿正文内容。
     * @param mediaType 草稿媒体类型。
     * @param autoSaved 是否为自动保存。
     * @return 保存成功后的草稿 DTO。
     */
    override suspend fun saveDraft(id: String?, title: String, content: String, mediaType: String, autoSaved: Boolean): RemoteDraftDto = withContext(Dispatchers.IO) {
        apiService.saveDraft(
            RemoteSaveDraftRequestDto(
                id = id,
                title = title,
                description = content,
                mediaType = mediaType,
                autoSaved = autoSaved
            )
        ).requireData()
    }

    /**
     * 获取草稿列表。
     *
     * @return 当前用户的草稿 DTO 列表。
     */
    override suspend fun getDrafts(): List<RemoteDraftDto> = withContext(Dispatchers.IO) {
        apiService.getDrafts().requireData()
    }
}
