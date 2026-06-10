package com.zhengyang.redbook.ui.my

/**
 * “我的”页面统一 UI 状态。
 *
 * @property isLoggedIn 当前是否已登录。
 * @property loginState 未登录态下使用的登录区域状态。
 * @property profile 个人资料头部信息。
 * @property stats 个人主页的统计信息。
 * @property interestPeople 兴趣推荐人物列表。
 * @property isInterestSectionVisible 兴趣推荐区域当前是否显示。
 */
data class MyUiState(
    val isLoggedIn: Boolean = false,
    val loginState: MyLoginUiState = MyLoginUiState(),
    val profile: MyProfileHeader = MyProfileHeader(),
    val stats: MyProfileStats = MyProfileStats(),
    val interestPeople: List<InterestPersonItem> = emptyList(),
    val isInterestSectionVisible: Boolean = true
)

/**
 * 未登录态下的登录面板状态。
 *
 * @property isAgreementChecked 用户是否勾选登录协议。
 * @property isOtherMethodsExpanded 是否展开“其他登录方式”。
 * @property isSubmitting 当前是否正在提交登录。
 * @property selectedMethod 当前选中的登录方式。
 * @property helperText 登录面板底部的辅助说明文案。
 * @property showAgreementError 是否高亮协议未勾选错误。
 */
data class MyLoginUiState(
    val isAgreementChecked: Boolean = false,
    val isOtherMethodsExpanded: Boolean = false,
    val isSubmitting: Boolean = false,
    val selectedMethod: LoginMethod = LoginMethod.WECHAT,
    val helperText: String = "登录后可展示主页、收藏和点赞内容",
    val showAgreementError: Boolean = false
)

/**
 * 登录方式枚举。
 */
enum class LoginMethod {
    /** 微信登录。 */
    WECHAT,

    /** Apple 登录。 */
    APPLE,

    /** 手机号登录。 */
    PHONE,

    /** QQ 登录。 */
    QQ
}

/**
 * 个人主页头部信息。
 *
 * @property id 用户主页 id。
 * @property name 显示昵称。
 * @property avatarUrl 头像地址。
 * @property avatarText 无头像时展示的文字头像内容。
 * @property avatarColorHex 文字头像底色。
 * @property bio 个人简介。
 */
data class MyProfileHeader(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String? = null,
    val avatarText: String = "",
    val avatarColorHex: String = "#FF8A9F",
    val bio: String? = null
)

/**
 * 个人主页统计信息。
 *
 * @property followingCount 关注数文案。
 * @property fansCount 粉丝数文案。
 * @property likesCount 获赞与收藏数文案。
 */
data class MyProfileStats(
    val followingCount: String = "",
    val fansCount: String = "",
    val likesCount: String = ""
)

/**
 * 兴趣推荐人物项。
 *
 * @property id 推荐人物 id。
 * @property avatarText 头像文字。
 * @property name 人物名称。
 * @property fansText 粉丝数描述。
 * @property avatarBackgroundRes 头像背景资源。
 */
data class InterestPersonItem(
    val id: String,
    val avatarText: String,
    val name: String,
    val fansText: String,
    val avatarBackgroundRes: Int
)
