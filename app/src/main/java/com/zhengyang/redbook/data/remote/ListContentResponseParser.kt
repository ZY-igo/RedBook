/**
 * 文件说明： ListContentResponseParser.kt
 * 作用： 封装远程数据访问相关逻辑，包括接口配置、请求行为和响应解析。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.remote

import com.zhengyang.redbook.data.remote.model.RemoteNoteDto
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class ListContentResponseParser @Inject constructor() {

    fun parse(responseBody: String): List<RemoteNoteDto> {
        val trimmedBody = responseBody.trim()
        if (trimmedBody.isEmpty()) return emptyList()

        return when {
            trimmedBody.startsWith("[") -> parseArray(JSONArray(trimmedBody))
            trimmedBody.startsWith("{") -> parseObject(JSONObject(trimmedBody))
            else -> emptyList()
        }
    }

    private fun parseObject(jsonObject: JSONObject): List<RemoteNoteDto> {
        val dataObject = jsonObject.optJSONObject(KEY_DATA)
        return when {
            jsonObject.has(KEY_ITEMS) -> parseArray(jsonObject.optJSONArray(KEY_ITEMS))
            jsonObject.has(KEY_LIST) -> parseArray(jsonObject.optJSONArray(KEY_LIST))
            jsonObject.has(KEY_DATA) && jsonObject.optJSONArray(KEY_DATA) != null -> {
                parseArray(jsonObject.optJSONArray(KEY_DATA))
            }
            dataObject != null && dataObject.optJSONArray(KEY_ITEMS) != null -> {
                parseArray(dataObject.optJSONArray(KEY_ITEMS))
            }
            dataObject != null && dataObject.optJSONArray(KEY_LIST) != null -> {
                parseArray(dataObject.optJSONArray(KEY_LIST))
            }
            dataObject != null && dataObject.optJSONArray(KEY_NOTES) != null -> {
                parseArray(dataObject.optJSONArray(KEY_NOTES))
            }
            jsonObject.has(KEY_NOTES) -> parseArray(jsonObject.optJSONArray(KEY_NOTES))
            else -> emptyList()
        }
    }

    private fun parseArray(jsonArray: JSONArray?): List<RemoteNoteDto> {
        if (jsonArray == null) return emptyList()
        return buildList(jsonArray.length()) {
            for (index in 0 until jsonArray.length()) {
                val itemObject = jsonArray.optJSONObject(index) ?: continue
                add(
                    RemoteNoteDto(
                        id = itemObject.optNullableString(KEY_ID),
                        title = itemObject.optNullableString(KEY_TITLE),
                        description = itemObject.optNullableString(KEY_DESCRIPTION),
                        likeCount = itemObject.optNullableInt(KEY_LIKE_COUNT),
                        author = itemObject.optNullableString(KEY_AUTHOR),
                        mediaType = itemObject.optNullableString(KEY_MEDIA_TYPE),
                        imageUrl = itemObject.optNullableString(KEY_IMAGE_URL),
                        videoUrl = itemObject.optNullableString(KEY_VIDEO_URL),
                        coverUrl = itemObject.optNullableString(KEY_COVER_URL),
                        coverHeightDp = itemObject.optNullableInt(KEY_COVER_HEIGHT_DP)
                    )
                )
            }
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (isNull(key) || !has(key)) return null
        return optString(key).takeIf { it.isNotBlank() }
    }

    private fun JSONObject.optNullableInt(key: String): Int? {
        if (isNull(key) || !has(key)) return null
        return optInt(key)
    }

    companion object {
        private const val KEY_DATA = "data"
        private const val KEY_ITEMS = "items"
        private const val KEY_LIST = "list"
        private const val KEY_NOTES = "notes"
        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_DESCRIPTION = "description"
        private const val KEY_LIKE_COUNT = "likeCount"
        private const val KEY_AUTHOR = "author"
        private const val KEY_MEDIA_TYPE = "mediaType"
        private const val KEY_IMAGE_URL = "imageUrl"
        private const val KEY_VIDEO_URL = "videoUrl"
        private const val KEY_COVER_URL = "coverUrl"
        private const val KEY_COVER_HEIGHT_DP = "coverHeightDp"
    }
}
