/**
 * 文件说明： HomeViewHolder.kt
 * 作用： 承载首页相关的界面状态与交互逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.home

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.zhengyang.redbook.databinding.ItemNoteBinding
import com.zhengyang.redbook.utils.dpToPx

/**
 * 首页列表项的 ViewHolder
 * 负责绑定和显示单个内容卡片的数据
 */
class HomeViewHolder(
    private val binding: ItemNoteBinding
) : RecyclerView.ViewHolder(binding.root) {

    /**
     * 绑定数据到 ViewHolder
     * @param item 要显示的内容卡片数据
     * @param onItemClick 点击事件的回调函数
     */
    fun bind(item: HomeCardItem, onItemClick: ((HomeCardItem) -> Unit)?) {
        // 设置封面容器的高度
        binding.coverContainer.layoutParams = (binding.coverContainer.layoutParams as ViewGroup.LayoutParams).apply {
            height = item.coverHeightDp.dpToPx()
        }
        // 设置封面背景为渐变色（从左上角到右下角）
        binding.coverContainer.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor(item.startColorHex), Color.parseColor(item.endColorHex))
        ).apply {
            cornerRadius = 0f
        }
        // 设置媒体类型标签：视频显示"VIDEO"，图片显示对应的badge
        binding.tvMediaBadge.text = if (item.mediaType == HomeCardItem.MediaType.VIDEO) "VIDEO" else item.badge
        // 设置标题文本
        binding.tvTitle.text = item.title
        // 设置作者名称
        binding.tvAuthorName.text = item.author
        // 设置点赞数
        binding.tvLikeCount.text = item.likeCount
        // 设置头像文字（取作者名称的第一个字符）
        binding.tvAvatar.text = item.author.take(1)
        // 设置头像背景为圆形的纯色背景
        binding.tvAvatar.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(item.avatarColorHex))
        }

        // 根据媒体类型选择不同的绑定方式
        if (item.mediaType == HomeCardItem.MediaType.VIDEO) {
            bindVideo(item)
        } else {
            bindImage(item)
        }

        // 设置整个卡片的点击事件
        binding.root.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    /**
     * 当 ViewHolder 从窗口分离时调用
     * 用于清理资源或停止动画
     */
    fun onDetached() = Unit

    /**
     * 回收 ViewHolder 时的清理工作
     * 清除监听器和释放图片资源，防止内存泄漏
     */
    fun recycle() {
        // 清除点击监听器，防止内存泄漏
        binding.root.setOnClickListener(null)
        // 清除封面图片，释放内存
        binding.ivCover.setImageDrawable(null)
        // 清除视频播放器引用，释放播放器资源
        binding.videoPlayerView.player = null
    }

    /**
     * 绑定图片类型的内容
     * 隐藏视频播放器和播放指示器，只显示封面图
     */
    private fun bindImage(item: HomeCardItem) {
        // 隐藏视频播放器视图
        binding.videoPlayerView.visibility = View.GONE
        // 隐藏播放指示器图标
        binding.ivPlayIndicator.visibility = View.GONE
        // 使用 Coil 图片加载库加载封面图
        binding.ivCover.load(item.imageUrl ?: item.videoCoverUrl) {
            // 启用淡入淡出效果
            crossfade(true)
        }
    }

    /**
     * 绑定视频类型的内容
     * 显示播放指示器，加载视频封面图
     * 注意：视频播放器当前被隐藏，只显示封面
     */
    private fun bindVideo(item: HomeCardItem) {
        // 隐藏视频播放器视图（当前实现中视频播放器不直接显示）
        binding.videoPlayerView.visibility = View.GONE
        // 显示播放指示器图标，提示用户这是视频内容
        binding.ivPlayIndicator.visibility = View.VISIBLE
        // 使用 Coil 图片加载库加载视频封面图
        binding.ivCover.load(item.videoCoverUrl ?: item.imageUrl) {
            // 启用淡入淡出效果
            crossfade(true)
        }
    }
}
