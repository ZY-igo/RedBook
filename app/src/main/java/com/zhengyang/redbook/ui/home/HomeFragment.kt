/**
 * 文件说明： HomeFragment.kt
 * 作用： 承载首页相关的界面状态与交互逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
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
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.postDelayed
import androidx.core.view.updateLayoutParams
import androidx.core.view.doOnAttach
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.drawerlayout.widget.DrawerLayout
import com.zhengyang.redbook.R
import com.zhengyang.redbook.databinding.FragmentHomeBinding
import com.zhengyang.redbook.ui.note.NoteDetailActivity
import com.zhengyang.redbook.ui.search.SearchActivity
import com.zhengyang.redbook.utils.dpToPx
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 首页 Fragment
 * 包含三个顶部标签页：关注、发现、附近
 * 支持频道切换、侧边栏、下拉刷新等功能
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    // ViewBinding 只在 onCreateView 到 onDestroyView 之间有效。
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    // 发现页与关注页分别维护各自的列表适配器，避免相互污染展示状态。
    private val adapter = HomeAdapter()
    private val followingAdapter = HomeAdapter()

    // 系统支持的全部频道，用于“更多频道”区域做补集展示。
    private val allChannels = mutableListOf<DiscoverCategoryItem>()

    // “我的频道”是用户当前顶部可见的频道集合，可增删、可重排展示区域。
    private val myChannels = mutableListOf<DiscoverCategoryItem>()

    // 当前发现页实际选中的频道。
    private var currentCategory: DiscoverCategoryItem? = null

    // 频道管理面板是否处于展开态。
    private var isCategoryExpanded = false

    // 频道管理面板是否处于编辑模式。
    private var isChannelEditMode = false

    // 顶部一级 Tab 当前选中项。
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

        // 先完成所有静态 UI 与监听器初始化，再统一触发首次渲染。
        applySystemBarInsets()
        setupTopTabs()
        setupDrawer()
        setupRecyclerViews()
        setupCategoryTabs()
        setupFollowingPage()
        collectUiState()

        binding.discoverRefreshLayout.setOnRefreshListener {
            currentCategory?.let(viewModel::refreshDiscover)
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
        renderTopTab(currentTopTab)
    }

    /**
     * 处理沉浸式布局下的系统栏 inset。
     * 这里分别修正：
     * 1. 顶部栏的 topMargin，避免压到状态栏；
     * 2. 抽屉内容区的顶部 padding，避免抽屉头部被遮挡；
     * 3. 抽屉底部功能区的 bottom padding，避免被导航栏遮挡。
     */
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

    /**
     * 初始化顶部三个一级 Tab 的标题与点击事件。
     * 这里只负责事件绑定，真正的视图切换由 renderTopTab 统一处理。
     */
    private fun setupTopTabs() {
        binding.tabFollowing.text = getString(R.string.tab_following)
        binding.tabDiscover.text = getString(R.string.tab_discover)
        binding.tabNearby.text = getString(R.string.tab_nearby)

        binding.tabFollowing.setOnClickListener { renderTopTab(TopTab.FOLLOWING) }
        binding.tabDiscover.setOnClickListener { renderTopTab(TopTab.DISCOVER) }
        binding.tabNearby.setOnClickListener { renderTopTab(TopTab.NEARBY) }
    }

    /**
     * 根据一级 Tab 切换页面主区域。
     * FOLLOWING: 展示关注流/空态；
     * DISCOVER: 展示当前已选频道；
     * NEARBY: 这里复用发现流，并固定渲染为 TRAVEL 频道的内容。
     */
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
            return
        }

        // “附近”没有单独数据源，当前用旅行频道模拟其内容表现。
        val targetCategory = when (tab) {
            TopTab.NEARBY -> allChannels.firstOrNull { it.id == "travel" }
            else -> currentCategory
        }
        targetCategory?.let(::renderCategory)
    }

    /**
     * 初始化侧边抽屉。
     * 包括遮罩色、抽屉开关时的系统栏风格切换，以及各功能分组的文案/图标填充。
     */
    private fun setupDrawer() {
        binding.drawerLayout.setScrimColor(requireContext().getColor(R.color.xhs_scrim))
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                updateSystemBarsForDrawer(isDrawerOpen = true)
            }

            override fun onDrawerClosed(drawerView: View) {
                updateSystemBarsForDrawer(isDrawerOpen = false)
            }
        })
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
        updateSystemBarsForDrawer(isDrawerOpen = false)
    }

    /**
     * 抽屉打开时将系统栏背景切为抽屉底色，并关闭浅色图标；
     * 抽屉关闭后恢复首页背景与浅色图标策略。
     */
    private fun updateSystemBarsForDrawer(isDrawerOpen: Boolean) {
        val window = activity?.window ?: return
        val bgColor = requireContext().getColor(if (isDrawerOpen) R.color.xhs_drawer_bg else R.color.xhs_bg)
        window.statusBarColor = bgColor
        window.navigationBarColor = bgColor
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDrawerOpen
            isAppearanceLightNavigationBars = !isDrawerOpen
        }
    }

    // 将一组抽屉行模型按顺序映射到容器中已有的子 View。
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

    // 配置抽屉底部三个快捷入口。
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

    /**
     * 初始化两个 RecyclerView。
     * 发现页与关注页共用同一套卡片样式，但数据源、点击事件和布局状态彼此独立。
     */
    private fun setupRecyclerViews() {
        adapter.onItemClick = { item ->
            startActivity(NoteDetailActivity.createIntent(requireContext(), item))
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.setHasFixedSize(false)
        if (binding.recyclerView.itemDecorationCount == 0) {
            binding.recyclerView.addItemDecoration(HomeSpacingDecoration())
        }

        followingAdapter.onItemClick = { item ->
            startActivity(NoteDetailActivity.createIntent(requireContext(), item))
        }
        binding.followingRecyclerView.adapter = followingAdapter
        binding.followingRecyclerView.setHasFixedSize(false)
        binding.followingRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        if (binding.followingRecyclerView.itemDecorationCount == 0) {
            binding.followingRecyclerView.addItemDecoration(HomeSpacingDecoration())
        }
    }

    // 关注页下拉刷新只做本地重新渲染，延迟关闭刷新动画以模拟网络请求反馈。
    private fun setupFollowingPage() {
        binding.followingRefreshLayout.setOnRefreshListener {
            viewModel.refreshFollowing()
            binding.followingRefreshLayout.postDelayed(720L) {
                _binding?.followingRefreshLayout?.setRefreshing(false)
            }
        }
    }

    /**
     * 初始化频道栏与频道管理面板相关交互。
     * 包括展开/收起、编辑模式切换，以及首帧的默认渲染。
     */
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

    /**
     * 渲染当前频道对应的发现流内容。
     * 每次切频道时都同步刷新顶部紧凑频道栏、展开面板选中态，以及列表布局管理器。
     */
    private fun renderCategory(category: DiscoverCategoryItem) {
        currentCategory = category
        renderCompactCategoryTabs()
        renderChannelManager()
        binding.recyclerView.layoutManager = createLayoutManager(category)
        viewModel.refreshDiscover(category)
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::renderHomeState)
            }
        }
    }

    private fun renderHomeState(state: HomeUiState) {
        syncCategories(state.categories)
        adapter.submitList(state.discoverItems)
        renderFollowingContent(state)
    }

    private fun syncCategories(categories: List<DiscoverCategoryItem>) {
        if (categories.isEmpty()) return

        allChannels.clear()
        allChannels.addAll(categories)

        if (myChannels.isEmpty()) {
            myChannels.addAll(categories.filter { it.isDefaultSelected })
        } else {
            val selectedIds = myChannels.map { it.id }.toSet()
            myChannels.clear()
            myChannels.addAll(categories.filter { it.id in selectedIds })
            if (myChannels.isEmpty()) {
                myChannels.addAll(categories.filter { it.isDefaultSelected })
            }
        }

        val currentId = currentCategory?.id
        currentCategory = myChannels.firstOrNull { it.id == currentId } ?: myChannels.firstOrNull()
        renderCompactCategoryTabs()
        renderChannelManager()
    }

    /**
     * 渲染“关注”页主内容。
     * 没有关注用户时显示空态与推荐关注；
     * 有关注用户时显示顶部 stories 区域和下方 feed 列表。
     */
    private fun renderFollowingContent(state: HomeUiState) {
        val hasFollowing = state.followingUsers.isNotEmpty()
        binding.followingEmptyContainer.visibility = if (hasFollowing) View.GONE else View.VISIBLE
        binding.followingFeedContainer.visibility = if (hasFollowing) View.VISIBLE else View.GONE

        if (!hasFollowing) {
            binding.followingEmptyTitle.text = getString(R.string.home_following_empty_title)
            binding.followingEmptySubtitle.text = getString(R.string.home_following_empty_subtitle)
            binding.followingSuggestTitle.text = getString(R.string.home_following_suggest_title)
            binding.followingSuggestHint.text = getString(R.string.message_close)
            renderFollowingSuggestions(state.suggestedUsers)
            return
        }

        renderFollowingStories(state.followingUsers)
        followingAdapter.submitList(state.followingFeedItems)
    }

    // 重新生成推荐关注卡片列表。这里直接移除并重建，逻辑简单且数据量小。
    private fun renderFollowingSuggestions(users: List<FollowingUserItem>) {
        val container = binding.followingSuggestionContainer
        container.removeAllViews()
        users.forEach { user ->
            container.addView(createFollowingSuggestionView(user))
        }
    }

    /**
     * 动态创建单个“推荐关注”卡片。
     * 右侧两个操作分别对应：
     * 1. 关注该用户并将其迁移到 followingUsers；
     * 2. 关闭该推荐，仅从推荐列表移除。
     */
    private fun createFollowingSuggestionView(user: FollowingUserItem): View {
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
                gravity = Gravity.CENTER
                includeFontPadding = false
                textSize = 13f
                setTextColor(requireContext().getColor(R.color.xhs_accent))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(15).toFloat()
                    setStroke(dp(1), requireContext().getColor(R.color.xhs_accent))
                    setColor(Color.TRANSPARENT)
                }
                setOnClickListener {
                    viewModel.followUser(user.id)
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
                    viewModel.dismissSuggestion(user.id)
                }
            })
        }
    }

    // 渲染关注页顶部横向头像区，作为“已关注用户”的快速可视化入口。
    private fun renderFollowingStories(users: List<FollowingUserItem>) {
        val container = binding.followingStoryContainer
        container.removeAllViews()
        users.forEach { user ->
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

    /**
     * 创建一个纯文本头像。
     * 头像使用用户名首字 + 纯色圆形背景模拟，避免引入真实图片资源。
     * compact=false 时会额外加描边，使其在 stories 场景里与背景分离得更清楚。
     */
    private fun createAvatarView(
        user: FollowingUserItem,
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

    /**
     * 渲染顶部紧凑频道栏。
     * 该区域只显示“我的频道”，用于快速切换内容分类。
     */
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
                isSelected = category.id == currentCategory?.id
                setOnClickListener {
                    renderCategory(category)
                }
            })
        }
    }

    /**
     * 渲染展开后的频道管理面板。
     * 上半部分是“我的频道”，下半部分是“推荐频道”。
     * 编辑模式下会同步切换按钮文案与提示语。
     */
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

    /**
     * 将频道按每行 4 个进行网格化排布。
     * 不足 4 个时补空白占位，保证整行宽度与间距稳定。
     */
    private fun renderChannelGrid(
        container: LinearLayout,
        channels: List<DiscoverCategoryItem>,
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

    /**
     * 创建频道管理面板中的单个频道项。
     * “我的频道”在非编辑态下点击是切换频道；
     * “推荐频道”点击是加入到我的频道；
     * “我的频道”在编辑态下且允许删除时，会额外显示右上角删除徽标。
     */
    private fun createChannelItemView(
        category: DiscoverCategoryItem,
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
                if (isMyChannelSection && category.id == currentCategory?.id && !isChannelEditMode) {
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

        // 推荐频道不显示删除入口；默认频道“推荐”也不允许删除。
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

    // 把频道加入“我的频道”，随后同步刷新顶部紧凑栏和展开面板。
    private fun addChannel(category: DiscoverCategoryItem) {
        if (myChannels.contains(category)) return
        myChannels.add(category)
        renderCompactCategoryTabs()
        renderChannelManager()
    }

    /**
     * 从“我的频道”移除指定频道。
     * 如果删除的是当前正在浏览的频道，则自动回退到剩余频道中的第一个，避免出现空选中态。
     */
    private fun removeChannel(category: DiscoverCategoryItem) {
        if (!canRemoveChannel(category)) return
        val removedCurrent = currentCategory?.id == category.id
        myChannels.remove(category)
        if (removedCurrent) {
            myChannels.firstOrNull()?.let(::renderCategory)
        } else {
            renderCompactCategoryTabs()
            renderChannelManager()
        }
    }

    // 至少保留一个频道，且默认“推荐”频道不可删除。
    private fun canRemoveChannel(category: DiscoverCategoryItem): Boolean {
        return category.id != "recommend" && myChannels.size > 1
    }

    /**
     * 控制频道管理面板的展开/收起状态，并处理箭头旋转与淡入淡出动画。
     * 收起时如果仍处于编辑模式，会先退出编辑模式，保证下次展开回到普通浏览态。
     */
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

        // 展开时箭头朝上，收起时箭头恢复朝下。
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
            // 先把面板放到略微上移且透明的初始状态，再执行展开动画。
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
            // 收起时动画结束再真正 GONE，避免动画过程中布局突然消失。
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

    /**
     * 根据频道的展示形式选择列表布局：
     * usesWaterfall=true 使用瀑布流；
     * 否则使用固定 2 列网格。
     */
    private fun createLayoutManager(category: DiscoverCategoryItem): RecyclerView.LayoutManager {
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
        // 主动断开 adapter 对 View 的引用，减少 Fragment View 销毁后的持有风险。
        binding.recyclerView.adapter = null
        binding.followingRecyclerView.adapter = null
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
