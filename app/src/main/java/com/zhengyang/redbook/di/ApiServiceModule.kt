package com.zhengyang.redbook.di

import com.zhengyang.redbook.BuildConfig
import com.zhengyang.redbook.data.mock.MockRedBookApiService
import com.zhengyang.redbook.data.remote.RedBookApiService
import com.zhengyang.redbook.data.remote.RemoteApiConfig
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
object ApiServiceModule {

    @Provides
    @Singleton
    fun provideRedBookApiService(
        okHttpClient: OkHttpClient,
        remoteApiConfig: RemoteApiConfig
    ): RedBookApiService {
        return if (BuildConfig.USE_MOCK_DATA) {
            MockRedBookApiService()
        } else {
            val retrofit = Retrofit.Builder()
                .baseUrl(remoteApiConfig.baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
            retrofit.create(RedBookApiService::class.java)
        }
    }
}