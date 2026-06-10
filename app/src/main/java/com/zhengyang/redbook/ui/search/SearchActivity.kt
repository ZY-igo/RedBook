package com.zhengyang.redbook.ui.search

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import com.zhengyang.redbook.databinding.ItemSearchGuessRowBinding
import com.zhengyang.redbook.databinding.ItemSearchHistoryChipBinding
import com.zhengyang.redbook.databinding.ItemSearchResultCardBinding
import com.zhengyang.redbook.databinding.ItemSearchResultTabBinding
import com.zhengyang.redbook.databinding.ItemSearchSuggestionRowBinding
import com.zhengyang.redbook.databinding.ViewSearchResultDividerBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val viewModel: SearchViewModel by viewModels()

    private var latestState = SearchUiState()
    private var guessRotationOffset = 0
    private var isSyncingInput = false

    private val inflater: LayoutInflater by lazy(LazyThreadSafetyMode.NONE) { layoutInflater }

    private val voiceSearchLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val voiceQuery = results?.firstOrNull()?.trim().orEmpty()
            if (voiceQuery.isNotEmpty()) {
                submitQuery(voiceQuery)
            }
        }

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
        binding.searchAction.setOnClickListener { submitCurrentInput() }
        binding.buttonScan.setOnClickListener { submitCurrentInputOrPrompt() }
        binding.voiceButton.setOnClickListener { launchVoiceSearch() }
        binding.clearHistoryAction.setOnClickListener { viewModel.clearHistory() }
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
            val isEnterUp =
                event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP
            if (isSearchAction || isEnterUp) {
                submitCurrentInput()
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
            val chipBinding = ItemSearchHistoryChipBinding.inflate(
                inflater,
                binding.historyChipContainer,
                false
            )
            (chipBinding.root.layoutParams as? LinearLayout.LayoutParams)?.let { params ->
                if (index > 0) {
                    params.marginStart = dp(8)
                }
            }
            chipBinding.historyChip.text = item
            chipBinding.historyChip.setOnClickListener { viewModel.submitSearch(item) }
            binding.historyChipContainer.addView(chipBinding.root)
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
            val rowBinding = ItemSearchGuessRowBinding.inflate(
                inflater,
                binding.trendingContainer,
                false
            )
            (rowBinding.root.layoutParams as? LinearLayout.LayoutParams)?.let { params ->
                if (rowIndex > 0) {
                    params.topMargin = dp(14)
                }
            }
            bindGuessText(rowBinding.leftGuess, rowItems[0])
            val rightItem = rowItems.getOrNull(1)
            if (rightItem != null) {
                bindGuessText(rowBinding.rightGuess, rightItem)
                rowBinding.rightGuess.isVisible = true
                rowBinding.rightSpacer.isVisible = false
            } else {
                rowBinding.rightGuess.isVisible = false
                rowBinding.rightSpacer.isVisible = true
            }
            binding.trendingContainer.addView(rowBinding.root)
        }
    }

    private fun bindGuessText(view: TextView, item: SearchGuessItem) {
        view.text = item.title
        view.setLineSpacing(0f, 1.2f)
        view.setOnClickListener { viewModel.submitSearch(item.title) }
    }

    private fun renderSuggestions() {
        binding.suggestionContainer.removeAllViews()
        latestState.suggestionItems.forEach { item ->
            val rowBinding = ItemSearchSuggestionRowBinding.inflate(
                inflater,
                binding.suggestionContainer,
                false
            )
            rowBinding.suggestionTitle.text = item.title
            rowBinding.suggestionMeta.text = item.meta
            rowBinding.root.setOnClickListener { viewModel.submitSearch(item.title) }
            binding.suggestionContainer.addView(rowBinding.root)
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
            val tabBinding = ItemSearchResultTabBinding.inflate(
                inflater,
                binding.resultTabContainer,
                false
            )
            (tabBinding.root.layoutParams as? LinearLayout.LayoutParams)?.let { params ->
                if (index > 0) {
                    params.marginStart = dp(10)
                }
            }
            bindResultTab(tabBinding, filter)
            binding.resultTabContainer.addView(tabBinding.root)
        }

        binding.resultListContainer.removeAllViews()
        filtered.forEachIndexed { index, item ->
            val cardBinding = ItemSearchResultCardBinding.inflate(
                inflater,
                binding.resultListContainer,
                false
            )
            bindResultCard(cardBinding, item)
            binding.resultListContainer.addView(cardBinding.root)
            if (index != filtered.lastIndex) {
                val dividerBinding = ViewSearchResultDividerBinding.inflate(
                    inflater,
                    binding.resultListContainer,
                    false
                )
                binding.resultListContainer.addView(dividerBinding.root)
            }
        }
    }

    private fun bindResultTab(binding: ItemSearchResultTabBinding, filter: ResultFilter) {
        val selected = filter == latestState.currentFilter
        binding.resultTab.text = filter.label
        binding.resultTab.setTypeface(
            binding.resultTab.typeface,
            if (selected) Typeface.BOLD else Typeface.NORMAL
        )
        binding.resultTab.backgroundTintList = ColorStateList.valueOf(
            if (selected) {
                color(R.color.xhs_search_filter_selected_bg)
            } else {
                color(android.R.color.transparent)
            }
        )
        binding.resultTab.setTextColor(
            if (selected) {
                color(R.color.xhs_search_filter_selected_text)
            } else {
                color(R.color.xhs_search_hint_text)
            }
        )
        binding.resultTab.setOnClickListener { viewModel.changeFilter(filter) }
    }

    private fun bindResultCard(binding: ItemSearchResultCardBinding, item: SearchResultItem) {
        binding.resultBadge.text = item.badge
        binding.resultBadge.backgroundTintList = ColorStateList.valueOf(color(item.badgeColorRes))
        binding.resultTitle.text = item.title
        binding.resultSubtitle.text = item.subtitle
        binding.resultMeta.text = item.meta
    }

    private fun showState(mode: SearchScreenMode) {
        binding.defaultContent.isVisible = mode == SearchScreenMode.DEFAULT
        binding.suggestionContent.isVisible = mode == SearchScreenMode.SUGGESTION
        binding.resultContent.isVisible = mode == SearchScreenMode.RESULT
        binding.voiceContainer.isVisible = mode == SearchScreenMode.DEFAULT
    }

    private fun submitQuery(query: String) {
        isSyncingInput = true
        binding.searchInput.setText(query)
        binding.searchInput.setSelection(query.length)
        isSyncingInput = false
        viewModel.submitSearch(query)
    }

    private fun submitCurrentInput() {
        viewModel.submitSearch(binding.searchInput.text?.toString().orEmpty())
    }

    private fun submitCurrentInputOrPrompt() {
        val query = binding.searchInput.text?.toString()?.trim().orEmpty()
        if (query.isNotEmpty()) {
            submitQuery(query)
            return
        }

        binding.searchInput.requestFocus()
        Toast.makeText(this, "请先输入要搜索的内容", Toast.LENGTH_SHORT).show()
    }

    private fun launchVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.search_voice_hint))
        }
        try {
            voiceSearchLauncher.launch(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "当前设备不支持语音识别", Toast.LENGTH_SHORT).show()
        }
    }

    private fun color(colorRes: Int): Int = ContextCompat.getColor(this, colorRes)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
