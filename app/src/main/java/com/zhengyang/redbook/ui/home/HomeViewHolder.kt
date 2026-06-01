package com.zhengyang.redbook.ui.home

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zhengyang.redbook.databinding.ItemNoteBinding

class HomeViewHolder(
    private val binding: ItemNoteBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: HomeCardItem) {
        val context = binding.root.context
        binding.coverContainer.layoutParams = (binding.coverContainer.layoutParams as ViewGroup.LayoutParams).apply {
            height = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                item.coverHeightDp.toFloat(),
                context.resources.displayMetrics
            ).toInt()
        }
        binding.coverContainer.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor(item.startColorHex), Color.parseColor(item.endColorHex))
        ).apply {
            cornerRadius = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                0f,
                context.resources.displayMetrics
            )
        }
        binding.tvTitle.text = item.title
        binding.tvAuthorName.text = item.author
        binding.tvLikeCount.text = item.likeCount
        binding.tvAvatar.text = item.author.take(1)
        binding.tvAvatar.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(item.avatarColorHex))
        }
    }
}
