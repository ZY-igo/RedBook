/**
 * 文件说明： RemoteNoteMockFactory.kt
 * 作用： 封装远程数据访问相关逻辑，包括接口配置、请求行为和响应解析。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.data.remote

import com.zhengyang.redbook.data.remote.model.RemoteNoteDto
import javax.inject.Inject

class RemoteNoteMockFactory @Inject constructor() {

    fun create(): List<RemoteNoteDto> {
        return listOf(
            RemoteNoteDto(
                id = "remote_1",
                title = "通勤穿搭这条真的很出片",
                description = "单图帖子",
                likeCount = 1280,
                author = "Momo",
                mediaType = "image",
                imageUrl = "https://picsum.photos/seed/redbook-image-1/720/1080",
                coverUrl = "https://picsum.photos/seed/redbook-image-1/720/1080",
                coverHeightDp = 280
            ),
            RemoteNoteDto(
                id = "remote_2",
                title = "Citywalk 夜景 vlog",
                description = "视频帖子",
                likeCount = 4521,
                author = "HaoTrip",
                mediaType = "video",
                videoUrl = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4",
                coverUrl = "https://picsum.photos/seed/redbook-video-1/720/1080",
                coverHeightDp = 300
            ),
            RemoteNoteDto(
                id = "remote_3",
                title = "低饱和妆容近拍细节",
                description = "单图帖子",
                likeCount = 876,
                author = "卷卷化妆间",
                mediaType = "image",
                imageUrl = "https://picsum.photos/seed/redbook-image-2/720/960",
                coverUrl = "https://picsum.photos/seed/redbook-image-2/720/960",
                coverHeightDp = 240
            )
        )
    }
}
