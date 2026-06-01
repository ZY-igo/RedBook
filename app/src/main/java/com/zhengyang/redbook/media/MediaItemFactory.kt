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
