/**
 * 文件说明：AppModule.kt
 * 作用：定义当前文件在项目中的核心实现与职责。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.di

import com.zhengyang.redbook.data.local.ListDao
import com.zhengyang.redbook.data.local.RedBookDatabase
import com.zhengyang.redbook.data.local.RedBookDatabaseMigrations
import com.zhengyang.redbook.data.remote.RedBookApiService
import com.zhengyang.redbook.data.remote.RemoteApiConfig
import com.zhengyang.redbook.data.remote.interceptor.DefaultHeadersInterceptor
import com.zhengyang.redbook.data.remote.interceptor.NetworkLoggingInterceptor
import com.zhengyang.redbook.data.remote.interceptor.RetryInterceptor
import com.zhengyang.redbook.data.repository.HomeRepository
import com.zhengyang.redbook.data.repository.HomeRepositoryImpl
import com.zhengyang.redbook.data.repository.MessageRepository
import com.zhengyang.redbook.data.repository.MessageRepositoryImpl
import com.zhengyang.redbook.data.repository.MyRepository
import com.zhengyang.redbook.data.repository.MyRepositoryImpl
import com.zhengyang.redbook.data.repository.NoteRepository
import com.zhengyang.redbook.data.repository.NoteRepositoryImpl
import com.zhengyang.redbook.data.repository.PublishRepository
import com.zhengyang.redbook.data.repository.PublishRepositoryImpl
import com.zhengyang.redbook.data.repository.SearchRepository
import com.zhengyang.redbook.data.repository.SearchRepositoryImpl
import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        remoteApiConfig: RemoteApiConfig
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(DefaultHeadersInterceptor(remoteApiConfig.defaultHeaders))
            .addInterceptor(RetryInterceptor())
            .addInterceptor(NetworkLoggingInterceptor())
            .connectTimeout(
                remoteApiConfig.connectTimeoutSeconds,
                remoteApiConfig.connectTimeoutUnit()
            )
            .readTimeout(
                remoteApiConfig.readTimeoutSeconds,
                remoteApiConfig.readTimeoutUnit()
            )
            .writeTimeout(
                remoteApiConfig.writeTimeoutSeconds,
                remoteApiConfig.writeTimeoutUnit()
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        remoteApiConfig: RemoteApiConfig
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(remoteApiConfig.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideRedBookApiService(
        retrofit: Retrofit
    ): RedBookApiService {
        return retrofit.create(RedBookApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRedBookDatabase(
        application: Application
    ): RedBookDatabase {
        return Room.databaseBuilder(
            application,
            RedBookDatabase::class.java,
            "redbook.db"
        ).addMigrations(*RedBookDatabaseMigrations.ALL).build()
    }

    @Provides
    @Singleton
    fun provideListDao(
        redBookDatabase: RedBookDatabase
    ): ListDao = redBookDatabase.listDao()

    @Provides
    @Singleton
    fun provideHomeRepository(
        homeRepositoryImpl: HomeRepositoryImpl
    ): HomeRepository = homeRepositoryImpl

    @Provides
    @Singleton
    fun provideMessageRepository(
        messageRepositoryImpl: MessageRepositoryImpl
    ): MessageRepository = messageRepositoryImpl

    @Provides
    @Singleton
    fun provideMyRepository(
        myRepositoryImpl: MyRepositoryImpl
    ): MyRepository = myRepositoryImpl

    @Provides
    @Singleton
    fun provideNoteRepository(
        noteRepositoryImpl: NoteRepositoryImpl
    ): NoteRepository = noteRepositoryImpl

    @Provides
    @Singleton
    fun providePublishRepository(
        publishRepositoryImpl: PublishRepositoryImpl
    ): PublishRepository = publishRepositoryImpl

    @Provides
    @Singleton
    fun provideSearchRepository(
        searchRepositoryImpl: SearchRepositoryImpl
    ): SearchRepository = searchRepositoryImpl
}
