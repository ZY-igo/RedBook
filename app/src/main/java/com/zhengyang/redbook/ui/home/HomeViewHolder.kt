package com.zhengyang.redbook.ui.home

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.zhengyang.redbook.R
import com.zhengyang.redbook.databinding.ItemNoteBinding
import com.zhengyang.redbook.utils.dpToPx

class HomeViewHolder(
    private val binding: ItemNoteBinding
) : RecyclerView.ViewHolder(binding.root) {

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
        binding.tvAvatar.text = item.author.take(1)
        binding.tvAvatar.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(item.avatarColorHex))
        }

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
        binding.ivCover.setImageDrawable(null)
        binding.ivCover.background = null
        binding.videoPlayerView.player = null
    }

    private fun bindImage(item: HomeCardItem) {
        binding.videoPlayerView.visibility = View.GONE
        binding.ivPlayIndicator.visibility = View.GONE
        binding.ivCover.load(item.imageUrl ?: item.videoCoverUrl) {
            placeholder(R.drawable.bg_xhs_image_placeholder)
            error(R.drawable.bg_xhs_image_error)
            fallback(R.drawable.bg_xhs_image_placeholder)
            crossfade(true)
        }
    }

    private fun bindVideo(item: HomeCardItem) {
        binding.videoPlayerView.visibility = View.GONE
        binding.ivPlayIndicator.visibility = View.VISIBLE
        binding.ivCover.load(item.videoCoverUrl ?: item.imageUrl) {
            placeholder(R.drawable.bg_xhs_image_placeholder)
            error(R.drawable.bg_xhs_image_error)
            fallback(R.drawable.bg_xhs_image_placeholder)
            crossfade(true)
        }
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
        binding.tvAvatar.text = ""
        binding.tvAvatar.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(placeholderColor)
        }
        binding.tvTitle.background = createTextPlaceholder()
        binding.tvAuthorName.background = createTextPlaceholder()
        binding.tvLikeCount.background = createTextPlaceholder()
    }

    private fun clearPlaceholderState() {
        binding.ivCover.background = null
        binding.tvTitle.background = null
        binding.tvAuthorName.background = null
        binding.tvLikeCount.background = null
        binding.tvMediaBadge.visibility = View.VISIBLE
    }

    private fun createTextPlaceholder(): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = 4.dpToPx().toFloat()
            setColor(binding.root.context.getColor(R.color.xhs_card_soft))
        }
    }
}
