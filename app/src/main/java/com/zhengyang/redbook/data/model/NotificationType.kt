/**
 * 文件说明：NotificationType.kt
 * 作用：定义当前文件在项目中的核心实现与职责。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.data.model

/**
 * 通知类型
 *
 * 用于区分不同来源和语义的通知，
 * 便于界面展示、筛选和后续行为分发。
 */
enum class NotificationType {
    /** 笔记被点赞。 */
    LIKE,

    /** 收到新的评论。 */
    COMMENT,

    /** 收到新的回复。 */
    REPLY,

    /** 收到新的关注。 */
    FOLLOW,

    /** 被提及。 */
    MENTION,

    /** 系统通知。 */
    SYSTEM
}
