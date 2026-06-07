package com.zhengyang.redbook.ui.home

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatTextView
import com.zhengyang.redbook.R
import com.zhengyang.redbook.databinding.FragmentHomeBinding
import kotlin.math.roundToInt

class HomeChannelRenderer(
    private val context: Context,
    private val binding: FragmentHomeBinding
) {

    data class ChannelCallbacks(
        val onMyChannelSelected: (DiscoverCategoryItem) -> Unit,
        val onRecommendedChannelSelected: (DiscoverCategoryItem) -> Unit,
        val onRemoveChannel: (DiscoverCategoryItem) -> Unit,
        val canRemoveChannel: (DiscoverCategoryItem) -> Boolean
    )

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
            val tabView = (container.getChildAt(index) as? AppCompatTextView)
                ?: createCompactTabView().also(container::addView)
            bindCompactTabView(
                tabView = tabView,
                category = category,
                currentCategoryId = currentCategoryId,
                textColors = textColors,
                onCategorySelected = onCategorySelected
            )
        }
    }

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

    private fun createCompactTabView(): AppCompatTextView {
        return AppCompatTextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { params ->
                params.marginEnd = dp(COMPACT_TAB_END_MARGIN_DP)
            }
            minHeight = dp(COMPACT_TAB_MIN_HEIGHT_DP)
            gravity = Gravity.CENTER
            setPadding(0, dp(COMPACT_TAB_TOP_PADDING_DP), 0, dp(COMPACT_TAB_BOTTOM_PADDING_DP))
            background = AppCompatResources.getDrawable(
                context,
                R.drawable.bg_xhs_category_tab
            )
            textSize = CHIP_TEXT_SIZE_SP
        }
    }

    private fun bindCompactTabView(
        tabView: AppCompatTextView,
        category: DiscoverCategoryItem,
        currentCategoryId: String?,
        textColors: ColorStateList?,
        onCategorySelected: (DiscoverCategoryItem) -> Unit
    ) {
        val isSelected = category.id == currentCategoryId
        tabView.setTextColor(textColors)
        tabView.text = category.title
        tabView.isSelected = isSelected
        tabView.contentDescription = context.getString(
            if (isSelected) R.string.home_channel_tab_selected_cd else R.string.home_channel_tab_cd,
            category.title
        )
        tabView.setOnClickListener { onCategorySelected(category) }
    }

    private fun renderChannelGrid(
        container: LinearLayout,
        channels: List<DiscoverCategoryItem>,
        sectionContext: ChannelSectionContext
    ) {
        val rowCount = if (channels.isEmpty()) 0 else (channels.size + CHANNEL_ROW_SIZE - 1) / CHANNEL_ROW_SIZE
        trimExtraChildren(container, rowCount)
        repeat(rowCount) { rowIndex ->
            val row = (container.getChildAt(rowIndex) as? LinearLayout)
                ?: createGridRow().also(container::addView)
            bindChannelRow(
                row = row,
                rowIndex = rowIndex,
                channels = channels,
                sectionContext = sectionContext
            )
        }
    }

    private fun createGridRow(): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { params ->
                params.bottomMargin = dp(GRID_ROW_BOTTOM_MARGIN_DP)
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
    }

    private fun bindChannelRow(
        row: LinearLayout,
        rowIndex: Int,
        channels: List<DiscoverCategoryItem>,
        sectionContext: ChannelSectionContext
    ) {
        trimExtraChildren(row, CHANNEL_ROW_SIZE)
        repeat(CHANNEL_ROW_SIZE) { columnIndex ->
            val cell = (row.getChildAt(columnIndex) as? FrameLayout)
                ?: createChannelCell().also(row::addView)
            val channelIndex = rowIndex * CHANNEL_ROW_SIZE + columnIndex
            val category = channels.getOrNull(channelIndex)
            if (category == null) {
                bindSpacerCell(cell)
            } else {
                bindChannelCell(cell, category, sectionContext)
            }
        }
    }

    private fun createChannelCell(): FrameLayout {
        return FrameLayout(context).apply {
            layoutParams = createChannelCellLayoutParams()
        }
    }

    private fun bindSpacerCell(cell: FrameLayout) {
        cell.visibility = View.INVISIBLE
        cell.isEnabled = false
        cell.isClickable = false
        cell.contentDescription = null
        cell.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        (cell.getChildAt(CHIP_INDEX) as? AppCompatTextView)?.setOnClickListener(null)
        (cell.getChildAt(DELETE_BADGE_INDEX) as? AppCompatTextView)?.setOnClickListener(null)
    }

    private fun bindChannelCell(
        cell: FrameLayout,
        category: DiscoverCategoryItem,
        sectionContext: ChannelSectionContext
    ) {
        cell.visibility = View.VISIBLE
        cell.isEnabled = true
        cell.isClickable = false
        cell.contentDescription = null
        cell.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO

        val chip = (cell.getChildAt(CHIP_INDEX) as? AppCompatTextView)
            ?: createChannelChip().also(cell::addView)
        bindChannelChip(chip, category, sectionContext)

        if (sectionContext.shouldShowDeleteBadge(category)) {
            val deleteBadge = (cell.getChildAt(DELETE_BADGE_INDEX) as? AppCompatTextView)
                ?: createDeleteBadge().also(cell::addView)
            bindDeleteBadge(deleteBadge, category, sectionContext)
        } else if (cell.childCount > DELETE_BADGE_INDEX) {
            cell.removeViewAt(DELETE_BADGE_INDEX)
        }
    }

    private fun createChannelChip(): AppCompatTextView {
        return AppCompatTextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(CHANNEL_CHIP_HEIGHT_DP)
            )
            gravity = Gravity.CENTER
            textSize = CHIP_TEXT_SIZE_SP
        }
    }

    private fun bindChannelChip(
        chip: AppCompatTextView,
        category: DiscoverCategoryItem,
        sectionContext: ChannelSectionContext
    ) {
        val isSelected = sectionContext.isSelected(category)
        chip.text = sectionContext.resolveChipTitle(category)
        chip.setTextColor(context.getColor(R.color.xhs_text_primary))
        chip.background = AppCompatResources.getDrawable(
            context,
            if (isSelected) {
                R.drawable.bg_xhs_channel_chip_selected
            } else {
                R.drawable.bg_xhs_channel_chip
            }
        )
        chip.isEnabled = sectionContext.isChipEnabled
        chip.isSelected = isSelected
        chip.contentDescription = sectionContext.resolveChipContentDescription(category)
        chip.setOnClickListener { sectionContext.onChipClicked(category) }
    }

    private fun createDeleteBadge(): AppCompatTextView {
        return AppCompatTextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                dp(DELETE_BADGE_SIZE_DP),
                dp(DELETE_BADGE_SIZE_DP),
                Gravity.TOP or Gravity.END
            ).also { params ->
                params.topMargin = dp(DELETE_BADGE_TOP_MARGIN_DP)
                params.marginEnd = dp(DELETE_BADGE_END_MARGIN_DP)
            }
            gravity = Gravity.CENTER
            text = context.getString(R.string.close_symbol)
            background = AppCompatResources.getDrawable(
                context,
                R.drawable.bg_xhs_channel_delete_badge
            )
            setTextColor(ColorStateList.valueOf(context.getColor(R.color.xhs_card)))
        }
    }

    private fun bindDeleteBadge(
        deleteBadge: AppCompatTextView,
        category: DiscoverCategoryItem,
        sectionContext: ChannelSectionContext
    ) {
        deleteBadge.contentDescription = context.getString(
            R.string.home_channel_remove_cd,
            category.title
        )
        deleteBadge.setOnClickListener { sectionContext.callbacks.onRemoveChannel(category) }
    }

    private fun createChannelCellLayoutParams(
        height: Int = ViewGroup.LayoutParams.WRAP_CONTENT
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(0, height, 1f).also { params ->
            params.marginStart = dp(CHANNEL_CELL_HORIZONTAL_MARGIN_DP)
            params.marginEnd = dp(CHANNEL_CELL_HORIZONTAL_MARGIN_DP)
        }
    }

    private fun trimExtraChildren(container: ViewGroup, targetCount: Int) {
        while (container.childCount > targetCount) {
            container.removeViewAt(container.childCount - 1)
        }
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).roundToInt()
    }

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

    private enum class ChannelSection {
        MY_CHANNELS,
        RECOMMENDED
    }

    private companion object {
        private const val CHIP_INDEX = 0
        private const val DELETE_BADGE_INDEX = 1
        private const val CHANNEL_ROW_SIZE = 4
        private const val COMPACT_TAB_END_MARGIN_DP = 22
        private const val COMPACT_TAB_MIN_HEIGHT_DP = 38
        private const val COMPACT_TAB_TOP_PADDING_DP = 8
        private const val COMPACT_TAB_BOTTOM_PADDING_DP = 12
        private const val GRID_ROW_BOTTOM_MARGIN_DP = 12
        private const val CHANNEL_CELL_HORIZONTAL_MARGIN_DP = 6
        private const val CHANNEL_CHIP_HEIGHT_DP = 36
        private const val DELETE_BADGE_SIZE_DP = 18
        private const val DELETE_BADGE_TOP_MARGIN_DP = -6
        private const val DELETE_BADGE_END_MARGIN_DP = -4
        private const val CHIP_TEXT_SIZE_SP = 14f
    }
}
