package com.zhengyang.redbook.service.foreground

// 系统通知对象，前台服务启动时需要把它交给 `startForeground(...)`。
import android.app.Notification
// 用于封装“点通知后该打开哪个页面/Intent”。
import android.app.PendingIntent
// Service 返回值之一：告诉系统这个服务被杀掉后不要自动重建。
import android.app.Service.START_NOT_STICKY
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
// 播放器异常类型，通知层只负责记录并刷新状态。
import androidx.media3.common.PlaybackException
// Media3 播放器统一接口，通知栏和 MediaSession 都围绕它工作。
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
// Media3 提供的“带媒体会话能力的 Service”基类。
import androidx.media3.session.MediaSessionService
// Media3 自带的播放器通知栏管理器，负责生成播放通知 UI。
import androidx.media3.ui.PlayerNotificationManager
import com.zhengyang.redbook.R
import com.zhengyang.redbook.media.VideoPlaybackManager
import com.zhengyang.redbook.utils.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// 让 Hilt 可以给这个 Service 注入依赖。
@AndroidEntryPoint
class NotificationService : MediaSessionService() {

    @Inject
    // 统一持有播放器和“后台播放状态”的管理器；Activity 和 Service 共用它。
    lateinit var playbackManager: VideoPlaybackManager

    // Media3 的会话对象，系统媒体控件、蓝牙耳机等会通过它与播放器通信。
    private var mediaSession: MediaSession? = null
    // 负责真正创建、更新、取消通知栏 UI 的管理器。
    private var playerNotificationManager: PlayerNotificationManager? = null

    // 监听播放器状态变化，只要播放/暂停/缓冲/报错发生变化，就重新同步前台服务状态。
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // 播放状态变化时，通知是否应保持前台、按钮状态是否更新，都需要重新判断。
            refreshForegroundState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            // BUFFERING / READY / ENDED 等状态变化也会影响通知展示。
            refreshForegroundState()
        }

        override fun onPlayerError(error: PlaybackException) {
            // 通知层不处理恢复策略，只记录日志并把界面状态刷新出来。
            AppLogger.w(TAG, "Playback notification error.", error)
            refreshForegroundState()
        }
    }

    override fun onCreate() {
        super.onCreate()
        // 从统一管理器里拿共享播放器，而不是在 Service 内自己 new 一个播放器。
        val player = playbackManager.player
        // 先安装监听器，确保后续任何状态变化都能回流到通知层。
        player.addListener(playerListener)
        // 建立媒体会话，让系统知道这个前台服务控制的是哪一个播放器。
        mediaSession = MediaSession.Builder(this, player).build()
        // 创建通知栏管理器，后面所有通知更新都通过它完成。
        playerNotificationManager = buildNotificationManager()
        // Service 刚创建时就同步一次，避免状态延迟。
        refreshForegroundState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 这个 Service 不靠复杂参数驱动，主要通过 action 区分“启动/刷新/停止/关闭通知”。
        when (intent?.action) {
            ACTION_STOP -> {
                // “停止播放”语义比“关闭通知”更强：既暂停播放器，也退出后台播放模式。
                playbackManager.player.pause()
                playbackManager.exitBackgroundPlayback()
                refreshForegroundState()
                return START_NOT_STICKY
            }

            ACTION_DISMISS -> {
                // 这里只表示通知被关闭，不额外强制 pause；是否 pause 由别处决定。
                playbackManager.exitBackgroundPlayback()
                refreshForegroundState()
                return START_NOT_STICKY
            }

            ACTION_START,
            ACTION_REFRESH -> {
                // 启动和刷新本质上都只需要重新把当前播放器状态映射到前台通知。
                refreshForegroundState()
                return START_STICKY
            }
        }
        // 未识别 action 时走父类默认逻辑。
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        // 把当前 MediaSession 暴露给系统/外部控制器。
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // 用户从最近任务划掉应用时，如果播放器本来就没在播，就顺手把后台播放状态清掉。
        // 反过来说：如果还在播放，则允许前台服务继续活着，保证后台播放不中断。
        if (!playbackManager.player.isPlaying) {
            playbackManager.exitBackgroundPlayback()
            refreshForegroundState()
        }
    }

    override fun onDestroy() {
        // 先解绑通知和播放器，避免销毁过程中通知管理器继续接收播放器回调。
        playerNotificationManager?.setPlayer(null)
        playerNotificationManager = null
        // 移除监听，防止 Service 死掉后还被共享播放器持有引用。
        playbackManager.player.removeListener(playerListener)
        // 释放媒体会话，通知系统这套媒体控制入口已经失效。
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun refreshForegroundState() {
        // 当前真实在播的播放器实例。
        val player = playbackManager.player
        // 这是业务层额外维护的“是否处于后台播放模式”状态，不等同于 `player.isPlaying`。
        val backgroundState = playbackManager.backgroundPlaybackState
        // 如果通知管理器都还没建好，后面没法继续更新，直接返回。
        val notificationManager = playerNotificationManager ?: return

        if (backgroundState == null) {
            // 没有后台播放状态时，说明当前不应该继续显示播放通知。
            // 先把通知管理器和播放器解绑，再移除前台服务并自杀。
            notificationManager.setPlayer(null)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        // 点击通知时应回到哪个页面，由业务层提供的 `resumeIntent` 决定。
        mediaSession?.setSessionActivity(buildContentIntent(backgroundState))
        // 把播放器交给通知管理器后，它会自动根据播放器状态刷新通知文案和按钮。
        notificationManager.setPlayer(player)
    }

    @OptIn(UnstableApi::class)
    private fun buildNotificationManager(): PlayerNotificationManager {
        // Builder 里先指定上下文、通知 ID 和通知渠道 ID。
        return PlayerNotificationManager.Builder(
            this,
            NOTIFICATION_ID,
            CHANNEL_ID
        )
            // 通知渠道名称，Android 8+ 会显示在系统通知设置页。
            .setChannelNameResourceId(R.string.notification_playback_channel_name)
            // 通知渠道描述，同样给系统设置页使用。
            .setChannelDescriptionResourceId(R.string.notification_playback_channel_description)
            // 状态栏和通知左侧小图标。
            .setSmallIconResourceId(R.mipmap.ic_launcher)
            .setMediaDescriptionAdapter(
                // 这个适配器决定通知里“标题、文案、点击行为、大图”的来源。
                object : PlayerNotificationManager.MediaDescriptionAdapter {
                    override fun createCurrentContentIntent(player: Player): PendingIntent? {
                        // 如果当前没有后台播放上下文，就不给通知设置点击跳转。
                        val state = playbackManager.backgroundPlaybackState ?: return null
                        return buildContentIntent(state)
                    }

                    override fun getCurrentContentTitle(player: Player): CharSequence {
                        // 优先展示业务传入的视频标题；没有时用兜底文案。
                        val state = playbackManager.backgroundPlaybackState
                        return state?.title?.takeIf(String::isNotBlank)
                            ?: getString(R.string.notification_playback_title_fallback)
                    }

                    override fun getCurrentContentText(player: Player): CharSequence {
                        // 副标题通常展示作者名；没有作者名时显示默认说明。
                        val state = playbackManager.backgroundPlaybackState
                        return state?.author?.takeIf(String::isNotBlank)
                            ?: getString(R.string.notification_playback_content)
                    }

                    override fun getCurrentLargeIcon(
                        player: Player,
                        callback: PlayerNotificationManager.BitmapCallback
                        // 这里返回 null，表示暂时不加载大图封面。
                    ) = null
                }
            )
            .setNotificationListener(
                // 监听通知真正被系统展示/取消的时机，用来切换前台服务状态。
                object : PlayerNotificationManager.NotificationListener {
                    override fun onNotificationPosted(
                        notificationId: Int,
                        notification: Notification,
                        ongoing: Boolean
                    ) {
                        if (ongoing) {
                            // ongoing=true 说明这是一个需要常驻的播放通知，必须提升为前台服务。
                            startForeground(notificationId, notification)
                        } else {
                            // 不是常驻通知时，不必再占着前台服务资格，但通知本身可以保留。
                            stopForeground(STOP_FOREGROUND_DETACH)
                        }
                    }

                    override fun onNotificationCancelled(
                        notificationId: Int,
                        dismissedByUser: Boolean
                    ) {
                        if (dismissedByUser) {
                            // 如果是用户手动划掉通知，则视为用户不想继续后台播放。
                            playbackManager.player.pause()
                            playbackManager.exitBackgroundPlayback()
                        }
                        // 无论谁触发取消，Service 这边都把前台资格和自身生命周期收掉。
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            )
            .build()
            .apply {
                // 当前业务只有单视频播放，不需要上一首/下一首。
                setUseNextAction(false)
                setUsePreviousAction(false)
                setUseNextActionInCompactView(false)
                setUsePreviousActionInCompactView(false)
                // 保留快进/快退，方便用户在通知栏里调整进度。
                setUseFastForwardAction(true)
                setUseRewindAction(true)
                mediaSession?.let { session ->
                    // 把 MediaSession token 交给通知管理器，系统媒体控件才能和本会话关联起来。
                    setMediaSessionToken(session.platformToken)
                }
            }
    }

    private fun buildContentIntent(
        state: VideoPlaybackManager.BackgroundPlaybackState
    ): PendingIntent {
        // 点击通知时，重新拉起用户离开前的那个详情页 Intent。
        return PendingIntent.getActivity(
            this,
            REQUEST_CODE_CONTENT,
            state.resumeIntent,
            // 更新已有 PendingIntent 的 extras，并要求 Intent 在系统侧不可变，更安全。
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        // 日志标签。
        private const val TAG = "NotificationService"
        // Android 通知渠道 ID，同一类播放通知必须落在同一渠道下。
        private const val CHANNEL_ID = "video_playback_channel"
        // 这类播放通知的固定通知 ID，方便系统做覆盖更新而不是重复创建多条。
        private const val NOTIFICATION_ID = 1001
        // 以下 action 都是给 `onStartCommand(...)` 分发用的命令字。
        private const val ACTION_START = "com.zhengyang.redbook.action.START_PLAYBACK_SERVICE"
        private const val ACTION_STOP = "com.zhengyang.redbook.action.STOP_PLAYBACK_SERVICE"
        private const val ACTION_REFRESH = "com.zhengyang.redbook.action.REFRESH_PLAYBACK_SERVICE"
        private const val ACTION_DISMISS = "com.zhengyang.redbook.action.DISMISS_PLAYBACK_SERVICE"
        // PendingIntent 请求码；固定值即可，因为这里只有一种“点通知返回详情页”的场景。
        private const val REQUEST_CODE_CONTENT = 1001

        fun start(serviceContext: Context) {
            // 启动前台服务要走 `startForegroundService(...)`，否则高版本系统可能直接拒绝。
            val intent = Intent(serviceContext, NotificationService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(serviceContext, intent)
        }

        fun refresh(serviceContext: Context) {
            // 刷新时也复用前台服务启动入口，让系统确保 Service 仍处于可运行状态。
            val intent = Intent(serviceContext, NotificationService::class.java).apply {
                action = ACTION_REFRESH
            }
            ContextCompat.startForegroundService(serviceContext, intent)
        }

        fun dismiss(serviceContext: Context) {
            // 关闭通知不一定需要重新提升前台资格，所以普通 `startService(...)` 就够了。
            val intent = Intent(serviceContext, NotificationService::class.java).apply {
                action = ACTION_DISMISS
            }
            serviceContext.startService(intent)
        }

        fun stop(serviceContext: Context) {
            // 停止播放同理，只需要把命令送达现有 Service。
            val intent = Intent(serviceContext, NotificationService::class.java).apply {
                action = ACTION_STOP
            }
            serviceContext.startService(intent)
        }
    }
}
