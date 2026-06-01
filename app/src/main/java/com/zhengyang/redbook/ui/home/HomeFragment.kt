package com.zhengyang.redbook.ui.home

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.postDelayed
import androidx.core.view.updateLayoutParams
import androidx.core.view.doOnAttach
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.zhengyang.redbook.R
import com.zhengyang.redbook.databinding.FragmentHomeBinding
import com.zhengyang.redbook.ui.home.HomeMockData.DiscoverCategory
import com.zhengyang.redbook.ui.home.HomeMockData.FollowingUser
import com.zhengyang.redbook.ui.search.SearchActivity
import com.zhengyang.redbook.utils.dpToPx

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val adapter = HomeAdapter()
    private val followingAdapter = HomeAdapter()
    private val suggestedUsers = HomeMockData.suggestedFollowingUsers().toMutableList()
    private val followingUsers = mutableListOf<FollowingUser>()
    private val allChannels = listOf(
        DiscoverCategory.RECOMMEND,
        DiscoverCategory.RED,
        DiscoverCategory.LIVE,
        DiscoverCategory.DRAMA,
        DiscoverCategory.TIPS,
        DiscoverCategory.OUTFIT,
        DiscoverCategory.FOOD,
        DiscoverCategory.EMOTION,
        DiscoverCategory.TRAVEL,
        DiscoverCategory.PHOTO,
        DiscoverCategory.CAR,
        DiscoverCategory.DANCE,
        DiscoverCategory.AVATAR,
        DiscoverCategory.WALLPAPER,
        DiscoverCategory.FUNNY,
        DiscoverCategory.FITNESS,
        DiscoverCategory.HOME,
        DiscoverCategory.GROOMING,
        DiscoverCategory.CAREER,
        DiscoverCategory.MUSIC,
        DiscoverCategory.RENOVATION,
        DiscoverCategory.TECH,
        DiscoverCategory.FILM,
        DiscoverCategory.PAINTING,
        DiscoverCategory.READING,
        DiscoverCategory.STUDY,
        DiscoverCategory.SNEAKERS,
        DiscoverCategory.SCIENCE,
        DiscoverCategory.GAME,
        DiscoverCategory.ART,
        DiscoverCategory.WEDDING,
        DiscoverCategory.ANIME,
        DiscoverCategory.CRAFT,
        DiscoverCategory.FAT_LOSS,
        DiscoverCategory.MOTOR,
        DiscoverCategory.SPORTS,
        DiscoverCategory.PET,
        DiscoverCategory.CELEBRITY,
        DiscoverCategory.CULTURE,
        DiscoverCategory.SOCIAL,
        DiscoverCategory.OUTDOOR,
        DiscoverCategory.MOM_BABY,
        DiscoverCategory.SKINCARE,
        DiscoverCategory.PSYCHOLOGY,
        DiscoverCategory.ESPORTS,
        DiscoverCategory.VARIETY,
        DiscoverCategory.TOYS,
        DiscoverCategory.CAMPUS,
        DiscoverCategory.CAMPING
    )
    private val myChannels = mutableListOf(
        DiscoverCategory.RECOMMEND,
        DiscoverCategory.RED,
        DiscoverCategory.LIVE,
        DiscoverCategory.DRAMA,
        DiscoverCategory.TIPS,
        DiscoverCategory.OUTFIT,
        DiscoverCategory.FOOD,
        DiscoverCategory.EMOTION,
        DiscoverCategory.TRAVEL,
        DiscoverCategory.PHOTO,
        DiscoverCategory.CAR,
        DiscoverCategory.DANCE,
        DiscoverCategory.AVATAR,
        DiscoverCategory.WALLPAPER,
        DiscoverCategory.FUNNY,
        DiscoverCategory.FITNESS,
        DiscoverCategory.HOME,
        DiscoverCategory.GROOMING,
        DiscoverCategory.CAREER,
        DiscoverCategory.MUSIC,
        DiscoverCategory.RENOVATION,
        DiscoverCategory.TECH,
        DiscoverCategory.FILM,
        DiscoverCategory.PAINTING,
        DiscoverCategory.READING
    )
    private var currentCategory = DiscoverCategory.RECOMMEND
    private var isCategoryExpanded = false
    private var isChannelEditMode = false
    private var currentTopTab = TopTab.DISCOVER

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applySystemBarInsets()
        setupTopTabs()
        setupDrawer()
        setupRecyclerViews()
        setupCategoryTabs()
        setupFollowingPage()

        binding.discoverRefreshLayout.setOnRefreshListener {
            renderCategory(currentCategory)
            binding.discoverRefreshLayout.postDelayed(720L) {
                _binding?.discoverRefreshLayout?.setRefreshing(false)
            }
        }

        binding.buttonMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.buttonSearch.setOnClickListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }
        renderCategory(currentCategory)
        renderTopTab(currentTopTab)
    }

    private fun applySystemBarInsets() {
        val baseTopMargin = (binding.topBar.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { topBar, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = baseTopMargin + statusBarInsets.top
            }
            insets
        }
        val drawerTopPadding = binding.drawerContent.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerContent) { drawerContent, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            drawerContent.setPadding(
                drawerContent.paddingLeft,
                drawerTopPadding + systemBars.top,
                drawerContent.paddingRight,
                drawerContent.paddingBottom
            )
            insets
        }
        val footerBottomPadding = binding.drawerFooter.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.drawerFooter) { drawerFooter, insets ->
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            drawerFooter.setPadding(
                drawerFooter.paddingLeft,
                drawerFooter.paddingTop,
                drawerFooter.paddingRight,
                footerBottomPadding + navigationBars.bottom
            )
            insets
        }
        binding.topBar.doOnAttach { ViewCompat.requestApplyInsets(it) }
        binding.drawerContent.doOnAttach { ViewCompat.requestApplyInsets(it) }
        binding.drawerFooter.doOnAttach { ViewCompat.requestApplyInsets(it) }
    }

    private fun setupTopTabs() {
        binding.tabFollowing.text = getString(R.string.tab_following)
        binding.tabDiscover.text = getString(R.string.tab_discover)
        binding.tabNearby.text = getString(R.string.tab_nearby)

        binding.tabFollowing.setOnClickListener { renderTopTab(TopTab.FOLLOWING) }
        binding.tabDiscover.setOnClickListener { renderTopTab(TopTab.DISCOVER) }
        binding.tabNearby.setOnClickListener { renderTopTab(TopTab.NEARBY) }
    }

    private fun renderTopTab(tab: TopTab) {
        currentTopTab = tab
        binding.tabFollowing.isSelected = tab == TopTab.FOLLOWING
        binding.tabDiscover.isSelected = tab == TopTab.DISCOVER
        binding.tabNearby.isSelected = tab == TopTab.NEARBY

        val showDiscoverContent = tab != TopTab.FOLLOWING
        binding.discoverContentContainer.visibility = if (showDiscoverContent) View.VISIBLE else View.GONE
        binding.followingContentContainer.visibility = if (showDiscoverContent) View.GONE else View.VISIBLE

        if (!showDiscoverContent) {
            setCategoryExpanded(false, animate = false)
            renderFollowingContent()
            return
        }

        renderCategory(
            when (tab) {
                TopTab.NEARBY -> DiscoverCategory.TRAVEL
                else -> currentCategory
            }
        )
    }

    private fun setupDrawer() {
        binding.drawerLayout.setScrimColor(requireContext().getColor(R.color.xhs_scrim))
        configureDrawerSection(
            binding.root.findViewById<View>(R.id.drawerAddFriends).parent as ViewGroup,
            listOf(DrawerRowModel(R.drawable.ic_xhs_user_add, getString(R.string.drawer_add_friends)))
        )
        configureDrawerSection(
            binding.root.findViewById<View>(R.id.drawerCreatorCenter).parent as ViewGroup,
            listOf(DrawerRowModel(R.drawable.ic_xhs_flash, getString(R.string.drawer_creator_center)))
        )
        configureDrawerSection(
            binding.root.findViewById<View>(R.id.drawerDrafts).parent as ViewGroup,
            listOf(
                DrawerRowModel(R.drawable.ic_xhs_file, getString(R.string.drawer_drafts)),
                DrawerRowModel(
                    R.drawable.ic_xhs_bell,
                    getString(R.string.drawer_activity),
                    getString(R.string.drawer_badge_new)
                ),
                DrawerRowModel(R.drawable.ic_xhs_history, getString(R.string.drawer_history)),
                DrawerRowModel(R.drawable.ic_xhs_download, getString(R.string.drawer_downloads))
            )
        )
        configureDrawerSection(
            binding.root.findViewById<View>(R.id.drawerOrders).parent as ViewGroup,
            listOf(
                DrawerRowModel(R.drawable.ic_xhs_order, getString(R.string.drawer_orders)),
                DrawerRowModel(R.drawable.ic_xhs_cart, getString(R.string.drawer_cart)),
                DrawerRowModel(R.drawable.ic_xhs_wallet, getString(R.string.drawer_wallet))
            )
        )
        configureDrawerSection(
            binding.root.findViewById<View>(R.id.drawerMiniApps).parent as ViewGroup,
            listOf(
                DrawerRowModel(R.drawable.ic_xhs_link, getString(R.string.drawer_mini_apps)),
                DrawerRowModel(R.drawable.ic_xhs_camera, getString(R.string.drawer_moments))
            )
        )
        configureFooter()
    }

    private fun configureDrawerSection(container: ViewGroup, rows: List<DrawerRowModel>) {
        rows.forEachIndexed { index, row ->
            val item = container.getChildAt(index) ?: return@forEachIndexed
            item.findViewById<ImageView>(R.id.icon).setImageResource(row.iconRes)
            item.findViewById<TextView>(R.id.title).text = row.title
            item.findViewById<TextView>(R.id.badge).apply {
                text = row.badge.orEmpty()
                visibility = if (row.badge == null) View.GONE else View.VISIBLE
            }
        }
    }

    private fun configureFooter() {
        val labels = listOf(
            getString(R.string.drawer_scan),
            getString(R.string.drawer_help),
            getString(R.string.drawer_settings)
        )
        val icons = listOf(
            R.drawable.ic_xhs_scan,
            R.drawable.ic_xhs_headset,
            R.drawable.ic_xhs_settings
        )
        labels.indices.forEach { index ->
            val item = binding.drawerFooter.getChildAt(index) as ViewGroup
            item.findViewById<ImageView>(R.id.footerIcon).setImageResource(icons[index])
            item.findViewById<TextView>(R.id.footerLabel).text = labels[index]
        }
    }

    private fun setupRecyclerViews() {
        binding.recyclerView.adapter = adapter
        binding.recyclerView.setHasFixedSize(false)
        if (binding.recyclerView.itemDecorationCount == 0) {
            binding.recyclerView.addItemDecoration(HomeSpacingDecoration())
        }

        binding.followingRecyclerView.adapter = followingAdapter
        binding.followingRecyclerView.setHasFixedSize(false)
        binding.followingRecyclerView.layoutManager = createLayoutManager(DiscoverCategory.RECOMMEND)
        if (binding.followingRecyclerView.itemDecorationCount == 0) {
            binding.followingRecyclerView.addItemDecoration(HomeSpacingDecoration())
        }
    }

    private fun setupFollowingPage() {
        binding.followingRefreshLayout.setOnRefreshListener {
            renderFollowingContent()
            binding.followingRefreshLayout.postDelayed(720L) {
                _binding?.followingRefreshLayout?.setRefreshing(false)
            }
        }
    }

    private fun setupCategoryTabs() {
        binding.categoryExpand.setOnClickListener {
            setCategoryExpanded(!isCategoryExpanded)
        }
        binding.panelCollapse.setOnClickListener {
            setCategoryExpanded(false)
        }
        binding.panelEditButton.setOnClickListener {
            isChannelEditMode = !isChannelEditMode
            renderChannelManager()
        }
        binding.panelCollapse.rotation = 180f
        renderCompactCategoryTabs()
        renderChannelManager()
        setCategoryExpanded(false, animate = false)
    }

    private fun renderCategory(category: DiscoverCategory) {
        currentCategory = category
        renderCompactCategoryTabs()
        renderChannelManager()
        binding.recyclerView.layoutManager = createLayoutManager(category)
        adapter.submitList(HomeMockData.itemsFor(category))
    }

    private fun renderFollowingContent() {
        val hasFollowing = followingUsers.isNotEmpty()
        binding.followingEmptyContainer.visibility = if (hasFollowing) View.GONE else View.VISIBLE
        binding.followingFeedContainer.visibility = if (hasFollowing) View.VISIBLE else View.GONE

        if (!hasFollowing) {
            binding.followingEmptyTitle.text = getString(R.string.home_following_empty_title)
            binding.followingEmptySubtitle.text = getString(R.string.home_following_empty_subtitle)
            binding.followingSuggestTitle.text = getString(R.string.home_following_suggest_title)
            binding.followingSuggestHint.text = getString(R.string.message_close)
            renderFollowingSuggestions()
            return
        }

        renderFollowingStories()
        followingAdapter.submitList(HomeMockData.followingFeedItems())
    }

    private fun renderFollowingSuggestions() {
        val container = binding.followingSuggestionContainer
        container.removeAllViews()
        suggestedUsers.forEach { user ->
            container.addView(createFollowingSuggestionView(user))
        }
    }

    private fun createFollowingSuggestionView(user: FollowingUser): View {
        return LinearLayout(requireContext()).apply {
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
                setColor(requireContext().getColor(R.color.xhs_card_soft))
            }

            addView(createAvatarView(user, 44, 17f, true))

            addView(LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).also {
                    it.marginStart = dp(12)
                    it.marginEnd = dp(12)
                }
                orientation = LinearLayout.VERTICAL

                addView(AppCompatTextView(requireContext()).apply {
                    text = if (user.badge == null) user.name else "${user.name}${user.badge}"
                    setTextColor(requireContext().getColor(R.color.xhs_text_primary))
                    textSize = 15f
                    setTypeface(typeface, Typeface.BOLD)
                })

                addView(AppCompatTextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).also { it.topMargin = dp(4) }
                    text = user.subtitle
                    setTextColor(requireContext().getColor(R.color.xhs_text_secondary))
                    textSize = 12f
                })
            })

            addView(AppCompatTextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dp(72), dp(30))
                text = getString(R.string.message_follow_cta)
                textSize = 13f
                setTextColor(requireContext().getColor(R.color.xhs_accent))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(15).toFloat()
                    setStroke(dp(1), requireContext().getColor(R.color.xhs_accent))
                    setColor(Color.TRANSPARENT)
                }
                setOnClickListener {
                    followUser(user)
                }
            })

            addView(AppCompatTextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(dp(28), dp(28)).also {
                    it.marginStart = dp(8)
                }
                text = getString(R.string.close_symbol)
                textSize = 16f
                setTextColor(requireContext().getColor(R.color.xhs_text_secondary))
                setOnClickListener {
                    dismissSuggestion(user)
                }
            })
        }
    }

    private fun renderFollowingStories() {
        val container = binding.followingStoryContainer
        container.removeAllViews()
        followingUsers.forEach { user ->
            container.addView(LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { it.marginEnd = dp(16) }
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL

                addView(createAvatarView(user, 58, 22f, false))

                addView(AppCompatTextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        dp(64),
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).also { it.topMargin = dp(6) }
                    gravity = Gravity.CENTER
                    maxLines = 1
                    text = user.name
                    setTextColor(requireContext().getColor(R.color.xhs_text_secondary))
                    textSize = 11f
                })
            })
        }
    }

    private fun createAvatarView(
        user: FollowingUser,
        sizeDp: Int,
        textSizeSp: Float,
        compact: Boolean
    ): TextView {
        return AppCompatTextView(requireContext()).apply {
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
                    setStroke(dp(2), requireContext().getColor(R.color.xhs_bg))
                }
            }
        }
    }

    private fun followUser(user: FollowingUser) {
        if (followingUsers.any { it.id == user.id }) return
        followingUsers.add(user)
        suggestedUsers.removeAll { it.id == user.id }
        renderFollowingContent()
    }

    private fun dismissSuggestion(user: FollowingUser) {
        suggestedUsers.removeAll { it.id == user.id }
        renderFollowingSuggestions()
    }

    private fun renderCompactCategoryTabs() {
        val container = binding.compactCategoryContainer
        container.removeAllViews()
        val textColors = AppCompatResources.getColorStateList(
            requireContext(),
            R.color.xhs_category_tab_text
        )
        myChannels.forEach { category ->
            container.addView(AppCompatTextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { params ->
                    params.marginEnd = dp(22)
                }
                minHeight = dp(38)
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, dp(12))
                background = AppCompatResources.getDrawable(requireContext(), R.drawable.bg_xhs_category_tab)
                setTextColor(textColors)
                textSize = 14f
                text = category.title
                isSelected = category == currentCategory
                setOnClickListener {
                    renderCategory(category)
                }
            })
        }
    }

    private fun renderChannelManager() {
        binding.panelEditButton.text = if (isChannelEditMode) getString(R.string.home_channel_edit_done) else getString(R.string.home_channel_edit_enter)
        binding.myChannelHint.text = if (isChannelEditMode) getString(R.string.home_channel_hint_delete) else getString(R.string.home_channel_hint_enter)
        renderChannelGrid(binding.myChannelContainer, myChannels, true)
        renderChannelGrid(
            binding.recommendedChannelContainer,
            allChannels.filterNot(myChannels::contains),
            false
        )
    }

    private fun renderChannelGrid(
        container: LinearLayout,
        channels: List<DiscoverCategory>,
        isMyChannelSection: Boolean
    ) {
        container.removeAllViews()
        channels.chunked(4).forEach { rowItems ->
            val row = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { params ->
                    params.bottomMargin = dp(12)
                }
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            rowItems.forEach { category ->
                row.addView(createChannelItemView(category, isMyChannelSection))
            }

            repeat(4 - rowItems.size) {
                row.addView(View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 0, 1f).also { params ->
                        params.marginStart = dp(6)
                        params.marginEnd = dp(6)
                    }
                })
            }
            container.addView(row)
        }
    }

    private fun createChannelItemView(
        category: DiscoverCategory,
        isMyChannelSection: Boolean
    ): View {
        val wrapper = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                .also { params ->
                    params.marginStart = dp(6)
                    params.marginEnd = dp(6)
                }
        }

        val chip = AppCompatTextView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(36)
            )
            gravity = Gravity.CENTER
            text = if (isMyChannelSection) category.title else "+${category.title}"
            textSize = 14f
            setTextColor(requireContext().getColor(R.color.xhs_text_primary))
            background = AppCompatResources.getDrawable(
                requireContext(),
                if (isMyChannelSection && category == currentCategory && !isChannelEditMode) {
                    R.drawable.bg_xhs_channel_chip_selected
                } else {
                    R.drawable.bg_xhs_channel_chip
                }
            )
            setOnClickListener {
                if (isMyChannelSection) {
                    if (!isChannelEditMode) {
                        renderCategory(category)
                        setCategoryExpanded(false)
                    }
                } else {
                    addChannel(category)
                }
            }
        }
        wrapper.addView(chip)

        if (isMyChannelSection && isChannelEditMode && canRemoveChannel(category)) {
            wrapper.addView(AppCompatTextView(requireContext()).apply {
                layoutParams = FrameLayout.LayoutParams(dp(18), dp(18), Gravity.TOP or Gravity.END)
                    .also { params ->
                        params.topMargin = dp(-6)
                        params.marginEnd = dp(-4)
                    }
                gravity = Gravity.CENTER
                text = getString(R.string.close_symbol)
                background = AppCompatResources.getDrawable(
                    requireContext(),
                    R.drawable.bg_xhs_channel_delete_badge
                )
                setTextColor(ColorStateList.valueOf(requireContext().getColor(R.color.xhs_card)))
                setOnClickListener {
                    removeChannel(category)
                }
            })
        }
        return wrapper
    }

    private fun addChannel(category: DiscoverCategory) {
        if (myChannels.contains(category)) return
        myChannels.add(category)
        renderCompactCategoryTabs()
        renderChannelManager()
    }

    private fun removeChannel(category: DiscoverCategory) {
        if (!canRemoveChannel(category)) return
        val removedCurrent = currentCategory == category
        myChannels.remove(category)
        if (removedCurrent) {
            renderCategory(myChannels.first())
        } else {
            renderCompactCategoryTabs()
            renderChannelManager()
        }
    }

    private fun canRemoveChannel(category: DiscoverCategory): Boolean {
        return category != DiscoverCategory.RECOMMEND && myChannels.size > 1
    }

    private fun setCategoryExpanded(expanded: Boolean, animate: Boolean = true) {
        val panelVisible = binding.categoryManagerPanel.visibility == View.VISIBLE
        if (expanded == isCategoryExpanded && panelVisible == expanded) return

        isCategoryExpanded = expanded
        if (!expanded && isChannelEditMode) {
            isChannelEditMode = false
            renderChannelManager()
        }

        binding.categoryExpand.animate().cancel()
        binding.categoryManagerPanel.animate().cancel()

        val rotationTarget = if (expanded) 180f else 0f
        if (animate) {
            binding.categoryExpand.animate()
                .rotation(rotationTarget)
                .setDuration(180L)
                .start()
        } else {
            binding.categoryExpand.rotation = rotationTarget
        }

        if (expanded) {
            binding.categoryManagerPanel.alpha = 0f
            binding.categoryManagerPanel.translationY = -dp(10).toFloat()
            binding.categoryManagerPanel.visibility = View.VISIBLE
            if (animate) {
                binding.categoryManagerPanel.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(180L)
                    .start()
            } else {
                binding.categoryManagerPanel.alpha = 1f
                binding.categoryManagerPanel.translationY = 0f
            }
        } else if (animate) {
            binding.categoryManagerPanel.animate()
                .alpha(0f)
                .translationY(-dp(10).toFloat())
                .setDuration(160L)
                .withEndAction {
                    binding.categoryManagerPanel.visibility = View.GONE
                }
                .start()
        } else {
            binding.categoryManagerPanel.alpha = 0f
            binding.categoryManagerPanel.translationY = -dp(10).toFloat()
            binding.categoryManagerPanel.visibility = View.GONE
        }
    }

    private fun createLayoutManager(category: DiscoverCategory): RecyclerView.LayoutManager {
        return if (category.usesWaterfall) {
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL).apply {
                gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
            }
        } else {
            GridLayoutManager(requireContext(), 2)
        }
    }

    private fun dp(value: Int): Int = value.dpToPx()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class DrawerRowModel(
        val iconRes: Int,
        val title: String,
        val badge: String? = null
    )

    private enum class TopTab {
        FOLLOWING,
        DISCOVER,
        NEARBY
    }
}
