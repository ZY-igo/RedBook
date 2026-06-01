package com.zhengyang.redbook.ui.my

import com.zhengyang.redbook.R

object MyMockData {

    fun profileStats(): MyProfileStats = MyProfileStats(
        followingCount = "2",
        fansCount = "0",
        likesCount = "0"
    )

    fun interestPeople(): List<InterestPersonItem> = listOf(
        InterestPersonItem(
            id = "xiaoqi",
            avatarText = "小七",
            name = "小七养生说",
            fansText = "粉丝 29 万",
            avatarBackgroundRes = R.drawable.bg_xhs_avatar_purple
        ),
        InterestPersonItem(
            id = "yangsheng",
            avatarText = "小禾",
            name = "养生小禾",
            fansText = "粉丝 8.9 万",
            avatarBackgroundRes = R.drawable.bg_xhs_avatar_green
        ),
        InterestPersonItem(
            id = "doctor",
            avatarText = "周医",
            name = "周医生变美日记",
            fansText = "粉丝 53 万",
            avatarBackgroundRes = R.drawable.bg_xhs_avatar_pink
        ),
        InterestPersonItem(
            id = "mumu",
            avatarText = "木木",
            name = "木木大M",
            fansText = "摄影师",
            avatarBackgroundRes = R.drawable.bg_xhs_avatar_blue_light
        )
    )
}
