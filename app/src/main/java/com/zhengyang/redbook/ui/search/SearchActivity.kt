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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.doAfterTextChanged
import com.zhengyang.redbook.R
import com.zhengyang.redbook.databinding.ActivitySearchBinding

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding

    private val historyItems = mutableListOf(
        "数码",
        "张元英",
        "横店群演",
        "短剧"
    )
    private val guessItems = listOf(
        GuessItem("张元英", "人气热搜词"),
        GuessItem("100种折纸方法大全", "教程收藏飙升"),
        GuessItem("横店群演", "体验类内容升温"),
        GuessItem("欢乐谷实景演绎体验", "周末出游热门"),
        GuessItem("长得特别漂亮的群演", "话题讨论增长"),
        GuessItem("张元英多高", "百科类搜索走高")
    )

    private var currentFilter = ResultFilter.ALL
    private var currentQuery = ""
    private var currentResults: List<SearchResultItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemBarInsets()
        setupInteractions()
        renderHistory()
        renderGuessGrid()
        showDefaultState()
    }

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

    private fun setupInteractions() {
        binding.buttonBack.setOnClickListener {
            if (binding.resultContent.isVisible && binding.searchInput.text.isNullOrBlank()) {
                showDefaultState()
            } else {
                finish()
            }
        }
        binding.searchAction.setOnClickListener {
            submitSearch(binding.searchInput.text?.toString().orEmpty())
        }
        binding.buttonScan.setOnClickListener {
            binding.searchInput.setText("张元英")
            binding.searchInput.setSelection(binding.searchInput.text?.length ?: 0)
            submitSearch("张元英")
        }
        binding.voiceButton.setOnClickListener {
            binding.searchInput.setText("数码")
            binding.searchInput.setSelection(binding.searchInput.text?.length ?: 0)
            submitSearch("数码")
        }
        binding.clearHistoryAction.setOnClickListener {
            historyItems.clear()
            renderHistory()
        }
        binding.moreGuessAction.setOnClickListener {
            val rotated = guessItems.drop(2) + guessItems.take(2)
            renderGuessGrid(rotated)
        }
        binding.searchInput.doAfterTextChanged { editable ->
            val query = editable?.toString()?.trim().orEmpty()
            if (query.isEmpty()) {
                currentQuery = ""
                showDefaultState()
            } else if (query != currentQuery || !binding.resultContent.isVisible) {
                renderSuggestionList(query)
                showSuggestionState()
            }
        }
        binding.searchInput.setOnEditorActionListener { _, actionId, event ->
            val isSearchAction = actionId == EditorInfo.IME_ACTION_SEARCH
            val isEnterUp = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
            if (isSearchAction || isEnterUp) {
                submitSearch(binding.searchInput.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }
    }

    private fun renderHistory() {
        binding.historyChipContainer.removeAllViews()
        binding.historySection.isVisible = historyItems.isNotEmpty()
        historyItems.forEachIndexed { index, item ->
            binding.historyChipContainer.addView(createHistoryChip(item, index == 0))
        }
    }

    private fun renderGuessGrid(items: List<GuessItem> = guessItems) {
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

    private fun renderSuggestionList(query: String) {
        val suggestions = buildSuggestions(query)
        binding.suggestionContainer.removeAllViews()
        suggestions.forEachIndexed { index, item ->
            binding.suggestionContainer.addView(createSuggestionRow(item, index == suggestions.lastIndex))
        }
    }

    private fun submitSearch(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.isEmpty()) {
            showDefaultState()
            return
        }

        currentQuery = query
        addToHistory(query)
        currentFilter = ResultFilter.ALL
        currentResults = resultsForQuery(query)
        binding.searchInput.setText(query)
        binding.searchInput.setSelection(query.length)
        renderResultTabs()
        renderResults()
        showResultState()
    }

    private fun addToHistory(query: String) {
        historyItems.remove(query)
        historyItems.add(0, query)
        if (historyItems.size > 6) {
            historyItems.removeAt(historyItems.lastIndex)
        }
        renderHistory()
    }

    private fun renderResultTabs() {
        binding.resultTabContainer.removeAllViews()
        ResultFilter.values().forEachIndexed { index, filter ->
            binding.resultTabContainer.addView(createResultTab(filter).apply {
                if (index > 0) {
                    (layoutParams as LinearLayout.LayoutParams).marginStart = dp(10)
                }
            })
        }
    }

    private fun renderResults() {
        val filtered = when (currentFilter) {
            ResultFilter.ALL -> currentResults
            else -> currentResults.filter { it.filter == currentFilter }
        }

        binding.resultKeyword.text = currentQuery
        binding.resultCount.text = getString(
            R.string.search_result_count,
            filtered.size,
            currentFilter.label
        )

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

    private fun showDefaultState() {
        binding.defaultContent.isVisible = true
        binding.suggestionContent.isVisible = false
        binding.resultContent.isVisible = false
        binding.voiceContainer.isVisible = true
    }

    private fun showSuggestionState() {
        binding.defaultContent.isVisible = false
        binding.suggestionContent.isVisible = true
        binding.resultContent.isVisible = false
        binding.voiceContainer.isVisible = false
    }

    private fun showResultState() {
        binding.defaultContent.isVisible = false
        binding.suggestionContent.isVisible = false
        binding.resultContent.isVisible = true
        binding.voiceContainer.isVisible = false
    }

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
            setOnClickListener { submitSearch(text) }
        }
    }

    private fun createGuessItemView(item: GuessItem): View {
        return AppCompatTextView(this).apply {
            text = item.title
            textSize = 14f
            setTextColor(color(R.color.xhs_search_text_secondary))
            setLineSpacing(0f, 1.2f)
            setOnClickListener { submitSearch(item.title) }
        }
    }

    private fun createSuggestionRow(item: GuessItem, isLast: Boolean): View {
        return LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, dp(12))

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

            setOnClickListener { submitSearch(item.title) }
        }
    }

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
            setTypeface(typeface, if (filter == currentFilter) Typeface.BOLD else Typeface.NORMAL)
            background = roundedDrawable(
                if (filter == currentFilter) R.color.xhs_search_filter_selected_bg else android.R.color.transparent,
                17f
            )
            setTextColor(
                if (filter == currentFilter) {
                    color(R.color.xhs_search_filter_selected_text)
                } else {
                    color(R.color.xhs_search_hint_text)
                }
            )
            setOnClickListener {
                currentFilter = filter
                renderResultTabs()
                renderResults()
            }
        }
    }

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

    private fun buildSuggestions(query: String): List<GuessItem> {
        val localMatches = (historyItems.map { GuessItem(it, "最近搜过") } + guessItems)
            .distinctBy { it.title }
            .filter { it.title.contains(query, ignoreCase = true) }

        if (localMatches.isNotEmpty()) {
            return localMatches.take(6)
        }

        return listOf(
            GuessItem(query, "直接搜索"),
            GuessItem("${query}攻略", "相关笔记"),
            GuessItem("${query}测评", "近期热门"),
            GuessItem("${query}同款", "商品和搭配"),
            GuessItem("${query}合集", "高收藏内容"),
            GuessItem("${query}避雷", "经验分享")
        )
    }

    private fun resultsForQuery(query: String): List<SearchResultItem> {
        val lowerQuery = query.lowercase()
        return when {
            query.contains("张元英") -> listOf(
                SearchResultItem(
                    ResultFilter.USERS,
                    "张元英",
                    "IVE 成员，舞台直拍、妆造解析、同款穿搭都在持续更新",
                    "231.6 万人正在看相关内容",
                    "用户",
                    R.color.xhs_search_quick_action_icon_bg
                ),
                SearchResultItem(
                    ResultFilter.NOTES,
                    "张元英妆容拆解，普通人怎么画更日常",
                    "从底妆、腮红到唇色顺着复刻，附平价替代清单",
                    "2.8 万收藏 · 昨天更新",
                    "笔记",
                    R.color.xhs_search_chip_bg
                ),
                SearchResultItem(
                    ResultFilter.TOPICS,
                    "张元英多高？比例、站姿和镜头感为什么这么强",
                    "把身高、头身比和拍照姿势放在一起讲明白",
                    "热议话题 · 6421 条讨论",
                    "话题",
                    R.color.xhs_search_hot_rank_bg
                )
            )

            query.contains("数码") || lowerQuery.contains("digital") -> listOf(
                SearchResultItem(
                    ResultFilter.NOTES,
                    "2026 上半年数码好物清单",
                    "耳机、相机、平板和桌搭配件按预算分档整理",
                    "1.4 万收藏 · 本周热门",
                    "笔记",
                    R.color.xhs_search_chip_bg
                ),
                SearchResultItem(
                    ResultFilter.GOODS,
                    "学生党数码配件避坑合集",
                    "从充电头到扩展坞，把不值得买的都先排掉",
                    "商品攻略 · 9800 人已浏览",
                    "商品",
                    R.color.xhs_search_quick_action_icon_bg
                ),
                SearchResultItem(
                    ResultFilter.USERS,
                    "数码研究所",
                    "专注手机、平板、电脑真实体验和横评",
                    "优质博主 · 89.2 万粉丝",
                    "用户",
                    R.color.xhs_search_hot_rank_bg
                )
            )

            query.contains("横店") || query.contains("群演") -> listOf(
                SearchResultItem(
                    ResultFilter.NOTES,
                    "横店群演一天到底怎么过",
                    "从接戏、候场到收工，把真实流程按时间线写清楚",
                    "体验分享 · 1.1 万收藏",
                    "笔记",
                    R.color.xhs_search_chip_bg
                ),
                SearchResultItem(
                    ResultFilter.TOPICS,
                    "长得特别漂亮的群演会更容易被看到吗",
                    "现场经验、导演视角和实际机会都有人在聊",
                    "话题讨论 · 3890 条内容",
                    "话题",
                    R.color.xhs_search_hot_rank_bg
                ),
                SearchResultItem(
                    ResultFilter.USERS,
                    "横店日常观察员",
                    "长期更新群演、剧组氛围和入行建议",
                    "用户主页 · 12.8 万粉丝",
                    "用户",
                    R.color.xhs_search_quick_action_icon_bg
                )
            )

            else -> listOf(
                SearchResultItem(
                    ResultFilter.NOTES,
                    "$query 入门攻略",
                    "把常见问题、路线和注意点整理成一篇就能看懂的版本",
                    "综合热度最高 · 6 小时前更新",
                    "笔记",
                    R.color.xhs_search_chip_bg
                ),
                SearchResultItem(
                    ResultFilter.TOPICS,
                    "$query 为什么最近突然火了",
                    "从平台趋势、真实体验和热度来源三个角度梳理",
                    "话题讨论 · 2104 条内容",
                    "话题",
                    R.color.xhs_search_hot_rank_bg
                ),
                SearchResultItem(
                    ResultFilter.USERS,
                    "$query 研究社",
                    "持续分享和 $query 相关的高质量内容与合集",
                    "优质账号推荐 · 今日活跃",
                    "用户",
                    R.color.xhs_search_quick_action_icon_bg
                )
            )
        }
    }

    private fun roundedDrawable(colorRes: Int, radiusDp: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp.toInt()).toFloat()
            setColor(color(colorRes))
        }
    }

    private fun color(colorRes: Int): Int = ContextCompat.getColor(this, colorRes)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private data class GuessItem(
        val title: String,
        val meta: String
    )

    private data class SearchResultItem(
        val filter: ResultFilter,
        val title: String,
        val subtitle: String,
        val meta: String,
        val badge: String,
        val badgeColorRes: Int
    )

    private enum class ResultFilter(val label: String) {
        ALL("综合"),
        NOTES("笔记"),
        USERS("用户"),
        TOPICS("话题"),
        GOODS("商品")
    }
}
