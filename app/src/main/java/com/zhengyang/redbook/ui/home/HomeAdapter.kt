package com.zhengyang.redbook.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import coil.ImageLoader
import com.zhengyang.redbook.databinding.ItemNoteBinding

/**
 * 首页卡片列表适配器。
 *
 * 负责创建 [HomeViewHolder]、提交卡片数据以及转发点击事件。
 *
 * @property imageLoader 用于卡片封面和头像加载的图片加载器。
 */
class HomeAdapter(
    private val imageLoader: ImageLoader
) : ListAdapter<HomeCardItem, HomeViewHolder>(HomeDiffCallback()) {

    /**
     * 列表项点击回调。
     */
    var onItemClick: ((HomeCardItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HomeViewHolder(binding, imageLoader)
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    /**
     * 条目回收时主动释放图片和播放器相关资源。
     */
    override fun onViewRecycled(holder: HomeViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    /**
     * 条目离开窗口时执行轻量清理。
     */
    override fun onViewDetachedFromWindow(holder: HomeViewHolder) {
        holder.onDetached()
        super.onViewDetachedFromWindow(holder)
    }

    /**
     * 首页卡片 Diff 计算器。
     */
    private class HomeDiffCallback : DiffUtil.ItemCallback<HomeCardItem>() {
        override fun areItemsTheSame(oldItem: HomeCardItem, newItem: HomeCardItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HomeCardItem, newItem: HomeCardItem): Boolean {
            return oldItem == newItem
        }
    }
}
