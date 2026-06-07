/**
 * 文件说明：MessageRepository.kt
 * 作用：定义消息场景的数据访问接口。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.ui.message.MessageRowItem
import com.zhengyang.redbook.ui.message.PersonSuggestionItem

/**
 * 消息页仓储接口
 *
 * 负责为消息页面提供消息入口数据和可能认识的人推荐数据，
 * 屏蔽远端接口结构与缓存细节。
 */
interface MessageRepository {
    /**
     * 获取消息页入口列表。
     *
     * @return 消息页入口项列表。
     */
    suspend fun getMessageRows(): List<MessageRowItem>

    /**
     * 获取可能认识的人推荐列表。
     *
     * @return 推荐用户列表。
     */
    suspend fun getPeopleSuggestions(): List<PersonSuggestionItem>
}
