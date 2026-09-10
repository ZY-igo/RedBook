package com.zhengyang.redbook.di

import android.app.Application
import androidx.room.Room
import com.zhengyang.redbook.data.auth.AuthRepository
import com.zhengyang.redbook.data.auth.AuthRepositoryImpl
import com.zhengyang.redbook.data.local.ListDao
import com.zhengyang.redbook.data.local.RedBookDatabase
import com.zhengyang.redbook.data.local.RedBookDatabaseMigrations
import com.zhengyang.redbook.data.remote.AuthApiService
import com.zhengyang.redbook.data.remote.RedBookApiService
import com.zhengyang.redbook.data.remote.RemoteApiConfig
import com.zhengyang.redbook.data.remote.interceptor.AuthTokenAuthenticator
import com.zhengyang.redbook.data.remote.interceptor.AuthTokenInterceptor
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
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * 全局共享的 Gson 实例。
     *
     * 使用 setLenient(true) 放宽 JSON 解析规则，允许后端返回带尾部数据、
     * 未转义字符等非严格 JSON 的情况，减少因后端响应不规范导致的解析崩溃。
     * 注意：lenient 不能让 Gson 接受 HTML/纯文本等完全非法的响应，
     * 真正的兜底仍依赖各 ViewModel/Repository 的 runCatching。
     */
    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().setLenient().create()

    @Provides
    @Singleton
    @Named("plain")
    fun providePlainOkHttpClient(
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
    fun provideOkHttpClient(
        @Named("plain") plainOkHttpClient: OkHttpClient,
        authTokenInterceptor: AuthTokenInterceptor,
        authTokenAuthenticator: AuthTokenAuthenticator
    ): OkHttpClient {
        return plainOkHttpClient.newBuilder()
            .addInterceptor(authTokenInterceptor)
            .authenticator(authTokenAuthenticator)
            .build()
    }

    @Provides
    @Singleton
    @Named("plain")
    fun providePlainRetrofit(
        @Named("plain") okHttpClient: OkHttpClient,
        remoteApiConfig: RemoteApiConfig,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(remoteApiConfig.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        remoteApiConfig: RemoteApiConfig,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(remoteApiConfig.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApiService(
        @Named("plain") retrofit: Retrofit
    ): AuthApiService = retrofit.create(AuthApiService::class.java)

    @Provides
    @Singleton
    fun provideRedBookDatabase(
        application: Application
    ): RedBookDatabase {
        return Room.databaseBuilder(
            application,
            RedBookDatabase::class.java,
            "redbook.db"
        )
            .addMigrations(*RedBookDatabaseMigrations.ALL)
            .build()
    }

    @Provides
    @Singleton
    fun provideListDao(
        redBookDatabase: RedBookDatabase
    ): ListDao = redBookDatabase.listDao()

    @Provides
    @Singleton
    fun provideAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository = authRepositoryImpl

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