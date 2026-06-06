package com.zhengyang.redbook.data.model

data class FollowRelationEntity(
    val id: String,
    val followerId: String,
    val followingId: String,
    val followedAt: Long
)
