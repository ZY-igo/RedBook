package com.zhengyang.redbook.di

import com.zhengyang.redbook.data.local.ListDao
import com.zhengyang.redbook.data.local.RedBookDatabase
import com.zhengyang.redbook.data.remote.HttpService
import com.zhengyang.redbook.data.remote.ListContentApiService
import com.zhengyang.redbook.data.remote.RemoteApiConfig
import com.zhengyang.redbook.data.repository.HomeRepository
import com.zhengyang.redbook.data.repository.HomeRepositoryImpl
import com.zhengyang.redbook.data.repository.MessageRepository
import com.zhengyang.redbook.data.repository.MessageRepositoryImpl
import com.zhengyang.redbook.data.repository.MyRepository
import com.zhengyang.redbook.data.repository.MyRepositoryImpl
import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
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
    fun provideListContentApiService(
        httpService: HttpService
    ): ListContentApiService = httpService

    @Provides
    @Singleton
    fun provideRedBookDatabase(
        application: Application
    ): RedBookDatabase {
        return Room.databaseBuilder(
            application,
            RedBookDatabase::class.java,
            "redbook.db"
        ).fallbackToDestructiveMigration().build()
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
}
