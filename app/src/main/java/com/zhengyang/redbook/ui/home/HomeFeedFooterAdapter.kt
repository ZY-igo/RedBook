package com.zhengyang.redbook.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.zhengyang.redbook.databinding.ItemHomeFeedFooterBinding

/**
 * 首页列表底部状态适配器。
 *
 * 用于展示“加载中”“离线缓存”“已经到底”等尾部提示。
 */
class HomeFeedFooterAdapter : RecyclerView.Adapter<HomeFeedFooterAdapter.FooterViewHolder>() {

    /**
     * 当前底部视图状态；为空时表示不展示 footer。
     */
    private var footerState: FooterState? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FooterViewHolder {
        val binding = ItemHomeFeedFooterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FooterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FooterViewHolder, position: Int) {
        holder.bind(requireNotNull(footerState))
    }

    override fun getItemViewType(position: Int): Int = VIEW_TYPE

    override fun getItemCount(): Int = if (footerState == null) 0 else 1

    /**
     * 保证瀑布流布局里 footer 始终横跨整行。
     */
    override fun onViewAttachedToWindow(holder: FooterViewHolder) {
        super.onViewAttachedToWindow(holder)
        val layoutParams = holder.itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams
        layoutParams?.isFullSpan = true
    }

    /**
     * 提交新的底部状态。
     *
     * @param message 底部文案；为空且不在加载时会隐藏 footer。
     * @param isLoading 当前是否在显示加载中状态。
     */
    fun submitState(message: String?, isLoading: Boolean) {
        val nextState = if (message == null && !isLoading) null else FooterState(
            message = message.orEmpty(),
            isLoading = isLoading
        )
        val hadItem = footerState != null
        val hasItem = nextState != null
        footerState = nextState

        if (hadItem || hasItem) {
            notifyDataSetChanged()
        }
    }

    /**
     * 底部提示项 ViewHolder。
     */
    class FooterViewHolder(
        private val binding: ItemHomeFeedFooterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * 根据状态刷新 footer 视图。
         *
         * @param state 当前底部状态。
         */
        fun bind(state: FooterState) {
            binding.footerProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.footerMessage.text = state.message
            binding.footerMessage.visibility =
                if (state.message.isBlank() && state.isLoading) View.GONE else View.VISIBLE
        }
    }

    /**
     * 底部展示状态。
     *
     * @property message 底部文案。
     * @property isLoading 是否展示加载进度。
     */
    data class FooterState(
        val message: String,
        val isLoading: Boolean
    )

    companion object {
        /**
         * footer 专用 view type。
         */
        const val VIEW_TYPE = Int.MAX_VALUE
    }
}
