package com.zhengyang.redbook.data.home

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 首页“我的频道”选择持久化存储。
 *
 * 负责把用户在频道管理面板里添加/删除的频道 id 列表持久化到
 * SharedPreferences，使应用重启后能恢复用户自定义的频道集合，
 * 而不是每次都退回到服务端下发的默认频道。
 *
 * 存储格式：频道 id 按顺序用 `,` 拼接成单个字符串，确保读回时
 * 仍能保留用户在“我的频道”里的排列顺序。
 */
@Singleton
class HomeChannelPrefsStorage @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val preferences by lazy(LazyThreadSafetyMode.NONE) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 读取已持久化的“我的频道”id 列表。
     *
     * @return 持久化的频道 id 列表；从未保存过时返回空列表。
     */
    fun loadMyChannelIds(): List<String> {
        val raw = preferences.getString(KEY_MY_CHANNEL_IDS, null) ?: return emptyList()
        if (raw.isEmpty()) return emptyList()
        return raw.split(SEPARATOR).filter { it.isNotEmpty() }
    }

    /**
     * 持久化“我的频道”id 列表。
     *
     * @param ids 当前“我的频道”中包含的频道 id，顺序会被保留。
     */
    fun saveMyChannelIds(ids: List<String>) {
        val raw = ids.joinToString(SEPARATOR)
        preferences.edit()
            .putString(KEY_MY_CHANNEL_IDS, raw)
            .apply()
    }

    private companion object {
        private const val PREFS_NAME = "home_channel_prefs"
        private const val KEY_MY_CHANNEL_IDS = "my_channel_ids"
        private const val SEPARATOR = ","
    }
}
