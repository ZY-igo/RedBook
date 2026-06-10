package com.zhengyang.redbook.ui.home

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.dispose
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import coil.size.ViewSizeResolver
import com.zhengyang.redbook.R
import com.zhengyang.redbook.databinding.ItemNoteBinding
import com.zhengyang.redbook.utils.AppLogger
import com.zhengyang.redbook.utils.dpToPx

/**
 * 首页卡片 ViewHolder。
 *
 * 负责处理正常卡片、视频卡片和骨架卡片的绑定与资源清理。
 */
class HomeViewHolder(
    private val binding: ItemNoteBinding,
    private val imageLoader: ImageLoader
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        private const val TAG = "HomeAvatar"
    }

    /**
     * 绑定一个首页卡片。
     *
     * @param item 当前需要展示的卡片数据。
     * @param onItemClick 条目点击回调。
     */
    fun bind(item: HomeCardItem, onItemClick: ((HomeCardItem) -> Unit)?) {
        if (item.isSkeleton) {
            bindSkeleton(item)
            return
        }

        binding.coverContainer.layoutParams = binding.coverContainer.layoutParams.apply {
            height = item.coverHeightDp.dpToPx()
        }
        clearPlaceholderState()
        binding.coverContainer.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor(item.startColorHex), Color.parseColor(item.endColorHex))
        )
        binding.tvMediaBadge.text =
            if (item.mediaType == HomeCardItem.MediaType.VIDEO) "VIDEO" else item.badge
        binding.tvTitle.text = item.title
        binding.tvAuthorName.text = item.author
        binding.tvLikeCount.text = item.likeCount
        bindAvatar(item.author, item.avatarUrl, item.avatarColorHex)

        if (item.mediaType == HomeCardItem.MediaType.VIDEO) {
            bindVideo(item)
        } else {
            bindImage(item)
        }

        binding.root.setOnClickListener { onItemClick?.invoke(item) }
    }

    /**
     * 条目离开窗口时的轻量回调。
     */
    fun onDetached() = Unit

    /**
     * 主动释放当前 ViewHolder 持有的图片和播放器资源。
     */
    fun recycle() {
        binding.root.setOnClickListener(null)
        binding.ivCover.dispose()
        binding.ivAvatar.dispose()
        binding.ivCover.setImageDrawable(null)
        binding.ivAvatar.setImageDrawable(null)
        binding.ivCover.background = null
        binding.ivAvatar.background = null
        binding.videoPlayerView.player = null
    }

    /**
     * 绑定图片类卡片。
     */
    private fun bindImage(item: HomeCardItem) {
        binding.videoPlayerView.visibility = View.GONE
        binding.ivPlayIndicator.visibility = View.GONE
        loadCover(item.imageUrl ?: item.videoCoverUrl)
    }

    /**
     * 绑定视频类卡片。
     *
     * 这里仍然只加载封面，不直接启动播放器。
     */
    private fun bindVideo(item: HomeCardItem) {
        binding.videoPlayerView.visibility = View.GONE
        binding.ivPlayIndicator.visibility = View.VISIBLE
        loadCover(item.videoCoverUrl ?: item.imageUrl)
    }

    /**
     * 绑定骨架屏占位卡片。
     */
    private fun bindSkeleton(item: HomeCardItem) {
        binding.coverContainer.layoutParams = binding.coverContainer.layoutParams.apply {
            height = item.coverHeightDp.dpToPx()
        }
        val placeholderColor = binding.root.context.getColor(R.color.xhs_card_soft)
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

    /**
     * 清理骨架屏给文本和图片留下的占位背景。
     */
    private fun clearPlaceholderState() {
        binding.ivCover.background = null
        binding.ivAvatar.background = null
        binding.tvTitle.background = null
        binding.tvAuthorName.background = null
        binding.tvLikeCount.background = null
        binding.tvMediaBadge.visibility = View.VISIBLE
    }

    /**
     * 绑定作者头像。
     *
     * 如果远程头像加载失败，则回退到作者首字母头像。
     */
    private fun bindAvatar(author: String, avatarUrl: String?, avatarColorHex: String) {
        AppLogger.d(TAG, "bind card avatar, author=$author, avatarUrl=$avatarUrl")
        binding.ivAvatar.dispose()
        val avatarBackground = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(avatarColorHex))
        }
        binding.tvAvatar.text = author.take(1)
        binding.tvAvatar.background = avatarBackground
        binding.ivAvatar.background = avatarBackground.constantState?.newDrawable()?.mutate()
        binding.ivAvatar.clipToOutline = true
        binding.ivAvatar.tag = avatarUrl
        if (avatarUrl.isNullOrBlank()) {
            binding.ivAvatar.visibility = View.GONE
            binding.ivAvatar.setImageDrawable(null)
            binding.tvAvatar.visibility = View.VISIBLE
            return
        }

        val avatarSize = binding.ivAvatar.layoutParams.width.takeIf { it > 0 } ?: 40.dpToPx()
        imageLoader.enqueue(
            ImageRequest.Builder(binding.ivAvatar.context)
                .data(avatarUrl)
                .size(avatarSize, avatarSize)
                .scale(Scale.FILL)
                .precision(Precision.INEXACT)
                .listener(
                    onError = { _, result ->
                        AppLogger.w(
                            TAG,
                            "avatar request error, author=$author, avatarUrl=$avatarUrl, message=${result.throwable.message}",
                            result.throwable
                        )
                    }
                )
                .target(
                    onStart = {
                        if (binding.ivAvatar.tag != avatarUrl) return@target
                        binding.ivAvatar.visibility = View.GONE
                        binding.tvAvatar.visibility = View.VISIBLE
                    },
                    onSuccess = { result ->
                        if (binding.ivAvatar.tag != avatarUrl) return@target
                        binding.ivAvatar.setImageDrawable(result)
                        binding.ivAvatar.visibility = View.VISIBLE
                        binding.tvAvatar.visibility = View.GONE
                    },
                    onError = {
                        if (binding.ivAvatar.tag != avatarUrl) return@target
                        binding.ivAvatar.setImageDrawable(null)
                        binding.ivAvatar.visibility = View.GONE
                        binding.tvAvatar.visibility = View.VISIBLE
                    }
                )
                .build()
        )
    }

    /**
     * 创建文本占位块背景。
     */
    private fun createTextPlaceholder(): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = 4.dpToPx().toFloat()
            setColor(binding.root.context.getColor(R.color.xhs_card_soft))
        }
    }

    /**
     * 加载卡片封面图。
     */
    private fun loadCover(url: String?) {
        imageLoader.enqueue(
            ImageRequest.Builder(binding.ivCover.context)
                .data(url)
                .target(binding.ivCover)
                .size(ViewSizeResolver(binding.ivCover))
                .scale(Scale.FILL)
                .precision(Precision.INEXACT)
                .placeholder(R.drawable.bg_xhs_image_placeholder)
                .error(R.drawable.bg_xhs_image_error)
                .fallback(R.drawable.bg_xhs_image_placeholder)
                .crossfade(true)
                .build()
        )
    }
}
