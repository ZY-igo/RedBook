/**
 * 文件说明：PublishRepository.kt
 * 作用：定义发布场景的数据访问接口。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.data.remote.model.RemoteDraftDto
import com.zhengyang.redbook.data.remote.model.RemoteNoteDetailDto

/**
 * 发布页仓储接口
 *
 * 负责为发布流程提供创建笔记、保存草稿和读取草稿列表的数据入口。
 */
interface PublishRepository {
    /**
     * 创建文本笔记。
     *
     * @param title 笔记标题。
     * @param content 笔记正文内容。
     * @param mediaType 发布内容媒体类型。
     * @return 创建成功后的笔记详情 DTO。
     */
    suspend fun createTextNote(title: String, content: String, mediaType: String): RemoteNoteDetailDto

    /**
     * 保存草稿。
     *
     * @param id 草稿 ID；为空时由服务端创建新草稿。
     * @param title 草稿标题。
     * @param content 草稿正文内容。
     * @param mediaType 草稿媒体类型。
     * @param autoSaved 是否为自动保存。
     * @return 保存成功后的草稿 DTO。
     */
    suspend fun saveDraft(id: String? = null, title: String, content: String, mediaType: String, autoSaved: Boolean): RemoteDraftDto

    /**
     * 获取草稿列表。
     *
     * @return 当前用户的草稿列表。
     */
    suspend fun getDrafts(): List<RemoteDraftDto>
}
