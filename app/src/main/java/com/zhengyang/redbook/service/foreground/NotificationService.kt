package com.zhengyang.redbook.service.foreground

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.ui.PlayerNotificationManager
import com.zhengyang.redbook.R
import com.zhengyang.redbook.media.VideoPlaybackManager
import com.zhengyang.redbook.utils.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NotificationService : MediaSessionService() {

    @Inject
    lateinit var playbackManager: VideoPlaybackManager

    private var mediaSession: MediaSession? = null
    private var playerNotificationManager: PlayerNotificationManager? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            refreshForegroundState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            refreshForegroundState()
        }

        override fun onPlayerError(error: PlaybackException) {
            AppLogger.w(TAG, "Playback notification error.", error)
            refreshForegroundState()
        }
    }

    override fun onCreate() {
        super.onCreate()
        val player = playbackManager.player
        player.addListener(playerListener)
        mediaSession = MediaSession.Builder(this, player).build()
        playerNotificationManager = buildNotificationManager()
        refreshForegroundState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                playbackManager.player.pause()
                playbackManager.exitBackgroundPlayback()
                refreshForegroundState()
                return START_NOT_STICKY
            }

            ACTION_DISMISS -> {
                playbackManager.exitBackgroundPlayback()
                refreshForegroundState()
                return START_NOT_STICKY
            }

            ACTION_START,
            ACTION_REFRESH -> {
                refreshForegroundState()
                return START_STICKY
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (!playbackManager.player.isPlaying) {
            playbackManager.exitBackgroundPlayback()
            refreshForegroundState()
        }
    }

    override fun onDestroy() {
        playerNotificationManager?.setPlayer(null)
        playerNotificationManager = null
        playbackManager.player.removeListener(playerListener)
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun refreshForegroundState() {
        val player = playbackManager.player
        val backgroundState = playbackManager.backgroundPlaybackState
        val notificationManager = playerNotificationManager ?: return

        if (backgroundState == null) {
            notificationManager.setPlayer(null)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        mediaSession?.setSessionActivity(buildContentIntent(backgroundState))
        notificationManager.setPlayer(player)
    }

    private fun buildNotificationManager(): PlayerNotificationManager {
        return PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            CHANNEL_ID
        )
            .setChannelNameResourceId(R.string.notification_playback_channel_name)
            .setChannelDescriptionResourceId(R.string.notification_playback_channel_description)
            .setSmallIconResourceId(R.mipmap.ic_launcher)
            .setMediaDescriptionAdapter(
                object : PlayerNotificationManager.MediaDescriptionAdapter {
                    override fun createCurrentContentIntent(player: Player): PendingIntent? {
                        val state = playbackManager.backgroundPlaybackState ?: return null
                        return buildContentIntent(state)
                    }

                    override fun getCurrentContentTitle(player: Player): CharSequence {
                        val state = playbackManager.backgroundPlaybackState
                        return state?.title?.takeIf(String::isNotBlank)
                            ?: getString(R.string.notification_playback_title_fallback)
                    }

                    override fun getCurrentContentText(player: Player): CharSequence {
                        val state = playbackManager.backgroundPlaybackState
                        return state?.author?.takeIf(String::isNotBlank)
                            ?: getString(R.string.notification_playback_content)
                    }

                    override fun getCurrentLargeIcon(
                        player: Player,
                        callback: PlayerNotificationManager.BitmapCallback
                    ) = null
                }
            )
            .setNotificationListener(
                object : PlayerNotificationManager.NotificationListener {
                    override fun onNotificationPosted(
                        notificationId: Int,
                        notification: Notification,
                        ongoing: Boolean
                    ) {
                        if (ongoing) {
                            startForeground(notificationId, notification)
                        } else {
                            stopForeground(STOP_FOREGROUND_DETACH)
                        }
                    }

                    override fun onNotificationCancelled(
                        notificationId: Int,
                        dismissedByUser: Boolean
                    ) {
                        if (dismissedByUser) {
                            playbackManager.player.pause()
                            playbackManager.exitBackgroundPlayback()
                        }
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            )
            .build()
            .apply {
                setUseNextAction(false)
                setUsePreviousAction(false)
                setUseNextActionInCompactView(false)
                setUsePreviousActionInCompactView(false)
                setUseFastForwardAction(true)
                setUseRewindAction(true)
                mediaSession?.let { session ->
                    setMediaSessionToken(session.platformToken)
                }
            }
    }

    private fun buildContentIntent(
        state: VideoPlaybackManager.BackgroundPlaybackState
    ): PendingIntent {
        return PendingIntent.getActivity(
            this,
            REQUEST_CODE_CONTENT,
            state.resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val TAG = "NotificationService"
        private const val CHANNEL_ID = "video_playback_channel"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_START = "com.zhengyang.redbook.action.START_PLAYBACK_SERVICE"
        private const val ACTION_STOP = "com.zhengyang.redbook.action.STOP_PLAYBACK_SERVICE"
        private const val ACTION_REFRESH = "com.zhengyang.redbook.action.REFRESH_PLAYBACK_SERVICE"
        private const val ACTION_DISMISS = "com.zhengyang.redbook.action.DISMISS_PLAYBACK_SERVICE"
        private const val REQUEST_CODE_CONTENT = 1001

        fun start(serviceContext: Context) {
            val intent = Intent(serviceContext, NotificationService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(serviceContext, intent)
        }

        fun refresh(serviceContext: Context) {
            val intent = Intent(serviceContext, NotificationService::class.java).apply {
                action = ACTION_REFRESH
            }
            ContextCompat.startForegroundService(serviceContext, intent)
        }

        fun dismiss(serviceContext: Context) {
            val intent = Intent(serviceContext, NotificationService::class.java).apply {
                action = ACTION_DISMISS
            }
            serviceContext.startService(intent)
        }

        fun stop(serviceContext: Context) {
            val intent = Intent(serviceContext, NotificationService::class.java).apply {
                action = ACTION_STOP
            }
            serviceContext.startService(intent)
        }
    }
}
