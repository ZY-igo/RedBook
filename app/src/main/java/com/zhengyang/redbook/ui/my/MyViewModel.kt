package com.zhengyang.redbook.ui.my

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhengyang.redbook.usecase.LoadMyUiStateParams
import com.zhengyang.redbook.usecase.LoadMyUiStateUseCase
import com.zhengyang.redbook.utils.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * “我的”页面 ViewModel。
 *
 * 负责加载个人主页状态，并管理未登录态下的登录面板交互状态。
 *
 * @property loadMyUiState 加载“我的”页面状态的用例。
 * @property uiState 提供给界面的只读状态流。
 */
@HiltViewModel
class MyViewModel @Inject constructor(
    private val loadMyUiState: LoadMyUiStateUseCase
) : ViewModel() {

    /**
     * ViewModel 内部可变状态。
     */
    private val _uiState = MutableStateFlow(MyUiState())

    /**
     * 暴露给 UI 层的只读状态流。
     */
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

    init {
        // 首次进入页面时先按当前默认登录态拉取一份初始数据。
        loadProfile(forceRefresh = false)
    }

    /**
     * 主动刷新“我的”页数据。
     */
    fun refresh() {
        loadProfile(forceRefresh = true)
    }

    /**
     * 关闭兴趣推荐区域。
     */
    fun dismissInterestSection() {
        _uiState.update { it.copy(isInterestSectionVisible = false) }
    }

    /**
     * 切换登录协议勾选状态。
     *
     * 同时清除上一次的协议错误提示。
     */
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

    /**
     * 展开或收起“其他登录方式”区域。
     */
    fun toggleOtherMethods() {
        _uiState.update { state ->
            state.copy(
                loginState = state.loginState.copy(
                    isOtherMethodsExpanded = !state.loginState.isOtherMethodsExpanded
                )
            )
        }
    }

    /**
     * 选择登录方式。
     *
     * @param method 用户刚刚选择的登录方式。
     */
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

    /**
     * 处理帮助按钮点击。
     */
    fun onHelpClick() {
        _uiState.update { state ->
            state.copy(
                loginState = state.loginState.copy(
                    helperText = "Try another sign-in method or recover your account."
                )
            )
        }
    }

    /**
     * 处理账号恢复入口点击。
     */
    fun onRecoverAccountClick() {
        _uiState.update { state ->
            state.copy(
                loginState = state.loginState.copy(
                    helperText = "Account recovery is not connected yet."
                )
            )
        }
    }

    /**
     * 提交登录操作。
     *
     * 当前实现是一个前端模拟流程：
     * 先校验协议勾选状态，再展示加载文案，最后延迟后切到已登录态。
     *
     * @param method 本次使用的登录方式。
     */
    fun submitLogin(method: LoginMethod) {
        val currentState = _uiState.value
        if (currentState.loginState.isSubmitting) return
        if (!currentState.loginState.isAgreementChecked) {
            _uiState.update { state ->
                state.copy(
                    loginState = state.loginState.copy(
                        selectedMethod = method,
                        showAgreementError = true,
                        helperText = "Accept the agreement before continuing."
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

            // 这里用短暂延迟模拟真实登录请求耗时。
            delay(900)
            loadProfile(loggedIn = true, forceRefresh = true)
        }
    }

    /**
     * 生成某种登录方式对应的帮助文案。
     */
    private fun helperTextFor(method: LoginMethod): String {
        return when (method) {
            LoginMethod.WECHAT -> "Use WeChat for a fast sign-in."
            LoginMethod.APPLE -> "Use Apple for a lightweight registration flow."
            LoginMethod.PHONE -> "Use your phone number for easier recovery."
            LoginMethod.QQ -> "Use QQ if that is already your primary account."
        }
    }

    /**
     * 生成登录提交中的加载文案。
     */
    private fun loadingTextFor(method: LoginMethod): String {
        return when (method) {
            LoginMethod.WECHAT -> "Opening WeChat sign-in..."
            LoginMethod.APPLE -> "Verifying Apple account..."
            LoginMethod.PHONE -> "Preparing phone verification..."
            LoginMethod.QQ -> "Opening QQ sign-in..."
        }
    }

    /**
     * 加载“我的”页数据。
     *
     * @param loggedIn 是否按已登录态加载。
     * @param forceRefresh 是否强制刷新底层数据源。
     */
    private fun loadProfile(loggedIn: Boolean = false, forceRefresh: Boolean = false) {
        viewModelScope.launch {
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
                    // 登录成功后直接切到已登录态，由后端/用例返回完整主页数据。
                    loaded.copy(isLoggedIn = true)
                } else {
                    // 未登录刷新时保留当前登录面板交互状态，避免界面闪回默认值。
                    loaded.copy(loginState = currentLogin)
                }
            }.onFailure {
                AppLogger.e("MyViewModel", "Failed to load my page state.", it)
                _uiState.update { state ->
                    state.copy(
                        loginState = state.loginState.copy(
                            isSubmitting = false,
                            helperText = "Failed to load page. Try again later."
                        )
                    )
                }
            }
        }
    }
}
