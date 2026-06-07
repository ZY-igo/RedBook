/**
 * 文件说明：SearchActivity.kt
 * 作用：承载搜索页面的界面初始化、状态渲染与交互分发逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.search

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.zhengyang.redbook.R
import com.zhengyang.redbook.databinding.ActivitySearchBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 搜索页面活动页
 *
 * 负责绑定搜索页视图、监听用户输入与点击事件，并根据 [SearchViewModel] 输出的状态渲染不同页面模式。
 * 该类只负责界面交互与展示，不承担搜索数据拼装职责。
 */
@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {

    /** 搜索页视图绑定对象，用于访问布局中的各个控件。 */
    private lateinit var binding: ActivitySearchBinding

    /** 搜索页面状态提供者，负责产出界面所需的只读状态流。 */
    private val viewModel: SearchViewModel by viewModels()

    /** 最近一次渲染完成的界面状态，用于点击事件中读取当前模式和数据。 */
    private var latestState = SearchUiState()

    /** 猜你想搜列表的轮播偏移量，按两项一组循环切换。 */
    private var guessRotationOffset = 0

    /** 是否正在由代码同步输入框内容，避免触发额外的文本变更回调。 */
    private var isSyncingInput = false

    /**
     * 初始化搜索页面
     *
     * @param savedInstanceState 系统恢复时传入的页面状态快照，首次创建时可能为 `null`。
     * @return 无返回值，执行后会完成布局绑定、交互注册与状态订阅。
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemBarInsets()
        setupInteractions()
        collectUiState()
    }

    /**
     * 适配系统栏与导航栏内边距
     *
     * 顶部搜索栏需要避开状态栏，底部语音面板需要避开导航栏，以保证沉浸式布局下的可用性。
     *
     * @return 无返回值，执行后会为对应视图注册窗口插入回调。
     */
    private fun applySystemBarInsets() {
        val topBarBaseTop = (binding.topBar.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { view, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = topBarBaseTop + statusBars.top
            }
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.voiceContainer) { view, insets ->
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                navigationBars.bottom
            )
            insets
        }

        binding.topBar.doOnAttach { ViewCompat.requestApplyInsets(it) }
        binding.voiceContainer.doOnAttach { ViewCompat.requestApplyInsets(it) }
    }

    /**
     * 注册页面交互事件
     *
     * 包括返回、搜索提交、快捷搜索、清空历史和输入监听等交互，统一在此完成界面事件分发。
     *
     * @return 无返回值，执行后页面上的主要交互控件均具备响应能力。
     */
    private fun setupInteractions() {
        binding.buttonBack.setOnClickListener {
            if (latestState.screenMode == SearchScreenMode.RESULT &&
                binding.searchInput.text.isNullOrBlank()
            ) {
                viewModel.loadInitial()
            } else {
                finish()
            }
        }
        binding.searchAction.setOnClickListener {
            viewModel.submitSearch(binding.searchInput.text?.toString().orEmpty())
        }
        binding.buttonScan.setOnClickListener {
            submitPresetQuery("张元英")
        }
        binding.voiceButton.setOnClickListener {
            submitPresetQuery("数码")
        }
        binding.clearHistoryAction.setOnClickListener {
            viewModel.clearHistory()
        }
        binding.moreGuessAction.setOnClickListener {
            val size = latestState.guessItems.size
            if (size > 0) {
                guessRotationOffset = (guessRotationOffset + 2) % size
                renderGuessGrid()
            }
        }
        binding.searchInput.doAfterTextChanged { editable ->
            if (!isSyncingInput) {
                viewModel.onQueryChanged(editable?.toString().orEmpty())
            }
        }
        binding.searchInput.setOnEditorActionListener { _, actionId, event ->
            val isSearchAction = actionId == EditorInfo.IME_ACTION_SEARCH
            val isEnterUp = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
            if (isSearchAction || isEnterUp) {
                viewModel.submitSearch(binding.searchInput.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }
    }

    /**
     * 订阅搜索页面状态流
     *
     * 仅在页面处于 `STARTED` 生命周期时收集数据，避免不可见状态下继续执行界面渲染。
     *
     * @return 无返回值，执行后会在生命周期内持续接收状态更新。
     */
    private fun collectUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    /**
     * 根据最新状态刷新整个搜索页面
     *
     * @param state 由 [SearchViewModel] 产出的最新界面状态。
     * @return 无返回值，执行后会按状态内容刷新历史、推荐、联想和结果区域。
     */
    private fun render(state: SearchUiState) {
        latestState = state
        syncInputText(state)
        if (state.screenMode == SearchScreenMode.DEFAULT) {
            guessRotationOffset = 0
        }
        renderHistory()
        renderGuessGrid()
        renderSuggestions()
        renderResults()
        showState(state.screenMode)
    }

    /**
     * 同步搜索输入框文本
     *
     * 当状态变更引起展示词变化时，使用代码写入输入框并标记同步过程，避免重复触发输入监听。
     *
     * @param state 当前准备渲染的界面状态。
     * @return 无返回值，执行后输入框文案会与状态保持一致。
     */
    private fun syncInputText(state: SearchUiState) {
        val target = if (state.screenMode == SearchScreenMode.DEFAULT) "" else state.currentQuery
        val current = binding.searchInput.text?.toString().orEmpty()
        if (current != target) {
            isSyncingInput = true
            binding.searchInput.setText(target)
            binding.searchInput.setSelection(target.length)
            isSyncingInput = false
        }
    }

    /**
     * 渲染搜索历史区域
     *
     * @return 无返回值，执行后历史区域会按当前状态重新生成标签视图。
     */
    private fun renderHistory() {
        binding.historyChipContainer.removeAllViews()
        binding.historySection.isVisible = latestState.historyItems.isNotEmpty()
        latestState.historyItems.forEachIndexed { index, item ->
            binding.historyChipContainer.addView(createHistoryChip(item, index == 0))
        }
    }

    /**
     * 渲染猜你想搜宫格区域
     *
     * 按当前偏移量对推荐数据做轮转，并以每行两个条目的形式重新生成布局。
     *
     * @return 无返回值，执行后推荐区域会刷新为当前轮播页内容。
     */
    private fun renderGuessGrid() {
        val baseItems = latestState.guessItems
        val items = if (baseItems.isEmpty()) {
            emptyList()
        } else {
            baseItems.drop(guessRotationOffset) + baseItems.take(guessRotationOffset)
        }

        binding.trendingContainer.removeAllViews()
        items.chunked(2).forEachIndexed { rowIndex, rowItems ->
            binding.trendingContainer.addView(LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { params ->
                    if (rowIndex > 0) params.topMargin = dp(14)
                }
                orientation = LinearLayout.HORIZONTAL

                rowItems.forEachIndexed { itemIndex, item ->
                    addView(createGuessItemView(item).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1f
                        ).also { params ->
                            if (itemIndex > 0) params.marginStart = dp(18)
                        }
                    })
                }

                if (rowItems.size == 1) {
                    addView(View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                    })
                }
            })
        }
    }

    /**
     * 渲染联想建议列表
     *
     * @return 无返回值，执行后建议容器会按当前建议数据重新生成行视图。
     */
    private fun renderSuggestions() {
        binding.suggestionContainer.removeAllViews()
        latestState.suggestionItems.forEachIndexed { index, item ->
            binding.suggestionContainer.addView(
                createSuggestionRow(item, index == latestState.suggestionItems.lastIndex)
            )
        }
    }

    /**
     * 渲染搜索结果区域
     *
     * 先依据当前筛选条件过滤结果，再刷新结果统计、筛选标签和结果列表卡片。
     *
     * @return 无返回值，执行后结果区展示与当前筛选状态保持一致。
     */
    private fun renderResults() {
        val filtered = when (latestState.currentFilter) {
            ResultFilter.ALL -> latestState.resultItems
            else -> latestState.resultItems.filter { it.filter == latestState.currentFilter }
        }

        binding.resultKeyword.text = latestState.currentQuery
        binding.resultCount.text = getString(
            R.string.search_result_count,
            filtered.size,
            latestState.currentFilter.label
        )

        binding.resultTabContainer.removeAllViews()
        ResultFilter.values().forEachIndexed { index, filter ->
            binding.resultTabContainer.addView(createResultTab(filter).apply {
                if (index > 0) {
                    (layoutParams as LinearLayout.LayoutParams).marginStart = dp(10)
                }
            })
        }

        binding.resultListContainer.removeAllViews()
        filtered.forEachIndexed { index, item ->
            binding.resultListContainer.addView(createResultCard(item))
            if (index != filtered.lastIndex) {
                binding.resultListContainer.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1)
                    ).also { params ->
                        params.topMargin = dp(14)
                        params.bottomMargin = dp(14)
                    }
                    setBackgroundColor(color(R.color.xhs_search_divider))
                })
            }
        }
    }

    /**
     * 切换页面展示模式
     *
     * @param mode 当前应显示的搜索页面模式。
     * @return 无返回值，执行后仅对应模式的内容区域会被展示。
     */
    private fun showState(mode: SearchScreenMode) {
        binding.defaultContent.isVisible = mode == SearchScreenMode.DEFAULT
        binding.suggestionContent.isVisible = mode == SearchScreenMode.SUGGESTION
        binding.resultContent.isVisible = mode == SearchScreenMode.RESULT
        binding.voiceContainer.isVisible = mode == SearchScreenMode.DEFAULT
    }

    /**
     * 使用预置关键词直接发起搜索
     *
     * 常用于扫一扫或语音按钮等快捷入口，先同步输入框显示，再调用 ViewModel 提交搜索。
     *
     * @param query 快捷入口对应的预置搜索词。
     * @return 无返回值，执行后页面会切换到搜索结果态。
     */
    private fun submitPresetQuery(query: String) {
        isSyncingInput = true
        binding.searchInput.setText(query)
        binding.searchInput.setSelection(query.length)
        isSyncingInput = false
        viewModel.submitSearch(query)
    }

    /**
     * 创建历史记录标签视图
     *
     * @param text 历史记录显示文案，也是点击后的搜索词。
     * @param isFirst 当前标签是否为首个元素，用于控制左间距。
     * @return 构建完成的历史记录标签视图。
     */
    private fun createHistoryChip(text: String, isFirst: Boolean): View {
        return AppCompatTextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(30)
            ).also { params ->
                if (!isFirst) params.marginStart = dp(8)
            }
            background = roundedDrawable(R.color.xhs_search_chip_bg, 15f)
            gravity = Gravity.CENTER
            minWidth = dp(56)
            setPadding(dp(12), 0, dp(12), 0)
            this.text = text
            textSize = 14f
            setTextColor(color(R.color.xhs_search_text_secondary))
            setOnClickListener { viewModel.submitSearch(text) }
        }
    }

    /**
     * 创建猜你想搜条目视图
     *
     * @param item 需要渲染的推荐条目模型。
     * @return 可直接加入父容器的推荐条目视图。
     */
    private fun createGuessItemView(item: SearchGuessItem): View {
        return AppCompatTextView(this).apply {
            text = item.title
            textSize = 14f
            setTextColor(color(R.color.xhs_search_text_secondary))
            setLineSpacing(0f, 1.2f)
            setOnClickListener { viewModel.submitSearch(item.title) }
        }
    }

    /**
     * 创建联想建议行视图
     *
     * @param item 当前联想建议条目模型。
     * @param isLast 当前条目是否为列表最后一项，可用于尾部样式控制。
     * @return 可直接加入父容器的联想建议行视图。
     */
    private fun createSuggestionRow(item: SearchGuessItem, isLast: Boolean): View {
        return LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, if (isLast) dp(12) else dp(12))

            addView(ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(16), dp(16))
                setImageResource(R.drawable.ic_xhs_search)
                imageTintList = ContextCompat.getColorStateList(context, R.color.xhs_search_icon_subtle)
            })

            addView(LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                ).also { params ->
                    params.marginStart = dp(12)
                }
                orientation = LinearLayout.VERTICAL

                addView(AppCompatTextView(context).apply {
                    text = item.title
                    textSize = 15f
                    setTextColor(color(R.color.xhs_search_text_primary))
                })

                addView(AppCompatTextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).also { params ->
                        params.topMargin = dp(4)
                    }
                    text = item.meta
                    textSize = 12f
                    setTextColor(color(R.color.xhs_search_hint_text))
                })
            })

            setOnClickListener { viewModel.submitSearch(item.title) }
        }
    }

    /**
     * 创建结果筛选标签视图
     *
     * @param filter 需要渲染的筛选类型。
     * @return 可直接加入筛选栏容器的标签视图。
     */
    private fun createResultTab(filter: ResultFilter): View {
        return AppCompatTextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(34)
            )
            gravity = Gravity.CENTER
            minWidth = dp(58)
            setPadding(dp(14), 0, dp(14), 0)
            text = filter.label
            textSize = 13f
            setTypeface(typeface, if (filter == latestState.currentFilter) Typeface.BOLD else Typeface.NORMAL)
            background = roundedDrawable(
                if (filter == latestState.currentFilter) {
                    R.color.xhs_search_filter_selected_bg
                } else {
                    android.R.color.transparent
                },
                17f
            )
            setTextColor(
                if (filter == latestState.currentFilter) {
                    color(R.color.xhs_search_filter_selected_text)
                } else {
                    color(R.color.xhs_search_hint_text)
                }
            )
            setOnClickListener {
                viewModel.changeFilter(filter)
            }
        }
    }

    /**
     * 创建搜索结果卡片视图
     *
     * @param item 当前需要展示的搜索结果模型。
     * @return 可直接加入结果列表容器的卡片视图。
     */
    private fun createResultCard(item: SearchResultItem): View {
        return LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.bg_xhs_search_section_card)
            setPadding(dp(16), dp(16), dp(16), dp(16))

            addView(AppCompatTextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(24)
                )
                background = roundedDrawable(item.badgeColorRes, 12f)
                gravity = Gravity.CENTER
                minWidth = dp(52)
                setPadding(dp(10), 0, dp(10), 0)
                text = item.badge
                textSize = 11f
                setTextColor(color(R.color.xhs_search_text_primary))
                setTypeface(typeface, Typeface.BOLD)
            })

            addView(AppCompatTextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { params ->
                    params.topMargin = dp(12)
                }
                text = item.title
                textSize = 17f
                setTextColor(color(R.color.xhs_search_text_primary))
                setTypeface(typeface, Typeface.BOLD)
            })

            addView(AppCompatTextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { params ->
                    params.topMargin = dp(6)
                }
                text = item.subtitle
                textSize = 14f
                setTextColor(color(R.color.xhs_search_text_secondary))
            })

            addView(AppCompatTextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { params ->
                    params.topMargin = dp(10)
                }
                text = item.meta
                textSize = 12f
                setTextColor(color(R.color.xhs_search_hint_text))
            })
        }
    }

    /**
     * 构建圆角背景
     *
     * @param colorRes 背景颜色资源标识。
     * @param radiusDp 圆角半径，单位为 dp。
     * @return 配置完成的圆角背景对象。
     */
    private fun roundedDrawable(colorRes: Int, radiusDp: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp.toInt()).toFloat()
            setColor(color(colorRes))
        }
    }

    /**
     * 解析颜色资源
     *
     * @param colorRes 颜色资源标识。
     * @return 当前主题下解析后的颜色值。
     */
    private fun color(colorRes: Int): Int = ContextCompat.getColor(this, colorRes)

    /**
     * 将 dp 转换为像素值
     *
     * @param value 需要转换的 dp 数值。
     * @return 对应屏幕密度下的像素整数值。
     */
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
