/**
 * 文件说明：FollowingUserEntity.kt
 * 作用：定义 Following User Entity 相关本地数据实体结构。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.data.model

/**
 * 关注推荐用户实体
 *
 * 用于保存推荐关注用户的基础展示信息，
 * 以便首页关注流或本地缓存场景直接读取并渲染。
 */
data class FollowingUserEntity(
    /** 推荐用户唯一标识。 */
    val id: String,

    /** 推荐用户昵称。 */
    val name: String,

    /** 推荐原因或补充说明文案。 */
    val subtitle: String,

    /** 头像背景色值，使用十六进制字符串表示。 */
    val avatarColorHex: String,

    /** 用户角标文案，例如“新作者”或“已认证”。 */
    val badge: String?,

    /** 在推荐列表中的排序值，值越小越靠前。 */
    val sortOrder: Int
)
