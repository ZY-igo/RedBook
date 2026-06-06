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

@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val viewModel: SearchViewModel by viewModels()

    private var latestState = SearchUiState()
    private var guessRotationOffset = 0
    private var isSyncingInput = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemBarInsets()
        setupInteractions()
        collectUiState()
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

    private fun collectUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

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

    private fun renderHistory() {
        binding.historyChipContainer.removeAllViews()
        binding.historySection.isVisible = latestState.historyItems.isNotEmpty()
        latestState.historyItems.forEachIndexed { index, item ->
            binding.historyChipContainer.addView(createHistoryChip(item, index == 0))
        }
    }

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

    private fun renderSuggestions() {
        binding.suggestionContainer.removeAllViews()
        latestState.suggestionItems.forEachIndexed { index, item ->
            binding.suggestionContainer.addView(
                createSuggestionRow(item, index == latestState.suggestionItems.lastIndex)
            )
        }
    }

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

    private fun showState(mode: SearchScreenMode) {
        binding.defaultContent.isVisible = mode == SearchScreenMode.DEFAULT
        binding.suggestionContent.isVisible = mode == SearchScreenMode.SUGGESTION
        binding.resultContent.isVisible = mode == SearchScreenMode.RESULT
        binding.voiceContainer.isVisible = mode == SearchScreenMode.DEFAULT
    }

    private fun submitPresetQuery(query: String) {
        isSyncingInput = true
        binding.searchInput.setText(query)
        binding.searchInput.setSelection(query.length)
        isSyncingInput = false
        viewModel.submitSearch(query)
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
            setOnClickListener { viewModel.submitSearch(text) }
        }
    }

    private fun createGuessItemView(item: SearchGuessItem): View {
        return AppCompatTextView(this).apply {
            text = item.title
            textSize = 14f
            setTextColor(color(R.color.xhs_search_text_secondary))
            setLineSpacing(0f, 1.2f)
            setOnClickListener { viewModel.submitSearch(item.title) }
        }
    }

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

    private fun roundedDrawable(colorRes: Int, radiusDp: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp.toInt()).toFloat()
            setColor(color(colorRes))
        }
    }

    private fun color(colorRes: Int): Int = ContextCompat.getColor(this, colorRes)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
