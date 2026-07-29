package com.zhengyang.redbook.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.zhengyang.redbook.media.MediaPlayerFactory
import com.zhengyang.redbook.utils.AppLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val TAG = "MediaModule"

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        val appContext = context.applicationContext
        AppLogger.d(
            TAG,
            "provideImageLoader: start build, cacheDir=${appContext.cacheDir.absolutePath}"
        )
        val imageLoader = ImageLoader.Builder(context)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder(appContext)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(appContext.cacheDir.resolve("coil_image_cache"))
                    .maxSizePercent(0.03)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
        AppLogger.d(
            TAG,
            "provideImageLoader: build complete, memoryMaxPercent=0.25, diskMaxPercent=0.03, diskDir=${appContext.cacheDir.resolve("coil_image_cache").absolutePath}, respectCacheHeaders=false"
        )
        return imageLoader
    }
}

@Module
@InstallIn(ActivityComponent::class)
object PlayerModule {

    @Provides
    @ActivityScoped
    /**
     * 为当前 Activity 提供一个作用域内复用的 ExoPlayer。
     *
     * 这里本身不直接拼装缓存细节，而是把当前 Activity 的 Context 和全局 OkHttpClient
     * 交给 `MediaPlayerFactory.create()` 统一构建。这样做的好处是：
     * 1. 业务侧只依赖 `ExoPlayer`，不需要知道缓存链路细节。
     * 2. 播放器网络请求与应用其他请求共用同一套 OkHttp 配置。
     * 3. 缓存能力集中在工厂层维护，后续改策略时不会把逻辑散落到页面里。
     */
    fun provideExoPlayer(
        @ActivityContext context: Context,
        okHttpClient: okhttp3.OkHttpClient
    ): ExoPlayer {
        AppLogger.d(
            TAG,
            "provideExoPlayer: create player for context=${context::class.java.simpleName}"
        )
        return MediaPlayerFactory.create(context, okHttpClient)
    }
}
