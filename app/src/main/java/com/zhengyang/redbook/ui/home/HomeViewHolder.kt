package com.zhengyang.redbook.ui.home

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.dispose
import coil.request.ImageRequest
import coil.size.ViewSizeResolver
import com.zhengyang.redbook.R
import com.zhengyang.redbook.databinding.ItemNoteBinding
import com.zhengyang.redbook.utils.AppLogger
import com.zhengyang.redbook.utils.applyCircleAvatarDefaults
import com.zhengyang.redbook.utils.applyFeedCoverDefaults
import com.zhengyang.redbook.utils.dpToPx

/**
 * 首页卡片 ViewHolder。
 *
 * 负责把单张首页卡片真正绑定到视图上：
 * 1. 骨架卡的占位状态
 * 2. 普通图文卡的封面、标题、作者、点赞数
 * 3. 视频卡的封面和播放角标
 * 4. 作者头像的远程加载和文字兜底
 */
class HomeViewHolder(
    private val binding: ItemNoteBinding,
    private val imageLoader: ImageLoader
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        private const val TAG = "HomeViewHolder"
    }

    fun bind(item: HomeCardItem, onItemClick: ((HomeCardItem) -> Unit)?) {
        AppLogger.d(
            TAG,
            "bind: id=${item.id}, isSkeleton=${item.isSkeleton}, mediaType=${item.mediaType}, title=${item.title}, author=${item.author}, coverHeightDp=${item.coverHeightDp}, imageUrl=${item.imageUrl}, videoCoverUrl=${item.videoCoverUrl}, avatarUrl=${item.avatarUrl}"
        )
        if (item.isSkeleton) {
            AppLogger.d(TAG, "bind: route=skeleton, id=${item.id}")
            bindSkeleton(item)
            return
        }

        binding.coverContainer.layoutParams = binding.coverContainer.layoutParams.apply {
            height = item.coverHeightDp.dpToPx()
        }
        AppLogger.d(
            TAG,
            "bind: applied cover height px=${binding.coverContainer.layoutParams.height}, id=${item.id}"
        )

        clearPlaceholderState()

        binding.coverContainer.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor(item.startColorHex), Color.parseColor(item.endColorHex))
        )
        AppLogger.d(
            TAG,
            "bind: gradient start=${item.startColorHex}, end=${item.endColorHex}, id=${item.id}"
        )

        binding.tvMediaBadge.text =
            if (item.mediaType == HomeCardItem.MediaType.VIDEO) "VIDEO" else item.badge
        binding.tvTitle.text = item.title
        binding.tvAuthorName.text = item.author
        binding.tvLikeCount.text = item.likeCount
        AppLogger.d(
            TAG,
            "bind: text updated badge=${binding.tvMediaBadge.text}, likeCount=${item.likeCount}, id=${item.id}"
        )

        bindAvatar(item.author, item.avatarUrl, item.avatarColorHex)

        if (item.mediaType == HomeCardItem.MediaType.VIDEO) {
            AppLogger.d(TAG, "bind: route=video, id=${item.id}")
            bindVideo(item)
        } else {
            AppLogger.d(TAG, "bind: route=image, id=${item.id}")
            bindImage(item)
        }

        binding.root.setOnClickListener {
            AppLogger.d(TAG, "bind onClick: id=${item.id}, title=${item.title}")
            onItemClick?.invoke(item)
        }
    }

    fun onDetached() {
        AppLogger.d(TAG, "onDetached: holder=${hashCode()}")
    }

    fun recycle() {
        AppLogger.d(
            TAG,
            "recycle: clear click listener, dispose image requests, clear drawables, release player reference"
        )
        binding.root.setOnClickListener(null)
        binding.ivCover.dispose()
        binding.ivAvatar.dispose()
        binding.ivCover.setImageDrawable(null)
        binding.ivAvatar.setImageDrawable(null)
        binding.ivCover.background = null
        binding.ivAvatar.background = null
        binding.videoPlayerView.player = null
    }

    private fun bindImage(item: HomeCardItem) {
        AppLogger.d(
            TAG,
            "bindImage: id=${item.id}, preferredImageUrl=${item.imageUrl}, fallbackVideoCoverUrl=${item.videoCoverUrl}"
        )
        binding.videoPlayerView.visibility = View.GONE
        binding.ivPlayIndicator.visibility = View.GONE
        loadCover(item.imageUrl ?: item.videoCoverUrl)
    }

    private fun bindVideo(item: HomeCardItem) {
        AppLogger.d(
            TAG,
            "bindVideo: id=${item.id}, preferredVideoCoverUrl=${item.videoCoverUrl}, fallbackImageUrl=${item.imageUrl}"
        )
        binding.videoPlayerView.visibility = View.GONE
        binding.ivPlayIndicator.visibility = View.VISIBLE
        loadCover(item.videoCoverUrl ?: item.imageUrl)
    }

    private fun bindSkeleton(item: HomeCardItem) {
        AppLogger.d(TAG, "bindSkeleton: id=${item.id}, coverHeightDp=${item.coverHeightDp}")
        binding.coverContainer.layoutParams = binding.coverContainer.layoutParams.apply {
            height = item.coverHeightDp.dpToPx()
        }
        val placeholderColor = binding.root.context.getColor(R.color.xhs_card_soft)
        AppLogger.d(TAG, "bindSkeleton: placeholderColor=$placeholderColor")
        binding.root.setOnClickListener(null)
        binding.coverContainer.background = GradientDrawable().apply {
            setColor(placeholderColor)
        }
        binding.ivCover.setImageDrawable(null)
        binding.ivCover.background = GradientDrawable().apply {
            setColor(placeholderColor)
        }
        binding.videoPlayerView.visibility = View.GONE
        binding.ivPlayIndicator.visibility = View.GONE
        binding.tvMediaBadge.visibility = View.INVISIBLE
        binding.tvTitle.text = " "
        binding.tvAuthorName.text = " "
        binding.tvLikeCount.text = " "
        binding.ivAvatar.visibility = View.GONE
        binding.ivAvatar.setImageDrawable(null)
        binding.tvAvatar.text = ""
        binding.tvAvatar.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(placeholderColor)
        }
        binding.tvAvatar.visibility = View.VISIBLE
        binding.tvTitle.background = createTextPlaceholder()
        binding.tvAuthorName.background = createTextPlaceholder()
        binding.tvLikeCount.background = createTextPlaceholder()
    }

    private fun clearPlaceholderState() {
        AppLogger.d(TAG, "clearPlaceholderState: reset placeholder backgrounds and badge visibility")
        binding.ivCover.background = null
        binding.ivAvatar.background = null
        binding.tvTitle.background = null
        binding.tvAuthorName.background = null
        binding.tvLikeCount.background = null
        binding.tvMediaBadge.visibility = View.VISIBLE
    }

    private fun bindAvatar(author: String, avatarUrl: String?, avatarColorHex: String) {
        AppLogger.d(
            TAG,
            "bindAvatar: author=$author, avatarUrl=$avatarUrl, avatarColorHex=$avatarColorHex"
        )
        binding.ivAvatar.dispose()
        val avatarBackground = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(avatarColorHex))
        }
        AppLogger.d(TAG, "bindAvatar: prepared fallback background for author=$author")

        binding.tvAvatar.text = author.take(1)
        binding.tvAvatar.background = avatarBackground
        binding.ivAvatar.background = avatarBackground.constantState?.newDrawable()?.mutate()
        binding.ivAvatar.tag = avatarUrl
        AppLogger.d(TAG, "bindAvatar: set image tag=$avatarUrl")

        if (avatarUrl.isNullOrBlank()) {
            AppLogger.d(TAG, "bindAvatar: avatarUrl empty, show text fallback author=$author")
            binding.ivAvatar.visibility = View.GONE
            binding.ivAvatar.setImageDrawable(null)
            binding.tvAvatar.visibility = View.VISIBLE
            return
        }

        val avatarSize = binding.ivAvatar.layoutParams.width.takeIf { it > 0 } ?: 40.dpToPx()
        AppLogger.d(
            TAG,
            "bindAvatar: enqueue avatar request author=$author, avatarSize=$avatarSize, avatarUrl=$avatarUrl"
        )
        imageLoader.enqueue(
            ImageRequest.Builder(binding.ivAvatar.context)
                .data(avatarUrl)
                .size(avatarSize, avatarSize)
                .applyCircleAvatarDefaults()
                .listener(
                    onStart = { request ->
                        AppLogger.d(
                            TAG,
                            "bindAvatar listener onStart: author=$author, avatarUrl=$avatarUrl, requestData=${request.data}"
                        )
                    },
                    onSuccess = { request, _ ->
                        AppLogger.d(
                            TAG,
                            "bindAvatar listener onSuccess: author=$author, avatarUrl=$avatarUrl, requestData=${request.data}"
                        )
                    },
                    onError = { request, result ->
                        AppLogger.w(
                            TAG,
                            "bindAvatar listener onError: author=$author, avatarUrl=$avatarUrl, requestData=${request.data}, message=${result.throwable.message}",
                            result.throwable
                        )
                    }
                )
                .target(
                    onStart = {
                        if (binding.ivAvatar.tag != avatarUrl) {
                            AppLogger.d(
                                TAG,
                                "bindAvatar target onStart ignored: stale callback, expectedTag=${binding.ivAvatar.tag}, callbackUrl=$avatarUrl"
                            )
                            return@target
                        }
                        AppLogger.d(
                            TAG,
                            "bindAvatar target onStart: keep text fallback visible, author=$author"
                        )
                        binding.ivAvatar.visibility = View.GONE
                        binding.tvAvatar.visibility = View.VISIBLE
                    },
                    onSuccess = { result ->
                        if (binding.ivAvatar.tag != avatarUrl) {
                            AppLogger.d(
                                TAG,
                                "bindAvatar target onSuccess ignored: stale callback, expectedTag=${binding.ivAvatar.tag}, callbackUrl=$avatarUrl"
                            )
                            return@target
                        }
                        AppLogger.d(
                            TAG,
                            "bindAvatar target onSuccess: show remote avatar, author=$author, avatarUrl=$avatarUrl"
                        )
                        binding.ivAvatar.setImageDrawable(result)
                        binding.ivAvatar.visibility = View.VISIBLE
                        binding.tvAvatar.visibility = View.GONE
                    },
                    onError = {
                        if (binding.ivAvatar.tag != avatarUrl) {
                            AppLogger.d(
                                TAG,
                                "bindAvatar target onError ignored: stale callback, expectedTag=${binding.ivAvatar.tag}, callbackUrl=$avatarUrl"
                            )
                            return@target
                        }
                        AppLogger.d(
                            TAG,
                            "bindAvatar target onError: fallback to text avatar, author=$author, avatarUrl=$avatarUrl"
                        )
                        binding.ivAvatar.setImageDrawable(null)
                        binding.ivAvatar.visibility = View.GONE
                        binding.tvAvatar.visibility = View.VISIBLE
                    }
                )
                .build()
        )
    }

    private fun createTextPlaceholder(): GradientDrawable {
        AppLogger.d(TAG, "createTextPlaceholder: create rounded rectangle placeholder")
        return GradientDrawable().apply {
            cornerRadius = 4.dpToPx().toFloat()
            setColor(binding.root.context.getColor(R.color.xhs_card_soft))
        }
    }

    private fun loadCover(url: String?) {
        AppLogger.d(TAG, "loadCover: url=$url")
        imageLoader.enqueue(
            ImageRequest.Builder(binding.ivCover.context)
                .data(url)
                .target(binding.ivCover)
                .listener(
                    onStart = { request ->
                        AppLogger.d(
                            TAG,
                            "loadCover listener onStart: requestData=${request.data}, viewWidth=${binding.ivCover.width}, viewHeight=${binding.ivCover.height}"
                        )
                    },
                    onSuccess = { request, _ ->
                        AppLogger.d(
                            TAG,
                            "loadCover listener onSuccess: requestData=${request.data}, imageView=${binding.ivCover.hashCode()}"
                        )
                    },
                    onError = { request, result ->
                        AppLogger.w(
                            TAG,
                            "loadCover listener onError: requestData=${request.data}, message=${result.throwable.message}",
                            result.throwable
                        )
                    }
                )
                .size(ViewSizeResolver(binding.ivCover))
                .applyFeedCoverDefaults()
                .build()
        )
    }
}
