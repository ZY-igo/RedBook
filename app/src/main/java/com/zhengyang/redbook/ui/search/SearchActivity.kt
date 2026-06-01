package com.zhengyang.redbook.ui.search

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updateLayoutParams
import com.zhengyang.redbook.R
import com.zhengyang.redbook.databinding.ActivitySearchBinding

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemBarInsets()
        bindContent()

        binding.buttonBack.setOnClickListener { finish() }
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

        val voiceBaseBottom = (binding.voiceContainer.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(binding.voiceContainer) { view, insets ->
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = voiceBaseBottom + navigationBars.bottom
            }
            insets
        }

        binding.topBar.doOnAttach { ViewCompat.requestApplyInsets(it) }
        binding.voiceContainer.doOnAttach { ViewCompat.requestApplyInsets(it) }
    }

    private fun bindContent() {
        binding.sectionHistoryTitle.text = getString(R.string.search_history_title)
        binding.sectionTrendTitle.text = getString(R.string.search_guess_title)
        binding.voiceHint.text = getString(R.string.search_voice_hint)

        val historyItems = listOf(
            getString(R.string.search_history_chip_1),
            getString(R.string.search_history_chip_2),
            getString(R.string.search_history_chip_3),
            getString(R.string.search_history_chip_4)
        )
        renderHistory(historyItems)
        binding.clearHistoryAction.setOnClickListener { renderHistory(emptyList()) }

        renderQuickActions(
            listOf(
                QuickAction(
                    title = getString(R.string.search_quick_action_note),
                    subtitle = getString(R.string.search_quick_action_note_subtitle),
                    iconRes = R.drawable.ic_xhs_search
                ),
                QuickAction(
                    title = getString(R.string.search_quick_action_user),
                    subtitle = getString(R.string.search_quick_action_user_subtitle),
                    iconRes = R.drawable.ic_xhs_user_add
                ),
                QuickAction(
                    title = getString(R.string.search_quick_action_goods),
                    subtitle = getString(R.string.search_quick_action_goods_subtitle),
                    iconRes = R.drawable.ic_xhs_store
                )
            )
        )

        renderTrending(
            listOf(
                TrendingItem(
                    title = getString(R.string.search_suggestion_1),
                    meta = getString(R.string.search_suggestion_1_meta)
                ),
                TrendingItem(
                    title = getString(R.string.search_suggestion_2),
                    meta = getString(R.string.search_suggestion_2_meta)
                ),
                TrendingItem(
                    title = getString(R.string.search_suggestion_3),
                    meta = getString(R.string.search_suggestion_3_meta)
                ),
                TrendingItem(
                    title = getString(R.string.search_suggestion_4),
                    meta = getString(R.string.search_suggestion_4_meta)
                ),
                TrendingItem(
                    title = getString(R.string.search_suggestion_5),
                    meta = getString(R.string.search_suggestion_5_meta)
                ),
                TrendingItem(
                    title = getString(R.string.search_suggestion_6),
                    meta = getString(R.string.search_suggestion_6_meta)
                )
            )
        )
    }

    private fun renderHistory(history: List<String>) {
        binding.historyChipContainer.removeAllViews()
        history.forEachIndexed { index, item ->
            binding.historyChipContainer.addView(createHistoryChip(item, index == 0))
        }
    }

    private fun renderQuickActions(actions: List<QuickAction>) {
        binding.quickActionContainer.removeAllViews()
        actions.forEachIndexed { index, action ->
            binding.quickActionContainer.addView(createQuickActionView(action, index))
        }
    }

    private fun renderTrending(items: List<TrendingItem>) {
        binding.trendingContainer.removeAllViews()
        items.forEachIndexed { index, item ->
            binding.trendingContainer.addView(createTrendingItemView(index + 1, item))
            if (index != items.lastIndex) {
                binding.trendingContainer.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(1)
                    )
                    setBackgroundColor(ContextCompat.getColor(context, R.color.xhs_search_divider))
                })
            }
        }
    }

    private fun createHistoryChip(text: String, isFirst: Boolean): View {
        return AppCompatTextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(34)
            ).also { params ->
                if (!isFirst) params.marginStart = dp(10)
            }
            background = ContextCompat.getDrawable(context, R.drawable.bg_xhs_search_chip)
            gravity = Gravity.CENTER
            minWidth = dp(72)
            setPadding(dp(14), 0, dp(14), 0)
            this.text = text
            textSize = 14f
            setTextColor(ContextCompat.getColor(context, R.color.xhs_search_text_primary))
        }
    }

    private fun createQuickActionView(action: QuickAction, index: Int): View {
        return LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).also { params ->
                if (index > 0) params.marginStart = dp(10)
            }
            background = ContextCompat.getDrawable(context, R.drawable.bg_xhs_search_quick_action)
            gravity = Gravity.CENTER_HORIZONTAL
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))

            addView(ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(34), dp(34))
                background = ContextCompat.getDrawable(context, R.drawable.bg_xhs_search_chip)
                setPadding(dp(8), dp(8), dp(8), dp(8))
                setImageResource(action.iconRes)
                imageTintList = ContextCompat.getColorStateList(context, R.color.xhs_search_icon)
            })

            addView(AppCompatTextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { params ->
                    params.topMargin = dp(12)
                }
                text = action.title
                textSize = 15f
                setTextColor(ContextCompat.getColor(context, R.color.xhs_search_text_primary))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })

            addView(AppCompatTextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { params ->
                    params.topMargin = dp(4)
                }
                gravity = Gravity.CENTER
                text = action.subtitle
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.xhs_search_hint_text))
            })
        }
    }

    private fun createTrendingItemView(rank: Int, item: TrendingItem): View {
        return LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, dp(12))

            addView(AppCompatTextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(34), dp(24))
                background = ContextCompat.getDrawable(context, R.drawable.bg_xhs_search_hot_rank)
                gravity = Gravity.CENTER
                text = rank.toString()
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.xhs_search_hot_rank_text))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
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
                    setTextColor(ContextCompat.getColor(context, R.color.xhs_search_text_primary))
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
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
                    setTextColor(ContextCompat.getColor(context, R.color.xhs_search_trend_meta))
                })
            })

            addView(ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(16), dp(16))
                setImageResource(R.drawable.ic_xhs_chevron_right)
                imageTintList = ContextCompat.getColorStateList(context, R.color.xhs_search_icon_subtle)
            })
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private data class QuickAction(
        val title: String,
        val subtitle: String,
        val iconRes: Int
    )

    private data class TrendingItem(
        val title: String,
        val meta: String
    )
}
