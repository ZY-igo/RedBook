/**
 * 文件说明：AppLogger.kt
 * 作用：集中封装应用运行日志的记录与输出逻辑。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.utils

import android.content.Context
import android.util.Log
import com.zhengyang.redbook.BuildConfig
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object AppLogger {
    private const val DEFAULT_TAG = "RedBook"
    private val timestampFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

    @Volatile
    private var minLevel: LogLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.INFO

    @Volatile
    private var fileStore: LogFileStore? = null

    fun initialize(context: Context, level: LogLevel = minLevel) {
        fileStore = LogFileStore.create(context.applicationContext)
        minLevel = level
        i("AppLogger", "Logger initialized. fileDir=${fileStore?.directory()?.absolutePath.orEmpty()}")
    }

    fun setMinLevel(level: LogLevel) {
        minLevel = level
        i("AppLogger", "Logger level changed to ${level.name}")
    }

    fun d(tag: String = DEFAULT_TAG, message: String) {
        log(LogLevel.DEBUG, tag, message)
    }

    fun i(tag: String = DEFAULT_TAG, message: String) {
        log(LogLevel.INFO, tag, message)
    }

    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        log(LogLevel.WARN, tag, message, throwable)
    }

    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, tag, message, throwable)
    }

    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        if (level.priority < minLevel.priority) return
        when (level) {
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
        }
        fileStore?.append(buildEntry(level, tag, message, throwable))
    }

    private fun buildEntry(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?
    ): String {
        val time = timestampFormatter.format(Instant.now())
        val throwableText = throwable?.let { "\n" + Log.getStackTraceString(it) }.orEmpty()
        return "$time ${level.label}/$tag: $message$throwableText"
    }

    enum class LogLevel(val priority: Int, val label: String) {
        DEBUG(10, "D"),
        INFO(20, "I"),
        WARN(30, "W"),
        ERROR(40, "E")
    }
}
