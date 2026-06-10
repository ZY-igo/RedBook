/**
 * 文件说明：MessageRepository.kt
 * 作用：定义消息模块的数据仓库接口，抽象消息数据的来源。
 * 备注：采用接口+实现分离模式，便于单元测试和模块解耦。
 */
package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.ui.message.MessageRowItem
import com.zhengyang.redbook.ui.message.PersonSuggestionItem

/**
 * 消息模块数据仓库接口。
 *
 * 定义消息模块对外部的数据需求抽象，包括：
 * - 消息列表数据（赞、评论、关注等不同类型的消息）
 * - 推荐用户列表（用于消息页的推荐关注区域）
 *
 * 设计原则：
 * - 所有方法都是 suspend 函数，必须在协程作用域内调用。
 * - forceRefresh 参数用于控制是否绕过缓存强制刷新。
 */
interface MessageRepository {

    /**
     * 获取消息列表。
     *
     * 用于消息页面展示各类消息通知，如：
     * - 赞和收藏
     * - 评论和@
     * - 新增关注
     * - 系统通知等
     *
     * @param forceRefresh 是否强制从网络刷新。false 时优先返回本地缓存。
     * @return 消息列表。
     */
    suspend fun getMessageRows(forceRefresh: Boolean = false): List<MessageRowItem>

    /**
     * 获取推荐用户列表。
     *
     * 用于消息页面底部的推荐关注区域，
     * 帮助用户发现更多感兴趣的其他用户。
     *
     * @param forceRefresh 是否强制从网络刷新。
     * @return 推荐用户列表。
     */
    suspend fun getPeopleSuggestions(forceRefresh: Boolean = false): List<PersonSuggestionItem>
}