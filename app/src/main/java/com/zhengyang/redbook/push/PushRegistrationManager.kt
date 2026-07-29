package com.zhengyang.redbook.push

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.zhengyang.redbook.data.auth.AuthRepository
import com.zhengyang.redbook.data.remote.RedBookApiService
import com.zhengyang.redbook.data.remote.model.RemotePushTokenRegistrationRequestDto
import com.zhengyang.redbook.data.remote.requireData
import com.zhengyang.redbook.utils.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Singleton
class PushRegistrationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val apiService: RedBookApiService
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun syncCurrentToken(force: Boolean = false) {
        if (!isFirebaseConfigured()) {
            AppLogger.w("PushRegistration", "Firebase not configured. Skip token sync.")
            return
        }
        if (!authRepository.isLoggedIn()) {
            AppLogger.d("PushRegistration", "No authenticated session. Skip token sync.")
            return
        }
        scope.launch {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                registerTokenInternal(token = token, force = force)
            }.onFailure {
                AppLogger.w("PushRegistration", "Failed to sync FCM token.", it)
            }
        }
    }

    fun registerToken(token: String, force: Boolean = false) {
        if (token.isBlank()) return
        if (!authRepository.isLoggedIn()) {
            AppLogger.d("PushRegistration", "Token received before login. Cache skipped.")
            return
        }
        scope.launch {
            runCatching {
                registerTokenInternal(token = token, force = force)
            }.onFailure {
                AppLogger.w("PushRegistration", "Failed to register FCM token.", it)
            }
        }
    }

    fun unregisterCurrentToken() {
        val token = prefs.getString(KEY_LAST_TOKEN, null) ?: return
        if (!authRepository.isLoggedIn()) return
        scope.launch {
            runCatching {
                apiService.unregisterPushToken(
                    RemotePushTokenRegistrationRequestDto(
                        token = token,
                        platform = PLATFORM_ANDROID,
                        deviceId = deviceId()
                    )
                ).requireData()
                clearSyncedState()
            }.onFailure {
                AppLogger.w("PushRegistration", "Failed to unregister FCM token.", it)
            }
        }
    }

    private suspend fun registerTokenInternal(token: String, force: Boolean) {
        val session = authRepository.currentSession() ?: return
        val lastToken = prefs.getString(KEY_LAST_TOKEN, null)
        val lastUserId = prefs.getString(KEY_LAST_USER_ID, null)
        if (!force && token == lastToken && session.userId == lastUserId) {
            return
        }
        apiService.registerPushToken(
            RemotePushTokenRegistrationRequestDto(
                token = token,
                platform = PLATFORM_ANDROID,
                deviceId = deviceId()
            )
        ).requireData()
        prefs.edit()
            .putString(KEY_LAST_TOKEN, token)
            .putString(KEY_LAST_USER_ID, session.userId)
            .apply()
    }

    private fun clearSyncedState() {
        prefs.edit()
            .remove(KEY_LAST_TOKEN)
            .remove(KEY_LAST_USER_ID)
            .apply()
    }

    private fun isFirebaseConfigured(): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    private fun deviceId(): String = prefs.getString(KEY_DEVICE_ID, null) ?: buildDeviceId().also { generated ->
        prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
    }

    private fun buildDeviceId(): String = "android-${System.currentTimeMillis()}"

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private companion object {
        private const val PREFS_NAME = "push_registration"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_LAST_TOKEN = "last_token"
        private const val KEY_LAST_USER_ID = "last_user_id"
        private const val PLATFORM_ANDROID = "android"
    }
}
