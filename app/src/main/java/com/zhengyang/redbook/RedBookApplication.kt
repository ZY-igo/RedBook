package com.zhengyang.redbook

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.zhengyang.redbook.media.MediaPlayerFactory
import com.zhengyang.redbook.push.PushRegistrationManager
import com.zhengyang.redbook.utils.AppLogger
import com.zhengyang.redbook.utils.GlobalExceptionHandler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class RedBookApplication : android.app.Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var pushRegistrationManager: PushRegistrationManager

    override fun onCreate() {
        super.onCreate()
        AppLogger.initialize(this)

        GlobalExceptionHandler.consumeLastCrashSummary(this)?.let { summary ->
            AppLogger.w("RedBookApplication", "Recovered from previous crash: $summary")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    this,
                    getString(R.string.app_recovered_from_crash),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        Thread.setDefaultUncaughtExceptionHandler(
            GlobalExceptionHandler(
                appContext = this,
                defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            )
        )

        applicationScope.launch {
            MediaPlayerFactory.warmUp(this@RedBookApplication)
        }
        pushRegistrationManager.syncCurrentToken()

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}
