package com.zhengyang.redbook.ui.my

data class MyUiState(
    val isLoggedIn: Boolean = false,
    val loginState: MyLoginUiState = MyLoginUiState(),
    val profile: MyProfileHeader = MyProfileHeader(),
    val stats: MyProfileStats = MyProfileStats(),
    val interestPeople: List<InterestPersonItem> = emptyList(),
    val isInterestSectionVisible: Boolean = true
)

data class MyLoginUiState(
    val isAgreementChecked: Boolean = false,
    val isOtherMethodsExpanded: Boolean = false,
    val isSubmitting: Boolean = false,
    val selectedMethod: LoginMethod = LoginMethod.WECHAT,
    val helperText: String = "登录后可展示主页、收藏和点赞内容",
    val showAgreementError: Boolean = false,
    val suggestedUserId: String = "",
    val suggestedNickname: String = ""
)

enum class LoginMethod {
    WECHAT,
    APPLE,
    PHONE,
    QQ
}

data class MyProfileHeader(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String? = null,
    val avatarText: String = "",
    val avatarColorHex: String = "#FF8A9F",
    val bio: String? = null
)

data class MyProfileStats(
    val followingCount: String = "",
    val fansCount: String = "",
    val likesCount: String = ""
)

data class InterestPersonItem(
    val id: String,
    val avatarText: String,
    val name: String,
    val fansText: String,
    val avatarBackgroundRes: Int
)
