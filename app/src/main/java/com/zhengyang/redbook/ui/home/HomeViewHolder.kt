/**
 * 文件说明：HomeViewHolder.kt
 * 作用：封装首页卡片列表项视图的持有、绑定与回收逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.home

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.dispose
import coil.request.ImageRequest
import com.zhengyang.redbook.R
import com.zhengyang.redbook.databinding.ItemNoteBinding
import com.zhengyang.redbook.utils.AppLogger
import com.zhengyang.redbook.utils.dpToPx

/**
 * 首页卡片视图持有者
 *
 * 负责根据卡片类型绑定图文、视频和骨架屏三类展示状态，
 * 并在回收时释放图片与播放器相关资源。
 */
class HomeViewHolder(
    /** 首页卡片条目视图绑定对象。 */
    private val binding: ItemNoteBinding,
    private val imageLoader: ImageLoader
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        private const val TAG = "HomeAvatar"
    }

    /**
     * 绑定首页卡片数据。
     *
     * @param item 当前需要渲染的首页卡片模型。
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
     * 条目脱离窗口时的轻量回调。
     */
    fun onDetached() = Unit

    /**
     * 回收条目资源。
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
     * 绑定图文卡片展示。
     *
     * @param item 当前图文卡片模型。
     */
    private fun bindImage(item: HomeCardItem) {
        binding.videoPlayerView.visibility = View.GONE
        binding.ivPlayIndicator.visibility = View.GONE
        loadCover(item.imageUrl ?: item.videoCoverUrl)
    }

    /**
     * 绑定视频卡片展示。
     *
     * @param item 当前视频卡片模型。
     */
    private fun bindVideo(item: HomeCardItem) {
        binding.videoPlayerView.visibility = View.GONE
        binding.ivPlayIndicator.visibility = View.VISIBLE
        loadCover(item.videoCoverUrl ?: item.imageUrl)
    }

    /**
     * 绑定骨架屏占位状态。
     *
     * @param item 当前骨架卡片模型。
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
     * 清除骨架态遗留的占位背景。
     */
    private fun clearPlaceholderState() {
        binding.ivCover.background = null
        binding.ivAvatar.background = null
        binding.tvTitle.background = null
        binding.tvAuthorName.background = null
        binding.tvLikeCount.background = null
        binding.tvMediaBadge.visibility = View.VISIBLE
    }

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
        imageLoader.enqueue(
            ImageRequest.Builder(binding.ivAvatar.context)
                .data(avatarUrl)
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
                        if (binding.ivAvatar.tag != avatarUrl) {
                            AppLogger.d(TAG, "skip stale avatar onStart, author=$author, avatarUrl=$avatarUrl")
                            return@target
                        }
                        binding.ivAvatar.visibility = View.GONE
                        binding.tvAvatar.visibility = View.VISIBLE
                    },
                    onSuccess = { result ->
                        if (binding.ivAvatar.tag != avatarUrl) {
                            AppLogger.d(TAG, "skip stale avatar onSuccess, author=$author, avatarUrl=$avatarUrl")
                            return@target
                        }
                        AppLogger.d(TAG, "avatar load success, author=$author, avatarUrl=$avatarUrl")
                        binding.ivAvatar.setImageDrawable(result)
                        binding.ivAvatar.visibility = View.VISIBLE
                        binding.tvAvatar.visibility = View.GONE
                    },
                    onError = {
                        if (binding.ivAvatar.tag != avatarUrl) {
                            AppLogger.d(TAG, "skip stale avatar onError, author=$author, avatarUrl=$avatarUrl")
                            return@target
                        }
                        AppLogger.w(TAG, "avatar load error, author=$author, avatarUrl=$avatarUrl")
                        binding.ivAvatar.setImageDrawable(null)
                        binding.ivAvatar.visibility = View.GONE
                        binding.tvAvatar.visibility = View.VISIBLE
                    }
                )
                .build()
        )
    }

    /**
     * 创建文本占位背景。
     *
     * @return 用于骨架屏文本区域的圆角纯色背景。
     */
    private fun createTextPlaceholder(): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = 4.dpToPx().toFloat()
            setColor(binding.root.context.getColor(R.color.xhs_card_soft))
        }
    }

    private fun loadCover(url: String?) {
        imageLoader.enqueue(
            ImageRequest.Builder(binding.ivCover.context)
                .data(url)
                .target(binding.ivCover)
                .placeholder(R.drawable.bg_xhs_image_placeholder)
                .error(R.drawable.bg_xhs_image_error)
                .fallback(R.drawable.bg_xhs_image_placeholder)
                .crossfade(true)
                .build()
        )
    }
}
