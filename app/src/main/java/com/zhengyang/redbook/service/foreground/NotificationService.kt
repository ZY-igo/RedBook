package com.zhengyang.redbook.service.foreground

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.zhengyang.redbook.R

class NotificationService : Service() {

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannelCompat.Builder(
                CHANNEL_ID,
                NotificationManagerCompat.IMPORTANCE_LOW
            )
                .setName(getString(R.string.notification_channel_name))
                .setDescription(getString(R.string.notification_channel_description))
                .build()

            NotificationManagerCompat.from(this).createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_content))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun doYourWork() {
        // Placeholder for async foreground work.
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "data_sync_channel"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_START = "com.zhengyang.redbook.action.START_PLAYBACK_SERVICE"
        private const val ACTION_STOP = "com.zhengyang.redbook.action.STOP_PLAYBACK_SERVICE"

        fun start(serviceContext: android.content.Context) {
            val intent = Intent(serviceContext, NotificationService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(serviceContext, intent)
        }

        fun stop(serviceContext: android.content.Context) {
            val intent = Intent(serviceContext, NotificationService::class.java).apply {
                action = ACTION_STOP
            }
            serviceContext.startService(intent)
        }
    }
}
