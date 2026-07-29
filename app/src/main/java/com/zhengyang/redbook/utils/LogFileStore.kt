/**
 * 文件说明：LogFileStore.kt
 * 作用：提供应用运行期日志文件的落盘、轮转与清理能力。
 * 备注：采用单线程执行器串行写入，避免多线程并发写文件导致的内容交错。
 */
package com.zhengyang.redbook.utils

import android.content.Context
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 应用日志文件存储器。
 *
 * 日志文件默认写入 `files/app_logs` 目录，按日期分文件，并保留有限数量的历史文件。
 */
class LogFileStore private constructor(
    private val logDir: File,
    private val zoneId: ZoneId,
    private val maxFileCount: Int
) {

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val fileNameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(zoneId)

    fun append(entry: String) {
        executor.execute {
            runCatching {
                ensureDirectory()
                pruneOldFilesIfNeeded()
                currentLogFile().appendText(entry + System.lineSeparator(), Charsets.UTF_8)
            }.onFailure {
                android.util.Log.w(TAG, "Failed to append log entry.", it)
            }
        }
    }

    fun directory(): File = logDir

    private fun currentLogFile(): File {
        val fileName = "redbook-${fileNameFormatter.format(Instant.now())}.log"
        return File(logDir, fileName)
    }

    private fun ensureDirectory() {
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
    }

    private fun pruneOldFilesIfNeeded() {
        val files = logDir.listFiles()
            ?.filter { it.isFile && it.extension.equals("log", ignoreCase = true) }
            ?.sortedByDescending(File::lastModified)
            .orEmpty()
        if (files.size <= maxFileCount) return
        files.drop(maxFileCount).forEach { stale ->
            runCatching { stale.delete() }
                .onFailure { android.util.Log.w(TAG, "Failed to delete stale log file: ${stale.name}", it) }
        }
    }

    companion object {
        private const val TAG = "LogFileStore"
        private const val LOG_DIR_NAME = "app_logs"
        private const val DEFAULT_MAX_FILE_COUNT = 7

        fun create(
            context: Context,
            zoneId: ZoneId = ZoneId.systemDefault(),
            maxFileCount: Int = DEFAULT_MAX_FILE_COUNT
        ): LogFileStore {
            return LogFileStore(
                logDir = File(context.filesDir, LOG_DIR_NAME),
                zoneId = zoneId,
                maxFileCount = maxFileCount
            )
        }
    }
}
