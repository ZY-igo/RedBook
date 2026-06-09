package com.zhengyang.redbook.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.zhengyang.redbook.databinding.ItemHomeFeedFooterBinding

class HomeFeedFooterAdapter : RecyclerView.Adapter<HomeFeedFooterAdapter.FooterViewHolder>() {

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

    override fun onViewAttachedToWindow(holder: FooterViewHolder) {
        super.onViewAttachedToWindow(holder)
        val layoutParams = holder.itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams
        layoutParams?.isFullSpan = true
    }

    fun submitState(message: String?, isLoading: Boolean) {
        val nextState = if (message == null && !isLoading) null else FooterState(
            message = message.orEmpty(),
            isLoading = isLoading
        )
        val hadItem = footerState != null
        val hasItem = nextState != null
        footerState = nextState
        
        // Post to next frame to avoid RecyclerView scroll callback violations
        if (hadItem || hasItem) {
            notifyDataSetChanged()
        }
    }

    class FooterViewHolder(
        private val binding: ItemHomeFeedFooterBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(state: FooterState) {
            binding.footerProgress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            binding.footerMessage.text = state.message
            binding.footerMessage.visibility =
                if (state.message.isBlank() && state.isLoading) View.GONE else View.VISIBLE
        }
    }

    data class FooterState(
        val message: String,
        val isLoading: Boolean
    )

    companion object {
        const val VIEW_TYPE = Int.MAX_VALUE
    }
}