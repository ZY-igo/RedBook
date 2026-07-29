package com.zhengyang.redbook.media

import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import com.zhengyang.redbook.utils.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import okhttp3.OkHttpClient

@Singleton
class VideoPlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {

    val player: ExoPlayer
        get() = playerInstance ?: createPlayer().also { playerInstance = it }

    val backgroundPlaybackState: BackgroundPlaybackState?
        get() = currentBackgroundPlaybackState

    fun enterBackgroundPlayback(state: BackgroundPlaybackState) {
        currentBackgroundPlaybackState = state
        AppLogger.d(TAG, "enterBackgroundPlayback title=${state.title}, author=${state.author}")
    }

    fun exitBackgroundPlayback() {
        currentBackgroundPlaybackState = null
        AppLogger.d(TAG, "exitBackgroundPlayback")
    }

    fun isBackgroundPlaybackActive(): Boolean = currentBackgroundPlaybackState != null

    fun releasePlayer() {
        playerInstance?.release()
        playerInstance = null
        currentBackgroundPlaybackState = null
        AppLogger.d(TAG, "releasePlayer")
    }

    private fun createPlayer(): ExoPlayer {
        AppLogger.d(TAG, "create shared ExoPlayer")
        return MediaPlayerFactory.create(context, okHttpClient)
    }

    data class BackgroundPlaybackState(
        val title: String,
        val author: String,
        val artworkUrl: String?,
        val resumeIntent: Intent
    )

    private var playerInstance: ExoPlayer? = null
    private var currentBackgroundPlaybackState: BackgroundPlaybackState? = null

    private companion object {
        private const val TAG = "VideoPlaybackManager"
    }
}
