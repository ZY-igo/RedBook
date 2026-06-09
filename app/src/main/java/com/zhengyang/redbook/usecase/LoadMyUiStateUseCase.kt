package com.zhengyang.redbook.usecase

import com.zhengyang.redbook.R
import com.zhengyang.redbook.ui.my.InterestPersonItem
import com.zhengyang.redbook.ui.my.MyProfileHeader
import com.zhengyang.redbook.ui.my.MyProfileStats
import com.zhengyang.redbook.ui.my.MyUiState
import javax.inject.Inject

class LoadMyUiStateUseCase @Inject constructor() : SuspendUseCase<Boolean, MyUiState>() {

    override suspend fun execute(input: Boolean): MyUiState {
        if (!input) return MyUiState(isLoggedIn = false)

        return MyUiState(
            isLoggedIn = true,
            profile = MyProfileHeader(
                id = "redbook_2048",
                name = "郑阳",
                avatarText = "郑",
                avatarColorHex = "#FF8A9F",
                bio = "记录生活，也记录灵感"
            ),
            stats = MyProfileStats(
                followingCount = "12",
                fansCount = "3",
                likesCount = "28"
            ),
            interestPeople = listOf(
                InterestPersonItem(
                    id = "p1",
                    avatarText = "L",
                    name = "林一木",
                    fansText = "1.2万粉丝",
                    avatarBackgroundRes = R.drawable.bg_xhs_avatar_green
                ),
                InterestPersonItem(
                    id = "p2",
                    avatarText = "A",
                    name = "阿甘厨房",
                    fansText = "8,421粉丝",
                    avatarBackgroundRes = R.drawable.bg_xhs_avatar_orange
                ),
                InterestPersonItem(
                    id = "p3",
                    avatarText = "M",
                    name = "Mori Travel",
                    fansText = "5,612粉丝",
                    avatarBackgroundRes = R.drawable.bg_xhs_avatar_purple
                )
            )
        )
    }
}
