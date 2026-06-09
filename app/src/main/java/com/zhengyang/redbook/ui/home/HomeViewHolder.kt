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

class HomeViewHolder(
    private val binding: ItemNoteBinding,
    private val imageLoader: ImageLoader
) : RecyclerView.ViewHolder(binding.root) {

    companion object {
        private const val TAG = "HomeAvatar"
    }

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

    fun onDetached() = Unit

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

    private fun bindImage(item: HomeCardItem) {
        binding.videoPlayerView.visibility = View.GONE
        binding.ivPlayIndicator.visibility = View.GONE
        loadCover(item.imageUrl ?: item.videoCoverUrl)
    }

    private fun bindVideo(item: HomeCardItem) {
        binding.videoPlayerView.visibility = View.GONE
        binding.ivPlayIndicator.visibility = View.VISIBLE
        loadCover(item.videoCoverUrl ?: item.imageUrl)
    }

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
