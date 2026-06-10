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

/**
 * “关注”页头部区域渲染器。
 *
 * 在“未关注任何人”时渲染推荐用户卡片，
 * 在已有关注关系时渲染故事头像条和关注 feed。
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

    /**
     * 根据首页状态刷新“关注”区域。
     *
     * @param state 当前首页状态。
     * @param adapter 用于承载关注 feed 的列表适配器。
     */
    fun render(state: HomeUiState, adapter: HomeAdapter) {
        val hasFollowing = state.followingUsers.isNotEmpty()
        binding.followingEmptyContainer.visibility = if (hasFollowing) View.GONE else View.VISIBLE
        binding.followingFeedContainer.visibility = if (hasFollowing) View.VISIBLE else View.GONE

        if (!hasFollowing) {
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

        renderStories(state.followingUsers)
        adapter.submitList(state.followingFeedItems)
    }

    /**
     * 渲染推荐关注用户列表。
     */
    private fun renderSuggestions(users: List<FollowingUserItem>) {
        binding.followingSuggestionContainer.removeAllViews()
        users.forEach { user ->
            val itemBinding = ItemHomeFollowingSuggestionBinding.inflate(
                inflater,
                binding.followingSuggestionContainer,
                false
            )
            itemBinding.nameText.text = if (user.badge == null) user.name else "${user.name}${user.badge}"
            itemBinding.subtitleText.text = user.subtitle
            itemBinding.followButton.setOnClickListener { onFollowUser(user.id) }
            itemBinding.dismissButton.setOnClickListener { onDismissSuggestion(user.id) }
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

    /**
     * 渲染已关注用户的头像故事条。
     */
    private fun renderStories(users: List<FollowingUserItem>) {
        binding.followingStoryContainer.removeAllViews()
        users.forEach { user ->
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

    /**
     * 绑定用户头像。
     *
     * 当头像 URL 不可用时，会退化成首字母头像。
     */
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
            "bind following avatar, userId=${user.id}, userName=${user.name}, avatarUrl=${user.avatarUrl}"
        )
        val avatarBackground = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(user.avatarColorHex))
            if (!compact) {
                setStroke(2, context.getColor(R.color.xhs_bg))
            }
        }
        container.background = null
        avatarText.text = user.name.take(1)
        avatarText.textSize = textSizeSp
        avatarText.setTypeface(avatarText.typeface, Typeface.BOLD)
        avatarText.background = avatarBackground
        avatarImage.background = avatarBackground.constantState?.newDrawable()?.mutate()
        avatarImage.clipToOutline = true
        if (user.avatarUrl.isNullOrBlank()) {
            avatarImage.visibility = View.GONE
            avatarText.visibility = View.VISIBLE
            return
        }

        avatarImage.load(user.avatarUrl) {
            crossfade(true)
            listener(
                onStart = {
                    avatarImage.visibility = View.GONE
                    avatarText.visibility = View.VISIBLE
                },
                onSuccess = { _, _ ->
                    AppLogger.d(
                        TAG,
                        "following avatar load success, userId=${user.id}, avatarUrl=${user.avatarUrl}"
                    )
                    avatarImage.visibility = View.VISIBLE
                    avatarText.visibility = View.GONE
                },
                onError = { _, result ->
                    AppLogger.w(
                        TAG,
                        "following avatar load error, userId=${user.id}, avatarUrl=${user.avatarUrl}, message=${result.throwable.message}",
                        result.throwable
                    )
                    avatarImage.visibility = View.GONE
                    avatarText.visibility = View.VISIBLE
                }
            )
        }
    }
}
