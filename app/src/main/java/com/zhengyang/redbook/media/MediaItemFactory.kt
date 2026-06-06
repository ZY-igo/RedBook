/**
 * 文件说明： MediaItemFactory.kt
 * 作用： 定义当前源码文件的核心实现，承担对应功能模块中的结构声明或行为编排职责。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.media

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes

object MediaItemFactory {

    fun fromUri(uri: Uri, mimeType: String? = null): MediaItem {
        return MediaItem.Builder()
            .setUri(uri)
            .applyMimeType(mimeType)
            .build()
    }

    fun fromUrl(url: String, mimeType: String? = null): MediaItem {
        return MediaItem.Builder()
            .setUri(url)
            .applyMimeType(mimeType)
            .build()
    }

    fun guessVideo(url: String): MediaItem = fromUrl(url, MimeTypes.VIDEO_MP4)

    fun guessAudio(url: String): MediaItem = fromUrl(url, MimeTypes.AUDIO_MP4)

    private fun MediaItem.Builder.applyMimeType(mimeType: String?): MediaItem.Builder {
        return if (mimeType.isNullOrBlank()) this else setMimeType(mimeType)
    }
}
