/**
 * 文件说明：HomeChannelRenderer.kt
 * 作用：负责首页频道栏与频道管理面板的视图渲染逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
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

/**
 * 首页频道渲染器
 *
 * 负责渲染顶部紧凑频道栏和展开后的频道管理面板，
 * 并根据当前编辑状态和选中状态切换频道项样式与交互行为。
 */
class HomeChannelRenderer(
    /** 当前页面上下文。 */
    private val context: Context,

    /** 首页视图绑定对象。 */
    private val binding: FragmentHomeBinding
) {

    /**
     * 频道面板交互回调集合。
     *
     * 用于把渲染层中的点击事件委托回 Fragment 或协调器处理。
     */
    data class ChannelCallbacks(
        /** 点击“我的频道”项时的回调。 */
        val onMyChannelSelected: (DiscoverCategoryItem) -> Unit,

        /** 点击推荐频道项时的回调。 */
        val onRecommendedChannelSelected: (DiscoverCategoryItem) -> Unit,

        /** 点击删除角标时的回调。 */
        val onRemoveChannel: (DiscoverCategoryItem) -> Unit,

        /** 判断频道是否允许删除的回调。 */
        val canRemoveChannel: (DiscoverCategoryItem) -> Boolean
    )

    /**
     * 渲染顶部紧凑频道栏。
     *
     * @param myChannels 当前“我的频道”列表。
     * @param currentCategoryId 当前选中的频道 ID。
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

    /**
     * 渲染展开后的频道管理面板。
     *
     * @param allChannels 全量频道列表。
     * @param myChannels 当前“我的频道”列表。
     * @param currentCategoryId 当前选中的频道 ID。
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
     * 创建顶部紧凑频道标签视图。
     *
     * @return 新的顶部频道标签视图。
     */
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

    /**
     * 绑定顶部紧凑频道标签状态。
     *
     * @param tabView 需要绑定的标签视图。
     * @param category 当前频道模型。
     * @param currentCategoryId 当前选中的频道 ID。
     * @param textColors 频道文字颜色状态列表。
     * @param onCategorySelected 点击频道后的回调。
     */
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

    /**
     * 渲染频道宫格区域。
     *
     * @param container 目标容器。
     * @param channels 当前需要渲染的频道列表。
     * @param sectionContext 当前宫格所属区块的渲染上下文。
     */
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

    /**
     * 创建一行频道宫格容器。
     *
     * @return 新的频道行容器。
     */
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

    /**
     * 绑定单行频道宫格内容。
     *
     * @param row 当前行容器。
     * @param rowIndex 当前行索引。
     * @param channels 当前区块所有频道列表。
     * @param sectionContext 当前区块渲染上下文。
     */
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

    /**
     * 创建频道宫格单元格容器。
     *
     * @return 新的频道单元格容器。
     */
    private fun createChannelCell(): FrameLayout {
        return FrameLayout(context).apply {
            layoutParams = createChannelCellLayoutParams()
        }
    }

    /**
     * 绑定空白占位单元格。
     *
     * @param cell 当前空白单元格容器。
     */
    private fun bindSpacerCell(cell: FrameLayout) {
        cell.visibility = View.INVISIBLE
        cell.isEnabled = false
        cell.isClickable = false
        cell.contentDescription = null
        cell.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        (cell.getChildAt(CHIP_INDEX) as? AppCompatTextView)?.setOnClickListener(null)
        (cell.getChildAt(DELETE_BADGE_INDEX) as? AppCompatTextView)?.setOnClickListener(null)
    }

    /**
     * 绑定实际频道单元格。
     *
     * @param cell 当前单元格容器。
     * @param category 当前频道模型。
     * @param sectionContext 当前区块渲染上下文。
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

    /**
     * 创建频道按钮视图。
     *
     * @return 新的频道按钮视图。
     */
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

    /**
     * 绑定频道按钮状态。
     *
     * @param chip 当前频道按钮。
     * @param category 当前频道模型。
     * @param sectionContext 当前区块渲染上下文。
     */
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

    /**
     * 创建频道删除角标视图。
     *
     * @return 新的删除角标视图。
     */
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

    /**
     * 绑定删除角标交互。
     *
     * @param deleteBadge 删除角标视图。
     * @param category 当前频道模型。
     * @param sectionContext 当前区块渲染上下文。
     */
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

    /**
     * 创建频道单元格布局参数。
     *
     * @param height 单元格高度。
     * @return 配置好的线性布局参数。
     */
    private fun createChannelCellLayoutParams(
        height: Int = ViewGroup.LayoutParams.WRAP_CONTENT
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(0, height, 1f).also { params ->
            params.marginStart = dp(CHANNEL_CELL_HORIZONTAL_MARGIN_DP)
            params.marginEnd = dp(CHANNEL_CELL_HORIZONTAL_MARGIN_DP)
        }
    }

    /**
     * 裁剪容器中多余的子视图。
     *
     * @param container 目标容器。
     * @param targetCount 目标子视图数量。
     */
    private fun trimExtraChildren(container: ViewGroup, targetCount: Int) {
        while (container.childCount > targetCount) {
            container.removeViewAt(container.childCount - 1)
        }
    }

    /**
     * 将 dp 转换为像素值。
     *
     * @param value 需要转换的 dp 数值。
     * @return 对应像素值。
     */
    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).roundToInt()
    }

    /**
     * 频道区块渲染上下文。
     *
     * 用于封装不同频道区块在选中态、文案、可点击性和删除能力上的差异。
     */
    private data class ChannelSectionContext(
        /** 当前页面上下文。 */
        val context: Context,
        /** 当前区块类型。 */
        val section: ChannelSection,
        /** 当前选中的频道 ID。 */
        val currentCategoryId: String?,
        /** 当前是否处于编辑模式。 */
        val isEditMode: Boolean,
        /** 区块交互回调集合。 */
        val callbacks: ChannelCallbacks
    ) {
        /** 当前区块中的频道按钮是否可点击。 */
        val isChipEnabled: Boolean
            get() = section == ChannelSection.RECOMMENDED || !isEditMode

        /**
         * 判断频道在当前区块中是否处于选中态。
         */
        fun isSelected(category: DiscoverCategoryItem): Boolean {
            return section == ChannelSection.MY_CHANNELS &&
                category.id == currentCategoryId &&
                !isEditMode
        }

        /**
         * 解析频道按钮显示文案。
         */
        fun resolveChipTitle(category: DiscoverCategoryItem): String {
            return if (section == ChannelSection.MY_CHANNELS) category.title else "+${category.title}"
        }

        /**
         * 解析频道按钮无障碍描述。
         */
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

        /**
         * 处理频道按钮点击事件。
         */
        fun onChipClicked(category: DiscoverCategoryItem) {
            when (section) {
                ChannelSection.MY_CHANNELS -> {
                    if (!isEditMode) callbacks.onMyChannelSelected(category)
                }

                ChannelSection.RECOMMENDED -> callbacks.onRecommendedChannelSelected(category)
            }
        }

        /**
         * 判断是否需要展示删除角标。
         */
        fun shouldShowDeleteBadge(category: DiscoverCategoryItem): Boolean {
            return section == ChannelSection.MY_CHANNELS &&
                isEditMode &&
                callbacks.canRemoveChannel(category)
        }
    }

    /**
     * 频道管理区块类型枚举。
     */
    private enum class ChannelSection {
        /** “我的频道”区块。 */
        MY_CHANNELS,
        /** 推荐频道区块。 */
        RECOMMENDED
    }

    private companion object {
        /** 频道按钮索引。 */
        private const val CHIP_INDEX = 0
        /** 删除角标索引。 */
        private const val DELETE_BADGE_INDEX = 1
        /** 每行频道数量。 */
        private const val CHANNEL_ROW_SIZE = 4
        /** 顶部频道标签右侧间距。 */
        private const val COMPACT_TAB_END_MARGIN_DP = 22
        /** 顶部频道标签最小高度。 */
        private const val COMPACT_TAB_MIN_HEIGHT_DP = 38
        /** 顶部频道标签上内边距。 */
        private const val COMPACT_TAB_TOP_PADDING_DP = 8
        /** 顶部频道标签下内边距。 */
        private const val COMPACT_TAB_BOTTOM_PADDING_DP = 12
        /** 频道行底部间距。 */
        private const val GRID_ROW_BOTTOM_MARGIN_DP = 12
        /** 频道单元格水平外边距。 */
        private const val CHANNEL_CELL_HORIZONTAL_MARGIN_DP = 6
        /** 频道按钮高度。 */
        private const val CHANNEL_CHIP_HEIGHT_DP = 36
        /** 删除角标尺寸。 */
        private const val DELETE_BADGE_SIZE_DP = 18
        /** 删除角标顶部偏移。 */
        private const val DELETE_BADGE_TOP_MARGIN_DP = -6
        /** 删除角标右侧偏移。 */
        private const val DELETE_BADGE_END_MARGIN_DP = -4
        /** 频道文字字号。 */
        private const val CHIP_TEXT_SIZE_SP = 14f
    }
}
