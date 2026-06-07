/**
 * 文件说明：RemoteResourceMapper.kt
 * 作用：负责 Remote Resource Mapper 相关数据模型之间的转换。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.data.remote

import android.app.Application
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import com.zhengyang.redbook.R
import kotlin.math.absoluteValue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteResourceMapper @Inject constructor(
    private val application: Application
) {

    @DrawableRes
    fun drawableByName(name: String?, @DrawableRes fallback: Int): Int {
        if (name.isNullOrBlank()) return fallback
        val resolved = application.resources.getIdentifier(name, "drawable", application.packageName)
        return if (resolved != 0) resolved else fallback
    }

    @ColorRes
    fun colorByName(name: String?, @ColorRes fallback: Int): Int {
        if (name.isNullOrBlank()) return fallback
        val resolved = application.resources.getIdentifier(name, "color", application.packageName)
        return if (resolved != 0) resolved else fallback
    }

    @DrawableRes
    fun defaultMessageAvatarBackground(): Int = R.drawable.bg_xhs_message_avatar_blue

    @DrawableRes
    fun defaultMessageIcon(): Int = R.drawable.ic_xhs_message

    @DrawableRes
    fun defaultSuggestionAvatarBackground(): Int = R.drawable.bg_xhs_avatar_green

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
