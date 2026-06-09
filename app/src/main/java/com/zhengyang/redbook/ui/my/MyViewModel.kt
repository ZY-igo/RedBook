/**
 * 文件说明：MyViewModel.kt
 * 作用：负责 My View Model 相关界面状态组织、数据加载与事件响应。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.ui.my

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhengyang.redbook.usecase.LoadMyUiStateUseCase
import com.zhengyang.redbook.utils.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyViewModel @Inject constructor(
    private val loadMyUiState: LoadMyUiStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyUiState())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun refresh() {
        loadProfile()
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
                    helperText = "登录遇到问题可尝试切换其他方式，或找回账号"
                )
            )
        }
    }

    fun onRecoverAccountClick() {
        _uiState.update { state ->
            state.copy(
                loginState = state.loginState.copy(
                    helperText = "找回流程暂未接真实接口，当前先保留页面交互"
                )
            )
        }
    }

    fun submitLogin(method: LoginMethod) {
        val currentState = _uiState.value
        if (currentState.loginState.isSubmitting) return
        if (!currentState.loginState.isAgreementChecked) {
            _uiState.update { state ->
                state.copy(
                    loginState = state.loginState.copy(
                        selectedMethod = method,
                        showAgreementError = true,
                        helperText = "请先勾选协议，再继续登录"
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    loginState = state.loginState.copy(
                        isSubmitting = true,
                        selectedMethod = method,
                        helperText = loadingTextFor(method),
                        showAgreementError = false
                    )
                )
            }
            delay(900)
            loadProfile(loggedIn = true)
        }
    }

    private fun helperTextFor(method: LoginMethod): String {
        return when (method) {
            LoginMethod.WECHAT -> "使用微信快速登录，同步常用社交关系"
            LoginMethod.APPLE -> "使用 Apple 登录，更适合轻量注册"
            LoginMethod.PHONE -> "使用手机号登录，便于后续找回账号"
            LoginMethod.QQ -> "使用 QQ 登录，适合已有 QQ 账号的用户"
        }
    }

    private fun loadingTextFor(method: LoginMethod): String {
        return when (method) {
            LoginMethod.WECHAT -> "正在拉起微信登录..."
            LoginMethod.APPLE -> "正在校验 Apple 账号..."
            LoginMethod.PHONE -> "正在准备手机号验证..."
            LoginMethod.QQ -> "正在拉起 QQ 登录..."
        }
    }

    private fun loadProfile(loggedIn: Boolean = false) {
        viewModelScope.launch {
            runCatching { loadMyUiState(loggedIn) }
                .onSuccess { loaded ->
                    val currentLogin = _uiState.value.loginState
                    _uiState.value = if (loggedIn) {
                        loaded.copy(isLoggedIn = true)
                    } else {
                        loaded.copy(
                            loginState = currentLogin
                        )
                    }
                }
                .onFailure {
                    AppLogger.e("MyViewModel", "Failed to load my page state.", it)
                    _uiState.update { state ->
                        state.copy(
                            loginState = state.loginState.copy(
                                isSubmitting = false,
                                helperText = "页面加载失败，请稍后重试"
                            )
                        )
                    }
                }
        }
    }
}
