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
