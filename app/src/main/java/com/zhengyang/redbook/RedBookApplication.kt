/**
 * 文件说明：RedBookApplication.kt
 * 作用：负责应用级初始化、全局依赖装配与运行期配置。
 * 备注：使用 @HiltAndroidApp 标记，触发 Hilt 依赖注入框架的初始化。
 */
package com.zhengyang.redbook

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.zhengyang.redbook.utils.AppLogger
import com.zhengyang.redbook.utils.GlobalExceptionHandler
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用程序入口类。
 * 
 * 继承自 Application，在应用启动时第一个被调用。
 * 使用 `@HiltAndroidApp` 注解标记，告知 Hilt 在此处初始化依赖注入框架。
 * 
 * 主要职责：
 * 1. 恢复上次崩溃信息并提示用户
 * 2. 设置全局异常处理器
 * 3. 配置夜间模式策略
 */
@HiltAndroidApp
class RedBookApplication : android.app.Application() {

    /**
     * 应用创建时的初始化方法。
     * 
     * 在应用启动时被系统调用，执行以下操作：
     * 1. 检查并恢复上次崩溃信息
     * 2. 设置全局异常处理器
     * 3. 配置夜间模式跟随系统
     */
    override fun onCreate() {
        super.onCreate()
        
        // 检查是否有上次崩溃的信息
        GlobalExceptionHandler.consumeLastCrashSummary(this)?.let { summary ->
            // 记录日志
            AppLogger.w("RedBookApplication", "Recovered from previous crash: $summary")
            
            // 在主线程中显示 Toast 提示用户
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    this,
                    getString(R.string.app_recovered_from_crash),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        
        // 设置全局异常处理器，捕获未处理的异常
        Thread.setDefaultUncaughtExceptionHandler(
            GlobalExceptionHandler(
                appContext = this,
                defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            )
        )
        
        // 配置夜间模式跟随系统设置
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}