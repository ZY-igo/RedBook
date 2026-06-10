package com.zhengyang.redbook.ui.home

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import com.zhengyang.redbook.R
import com.zhengyang.redbook.databinding.FragmentHomeBinding
import com.zhengyang.redbook.databinding.ItemHomeChannelCellContentBinding
import com.zhengyang.redbook.databinding.ItemHomeChannelGridRowBinding
import com.zhengyang.redbook.databinding.ItemHomeCompactCategoryTabBinding

/**
 * 首页频道区域渲染器。
 *
 * 负责渲染顶部紧凑分类 tab，以及抽屉内的频道管理面板。
 */
class HomeChannelRenderer(
    private val context: Context,
    private val binding: FragmentHomeBinding
) {

    /**
     * 频道管理面板交互回调集合。
     */
    data class ChannelCallbacks(
        val onMyChannelSelected: (DiscoverCategoryItem) -> Unit,
        val onRecommendedChannelSelected: (DiscoverCategoryItem) -> Unit,
        val onRemoveChannel: (DiscoverCategoryItem) -> Unit,
        val canRemoveChannel: (DiscoverCategoryItem) -> Boolean
    )

    private val inflater: LayoutInflater by lazy(LazyThreadSafetyMode.NONE) {
        LayoutInflater.from(context)
    }

    /**
     * 渲染顶部紧凑分类 tab。
     *
     * @param myChannels 当前需要展示的频道列表。
     * @param currentCategoryId 当前选中频道 id。
     * @param onCategorySelected 点击频道后的回调。
     */
    fun renderCompactTabs(
        myChannels: List<DiscoverCategoryItem>,
        currentCategoryId: String?,
        onCategorySelected: (DiscoverCategoryItem) -> Unit
    ) {
        val container = binding.compactCategoryContainer
        val textColors = AppCompatResources.getColorStateList(
            context,
            R.color.xhs_category_tab_text
        )

        trimExtraChildren(container, myChannels.size)
        myChannels.forEachIndexed { index, category ->
            val tabView = container.getChildAt(index) ?: ItemHomeCompactCategoryTabBinding.inflate(
                inflater,
                container,
                false
            ).root.also(container::addView)
            bindCompactTabView(tabView, category, currentCategoryId, textColors, onCategorySelected)
        }
    }

    /**
     * 渲染频道管理面板。
     *
     * @param allChannels 所有可选频道。
     * @param myChannels 当前“我的频道”。
     * @param currentCategoryId 当前选中频道 id。
     * @param isEditMode 当前是否处于编辑模式。
     * @param callbacks 频道交互回调集合。
     */
    fun renderManager(
        allChannels: List<DiscoverCategoryItem>,
        myChannels: List<DiscoverCategoryItem>,
        currentCategoryId: String?,
        isEditMode: Boolean,
        callbacks: ChannelCallbacks
    ) {
        binding.panelEditButton.text = if (isEditMode) {
            context.getString(R.string.home_channel_edit_done)
        } else {
            context.getString(R.string.home_channel_edit_enter)
        }
        binding.myChannelHint.text = if (isEditMode) {
            context.getString(R.string.home_channel_hint_delete)
        } else {
            context.getString(R.string.home_channel_hint_enter)
        }

        val myChannelIds = myChannels.mapTo(linkedSetOf(), DiscoverCategoryItem::id)
        renderChannelGrid(
            container = binding.myChannelContainer,
            channels = myChannels,
            sectionContext = ChannelSectionContext(
                context = context,
                section = ChannelSection.MY_CHANNELS,
                currentCategoryId = currentCategoryId,
                isEditMode = isEditMode,
                callbacks = callbacks
            )
        )
        renderChannelGrid(
            container = binding.recommendedChannelContainer,
            channels = allChannels.filterNot { it.id in myChannelIds },
            sectionContext = ChannelSectionContext(
                context = context,
                section = ChannelSection.RECOMMENDED,
                currentCategoryId = currentCategoryId,
                isEditMode = isEditMode,
                callbacks = callbacks
            )
        )
    }

    /**
     * 绑定单个顶部 tab 视图。
     */
    private fun bindCompactTabView(
        view: View,
        category: DiscoverCategoryItem,
        currentCategoryId: String?,
        textColors: ColorStateList?,
        onCategorySelected: (DiscoverCategoryItem) -> Unit
    ) {
        val binding = ItemHomeCompactCategoryTabBinding.bind(view)
        val isSelected = category.id == currentCategoryId
        binding.compactCategoryTab.setTextColor(textColors)
        binding.compactCategoryTab.text = category.title
        binding.compactCategoryTab.isSelected = isSelected
        binding.compactCategoryTab.contentDescription = context.getString(
            if (isSelected) R.string.home_channel_tab_selected_cd else R.string.home_channel_tab_cd,
            category.title
        )
        binding.compactCategoryTab.setOnClickListener { onCategorySelected(category) }
    }

    /**
     * 按行渲染频道宫格。
     */
    private fun renderChannelGrid(
        container: LinearLayout,
        channels: List<DiscoverCategoryItem>,
        sectionContext: ChannelSectionContext
    ) {
        val rowCount = if (channels.isEmpty()) 0 else (channels.size + CHANNEL_ROW_SIZE - 1) / CHANNEL_ROW_SIZE
        trimExtraChildren(container, rowCount)
        repeat(rowCount) { rowIndex ->
            val rowView = container.getChildAt(rowIndex) ?: ItemHomeChannelGridRowBinding.inflate(
                inflater,
                container,
                false
            ).root.also(container::addView)
            bindChannelRow(
                rowBinding = ItemHomeChannelGridRowBinding.bind(rowView),
                rowIndex = rowIndex,
                channels = channels,
                sectionContext = sectionContext
            )
        }
    }

    /**
     * 绑定某一行中的四个频道单元格。
     */
    private fun bindChannelRow(
        rowBinding: ItemHomeChannelGridRowBinding,
        rowIndex: Int,
        channels: List<DiscoverCategoryItem>,
        sectionContext: ChannelSectionContext
    ) {
        val cells = listOf(
            rowBinding.cellOne,
            rowBinding.cellTwo,
            rowBinding.cellThree,
            rowBinding.cellFour
        )
        cells.forEachIndexed { columnIndex, cell ->
            val channelIndex = rowIndex * CHANNEL_ROW_SIZE + columnIndex
            val category = channels.getOrNull(channelIndex)
            if (category == null) {
                bindSpacerCell(cell)
            } else {
                bindChannelCell(cell, category, sectionContext)
            }
        }
    }

    /**
     * 把空白占位 cell 设为不可交互的 spacer。
     */
    private fun bindSpacerCell(cell: FrameLayout) {
        cell.visibility = View.INVISIBLE
        cell.isEnabled = false
        cell.isClickable = false
        cell.contentDescription = null
        cell.removeAllViews()
    }

    /**
     * 绑定一个真实频道 cell。
     */
    private fun bindChannelCell(
        cell: FrameLayout,
        category: DiscoverCategoryItem,
        sectionContext: ChannelSectionContext
    ) {
        cell.visibility = View.VISIBLE
        cell.isEnabled = true
        cell.isClickable = false
        cell.contentDescription = null

        val contentBinding = if (cell.childCount == 0) {
            ItemHomeChannelCellContentBinding.inflate(inflater, cell, true)
        } else {
            ItemHomeChannelCellContentBinding.bind(cell.getChildAt(0))
        }
        val isSelected = sectionContext.isSelected(category)
        contentBinding.channelChip.text = sectionContext.resolveChipTitle(category)
        contentBinding.channelChip.setTextColor(context.getColor(R.color.xhs_text_primary))
        contentBinding.channelChip.background = AppCompatResources.getDrawable(
            context,
            if (isSelected) {
                R.drawable.bg_xhs_channel_chip_selected
            } else {
                R.drawable.bg_xhs_channel_chip
            }
        )
        contentBinding.channelChip.isEnabled = sectionContext.isChipEnabled
        contentBinding.channelChip.isSelected = isSelected
        contentBinding.channelChip.contentDescription =
            sectionContext.resolveChipContentDescription(category)
        contentBinding.channelChip.setOnClickListener { sectionContext.onChipClicked(category) }

        val showDelete = sectionContext.shouldShowDeleteBadge(category)
        contentBinding.deleteBadge.visibility = if (showDelete) View.VISIBLE else View.GONE
        contentBinding.deleteBadge.contentDescription = if (showDelete) {
            context.getString(R.string.home_channel_remove_cd, category.title)
        } else {
            null
        }
        contentBinding.deleteBadge.setOnClickListener(
            if (showDelete) {
                View.OnClickListener { sectionContext.callbacks.onRemoveChannel(category) }
            } else {
                null
            }
        )
    }

    /**
     * 删除容器中多余的子 View，避免重复 inflate。
     */
    private fun trimExtraChildren(container: ViewGroup, targetCount: Int) {
        while (container.childCount > targetCount) {
            container.removeViewAt(container.childCount - 1)
        }
    }

    /**
     * 渲染单个频道分区时需要的上下文。
     */
    private data class ChannelSectionContext(
        val context: Context,
        val section: ChannelSection,
        val currentCategoryId: String?,
        val isEditMode: Boolean,
        val callbacks: ChannelCallbacks
    ) {
        val isChipEnabled: Boolean
            get() = section == ChannelSection.RECOMMENDED || !isEditMode

        fun isSelected(category: DiscoverCategoryItem): Boolean {
            return section == ChannelSection.MY_CHANNELS &&
                category.id == currentCategoryId &&
                !isEditMode
        }

        fun resolveChipTitle(category: DiscoverCategoryItem): String {
            return if (section == ChannelSection.MY_CHANNELS) category.title else "+${category.title}"
        }

        fun resolveChipContentDescription(category: DiscoverCategoryItem): String {
            return when (section) {
                ChannelSection.MY_CHANNELS -> {
                    if (isSelected(category)) {
                        context.getString(R.string.home_channel_tab_selected_cd, category.title)
                    } else {
                        context.getString(R.string.home_channel_tab_cd, category.title)
                    }
                }

                ChannelSection.RECOMMENDED -> {
                    context.getString(R.string.home_channel_add_cd, category.title)
                }
            }
        }

        fun onChipClicked(category: DiscoverCategoryItem) {
            when (section) {
                ChannelSection.MY_CHANNELS -> {
                    if (!isEditMode) callbacks.onMyChannelSelected(category)
                }

                ChannelSection.RECOMMENDED -> callbacks.onRecommendedChannelSelected(category)
            }
        }

        fun shouldShowDeleteBadge(category: DiscoverCategoryItem): Boolean {
            return section == ChannelSection.MY_CHANNELS &&
                isEditMode &&
                callbacks.canRemoveChannel(category)
        }
    }

    /**
     * 频道管理面板的分区类型。
     */
    private enum class ChannelSection {
        MY_CHANNELS,
        RECOMMENDED
    }

    private companion object {
        /**
         * 频道管理面板每行固定显示的频道数量。
         */
        private const val CHANNEL_ROW_SIZE = 4
    }
}
