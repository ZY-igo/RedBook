/**
 * 文件说明：FollowRelationEntity.kt
 * 作用：定义 Follow Relation Entity 相关本地数据实体结构。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.data.model

/**
 * 关注关系实体
 *
 * 用于描述一个用户对另一个用户发起关注的关系，
 * 便于本地缓存、关系同步或统计逻辑复用。
 */
data class FollowRelationEntity(
    /** 关系记录唯一标识。 */
    val id: String,

    /** 发起关注的用户 ID。 */
    val followerId: String,

    /** 被关注的目标用户 ID。 */
    val followingId: String,

    /** 建立关注关系的时间戳，单位为毫秒。 */
    val followedAt: Long
)
