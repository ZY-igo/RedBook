package com.zhengyang.redbook.ui.home

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import com.zhengyang.redbook.R
import com.zhengyang.redbook.databinding.FragmentHomeBinding
import com.zhengyang.redbook.utils.dpToPx

class HomeFollowingSectionRenderer(
    private val context: Context,
    private val binding: FragmentHomeBinding,
    private val onFollowUser: (String) -> Unit,
    private val onDismissSuggestion: (String) -> Unit
) {
    fun render(state: HomeUiState, adapter: HomeAdapter) {
        val hasFollowing = state.followingUsers.isNotEmpty()
        binding.followingEmptyContainer.visibility = if (hasFollowing) View.GONE else View.VISIBLE
        binding.followingFeedContainer.visibility = if (hasFollowing) View.VISIBLE else View.GONE

        if (!hasFollowing) {
            binding.followingEmptyTitle.text = context.getString(R.string.home_following_empty_title)
            binding.followingEmptySubtitle.text = context.getString(R.string.home_following_empty_subtitle)
            binding.followingSuggestTitle.text = context.getString(R.string.home_following_suggest_title)
            binding.followingSuggestHint.text = context.getString(R.string.message_close)
            renderSuggestions(state.suggestedUsers)
            return
        }

        renderStories(state.followingUsers)
        adapter.submitList(state.followingFeedItems)
    }

    private fun renderSuggestions(users: List<FollowingUserItem>) {
        binding.followingSuggestionContainer.removeAllViews()
        users.forEach { user ->
            binding.followingSuggestionContainer.addView(createSuggestionView(user))
        }
    }

    private fun renderStories(users: List<FollowingUserItem>) {
        binding.followingStoryContainer.removeAllViews()
        users.forEach { user ->
            binding.followingStoryContainer.addView(
                LinearLayout(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).also { it.marginEnd = dp(16) }
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL

                    addView(createAvatarView(user, 58, 22f, compact = false))

                    addView(AppCompatTextView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            dp(64),
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).also { it.topMargin = dp(6) }
                        gravity = Gravity.CENTER
                        maxLines = 1
                        text = user.name
                        setTextColor(context.getColor(R.color.xhs_text_secondary))
                        textSize = 11f
                    })
                }
            )
        }
    }

    private fun createSuggestionView(user: FollowingUserItem): View {
        return LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).also { params ->
                params.marginStart = dp(12)
                params.marginEnd = dp(12)
                params.bottomMargin = dp(8)
            }
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(context.getColor(R.color.xhs_card_soft))
            }

            addView(createAvatarView(user, 44, 17f, compact = true))

            addView(LinearLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
                    it.marginStart = dp(12)
                    it.marginEnd = dp(12)
                }
                orientation = LinearLayout.VERTICAL

                addView(AppCompatTextView(context).apply {
                    text = if (user.badge == null) user.name else "${user.name}${user.badge}"
                    setTextColor(context.getColor(R.color.xhs_text_primary))
                    textSize = 15f
                    setTypeface(typeface, Typeface.BOLD)
                })

                addView(AppCompatTextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).also { it.topMargin = dp(4) }
                    text = user.subtitle
                    setTextColor(context.getColor(R.color.xhs_text_secondary))
                    textSize = 12f
                })
            })

            addView(AppCompatTextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(72), dp(30))
                text = context.getString(R.string.message_follow_cta)
                gravity = Gravity.CENTER
                includeFontPadding = false
                textSize = 13f
                setTextColor(context.getColor(R.color.xhs_accent))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(15).toFloat()
                    setStroke(dp(1), context.getColor(R.color.xhs_accent))
                    setColor(Color.TRANSPARENT)
                }
                setOnClickListener { onFollowUser(user.id) }
            })

            addView(AppCompatTextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).also {
                    it.marginStart = dp(8)
                }
                text = context.getString(R.string.close_symbol)
                textSize = 16f
                setTextColor(context.getColor(R.color.xhs_text_secondary))
                setOnClickListener { onDismissSuggestion(user.id) }
            })
        }
    }

    private fun createAvatarView(
        user: FollowingUserItem,
        sizeDp: Int,
        textSizeSp: Float,
        compact: Boolean
    ): TextView {
        return AppCompatTextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
            gravity = Gravity.CENTER
            text = user.name.take(1)
            textSize = textSizeSp
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(user.avatarColorHex))
                if (!compact) {
                    setStroke(dp(2), context.getColor(R.color.xhs_bg))
                }
            }
        }
    }

    private fun dp(value: Int): Int = value.dpToPx()
}
