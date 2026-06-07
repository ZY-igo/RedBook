/**
 * 文件说明： RedBookApplication.kt
 * 作用： 定义应用 Application，并负责全局启动初始化配置。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.zhengyang.redbook.utils.AppLogger
import com.zhengyang.redbook.utils.GlobalExceptionHandler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RedBookApplication : android.app.Application() {

    override fun onCreate() {
        super.onCreate()
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
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}
