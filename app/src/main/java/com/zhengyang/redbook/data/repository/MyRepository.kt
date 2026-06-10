/**
 * 文件说明：MyRepository.kt
 * 作用：定义"我的"模块的数据仓库接口，抽象个人数据来源。
 * 备注：采用接口+实现分离模式，便于单元测试和模块解耦。
 */
package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.ui.my.InterestPersonItem
import com.zhengyang.redbook.ui.my.MyProfileHeader
import com.zhengyang.redbook.ui.my.MyProfileStats

/**
 * "我的"页面数据仓库接口。
 *
 * 定义"我的"模块对外部的数据需求抽象，包括：
 * - 个人资料信息（头像、昵称、简介等）
 * - 个人数据统计（关注数、粉丝数、获赞数）
 * - 推荐关注的人列表
 * - 头像上传功能
 *
 * 设计原则：
 * - 所有方法都是 suspend 函数，必须在协程作用域内调用。
 * - forceRefresh 参数用于控制是否绕过缓存强制刷新。
 * - 头像上传返回更新后的个人资料，便于调用方直接刷新 UI。
 */
interface MyRepository {

    /**
     * 获取个人资料头部信息。
     *
     * 用于"我的"页面顶部的个人资料展示。
     *
     * @param forceRefresh 是否强制从网络刷新。false 时优先返回本地缓存。
     * @return 个人资料头部数据。
     */
    suspend fun getProfile(forceRefresh: Boolean = false): MyProfileHeader

    /**
     * 获取个人数据统计。
     *
     * 用于"我的"页面展示关注数、粉丝数、获赞数等统计数据。
     *
     * @param forceRefresh 是否强制从网络刷新。
     * @return 个人统计数据。
     */
    suspend fun getProfileStats(forceRefresh: Boolean = false): MyProfileStats

    /**
     * 获取推荐关注的人列表。
     *
     * 用于"我的"页面底部的"你可能感兴趣的人"区域。
     *
     * @param forceRefresh 是否强制从网络刷新。
     * @return 推荐关注的人列表。
     */
    suspend fun getInterestPeople(forceRefresh: Boolean = false): List<InterestPersonItem>

    /**
     * 上传新头像。
     *
     * @param fileName 上传文件的文件名。
     * @param contentType 上传文件的 MIME 类型。
     * @param bytes 上传文件的二进制内容。
     * @return 更新后的个人资料头部信息。
     */
    suspend fun uploadAvatar(fileName: String, contentType: String, bytes: ByteArray): MyProfileHeader
}