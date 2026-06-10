/**
 * 文件说明：RemoteResourceMapper.kt
 * 作用：处理服务端资源标识与本地 Android 资源之间的映射。
 * 备注：后端通常只返回资源名或颜色键，这里负责把它们解析成可直接使用的资源 ID。
 */
package com.zhengyang.redbook.data.remote

import android.app.Application
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.zhengyang.redbook.R
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

/**
 * 远端资源映射器。
 *
 * 职责是把服务端下发的“轻量资源描述”转成本地真实资源。
 * 常见场景包括：
 * 1. 服务端返回 drawable 名称，前端查找本地图片资源。
 * 2. 服务端返回 color 名称，前端查找本地颜色资源。
 * 3. 服务端只提供头像颜色线索，前端映射到固定视觉风格的背景资源。
 */
@Singleton
class RemoteResourceMapper @Inject constructor(
    private val application: Application
) {

    /**
     * 根据 drawable 名称解析本地图片资源 ID。
     *
     * 若名称为空或资源不存在，则回退到调用方指定的默认资源，
     * 避免因为后端配置缺失导致页面直接崩溃或出现空白。
     */
    @DrawableRes
    fun drawableByName(name: String?, @DrawableRes fallback: Int): Int {
        if (name.isNullOrBlank()) return fallback
        val resolved = application.resources.getIdentifier(name, "drawable", application.packageName)
        return if (resolved != 0) resolved else fallback
    }

    /**
     * 根据颜色资源名称解析本地颜色资源 ID。
     *
     * 这里返回的是 `@ColorRes`，让调用方决定何时解析成真正颜色值，
     * 以便继续复用 Android 资源体系。
     */
    @ColorRes
    fun colorByName(name: String?, @ColorRes fallback: Int): Int {
        if (name.isNullOrBlank()) return fallback
        val resolved = application.resources.getIdentifier(name, "color", application.packageName)
        return if (resolved != 0) resolved else fallback
    }

    /** 消息页头像背景的默认兜底资源。 */
    @DrawableRes
    fun defaultMessageAvatarBackground(): Int = R.drawable.bg_xhs_message_avatar_blue

    /** 消息页图标的默认兜底资源。 */
    @DrawableRes
    fun defaultMessageIcon(): Int = R.drawable.ic_xhs_message

    /** 推荐用户头像背景的默认兜底资源。 */
    @DrawableRes
    fun defaultSuggestionAvatarBackground(): Int = R.drawable.bg_xhs_avatar_green

    /**
     * 根据颜色十六进制字符串稳定映射头像背景资源。
     *
     * 这里不是直接把服务端颜色值绘制到 UI 上，而是映射到一组受控的背景资源，
     * 目的是保持视觉风格统一，并避免异常颜色值破坏界面观感。
     */
    @DrawableRes
    fun avatarBackgroundForColorHex(colorHex: String?): Int {
        val palette = listOf(
            R.drawable.bg_xhs_avatar_pink,
            R.drawable.bg_xhs_avatar_blue_light,
            R.drawable.bg_xhs_avatar_orange,
            R.drawable.bg_xhs_avatar_green,
            R.drawable.bg_xhs_avatar_teal,
            R.drawable.bg_xhs_avatar_purple
        )
        val index = (colorHex?.hashCode() ?: 0).absoluteValue % palette.size
        return palette[index]
    }
}
