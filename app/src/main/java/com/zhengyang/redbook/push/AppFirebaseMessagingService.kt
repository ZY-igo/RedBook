package com.zhengyang.redbook.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zhengyang.redbook.R
import com.zhengyang.redbook.SplashActivity
import com.zhengyang.redbook.utils.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AppFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var pushRegistrationManager: PushRegistrationManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        pushRegistrationManager.registerToken(token, force = true)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(R.string.push_default_title)
        val body = message.notification?.body
            ?: message.data["body"]
            ?: getString(R.string.push_default_body)
        val route = message.data["route"]
        showNotification(title = title, body = body, route = route)
    }

    private fun showNotification(title: String, body: String, route: String?) {
        ensureChannel()
        val intent = Intent(this, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            route?.let { putExtra(EXTRA_PUSH_ROUTE, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            route.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()
        runCatching {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        }.onFailure {
            AppLogger.w("AppFirebaseMessaging", "Failed to display notification.", it)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.push_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.push_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_PUSH_ROUTE = "push_route"
        private const val CHANNEL_ID = "redbook_push"
    }
}
