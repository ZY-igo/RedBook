package com.zhengyang.redbook.ui.home

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import coil.load
import com.zhengyang.redbook.R
import com.zhengyang.redbook.databinding.FragmentHomeBinding
import com.zhengyang.redbook.databinding.ItemHomeFollowingStoryBinding
import com.zhengyang.redbook.databinding.ItemHomeFollowingSuggestionBinding
import com.zhengyang.redbook.utils.AppLogger
import com.zhengyang.redbook.utils.applyCircleAvatarDefaults

/**
 * “关注”页头部区域渲染器。
 *
 * 负责根据当前首页状态渲染两种模式：
 * 1. 没有关注任何人时，显示推荐关注用户卡片
 * 2. 已经有关注关系时，显示顶部故事头像条和关注 feed
 */
class HomeFollowingSectionRenderer(
    private val context: Context,
    private val binding: FragmentHomeBinding,
    private val onFollowUser: (String) -> Unit,
    private val onDismissSuggestion: (String) -> Unit
) {
    companion object {
        private const val TAG = "FollowingAvatar"
    }

    private val inflater: LayoutInflater by lazy(LazyThreadSafetyMode.NONE) {
        LayoutInflater.from(context)
    }

    fun render(state: HomeUiState, adapter: HomeAdapter) {
        val hasFollowing = state.followingUsers.isNotEmpty()
        AppLogger.d(
            TAG,
            "render: hasFollowing=$hasFollowing, followingUsers=${state.followingUsers.size}, suggestedUsers=${state.suggestedUsers.size}, feedItems=${state.followingFeedItems.size}, followingErrorMessage=${state.followingErrorMessage}"
        )
        binding.followingEmptyContainer.visibility = if (hasFollowing) View.GONE else View.VISIBLE
        binding.followingFeedContainer.visibility = if (hasFollowing) View.VISIBLE else View.GONE

        if (!hasFollowing) {
            AppLogger.d(TAG, "render: route=empty_state_with_suggestions")
            binding.followingEmptyTitle.text = context.getString(R.string.home_following_empty_title)
            binding.followingEmptySubtitle.text = if (state.followingErrorMessage.isNullOrBlank()) {
                context.getString(R.string.home_following_empty_subtitle)
            } else {
                context.getString(R.string.home_feed_offline_following)
            }
            binding.followingSuggestTitle.text = context.getString(R.string.home_following_suggest_title)
            binding.followingSuggestHint.text = context.getString(R.string.message_close)
            renderSuggestions(state.suggestedUsers)
            return
        }

        AppLogger.d(TAG, "render: route=stories_and_feed")
        renderStories(state.followingUsers)
        adapter.submitList(state.followingFeedItems)
    }

    private fun renderSuggestions(users: List<FollowingUserItem>) {
        AppLogger.d(TAG, "renderSuggestions: users=${users.size}")
        binding.followingSuggestionContainer.removeAllViews()
        users.forEach { user ->
            AppLogger.d(
                TAG,
                "renderSuggestions item: userId=${user.id}, name=${user.name}, badge=${user.badge}, subtitle=${user.subtitle}, avatarUrl=${user.avatarUrl}"
            )
            val itemBinding = ItemHomeFollowingSuggestionBinding.inflate(
                inflater,
                binding.followingSuggestionContainer,
                false
            )
            itemBinding.nameText.text = if (user.badge == null) user.name else "${user.name}${user.badge}"
            itemBinding.subtitleText.text = user.subtitle
            itemBinding.followButton.setOnClickListener {
                AppLogger.d(TAG, "renderSuggestions follow click: userId=${user.id}")
                onFollowUser(user.id)
            }
            itemBinding.dismissButton.setOnClickListener {
                AppLogger.d(TAG, "renderSuggestions dismiss click: userId=${user.id}")
                onDismissSuggestion(user.id)
            }
            bindAvatar(
                user = user,
                container = itemBinding.avatarContainer,
                avatarText = itemBinding.avatarText,
                avatarImage = itemBinding.avatarImage,
                textSizeSp = 17f,
                compact = true
            )
            binding.followingSuggestionContainer.addView(itemBinding.root)
        }
    }

    private fun renderStories(users: List<FollowingUserItem>) {
        AppLogger.d(TAG, "renderStories: users=${users.size}")
        binding.followingStoryContainer.removeAllViews()
        users.forEach { user ->
            AppLogger.d(
                TAG,
                "renderStories item: userId=${user.id}, name=${user.name}, avatarUrl=${user.avatarUrl}"
            )
            val itemBinding = ItemHomeFollowingStoryBinding.inflate(
                inflater,
                binding.followingStoryContainer,
                false
            )
            itemBinding.nameText.text = user.name
            bindAvatar(
                user = user,
                container = itemBinding.avatarContainer,
                avatarText = itemBinding.avatarText,
                avatarImage = itemBinding.avatarImage,
                textSizeSp = 22f,
                compact = false
            )
            binding.followingStoryContainer.addView(itemBinding.root)
        }
    }

    private fun bindAvatar(
        user: FollowingUserItem,
        container: FrameLayout,
        avatarText: TextView,
        avatarImage: AppCompatImageView,
        textSizeSp: Float,
        compact: Boolean
    ) {
        AppLogger.d(
            TAG,
            "bindAvatar: userId=${user.id}, userName=${user.name}, avatarUrl=${user.avatarUrl}, avatarColorHex=${user.avatarColorHex}, textSizeSp=$textSizeSp, compact=$compact"
        )
        val avatarBackground = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(user.avatarColorHex))
            if (!compact) {
                setStroke(2, context.getColor(R.color.xhs_bg))
            }
        }
        AppLogger.d(
            TAG,
            "bindAvatar: prepared background compact=$compact, hasStroke=${!compact}"
        )
        container.background = null
        avatarText.text = user.name.take(1)
        avatarText.textSize = textSizeSp
        avatarText.setTypeface(avatarText.typeface, Typeface.BOLD)
        avatarText.background = avatarBackground
        avatarImage.background = avatarBackground.constantState?.newDrawable()?.mutate()

        if (user.avatarUrl.isNullOrBlank()) {
            AppLogger.d(
                TAG,
                "bindAvatar: avatarUrl empty, fallback to text avatar for userId=${user.id}"
            )
            avatarImage.visibility = View.GONE
            avatarText.visibility = View.VISIBLE
            return
        }

        AppLogger.d(TAG, "bindAvatar: start async image load for userId=${user.id}")
        avatarImage.load(user.avatarUrl) {
            applyCircleAvatarDefaults()
            listener(
                onStart = {
                    AppLogger.d(
                        TAG,
                        "bindAvatar onStart: keep text avatar visible while loading, userId=${user.id}"
                    )
                    avatarImage.visibility = View.GONE
                    avatarText.visibility = View.VISIBLE
                },
                onSuccess = { _, _ ->
                    AppLogger.d(
                        TAG,
                        "bindAvatar onSuccess: show remote avatar, userId=${user.id}, avatarUrl=${user.avatarUrl}"
                    )
                    avatarImage.visibility = View.VISIBLE
                    avatarText.visibility = View.GONE
                },
                onError = { _, result ->
                    AppLogger.w(
                        TAG,
                        "bindAvatar onError: fallback to text avatar, userId=${user.id}, avatarUrl=${user.avatarUrl}, message=${result.throwable.message}",
                        result.throwable
                    )
                    avatarImage.visibility = View.GONE
                    avatarText.visibility = View.VISIBLE
                }
            )
        }
    }
}
