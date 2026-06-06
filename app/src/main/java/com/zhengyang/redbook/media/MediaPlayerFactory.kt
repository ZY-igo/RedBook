/**
 * 文件说明： MediaPlayerFactory.kt
 * 作用： 定义当前源码文件的核心实现，承担对应功能模块中的结构声明或行为编排职责。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.media

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import okhttp3.OkHttpClient
import java.io.File

object MediaPlayerFactory {

    private const val CACHE_DIR_NAME = "video_cache"
    private const val CACHE_SIZE_BYTES = 128L * 1024L * 1024L

    private val playbackAudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .build()

    @Volatile
    private var mediaCache: SimpleCache? = null

    fun create(context: Context, okHttpClient: OkHttpClient = OkHttpClient()): ExoPlayer {
        val appContext = context.applicationContext
        val upstreamFactory = OkHttpDataSource.Factory(okHttpClient)
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(getCache(appContext))
            .setUpstreamDataSourceFactory(
                DefaultDataSource.Factory(appContext, upstreamFactory)
            )
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        return ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .build()
            .apply {
                setAudioAttributes(playbackAudioAttributes, true)
                setHandleAudioBecomingNoisy(true)
            }
    }

    fun prepare(player: ExoPlayer, mediaItem: MediaItem, playWhenReady: Boolean = true) {
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = playWhenReady
    }

    private fun getCache(context: Context): SimpleCache {
        mediaCache?.let { return it }
        return synchronized(this) {
            mediaCache ?: SimpleCache(
                File(context.cacheDir, CACHE_DIR_NAME),
                LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES),
                StandaloneDatabaseProvider(context)
            ).also { mediaCache = it }
        }
    }
}
