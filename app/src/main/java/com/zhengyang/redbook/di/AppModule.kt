/**
 * 文件说明：AppModule.kt
 * 作用：定义应用级别的依赖注入模块，配置全局单例依赖。
 * 备注：使用 Hilt 框架管理依赖，所有提供的对象均为单例。
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

/**
 * Hilt 模块，提供应用级别的单例依赖。
 * 
 * 使用 `@InstallIn(SingletonComponent::class)` 标记，表明这些依赖在整个应用生命周期内有效。
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * 提供 OkHttpClient 实例。
     * 
     * 配置了以下拦截器（按顺序执行）：
     * 1. DefaultHeadersInterceptor：添加默认请求头
     * 2. RetryInterceptor：自动重试失败的请求
     * 3. NetworkLoggingInterceptor：记录网络日志
     * 
     * 同时配置了连接超时、读取超时和写入超时。
     * 
     * @param remoteApiConfig 远程 API 配置对象。
     * @return 配置完成的 OkHttpClient 实例。
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        remoteApiConfig: RemoteApiConfig
    ): OkHttpClient {
        return OkHttpClient.Builder()
            // 添加默认请求头拦截器
            .addInterceptor(DefaultHeadersInterceptor(remoteApiConfig.defaultHeaders))
            // 添加重试拦截器
            .addInterceptor(RetryInterceptor())
            // 添加网络日志拦截器
            .addInterceptor(NetworkLoggingInterceptor())
            // 设置连接超时时间
            .connectTimeout(
                remoteApiConfig.connectTimeoutSeconds,
                remoteApiConfig.connectTimeoutUnit()
            )
            // 设置读取超时时间
            .readTimeout(
                remoteApiConfig.readTimeoutSeconds,
                remoteApiConfig.readTimeoutUnit()
            )
            // 设置写入超时时间
            .writeTimeout(
                remoteApiConfig.writeTimeoutSeconds,
                remoteApiConfig.writeTimeoutUnit()
            )
            .build()
    }

    /**
     * 提供 Retrofit 实例。
     * 
     * 配置了：
     * 1. Base URL（从 RemoteApiConfig 获取）
     * 2. OkHttpClient（用于发起请求）
     * 3. Gson 转换器（解析 JSON 响应）
     * 4. Scalars 转换器（处理纯文本响应）
     * 
     * @param okHttpClient OkHttpClient 实例。
     * @param remoteApiConfig 远程 API 配置对象。
     * @return 配置完成的 Retrofit 实例。
     */
    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        remoteApiConfig: RemoteApiConfig
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(remoteApiConfig.baseUrl)
            .client(okHttpClient)
            // 添加 Gson 转换器，用于解析 JSON
            .addConverterFactory(GsonConverterFactory.create())
            // 添加 Scalars 转换器，用于处理纯文本响应
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    /**
     * 提供 RedBookApiService 接口实现。
     * 
     * 通过 Retrofit 动态代理创建 API 服务接口的实现类。
     * 
     * @param retrofit Retrofit 实例。
     * @return RedBookApiService 接口的实现对象。
     */
    @Provides
    @Singleton
    fun provideRedBookApiService(
        retrofit: Retrofit
    ): RedBookApiService {
        return retrofit.create(RedBookApiService::class.java)
    }

    /**
     * 提供 Room 数据库实例。
     * 
     * 配置了：
     * 1. 数据库名称："redbook.db"
     * 2. 数据库迁移策略：应用所有已定义的迁移
     * 
     * @param application Android Application 实例。
     * @return RedBookDatabase 数据库实例。
     */
    @Provides
    @Singleton
    fun provideRedBookDatabase(
        application: Application
    ): RedBookDatabase {
        return Room.databaseBuilder(
            application,
            RedBookDatabase::class.java,
            "redbook.db"  // 数据库文件名
        )
            // 应用所有数据库迁移
            .addMigrations(*RedBookDatabaseMigrations.ALL)
            .build()
    }

    /**
     * 提供 ListDao 数据访问对象。
     * 
     * 通过数据库实例获取 DAO 对象，用于执行数据库操作。
     * 
     * @param redBookDatabase 数据库实例。
     * @return ListDao 实例。
     */
    @Provides
    @Singleton
    fun provideListDao(
        redBookDatabase: RedBookDatabase
    ): ListDao = redBookDatabase.listDao()

    /**
     * 提供 HomeRepository 接口实现。
     * 
     * 使用接口注入，便于测试时替换实现。
     * 
     * @param homeRepositoryImpl HomeRepository 的具体实现类。
     * @return HomeRepository 接口实例。
     */
    @Provides
    @Singleton
    fun provideHomeRepository(
        homeRepositoryImpl: HomeRepositoryImpl
    ): HomeRepository = homeRepositoryImpl

    /**
     * 提供 MessageRepository 接口实现。
     * 
     * @param messageRepositoryImpl MessageRepository 的具体实现类。
     * @return MessageRepository 接口实例。
     */
    @Provides
    @Singleton
    fun provideMessageRepository(
        messageRepositoryImpl: MessageRepositoryImpl
    ): MessageRepository = messageRepositoryImpl

    /**
     * 提供 MyRepository 接口实现。
     * 
     * @param myRepositoryImpl MyRepository 的具体实现类。
     * @return MyRepository 接口实例。
     */
    @Provides
    @Singleton
    fun provideMyRepository(
        myRepositoryImpl: MyRepositoryImpl
    ): MyRepository = myRepositoryImpl

    /**
     * 提供 NoteRepository 接口实现。
     * 
     * @param noteRepositoryImpl NoteRepository 的具体实现类。
     * @return NoteRepository 接口实例。
     */
    @Provides
    @Singleton
    fun provideNoteRepository(
        noteRepositoryImpl: NoteRepositoryImpl
    ): NoteRepository = noteRepositoryImpl

    /**
     * 提供 PublishRepository 接口实现。
     * 
     * @param publishRepositoryImpl PublishRepository 的具体实现类。
     * @return PublishRepository 接口实例。
     */
    @Provides
    @Singleton
    fun providePublishRepository(
        publishRepositoryImpl: PublishRepositoryImpl
    ): PublishRepository = publishRepositoryImpl

    /**
     * 提供 SearchRepository 接口实现。
     * 
     * @param searchRepositoryImpl SearchRepository 的具体实现类。
     * @return SearchRepository 接口实例。
     */
    @Provides
    @Singleton
    fun provideSearchRepository(
        searchRepositoryImpl: SearchRepositoryImpl
    ): SearchRepository = searchRepositoryImpl
}