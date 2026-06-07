/**
 * 文件说明：RedBookApplication.kt
 * 作用：负责应用级初始化、全局依赖装配与运行期配置。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
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
