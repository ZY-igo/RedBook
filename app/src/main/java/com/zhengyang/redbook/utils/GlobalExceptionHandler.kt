/**
 * 文件说明：GlobalExceptionHandler.kt
 * 作用：处理 Global Exception Handler 相关异常捕获与兜底逻辑。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.utils

import android.content.Context

class GlobalExceptionHandler(
    private val appContext: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        persistCrashSummary(thread, throwable)
        AppLogger.e(
            tag = "GlobalExceptionHandler",
            message = "Uncaught exception on thread=${thread.name}",
            throwable = throwable
        )
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun persistCrashSummary(thread: Thread, throwable: Throwable) {
        runCatching {
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_CRASH_MESSAGE, throwable.message ?: throwable::class.java.simpleName)
                .putString(KEY_LAST_CRASH_THREAD, thread.name)
                .putLong(KEY_LAST_CRASH_AT, System.currentTimeMillis())
                .commit()
        }.onFailure {
            AppLogger.w("GlobalExceptionHandler", "Failed to persist crash summary.", it)
        }
    }

    companion object {
        private const val PREFS_NAME = "global_exception_handler"
        private const val KEY_LAST_CRASH_MESSAGE = "last_crash_message"
        private const val KEY_LAST_CRASH_THREAD = "last_crash_thread"
        private const val KEY_LAST_CRASH_AT = "last_crash_at"

        fun consumeLastCrashSummary(context: Context): String? {
            val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val message = preferences.getString(KEY_LAST_CRASH_MESSAGE, null) ?: return null
            val threadName = preferences.getString(KEY_LAST_CRASH_THREAD, null).orEmpty()
            preferences.edit()
                .remove(KEY_LAST_CRASH_MESSAGE)
                .remove(KEY_LAST_CRASH_THREAD)
                .remove(KEY_LAST_CRASH_AT)
                .apply()
            return if (threadName.isBlank()) message else "$message ($threadName)"
        }
    }
}
