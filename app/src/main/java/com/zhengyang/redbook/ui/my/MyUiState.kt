/**
 * 文件说明： MyUiState.kt
 * 作用： 承载个人中心与资料页相关的界面状态与交互逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.my

data class MyUiState(
    val stats: MyProfileStats = MyProfileStats(),
    val interestPeople: List<InterestPersonItem> = emptyList(),
    val isInterestSectionVisible: Boolean = true
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
