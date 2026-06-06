/**
 * 文件说明： AssetSeedDataSource.kt
 * 作用： 封装本地数据访问能力，例如 Room、预置资源读取和初始化逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.local

import android.app.Application
import com.zhengyang.redbook.data.model.DiscoverCategoryEntity
import com.zhengyang.redbook.data.model.FollowingUserEntity
import com.zhengyang.redbook.data.model.HomeCardEntity
import com.zhengyang.redbook.data.model.InterestPersonEntity
import com.zhengyang.redbook.data.model.MessageRowEntity
import com.zhengyang.redbook.data.model.MyProfileEntity
import com.zhengyang.redbook.data.model.PersonSuggestionEntity
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetSeedDataSource @Inject constructor(
    private val application: Application
) {
    fun load(): SeedPayload {
        val json = application.assets.open(BOOTSTRAP_FILE).bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        return SeedPayload(
            discoverCategories = root.getJSONArray("discoverCategories").map { item ->
                DiscoverCategoryEntity(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    bucket = item.getString("bucket"),
                    usesWaterfall = item.getBoolean("usesWaterfall"),
                    sortOrder = item.getInt("sortOrder"),
                    isDefaultSelected = item.getBoolean("isDefaultSelected")
                )
            },
            homeCards = root.getJSONArray("homeCards").map { item ->
                HomeCardEntity(
                    id = item.getString("id"),
                    sectionKey = item.getString("sectionKey"),
                    title = item.getString("title"),
                    author = item.getString("author"),
                    likeCount = item.getString("likeCount"),
                    badge = item.getString("badge"),
                    coverLabel = item.getString("coverLabel"),
                    coverHeightDp = item.getInt("coverHeightDp"),
                    mediaType = item.getString("mediaType"),
                    imageUrl = item.optString("imageUrl").takeIf { it.isNotEmpty() },
                    videoUrl = item.optString("videoUrl").takeIf { it.isNotEmpty() },
                    videoCoverUrl = item.optString("videoCoverUrl").takeIf { it.isNotEmpty() },
                    startColorHex = item.getString("startColorHex"),
                    endColorHex = item.getString("endColorHex"),
                    avatarColorHex = item.getString("avatarColorHex"),
                    sortOrder = item.getInt("sortOrder")
                )
            },
            followingUsers = root.getJSONArray("followingUsers").map { item ->
                FollowingUserEntity(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    subtitle = item.getString("subtitle"),
                    avatarColorHex = item.getString("avatarColorHex"),
                    badge = item.optString("badge").takeIf { it.isNotEmpty() },
                    sortOrder = item.getInt("sortOrder")
                )
            },
            messageRows = root.getJSONArray("messageRows").map { item ->
                MessageRowEntity(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    subtitle = item.getString("subtitle"),
                    timeText = item.getString("timeText"),
                    backgroundRes = resolveDrawable(item.getString("backgroundRes")),
                    iconRes = item.optString("iconRes").takeIf { it.isNotEmpty() }?.let(::resolveDrawable),
                    avatarText = item.optString("avatarText").takeIf { it.isNotEmpty() },
                    iconSizeDp = item.getInt("iconSizeDp"),
                    showsVerifiedBadge = item.getBoolean("showsVerifiedBadge"),
                    showsRedDot = item.getBoolean("showsRedDot"),
                    sortOrder = item.getInt("sortOrder")
                )
            },
            personSuggestions = root.getJSONArray("personSuggestions").map { item ->
                PersonSuggestionEntity(
                    id = item.getString("id"),
                    avatarText = item.getString("avatarText"),
                    name = item.getString("name"),
                    subtitle = item.getString("subtitle"),
                    avatarBackgroundRes = resolveDrawable(item.getString("avatarBackgroundRes")),
                    sortOrder = item.getInt("sortOrder")
                )
            },
            myProfile = root.getJSONObject("myProfile").let { item ->
                MyProfileEntity(
                    name = item.optString("name").ifEmpty { "小红薯用户" },
                    avatarText = item.optString("avatarText").ifEmpty { "我" },
                    avatarColorHex = item.optString("avatarColorHex").ifEmpty { "#FF8A9F" },
                    bio = item.optString("bio").takeIf { it.isNotEmpty() },
                    followingCount = item.optIntCompat("followingCount"),
                    fansCount = item.optIntCompat("fansCount"),
                    likesCount = item.optIntCompat("likesCount"),
                    noteCount = item.optIntCompat("noteCount")
                )
            },
            interestPeople = root.getJSONArray("interestPeople").map { item ->
                InterestPersonEntity(
                    id = item.getString("id"),
                    avatarText = item.getString("avatarText"),
                    name = item.getString("name"),
                    fansText = item.getString("fansText"),
                    avatarBackgroundRes = resolveDrawable(item.getString("avatarBackgroundRes")),
                    sortOrder = item.getInt("sortOrder")
                )
            }
        )
    }

    private fun resolveDrawable(name: String): Int {
        return application.resources.getIdentifier(name, "drawable", application.packageName)
    }

    private fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> {
        return List(length()) { index -> transform(getJSONObject(index)) }
    }

    private fun JSONObject.optIntCompat(key: String): Int {
        return when (val raw = opt(key)) {
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull() ?: 0
            else -> 0
        }
    }

    companion object {
        private const val BOOTSTRAP_FILE = "bootstrap_data.json"
    }
}

data class SeedPayload(
    val discoverCategories: List<DiscoverCategoryEntity>,
    val homeCards: List<HomeCardEntity>,
    val followingUsers: List<FollowingUserEntity>,
    val messageRows: List<MessageRowEntity>,
    val personSuggestions: List<PersonSuggestionEntity>,
    val myProfile: MyProfileEntity,
    val interestPeople: List<InterestPersonEntity>
)
