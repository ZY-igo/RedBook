package com.zhengyang.redbook.data.repository

import com.zhengyang.redbook.ui.my.InterestPersonItem
import com.zhengyang.redbook.ui.my.MyProfileStats

interface MyRepository {
    suspend fun getProfileStats(): MyProfileStats
    suspend fun getInterestPeople(): List<InterestPersonItem>
}
