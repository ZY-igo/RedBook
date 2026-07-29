/**
 * 文件说明：MediaPlayerFactory.kt
 * 作用：集中创建 Media Player Factory 相关对象或默认数据。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.zhengyang.redbook.utils.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import java.io.File

object MediaPlayerFactory {
    private const val TAG = "MediaPlayerFactory"

    // ExoPlayer 的磁盘缓存目录名。最终目录位于 app 的 cacheDir/video_cache。
    // 之所以放在 cacheDir 下，是因为这类视频分片/文件属于可丢弃缓存，
    // 系统或用户清理缓存时可以被安全删除。
    private const val CACHE_DIR_NAME = "video_cache"
    // LRU 缓存的目标容量。SimpleCache 写入新数据时会配合
    // LeastRecentlyUsedCacheEvictor 按“最近最少使用”策略淘汰旧文件，
    // 尽量在不影响近期播放体验的前提下控制本地占用。
    private const val CACHE_SIZE_BYTES = 128L * 1024L * 1024L
    // 当我们检测到目录体积明显超出预期上限时，直接整体重建缓存目录。
    // 这是一层兜底保护，用来应对异常退出、数据库状态与磁盘文件不一致、
    // 或历史残留文件导致的“缓存失控增长”。
    private const val CACHE_RESET_THRESHOLD_BYTES = 160L * 1024L * 1024L

    // 全局复用的视频播放音频属性。这里声明“媒体播放 + 电影内容”，
    // 这样系统可以正确处理音频焦点、耳机拔出、通知打断等行为。
    private val playbackAudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .build()

    // SimpleCache 在一个进程内通常应保持单例。
    // 1. 同一目录被多个 SimpleCache 实例同时占用容易产生锁冲突。
    // 2. 复用单例可以让多个 ExoPlayer 共享同一份磁盘缓存。
    // 3. volatile 保证不同线程读取到最新引用。
    @Volatile
    private var mediaCache: SimpleCache? = null

    /**
     * 创建带有“网络上游 + 本地缓存”能力的 ExoPlayer。
     *
     * 整条链路如下：
     * 1. `OkHttpDataSource.Factory` 负责真正的网络请求，属于上游数据源。
     * 2. `DefaultDataSource.Factory` 把上游包装成 ExoPlayer 常用的通用数据源工厂。
     * 3. `CacheDataSource.Factory` 把“缓存读取/缓存写入/网络回源”三件事组合起来。
     * 4. `DefaultMediaSourceFactory` 在真正创建 MediaSource 时会使用上面的
     *    CacheDataSource.Factory，因此后续 `setMediaItem + prepare()` 时会自动走缓存链路。
     *
     * 结果是：播放命中缓存时直接读磁盘；未命中时从网络读取，并把读取到的数据写入缓存。
     */
    @OptIn(UnstableApi::class)
    fun create(context: Context, okHttpClient: OkHttpClient = OkHttpClient()): ExoPlayer {
        // 只保留 ApplicationContext，避免播放器或缓存对象意外持有 Activity。
        val appContext = context.applicationContext

        // 上游网络数据源工厂。
        // 当缓存 miss 时，CacheDataSource 会委托它向远端 URL 发起 HTTP 请求。
        // 这里显式复用应用统一注入的 OkHttpClient，因此播放器侧也继承同一套
        // 连接池、超时、拦截器、认证头等网络配置。
        val upstreamFactory = OkHttpDataSource.Factory(okHttpClient)

        // CacheDataSource 是整个缓存链路的核心。
        // 它不是“只有缓存”的数据源，而是一个带决策能力的组合数据源：
        // 1. 先尝试从 SimpleCache 读取已缓存片段。
        // 2. 对未命中的范围回退到 upstreamFactory 发起网络请求。
        // 3. 把成功下载的数据同步写回 SimpleCache，供后续复用。
        val cacheDataSourceFactory = CacheDataSource.Factory()
            // 绑定用于磁盘缓存的 SimpleCache 单例。
            .setCache(getCache(appContext))
            // 指定缓存 miss 时要走的“上游”数据源工厂。
            // DefaultDataSource 再包一层的目的，是让 ExoPlayer 使用统一入口处理
            // http(s) 等常见 scheme；这里的核心上游仍然是 OkHttpDataSource。
            .setUpstreamDataSourceFactory(
                DefaultDataSource.Factory(appContext, upstreamFactory)
            )
            // 如果缓存层出现损坏、索引异常等问题，不要让播放直接失败，
            // 而是临时忽略缓存，直接走网络，优先保证可播放性。
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        // DefaultMediaSourceFactory 会在后续根据 MediaItem 的类型/协议创建具体 MediaSource，
        // 并把这里传入的 CacheDataSource.Factory 继续向下传递。
        // 这就是为什么业务层只看到 ExoPlayer.prepare()，却能自动命中缓存的根因。
        return ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .build()
            .apply {
                // 告诉系统当前播放器的音频语义，并在需要时申请音频焦点。
                setAudioAttributes(playbackAudioAttributes, true)
                // 耳机拔出、蓝牙音频断开等“音频即将外放”的场景下自动暂停。
                setHandleAudioBecomingNoisy(true)
            }
    }

    /**
     * 预热缓存目录与缓存索引。
     *
     * 这个方法不创建播放器，只做两件事：
     * 1. 提前检查缓存目录是否健康。
     * 2. 提前触发一次缓存相关对象访问，让首次真正播放时少做一点初始化工作。
     */
    fun warmUp(context: Context) {
        val appContext = context.applicationContext
        runCatching {
            ensureCacheHealth(appContext)
            val snapshot = snapshot(appContext)
            AppLogger.i(
                TAG,
                "Video cache ready. bytes=${snapshot.totalBytes}, files=${snapshot.fileCount}, " +
                    "limit=$CACHE_SIZE_BYTES"
            )
        }.onFailure {
            AppLogger.w(TAG, "Video cache warm-up failed.", it)
        }
    }

    /**
     * 准备并可选自动播放指定媒体项。
     *
     * 这里的逻辑本身很简单，但它会触发上面在 `create()` 中配置好的整条数据源链路：
     * 1. `setMediaItem()` 只是告诉播放器即将播放哪个资源。
     * 2. `prepare()` 才会真正开始创建 MediaSource、打开 DataSource、读取数据。
     * 3. 由于播放器持有的是 `DefaultMediaSourceFactory(cacheDataSourceFactory)`，
     *    所以 prepare 期间实际打开的是 CacheDataSource，而不是直接裸连网络。
     */
    fun prepare(player: ExoPlayer, mediaItem: MediaItem, playWhenReady: Boolean = true) {
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = playWhenReady
    }

    /**
     * 以“写入 SimpleCache 前缀字节”的方式预热视频。
     *
     * 这不是把整段视频离线下载完，而是主动缓存每个视频的前几 MB：
     * 1. 对短视频来说，这通常已经足够覆盖首屏播放和最初几秒。
     * 2. 用户真正点击播放时，ExoPlayer 会优先命中这部分缓存。
     * 3. 一旦缓存总量达到预热上限，就停止继续预热，避免把播放缓存整体挤满。
     *
     * 当前实现是为“推荐流/详情页提前预热当前或下一个视频”设计的保守版本。
     */
    suspend fun preload(
        context: Context,
        urls: List<String>,
        okHttpClient: OkHttpClient,
        config: VideoPreloadConfig = VideoPreloadConfig()
    ) {
        if (!config.enabled) {
            AppLogger.d(TAG, "preload skipped: disabled by config")
            return
        }

        val preloadTargets = urls.asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .take(config.preloadCount.coerceAtLeast(0))
            .toList()
        if (preloadTargets.isEmpty()) {
            AppLogger.d(TAG, "preload skipped: no valid urls")
            return
        }

        val appContext = context.applicationContext
        var cacheSnapshot = snapshot(appContext)
        if (cacheSnapshot.totalBytes >= config.maxCacheBytes) {
            AppLogger.d(
                TAG,
                "preload skipped: cache already above preload budget, bytes=${cacheSnapshot.totalBytes}, budget=${config.maxCacheBytes}"
            )
            return
        }

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(getCache(appContext))
            .setUpstreamDataSourceFactory(
                DefaultDataSource.Factory(appContext, OkHttpDataSource.Factory(okHttpClient))
            )
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        for (url in preloadTargets) {
            currentCoroutineContext().ensureActive()
            cacheSnapshot = snapshot(appContext)
            if (cacheSnapshot.totalBytes >= config.maxCacheBytes) {
                AppLogger.d(
                    TAG,
                    "preload stopped: cache reached preload budget, bytes=${cacheSnapshot.totalBytes}, budget=${config.maxCacheBytes}"
                )
                break
            }

            val dataSource = cacheDataSourceFactory.createDataSourceForDownloading()
            val dataSpec = DataSpec.Builder()
                .setUri(url)
                .setLength(config.bytesPerVideo)
                .build()
            val writer = CacheWriter(
                dataSource,
                dataSpec,
                null
            ) { requestLength, bytesCached, newBytesCached ->
                AppLogger.d(
                    TAG,
                    "preload progress: url=$url, requestLength=$requestLength, bytesCached=$bytesCached, newBytesCached=$newBytesCached"
                )
            }

            val completed = withTimeoutOrNull(config.perItemTimeoutMs) {
                withContext(Dispatchers.IO) {
                    currentCoroutineContext().ensureActive()
                    writer.cache()
                }
                true
            } ?: false

            if (!completed) {
                writer.cancel()
                AppLogger.w(
                    TAG,
                    "preload timeout/cancelled: url=$url, timeoutMs=${config.perItemTimeoutMs}"
                )
            } else {
                AppLogger.d(
                    TAG,
                    "preload complete: url=$url, targetBytes=${config.bytesPerVideo}"
                )
            }
        }
    }

    fun snapshot(context: Context): VideoCacheSnapshot {
        val cacheDir = cacheDirectory(context.applicationContext)
        return VideoCacheSnapshot(
            directory = cacheDir,
            totalBytes = cacheDir.directorySize(),
            fileCount = cacheDir.fileCount(),
            targetBytes = CACHE_SIZE_BYTES
        )
    }

    fun clear(context: Context): Boolean {
        val appContext = context.applicationContext
        return synchronized(this) {
            val cacheDir = cacheDirectory(appContext)
            runCatching {
                // 先 release 再删目录，避免 SimpleCache 仍持有文件锁或数据库句柄。
                mediaCache?.release()
                mediaCache = null
                cacheDir.deleteRecursively()
                cacheDir.mkdirs()
            }.onSuccess {
                AppLogger.i(TAG, "Video cache cleared. dir=${cacheDir.absolutePath}")
            }.onFailure {
                AppLogger.w(TAG, "Failed to clear video cache.", it)
            }.isSuccess
        }
    }

    /**
     * 获取或创建 SimpleCache 单例。
     *
     * SimpleCache 内部会维护缓存文件索引数据库，并对缓存目录加锁。
     * 因此这里必须保证：
     * 1. 同进程尽量只创建一个实例。
     * 2. 创建前先检查目录健康状态。
     * 3. 创建后缓存下来，供所有播放器共享。
     */
    private fun getCache(context: Context): SimpleCache {
        mediaCache?.let { return it }
        return synchronized(this) {
            ensureCacheHealth(context.applicationContext)
            mediaCache ?: SimpleCache(
                // 实际存放视频缓存文件的目录。
                cacheDirectory(context.applicationContext),
                // LRU 淘汰器：超过目标大小时回收最久未使用的缓存内容。
                LeastRecentlyUsedCacheEvictor(CACHE_SIZE_BYTES),
                // SimpleCache 需要一个数据库提供者保存索引元数据。
                // 这些元数据用于记录某个 URL 的哪些 byte range 已经缓存。
                StandaloneDatabaseProvider(context)
            ).also { mediaCache = it }
        }
    }

    /**
     * 对缓存目录做轻量健康检查。
     *
     * 注意：这不是逐文件校验，只做目录存在性和总体积阈值判断。
     * 对当前业务来说，这样的成本更低，也足够拦截最常见的异常膨胀问题。
     */
    private fun ensureCacheHealth(context: Context) {
        val cacheDir = cacheDirectory(context)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
            return
        }
        val currentBytes = cacheDir.directorySize()
        if (currentBytes <= CACHE_RESET_THRESHOLD_BYTES) return
        AppLogger.w(
            TAG,
            "Video cache directory exceeded reset threshold. bytes=$currentBytes, " +
                "threshold=$CACHE_RESET_THRESHOLD_BYTES"
        )
        mediaCache?.release()
        mediaCache = null
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }

    // 统一计算视频缓存目录位置，避免各处硬编码路径。
    private fun cacheDirectory(context: Context): File = File(context.cacheDir, CACHE_DIR_NAME)

    // 递归统计目录大小，用于日志与健康检查。
    private fun File.directorySize(): Long {
        if (!exists()) return 0L
        if (isFile) return length()
        return listFiles()?.sumOf { it.directorySize() } ?: 0L
    }

    // 递归统计文件数量，便于观察缓存中大致有多少分片/文件。
    private fun File.fileCount(): Int {
        if (!exists()) return 0
        if (isFile) return 1
        return listFiles()?.sumOf { it.fileCount() } ?: 0
    }

    data class VideoCacheSnapshot(
        val directory: File,
        val totalBytes: Long,
        val fileCount: Int,
        val targetBytes: Long
    )

    data class VideoPreloadConfig(
        val enabled: Boolean = true,
        val preloadCount: Int = 2,
        val bytesPerVideo: Long = 2L * 1024L * 1024L,
        val maxCacheBytes: Long = 100L * 1024L * 1024L,
        val perItemTimeoutMs: Long = 4_000L
    )
}
