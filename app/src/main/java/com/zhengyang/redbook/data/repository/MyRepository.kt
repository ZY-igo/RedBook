/**
 * 文件说明：MyRepository.kt
 * 作用：定义个人页场景的数据访问接口。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.ui.my.InterestPersonItem
import com.zhengyang.redbook.ui.my.MyProfileHeader
import com.zhengyang.redbook.ui.my.MyProfileStats

/**
 * 个人页仓储接口
 *
 * 负责为个人页提供资料头部、统计信息和感兴趣的人等数据入口，
 * 屏蔽底层接口结构与缓存细节。
 */
interface MyRepository {
    /**
     * 获取个人页头部资料。
     *
     * @return 个人页头部展示模型。
     */
    suspend fun getProfile(): MyProfileHeader

    /**
     * 获取个人页统计信息。
     *
     * @return 个人页统计展示模型。
     */
    suspend fun getProfileStats(): MyProfileStats

    /**
     * 获取感兴趣的人列表。
     *
     * @return 感兴趣的人展示模型列表。
     */
    suspend fun getInterestPeople(): List<InterestPersonItem>

    suspend fun uploadAvatar(fileName: String, contentType: String, bytes: ByteArray): MyProfileHeader
}
