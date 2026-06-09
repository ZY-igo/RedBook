package com.zhengyang.redbook.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import coil.ImageLoader
import com.zhengyang.redbook.media.MediaPlayerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }
}

@Module
@InstallIn(ActivityComponent::class)
object PlayerModule {

    @Provides
    @ActivityScoped
    fun provideExoPlayer(
        @ActivityContext context: Context,
        okHttpClient: okhttp3.OkHttpClient
    ): ExoPlayer {
        return MediaPlayerFactory.create(context, okHttpClient)
    }
}
