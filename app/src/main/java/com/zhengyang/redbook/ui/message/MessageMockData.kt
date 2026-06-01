package com.zhengyang.redbook.ui.message

import com.zhengyang.redbook.R

object MessageMockData {

    fun messageRows(): List<MessageRowItem> = listOf(
        MessageRowItem(
            id = "activity",
            title = "活动消息",
            subtitle = "欢迎登录！一起来探索最近的热门内容",
            timeText = "星期四",
            backgroundRes = R.drawable.bg_xhs_message_avatar_blue,
            iconRes = R.drawable.ic_xhs_message,
            iconSizeDp = 20,
            showsRedDot = true
        ),
        MessageRowItem(
            id = "system",
            title = "系统消息",
            subtitle = "协议更新通知",
            timeText = "2021-01-25",
            backgroundRes = R.drawable.bg_xhs_message_avatar_blue,
            iconRes = R.drawable.ic_xhs_bell,
            iconSizeDp = 18
        ),
        MessageRowItem(
            id = "assistant",
            title = "小红书创作助手",
            subtitle = "你的桃花运怎么样？戳我测测桃花运吧～",
            timeText = "2020-11-07",
            backgroundRes = R.drawable.bg_xhs_avatar_teal,
            avatarText = "助",
            showsVerifiedBadge = true
        )
    )

    fun peopleSuggestions(): List<PersonSuggestionItem> = listOf(
        PersonSuggestionItem(
            id = "wellness",
            avatarText = "禾",
            name = "养生小禾",
            subtitle = "关注欧阳食养的人也关注",
            avatarBackgroundRes = R.drawable.bg_xhs_avatar_green
        ),
        PersonSuggestionItem(
            id = "doctor",
            avatarText = "周",
            name = "周医生变美日记",
            subtitle = "关注赵走在 Roo. 旁的人也关注",
            avatarBackgroundRes = R.drawable.bg_xhs_avatar_pink
        ),
        PersonSuggestionItem(
            id = "beauty",
            avatarText = "薇",
            name = "薇薇的新颜美记",
            subtitle = "美妆内容热门作者",
            avatarBackgroundRes = R.drawable.bg_xhs_avatar_teal
        ),
        PersonSuggestionItem(
            id = "travel",
            avatarText = "乔",
            name = "乔琦琪",
            subtitle = "旅行摄影热门创作者",
            avatarBackgroundRes = R.drawable.bg_xhs_avatar_orange
        ),
        PersonSuggestionItem(
            id = "wood",
            avatarText = "木",
            name = "木琦大昕",
            subtitle = "关注陈巴食养的人也关注",
            avatarBackgroundRes = R.drawable.bg_xhs_avatar_blue_light
        )
    )
}
