package com.zhengyang.redbook.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.zhengyang.redbook.databinding.ItemNoteBinding

class HomeAdapter : ListAdapter<HomeCardItem, HomeViewHolder>(HomeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HomeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private class HomeDiffCallback : DiffUtil.ItemCallback<HomeCardItem>() {
        override fun areItemsTheSame(oldItem: HomeCardItem, newItem: HomeCardItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: HomeCardItem, newItem: HomeCardItem): Boolean {
            return oldItem == newItem
        }
    }
}
