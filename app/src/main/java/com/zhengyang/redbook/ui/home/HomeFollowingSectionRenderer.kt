/**
 * 文件说明：HomeFollowingSectionRenderer.kt
 * 作用：负责首页关注页顶部区块与推荐用户区的视图渲染逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.home

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import coil.load
import com.zhengyang.redbook.R
import com.zhengyang.redbook.databinding.FragmentHomeBinding
import com.zhengyang.redbook.utils.AppLogger
import com.zhengyang.redbook.utils.dpToPx

/**
 * 首页关注区渲染器
 *
 * 负责根据首页状态渲染关注页空态、推荐用户列表、已关注用户故事条和关注流列表。
 * 该类只处理关注页顶部内容区的视图拼装，不负责状态计算。
 */
class HomeFollowingSectionRenderer(
    /** 当前页面上下文。 */
    private val context: Context,

    /** 首页视图绑定对象。 */
    private val binding: FragmentHomeBinding,

    /** 点击关注推荐用户时的回调。 */
    private val onFollowUser: (String) -> Unit,

    /** 点击关闭推荐用户时的回调。 */
    private val onDismissSuggestion: (String) -> Unit
) {
    companion object {
        private const val TAG = "FollowingAvatar"
    }

    /**
     * 渲染关注页顶部区块。
     *
     * @param state 最新首页状态。
     * @param adapter 关注流列表适配器。
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
     * 渲染推荐用户列表。
     *
     * @param users 推荐用户集合。
     */
    private fun renderSuggestions(users: List<FollowingUserItem>) {
        binding.followingSuggestionContainer.removeAllViews()
        users.forEach { user ->
            binding.followingSuggestionContainer.addView(createSuggestionView(user))
        }
    }

    /**
     * 渲染已关注用户故事条。
     *
     * @param users 已关注用户集合。
     */
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

    /**
     * 创建单个推荐用户卡片视图。
     *
     * @param user 当前推荐用户模型。
     * @return 可直接加入容器的推荐卡片视图。
     */
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

    /**
     * 创建用户头像视图。
     *
     * @param user 当前用户模型。
     * @param sizeDp 头像尺寸。
     * @param textSizeSp 头像文字字号。
     * @param compact 是否使用紧凑样式。
     * @return 用户头像文本视图。
     */
    private fun createAvatarView(
        user: FollowingUserItem,
        sizeDp: Int,
        textSizeSp: Float,
        compact: Boolean
    ): View {
        AppLogger.d(
            TAG,
            "bind following avatar, userId=${user.id}, userName=${user.name}, avatarUrl=${user.avatarUrl}"
        )
        val avatarBackground = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(user.avatarColorHex))
            if (!compact) {
                setStroke(dp(2), context.getColor(R.color.xhs_bg))
            }
        }
        return FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
            val avatarTextView = AppCompatTextView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER
                text = user.name.take(1)
                textSize = textSizeSp
                setTextColor(Color.WHITE)
                setTypeface(typeface, Typeface.BOLD)
                background = avatarBackground
            }
            addView(AppCompatImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = avatarBackground.constantState?.newDrawable()?.mutate()
                clipToOutline = true
                if (user.avatarUrl.isNullOrBlank()) {
                    visibility = View.GONE
                } else {
                    load(user.avatarUrl) {
                        crossfade(true)
                        listener(
                            onStart = {
                                visibility = View.GONE
                                avatarTextView.visibility = View.VISIBLE
                            },
                            onSuccess = { _, _ ->
                                AppLogger.d(
                                    TAG,
                                    "following avatar load success, userId=${user.id}, avatarUrl=${user.avatarUrl}"
                                )
                                visibility = View.VISIBLE
                                avatarTextView.visibility = View.GONE
                            },
                            onError = { _, result ->
                                AppLogger.w(
                                    TAG,
                                    "following avatar load error, userId=${user.id}, avatarUrl=${user.avatarUrl}, message=${result.throwable.message}",
                                    result.throwable
                                )
                                visibility = View.GONE
                                avatarTextView.visibility = View.VISIBLE
                            }
                        )
                    }
                }
            })
            addView(avatarTextView)
        }
    }

    /**
     * 将 dp 转换为像素值。
     *
     * @param value 需要转换的 dp 数值。
     * @return 对应像素值。
     */
    private fun dp(value: Int): Int = value.dpToPx()
}
