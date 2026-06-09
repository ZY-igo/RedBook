/**
 * 文件说明：HomeAdapter.kt
 * 作用：负责首页卡片列表项的组装、复用与绑定调度。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import coil.ImageLoader
import com.zhengyang.redbook.databinding.ItemNoteBinding

/**
 * 首页卡片列表适配器
 *
 * 负责创建 [HomeViewHolder]、提交卡片数据并转发点击事件，
 * 同时在视图回收和脱离窗口时触发持有者清理逻辑。
 */
class HomeAdapter(
    private val imageLoader: ImageLoader
) : ListAdapter<HomeCardItem, HomeViewHolder>(HomeDiffCallback()) {

    /** 卡片点击回调，由外层界面决定点击后的跳转行为。 */
    var onItemClick: ((HomeCardItem) -> Unit)? = null

    /**
     * 创建首页卡片视图持有者。
     *
     * @param parent 父容器。
     * @param viewType 当前视图类型。
     * @return 绑定完成布局的首页卡片持有者。
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HomeViewHolder(binding, imageLoader)
    }

    /**
     * 绑定指定位置的首页卡片数据。
     *
     * @param holder 当前条目持有者。
     * @param position 当前绑定位置。
     */
    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    /**
     * 条目视图回收时释放资源。
     *
     * @param holder 当前条目持有者。
     */
    override fun onViewRecycled(holder: HomeViewHolder) {
        holder.recycle()
        super.onViewRecycled(holder)
    }

    /**
     * 条目视图离开窗口时执行轻量清理。
     *
     * @param holder 当前条目持有者。
     */
    override fun onViewDetachedFromWindow(holder: HomeViewHolder) {
        holder.onDetached()
        super.onViewDetachedFromWindow(holder)
    }

    /**
     * 首页卡片差分比较器。
     *
     * 负责判断列表项身份和内容变化，以支持高效刷新。
     */
    private class HomeDiffCallback : DiffUtil.ItemCallback<HomeCardItem>() {
        /**
         * 判断两个列表项是否表示同一条内容。
         */
        override fun areItemsTheSame(oldItem: HomeCardItem, newItem: HomeCardItem): Boolean {
            return oldItem.id == newItem.id
        }

        /**
         * 判断两个列表项内容是否完全一致。
         */
        override fun areContentsTheSame(oldItem: HomeCardItem, newItem: HomeCardItem): Boolean {
            return oldItem == newItem
        }
    }
}
