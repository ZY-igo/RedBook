/**
 * 文件说明：MyUiState.kt
 * 作用：定义 My Ui State 场景使用的界面状态模型。
 * 备注：用于标注当前源码文件的职责，便于后续维护与排查。
 */
package com.zhengyang.redbook.ui.my

data class MyUiState(
    val profile: MyProfileHeader = MyProfileHeader(),
    val stats: MyProfileStats = MyProfileStats(),
    val interestPeople: List<InterestPersonItem> = emptyList(),
    val isInterestSectionVisible: Boolean = true
)

data class MyProfileHeader(
    val id: String = "",
    val name: String = "",
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
