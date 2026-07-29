package com.zhengyang.redbook.ui.my

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhengyang.redbook.data.auth.AuthLoginRequest
import com.zhengyang.redbook.data.auth.AuthRepository
import com.zhengyang.redbook.push.PushRegistrationManager
import com.zhengyang.redbook.usecase.LoadMyUiStateParams
import com.zhengyang.redbook.usecase.LoadMyUiStateUseCase
import com.zhengyang.redbook.utils.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MyViewModel @Inject constructor(
    private val loadMyUiState: LoadMyUiStateUseCase,
    private val authRepository: AuthRepository,
    private val pushRegistrationManager: PushRegistrationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyUiState())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

    init {
        restoreAndLoadProfile(forceRefresh = false)
    }

    fun refresh() {
        restoreAndLoadProfile(forceRefresh = true)
    }

    fun dismissInterestSection() {
        _uiState.update { it.copy(isInterestSectionVisible = false) }
    }

    fun toggleAgreement() {
        _uiState.update { state ->
            state.copy(
                loginState = state.loginState.copy(
                    isAgreementChecked = !state.loginState.isAgreementChecked,
                    showAgreementError = false
                )
            )
        }
    }

    fun toggleOtherMethods() {
        _uiState.update { state ->
            state.copy(
                loginState = state.loginState.copy(
                    isOtherMethodsExpanded = !state.loginState.isOtherMethodsExpanded
                )
            )
        }
    }

    fun selectLoginMethod(method: LoginMethod) {
        _uiState.update { state ->
            state.copy(
                loginState = state.loginState.copy(
                    selectedMethod = method,
                    isOtherMethodsExpanded = method == LoginMethod.PHONE || method == LoginMethod.QQ,
                    helperText = helperTextFor(method),
                    showAgreementError = false
                )
            )
        }
    }

    fun onHelpClick() {
        _uiState.update { state ->
            state.copy(
                loginState = state.loginState.copy(
                    helperText = "输入已有账号 ID 可继续登录，留空账号 ID 会自动创建新账号。"
                )
            )
        }
    }

    fun onRecoverAccountClick() {
        val hint = authRepository.accountHint()
        val helperText = if (hint.userId.isBlank()) {
            "没有找到上次登录记录，请输入昵称后创建或继续登录。"
        } else {
            "已为你预填上次使用的账号信息，可直接继续登录。"
        }
        _uiState.update { state ->
            state.copy(
                loginState = state.loginState.copy(
                    helperText = helperText,
                    suggestedUserId = hint.userId,
                    suggestedNickname = hint.nickname
                )
            )
        }
    }

    fun submitLogin(method: LoginMethod, rawUserId: String, rawNickname: String) {
        val currentState = _uiState.value
        if (currentState.loginState.isSubmitting) return
        if (!currentState.loginState.isAgreementChecked) {
            _uiState.update { state ->
                state.copy(
                    loginState = state.loginState.copy(
                        selectedMethod = method,
                        showAgreementError = true,
                        helperText = "继续前请先勾选用户协议。"
                    )
                )
            }
            return
        }

        val nickname = rawNickname.trim()
        if (nickname.isBlank()) {
            _uiState.update { state ->
                state.copy(
                    loginState = state.loginState.copy(
                        selectedMethod = method,
                        helperText = "请输入昵称后再继续。"
                    )
                )
            }
            return
        }

        val normalizedUserId = rawUserId.trim().ifBlank { generateUserId(nickname, method) }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    loginState = state.loginState.copy(
                        isSubmitting = true,
                        selectedMethod = method,
                        helperText = loadingTextFor(method),
                        showAgreementError = false,
                        suggestedUserId = normalizedUserId,
                        suggestedNickname = nickname
                    )
                )
            }

            runCatching {
                authRepository.login(
                    AuthLoginRequest(
                        userId = normalizedUserId,
                        nickname = nickname
                    )
                )
            }.onSuccess {
                pushRegistrationManager.syncCurrentToken(force = true)
                loadProfile(loggedIn = true, forceRefresh = true)
            }.onFailure { error ->
                AppLogger.e("MyViewModel", "Login failed.", error)
                _uiState.update { state ->
                    state.copy(
                        isLoggedIn = false,
                        loginState = state.loginState.copy(
                            isSubmitting = false,
                            helperText = error.message ?: "登录失败，请稍后重试。"
                        )
                    )
                }
            }
        }
    }

    fun logout() {
        if (!authRepository.isLoggedIn()) return
        viewModelScope.launch {
            pushRegistrationManager.unregisterCurrentToken()
            runCatching { authRepository.logout() }
                .onFailure { AppLogger.w("MyViewModel", "Logout request failed.", it) }
            val hint = authRepository.accountHint()
            _uiState.value = MyUiState(
                isLoggedIn = false,
                loginState = _uiState.value.loginState.copy(
                    isSubmitting = false,
                    helperText = "已退出登录。",
                    suggestedUserId = hint.userId,
                    suggestedNickname = hint.nickname
                )
            )
        }
    }

    private fun restoreAndLoadProfile(forceRefresh: Boolean) {
        viewModelScope.launch {
            val hasSession = runCatching { authRepository.restoreSession() }
                .onFailure { AppLogger.w("MyViewModel", "Failed to restore session.", it) }
                .getOrDefault(false)
            if (hasSession) {
                pushRegistrationManager.syncCurrentToken()
            }
            loadProfile(loggedIn = hasSession, forceRefresh = forceRefresh)
        }
    }

    private fun helperTextFor(method: LoginMethod): String {
        return when (method) {
            LoginMethod.WECHAT -> "微信入口已接入真实会话，可用于登录或首登创建账号。"
            LoginMethod.APPLE -> "Apple 入口复用当前会话接口，首次登录会自动创建账号。"
            LoginMethod.PHONE -> "输入已有账号 ID 可继续登录，留空则创建新账号。"
            LoginMethod.QQ -> "QQ 入口同样接入当前后端会话体系。"
        }
    }

    private fun loadingTextFor(method: LoginMethod): String {
        return when (method) {
            LoginMethod.WECHAT -> "正在通过微信入口建立会话..."
            LoginMethod.APPLE -> "正在验证 Apple 入口..."
            LoginMethod.PHONE -> "正在处理手机号入口..."
            LoginMethod.QQ -> "正在通过 QQ 入口建立会话..."
        }
    }

    private fun loadProfile(loggedIn: Boolean, forceRefresh: Boolean) {
        viewModelScope.launch {
            val hint = authRepository.accountHint()
            runCatching {
                loadMyUiState(
                    LoadMyUiStateParams(
                        loggedIn = loggedIn,
                        forceRefresh = forceRefresh
                    )
                )
            }.onSuccess { loaded ->
                val currentLogin = _uiState.value.loginState
                _uiState.value = if (loggedIn) {
                    loaded.copy(isLoggedIn = true)
                } else {
                    loaded.copy(
                        loginState = currentLogin.copy(
                            isSubmitting = false,
                            suggestedUserId = hint.userId,
                            suggestedNickname = hint.nickname
                        )
                    )
                }
            }.onFailure { error ->
                AppLogger.e("MyViewModel", "Failed to load my page state.", error)
                _uiState.update { state ->
                    state.copy(
                        isLoggedIn = false,
                        loginState = state.loginState.copy(
                            isSubmitting = false,
                            suggestedUserId = hint.userId,
                            suggestedNickname = hint.nickname,
                            helperText = if (loggedIn) {
                                "加载个人主页失败，请稍后重试。"
                            } else {
                                "会话已恢复，你可以继续登录。"
                            }
                        )
                    )
                }
            }
        }
    }

    private fun generateUserId(nickname: String, method: LoginMethod): String {
        val normalized = nickname.lowercase()
            .replace(Regex("[^a-z0-9]+"), "")
            .take(12)
            .ifBlank { "user" }
        val prefix = when (method) {
            LoginMethod.WECHAT -> "wx"
            LoginMethod.APPLE -> "apple"
            LoginMethod.PHONE -> "phone"
            LoginMethod.QQ -> "qq"
        }
        return "${prefix}_${normalized}_${System.currentTimeMillis().toString().takeLast(6)}"
    }
}
