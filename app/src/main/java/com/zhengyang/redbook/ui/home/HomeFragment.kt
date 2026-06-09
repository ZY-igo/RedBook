/**
 * 文件说明：HomeFragment.kt
 * 作用：承载首页模块的视图渲染、状态消费与交互分发逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.home

import android.content.Intent
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewConfiguration
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updateLayoutParams
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import coil.ImageLoader
import com.google.android.material.snackbar.Snackbar
import com.zhengyang.redbook.R
import com.zhengyang.redbook.databinding.FragmentHomeBinding
import com.zhengyang.redbook.ui.note.NoteDetailActivity
import com.zhengyang.redbook.ui.search.SearchActivity
import com.zhengyang.redbook.utils.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 首页主界面片段
 *
 * 负责协调发现流、关注流、抽屉、频道管理和网络提示等首页交互，
 * 并根据 [HomeViewModel] 输出的状态刷新页面内容。
 */
@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    /** 首页视图绑定对象，仅在 View 生命周期内有效。 */
    private var _binding: FragmentHomeBinding? = null
    /** 非空视图绑定访问器，仅允许在视图已创建阶段访问。 */
    private val binding get() = _binding!!
    /** 首页状态提供者，负责产出页面状态与一次性事件。 */
    private val viewModel: HomeViewModel by viewModels()

    @Inject
    lateinit var imageLoader: ImageLoader

    /** 发现流列表适配器。 */
    private lateinit var adapter: HomeAdapter
    /** 发现流滑动预览层适配器。 */
    private lateinit var previewAdapter: HomeAdapter
    /** 关注流列表适配器。 */
    private lateinit var followingAdapter: HomeAdapter
    /** 发现流底部状态适配器。 */
    private lateinit var discoverFooterAdapter: HomeFeedFooterAdapter
    /** 关注流底部状态适配器。 */
    private lateinit var followingFooterAdapter: HomeFeedFooterAdapter
    /** 频道管理协调器，负责维护频道选择与编辑状态。 */
    private val channelCoordinator = HomeChannelCoordinator()
    /** 网络断开时展示的提示条。 */
    private var networkSnackbar: Snackbar? = null
    /** 网络状态回调，用于监听首页可见期间的网络变化。 */
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /** 关注页顶部区块渲染器。 */
    private var followingSectionRenderer: HomeFollowingSectionRenderer? = null
    /** 分类栏与频道面板渲染器。 */
    private var channelRenderer: HomeChannelRenderer? = null

    /** 当前频道管理面板是否处于展开状态。 */
    private var isCategoryExpanded = false
    /** 当前选中的顶部页签。 */
    private var currentTopTab = TopTab.DISCOVER
    private var isCategorySectionVisible = true
    private var isDiscoverTransitionAnimating = false
    private var activeDiscoverSwipe: DiscoverSwipeSession? = null

    /**
     * 初始化首页视图与交互。
     *
     * @param view 当前 Fragment 根视图。
     * @param savedInstanceState 系统恢复时传入的状态快照。
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)
        adapter = HomeAdapter(imageLoader)
        previewAdapter = HomeAdapter(imageLoader)
        followingAdapter = HomeAdapter(imageLoader)
        discoverFooterAdapter = HomeFeedFooterAdapter()
        followingFooterAdapter = HomeFeedFooterAdapter()
        followingSectionRenderer = HomeFollowingSectionRenderer(
            context = requireContext(),
            binding = binding,
            onFollowUser = viewModel::followUser,
            onDismissSuggestion = viewModel::dismissSuggestion
        )
        channelRenderer = HomeChannelRenderer(
            context = requireContext(),
            binding = binding
        )

        applySystemBarInsets()
        setupTopTabs()
        setupDrawer()
        setupRecyclerViews()
        setupPagination()
        setupCategorySectionScrollBehavior()
        setupCategoryTabs()
        setupFollowingPage()
        collectUiState()

        binding.discoverRefreshLayout.setOnRefreshListener {
            channelCoordinator.currentCategory?.let(viewModel::refreshDiscover)
        }
        binding.buttonMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.buttonSearch.setOnClickListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }

        renderTopTab(currentTopTab)
        showCategorySection(animate = false)
    }

    /**
     * 处理系统栏和抽屉区域的安全内边距。
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
     * 初始化顶部页签文案与点击事件。
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
     * 切换顶部页签并刷新对应内容区。
     *
     * @param tab 当前需要展示的顶部页签。
     */
    private fun renderTopTab(tab: TopTab) {
        currentTopTab = tab
        binding.tabFollowing.isSelected = tab == TopTab.FOLLOWING
        binding.tabDiscover.isSelected = tab == TopTab.DISCOVER
        binding.tabNearby.isSelected = tab == TopTab.NEARBY
        showCategorySection(animate = false)

        val showDiscoverContent = tab != TopTab.FOLLOWING
        binding.discoverContentContainer.visibility = if (showDiscoverContent) View.VISIBLE else View.GONE
        binding.followingContentContainer.visibility = if (showDiscoverContent) View.GONE else View.VISIBLE

        if (!showDiscoverContent) {
            setCategoryExpanded(false, animate = false)
            return
        }

        val targetCategory = when (tab) {
            TopTab.NEARBY -> channelCoordinator.findNearbyFallbackCategory()
            else -> channelCoordinator.currentCategory
        }
        targetCategory?.let(::renderCategory)
    }

    /**
     * 初始化抽屉内容与抽屉状态监听。
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
     * 根据抽屉开合状态调整系统栏外观。
     *
     * @param isDrawerOpen 当前抽屉是否处于打开状态。
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
        adapter.onItemClick = { item ->
            startActivity(NoteDetailActivity.createIntent(requireContext(), item))
        }
        binding.recyclerView.adapter = ConcatAdapter(adapter, discoverFooterAdapter)
        binding.recyclerView.setHasFixedSize(false)
        binding.recyclerView.itemAnimator = null
        if (binding.recyclerView.itemDecorationCount == 0) {
            binding.recyclerView.addItemDecoration(HomeSpacingDecoration())
        }

        previewAdapter.onItemClick = { item ->
            startActivity(NoteDetailActivity.createIntent(requireContext(), item))
        }
        binding.discoverSwipePreviewRecyclerView.adapter = previewAdapter
        binding.discoverSwipePreviewRecyclerView.setHasFixedSize(false)
        binding.discoverSwipePreviewRecyclerView.itemAnimator = null
        if (binding.discoverSwipePreviewRecyclerView.itemDecorationCount == 0) {
            binding.discoverSwipePreviewRecyclerView.addItemDecoration(HomeSpacingDecoration())
        }

        followingAdapter.onItemClick = { item ->
            startActivity(NoteDetailActivity.createIntent(requireContext(), item))
        }
        binding.followingRecyclerView.adapter = ConcatAdapter(followingAdapter, followingFooterAdapter)
        binding.followingRecyclerView.setHasFixedSize(false)
        binding.followingRecyclerView.itemAnimator = null
        binding.followingRecyclerView.layoutManager = createGridLayoutManager(
            spanCount = 2,
            adapterProvider = { binding.followingRecyclerView.adapter }
        )
        if (binding.followingRecyclerView.itemDecorationCount == 0) {
            binding.followingRecyclerView.addItemDecoration(HomeSpacingDecoration())
        }

        setupDiscoverSwipeNavigation()
    }

    private fun setupPagination() {
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                if (shouldLoadMore(recyclerView)) {
                    channelCoordinator.currentCategory?.let(viewModel::loadMoreDiscover)
                }
            }
        })
        binding.followingRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                if (shouldLoadMore(recyclerView)) {
                    viewModel.loadMoreFollowing()
                }
            }
        })
    }

    private fun setupCategorySectionScrollBehavior() {
        binding.recyclerView.addOnScrollListener(createCategorySectionScrollListener())
        binding.followingRecyclerView.addOnScrollListener(createCategorySectionScrollListener())
    }

    private fun createCategorySectionScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (isCategoryExpanded) return
                if (!recyclerView.canScrollVertically(-1)) {
                    showCategorySection()
                    return
                }
                if (currentTopTab == TopTab.FOLLOWING) {
                    return
                }
                if (dy > 0) {
                    hideCategorySection()
                }
            }
        }
    }

    private fun setupFollowingPage() {
        binding.followingRefreshLayout.setOnRefreshListener {
            viewModel.refreshFollowing(force = true)
        }
    }

    private fun setupDiscoverSwipeNavigation() {
        val touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop
        binding.recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            private var downX = 0f
            private var downY = 0f
            private var isDragging = false

            override fun onInterceptTouchEvent(
                recyclerView: RecyclerView,
                event: MotionEvent
            ): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.x
                        downY = event.y
                        isDragging = false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if (!canHandleDiscoverSwipe()) return false

                        val deltaX = event.x - downX
                        val deltaY = event.y - downY
                        val absDeltaX = kotlin.math.abs(deltaX)
                        val absDeltaY = kotlin.math.abs(deltaY)
                        val isHorizontalSwipe =
                            absDeltaX > touchSlop &&
                                absDeltaX > absDeltaY * HORIZONTAL_SWIPE_DOMINANCE_RATIO
                        if (!isHorizontalSwipe) return false

                        if (!isDragging) {
                            isDragging = startDiscoverSwipe(toNext = deltaX < 0f)
                        }
                        if (isDragging) {
                            updateDiscoverSwipe(deltaX)
                        }
                        return isDragging
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        if (isDragging) {
                            finishDiscoverSwipe(commit = shouldCommitDiscoverSwipe(event.x - downX))
                        }
                        isDragging = false
                    }
                }
                return isDragging
            }

            override fun onTouchEvent(recyclerView: RecyclerView, event: MotionEvent) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_MOVE -> {
                        if (isDragging) {
                            updateDiscoverSwipe(event.x - downX)
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            finishDiscoverSwipe(commit = shouldCommitDiscoverSwipe(event.x - downX))
                            isDragging = false
                        }
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        if (isDragging) {
                            finishDiscoverSwipe(commit = false)
                            isDragging = false
                        }
                    }
                }
            }
        })
    }

    private fun setupCategoryTabs() {
        binding.categoryExpand.setOnClickListener {
            setCategoryExpanded(!isCategoryExpanded)
        }
        binding.panelCollapse.setOnClickListener {
            setCategoryExpanded(false)
        }
        binding.panelEditButton.setOnClickListener {
            channelCoordinator.toggleEditMode()
            renderChannelManager()
        }
        binding.panelCollapse.rotation = 180f
        renderCompactCategoryTabs()
        renderChannelManager()
        setCategoryExpanded(false, animate = false)
    }

    private fun renderCategory(category: DiscoverCategoryItem) {
        applySelectedCategory(category)
        viewModel.selectDiscoverCategory(category)
    }

    private fun applySelectedCategory(category: DiscoverCategoryItem) {
        channelCoordinator.setCurrentCategory(category)
        renderCompactCategoryTabs()
        renderChannelManager()
        binding.recyclerView.layoutManager = createLayoutManager(category)
    }

    private fun canHandleDiscoverSwipe(): Boolean {
        return currentTopTab == TopTab.DISCOVER &&
            !isCategoryExpanded &&
            !isDiscoverTransitionAnimating &&
            channelCoordinator.myChannels.size > 1
    }

    private fun startDiscoverSwipe(toNext: Boolean): Boolean {
        val channels = channelCoordinator.myChannels
        val currentCategory = channelCoordinator.currentCategory ?: return false
        val currentIndex = channels.indexOfFirst { it.id == currentCategory.id }
        if (currentIndex == -1) return false

        val targetIndex = if (toNext) currentIndex + 1 else currentIndex - 1
        val targetCategory = channels.getOrNull(targetIndex) ?: return false
        val contentWidth = binding.discoverContentContainer.width
            .takeIf { it > 0 }
            ?: binding.root.width
            .takeIf { it > 0 }
            ?: return false
        bindDiscoverSwipePreview(targetCategory)
        activeDiscoverSwipe = DiscoverSwipeSession(
            targetCategory = targetCategory,
            toNext = toNext,
            width = contentWidth.toFloat()
        )
        isDiscoverTransitionAnimating = true
        binding.recyclerView.stopScroll()
        binding.discoverSwipePreviewRecyclerView.stopScroll()
        binding.discoverSwipePreviewContainer.visibility = View.VISIBLE
        resetDiscoverSwipeTranslations()
        return true
    }

    private fun updateDiscoverSwipe(deltaX: Float) {
        val session = activeDiscoverSwipe ?: return
        val width = session.width
        val constrainedDelta = deltaX.coerceIn(-width, width)
        if (session.toNext && constrainedDelta > 0f) return
        if (!session.toNext && constrainedDelta < 0f) return

        val previewBaseTranslation = if (session.toNext) width else -width
        setDiscoverSwipeTranslations(
            currentTranslation = constrainedDelta,
            previewTranslation = previewBaseTranslation + constrainedDelta
        )
    }

    private fun shouldCommitDiscoverSwipe(deltaX: Float): Boolean {
        val session = activeDiscoverSwipe ?: return false
        return kotlin.math.abs(deltaX) >= session.width * DISCOVER_SWIPE_COMMIT_THRESHOLD
    }

    private fun finishDiscoverSwipe(commit: Boolean) {
        val session = activeDiscoverSwipe ?: return
        val width = session.width
        val currentTarget = if (commit) {
            if (session.toNext) -width else width
        } else {
            0f
        }
        val previewStart = if (session.toNext) width else -width
        val previewTarget = if (commit) 0f else previewStart

        val animatedViews = discoverSwipeAnimatedViews()
        val animatedPreview = binding.discoverSwipePreviewContainer
        var remainingAnimations = animatedViews.size + 1
        val onAnimationEnd = {
            remainingAnimations -= 1
            if (remainingAnimations == 0) {
                if (commit) {
                    applySelectedCategory(session.targetCategory)
                    viewModel.selectDiscoverCategory(session.targetCategory)
                }
                clearDiscoverSwipeState()
            }
        }

        animatedViews.forEach { view ->
            view.animate()
                .translationX(currentTarget)
                .setDuration(DISCOVER_SWIPE_SETTLE_DURATION)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction(onAnimationEnd)
                .start()
        }
        animatedPreview.animate()
            .translationX(previewTarget)
            .setDuration(DISCOVER_SWIPE_SETTLE_DURATION)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction(onAnimationEnd)
            .start()
    }

    private fun bindDiscoverSwipePreview(category: DiscoverCategoryItem) {
        binding.discoverSwipePreviewRecyclerView.layoutManager = createLayoutManager(category)
        previewAdapter.submitList(viewModel.peekDiscoverItems(category.id) ?: HomeSkeletonFactory.discover())
    }

    private fun resetDiscoverSwipeTranslations() {
        val session = activeDiscoverSwipe ?: return
        val previewStart = if (session.toNext) session.width else -session.width
        setDiscoverSwipeTranslations(
            currentTranslation = 0f,
            previewTranslation = previewStart
        )
    }

    private fun setDiscoverSwipeTranslations(
        currentTranslation: Float,
        previewTranslation: Float
    ) {
        discoverSwipeAnimatedViews().forEach { view ->
            view.animate().cancel()
            view.translationX = currentTranslation
        }
        binding.discoverSwipePreviewContainer.animate().cancel()
        binding.discoverSwipePreviewContainer.translationX = previewTranslation
    }

    private fun discoverSwipeAnimatedViews(): List<View> {
        return listOf(binding.discoverRefreshLayout, binding.discoverEmptyContainer)
    }

    private fun clearDiscoverSwipeState() {
        discoverSwipeAnimatedViews().forEach { view ->
            view.animate().cancel()
            view.translationX = 0f
        }
        binding.discoverSwipePreviewContainer.animate().cancel()
        binding.discoverSwipePreviewContainer.translationX = 0f
        binding.discoverSwipePreviewContainer.visibility = View.GONE
        previewAdapter.submitList(emptyList())
        activeDiscoverSwipe = null
        isDiscoverTransitionAnimating = false
        renderDiscoverState(viewModel.uiState.value)
    }

    /**
     * 收集首页状态流与一次性事件流。
     */
    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::renderHomeState)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect(::handleUiEvent)
            }
        }
    }

    /**
     * 根据最新首页状态刷新页面内容。
     *
     * @param state 最新首页状态。
     */
    private fun renderHomeState(state: HomeUiState) {
        val binding = _binding ?: return
        if (!state.isDiscoverRefreshing) {
            binding.discoverRefreshLayout.setRefreshing(false)
        }
        if (!state.isFollowingRefreshing) {
            binding.followingRefreshLayout.setRefreshing(false)
        }
        syncCategories(state.categories)
        if (activeDiscoverSwipe == null) {
            renderDiscoverState(state)
        }
        followingSectionRenderer?.render(state, followingAdapter)
        renderFollowingFooter(state)
    }

    private fun renderDiscoverState(state: HomeUiState) {
        if (_binding == null) return
        adapter.submitList(state.discoverItems)
        renderDiscoverEmptyState(state)
        renderDiscoverFooter(state)
    }

    /**
     * 处理首页一次性事件。
     *
     * @param event 需要即时消费的首页事件。
     */
    private fun handleUiEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.ShowMessage -> {
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun syncCategories(categories: List<DiscoverCategoryItem>) {
        val previousCategoryId = channelCoordinator.currentCategory?.id
        channelCoordinator.sync(categories)
        renderCompactCategoryTabs()
        renderChannelManager()

        val currentCategory = when (currentTopTab) {
            TopTab.NEARBY -> channelCoordinator.findNearbyFallbackCategory()
            else -> channelCoordinator.currentCategory
        } ?: return
        if (currentTopTab == TopTab.FOLLOWING) return

        val shouldBootstrapSelection =
            previousCategoryId == null ||
                binding.recyclerView.layoutManager == null ||
                previousCategoryId != currentCategory.id
        if (shouldBootstrapSelection) {
            applySelectedCategory(currentCategory)
        }

        val state = viewModel.uiState.value
        val shouldRequestInitialDiscover =
            state.discoverItems.isEmpty() &&
                !state.isDiscoverRefreshing &&
                !state.isDiscoverLoadingMore
        if (shouldRequestInitialDiscover) {
            viewModel.selectDiscoverCategory(currentCategory)
        }
    }

    private fun renderDiscoverEmptyState(state: HomeUiState) {
        val binding = _binding ?: return
        val shouldShow = !state.isInitialLoading &&
            !state.isDiscoverRefreshing &&
            state.discoverItems.isEmpty() &&
            !state.discoverErrorMessage.isNullOrBlank()
        binding.discoverEmptyContainer.visibility = if (shouldShow) View.VISIBLE else View.GONE
        if (!shouldShow) return
        binding.discoverEmptyTitle.text = getString(R.string.home_discover_empty_title)
        binding.discoverEmptySubtitle.text = getString(R.string.home_discover_empty_subtitle)
    }

    private fun renderDiscoverFooter(state: HomeUiState) {
        if (_binding == null) return
        discoverFooterAdapter.submitState(
            message = when {
                state.isDiscoverLoadingMore -> getString(R.string.home_feed_loading_more)
                !state.discoverErrorMessage.isNullOrBlank() && state.discoverItems.isNotEmpty() -> {
                    getString(R.string.home_feed_offline_cached)
                }
                !state.discoverHasMore && state.discoverItems.isNotEmpty() -> getString(R.string.home_feed_end)
                else -> null
            },
            isLoading = state.isDiscoverLoadingMore
        )
    }

    private fun renderFollowingFooter(state: HomeUiState) {
        if (_binding == null) return
        followingFooterAdapter.submitState(
            message = when {
                state.isFollowingLoadingMore -> getString(R.string.home_feed_loading_more)
                !state.followingErrorMessage.isNullOrBlank() && state.followingFeedItems.isNotEmpty() -> {
                    getString(R.string.home_feed_offline_following)
                }
                !state.followingHasMore && state.followingFeedItems.isNotEmpty() -> getString(R.string.home_feed_end)
                else -> null
            },
            isLoading = state.isFollowingLoadingMore
        )
    }

    private fun renderCompactCategoryTabs() {
        channelRenderer?.renderCompactTabs(
            myChannels = visibleChannelsForCurrentTopTab(),
            currentCategoryId = channelCoordinator.currentCategory?.id,
            onCategorySelected = ::renderCategory
        )
        scrollCurrentCategoryTabIntoView()
    }

    private fun renderChannelManager() {
        val visibleChannels = visibleChannelsForCurrentTopTab()
        channelRenderer?.renderManager(
            allChannels = visibleChannelsForManager(),
            myChannels = visibleChannels,
            currentCategoryId = channelCoordinator.currentCategory?.id,
            isEditMode = channelCoordinator.isEditMode,
            callbacks = HomeChannelRenderer.ChannelCallbacks(
                onMyChannelSelected = {
                    renderCategory(it)
                    setCategoryExpanded(false)
                },
                onRecommendedChannelSelected = ::addChannel,
                onRemoveChannel = ::removeChannel,
                canRemoveChannel = ::canRemoveChannel
            )
        )
    }

    private fun addChannel(category: DiscoverCategoryItem) {
        if (!channelCoordinator.addChannel(category)) return
        renderCompactCategoryTabs()
        renderChannelManager()
    }

    private fun removeChannel(category: DiscoverCategoryItem) {
        when (val result = channelCoordinator.removeChannel(category)) {
            HomeChannelCoordinator.ChannelRemovalResult.Unchanged -> return
            HomeChannelCoordinator.ChannelRemovalResult.Removed -> {
                renderCompactCategoryTabs()
                renderChannelManager()
            }
            is HomeChannelCoordinator.ChannelRemovalResult.CurrentCategoryChanged -> {
                result.category?.let(::renderCategory)
            }
        }
    }

    private fun canRemoveChannel(category: DiscoverCategoryItem): Boolean {
        return channelCoordinator.canRemoveChannel(category)
    }

    private fun setCategoryExpanded(expanded: Boolean, animate: Boolean = true) {
        val panelVisible = binding.categoryManagerPanel.visibility == View.VISIBLE
        if (expanded == isCategoryExpanded && panelVisible == expanded) return

        isCategoryExpanded = expanded
        if (!expanded && channelCoordinator.isEditMode) {
            channelCoordinator.exitEditMode()
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

    private fun showCategorySection(animate: Boolean = true) {
        val section = binding.categorySection
        val hideOffset = categorySectionHideOffset()
        section.animate().cancel()
        isCategorySectionVisible = true

        if (currentTopTab == TopTab.FOLLOWING) {
            section.visibility = View.VISIBLE
            section.alpha = 1f
            section.translationY = 0f
            return
        }

        if (!animate || hideOffset <= 0f) {
            section.visibility = View.VISIBLE
            section.alpha = 1f
            section.translationY = 0f
            return
        }

        if (section.visibility != View.VISIBLE) {
            section.visibility = View.VISIBLE
            section.alpha = 0f
            section.translationY = -hideOffset
        }
        section.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(TOP_BAR_SHOW_DURATION)
            .setInterpolator(OvershootInterpolator(TOP_BAR_OVERSHOOT_TENSION))
            .start()
    }

    private fun hideCategorySection(animate: Boolean = true) {
        val section = binding.categorySection
        val hideOffset = categorySectionHideOffset()
        if (!isCategorySectionVisible && section.visibility != View.VISIBLE) return
        if (isCategoryExpanded) return

        section.animate().cancel()
        isCategorySectionVisible = false

        if (!animate || hideOffset <= 0f) {
            section.translationY = -hideOffset
            section.alpha = 0f
            section.visibility = View.GONE
            return
        }

        section.animate()
            .translationY(-hideOffset)
            .alpha(0f)
            .setDuration(TOP_BAR_HIDE_DURATION)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                if (!isCategorySectionVisible) {
                    section.visibility = View.GONE
                }
            }
            .start()
    }

    private fun categorySectionHideOffset(): Float {
        val section = binding.categorySection
        return if (section.height > 0) section.height.toFloat() else 0f
    }

    private fun createLayoutManager(category: DiscoverCategoryItem): RecyclerView.LayoutManager {
        return if (category.usesWaterfall) {
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL).apply {
                gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
            }
        } else {
            createGridLayoutManager(
                spanCount = 2,
                adapterProvider = { binding.recyclerView.adapter }
            )
        }
    }

    private fun createGridLayoutManager(
        spanCount: Int,
        adapterProvider: () -> RecyclerView.Adapter<*>?
    ): GridLayoutManager {
        return GridLayoutManager(requireContext(), spanCount).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val adapter = adapterProvider()
                    val itemCount = adapter?.itemCount ?: return 1
                    val isFooterPosition = itemCount > 0 && position == itemCount - 1
                    return if (isFooterPosition) {
                        spanCount
                    } else {
                        1
                    }
                }
            }
        }
    }

    /**
     * 判断列表是否需要触发加载更多。
     *
     * @param recyclerView 当前滚动中的列表控件。
     * @return `true` 表示最后可见位置已接近尾部。
     */
    private fun shouldLoadMore(recyclerView: RecyclerView): Boolean {
        val layoutManager = recyclerView.layoutManager ?: return false
        val itemCount = recyclerView.adapter?.itemCount ?: return false
        val lastVisible = when (layoutManager) {
            is GridLayoutManager -> layoutManager.findLastVisibleItemPosition()
            is StaggeredGridLayoutManager -> layoutManager.findLastVisibleItemPositions(null).maxOrNull() ?: 0
            else -> return false
        }
        return itemCount > 0 && lastVisible >= itemCount - LOAD_MORE_THRESHOLD
    }

    override fun onStart() {
        super.onStart()
        registerNetworkCallback()
    }

    override fun onStop() {
        unregisterNetworkCallback()
        super.onStop()
    }

    /**
     * 注册默认网络回调。
     */
    private fun registerNetworkCallback() {
        if (networkCallback != null) return
        val connectivityManager = context?.getSystemService(ConnectivityManager::class.java)
        if (connectivityManager == null) {
            AppLogger.w("HomeFragment", "ConnectivityManager unavailable; skip network callback registration.")
            return
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _binding?.root?.post {
                    networkSnackbar?.dismiss()
                    networkSnackbar = null
                }
            }

            override fun onLost(network: Network) {
                _binding?.root?.post {
                    showNetworkLostMessage()
                }
            }
        }
        networkCallback = callback
        runCatching { connectivityManager.registerDefaultNetworkCallback(callback) }
            .onFailure { AppLogger.w("HomeFragment", "Failed to register network callback.", it) }
        if (!isNetworkAvailable(connectivityManager)) {
            showNetworkLostMessage()
        }
    }

    /**
     * 注销默认网络回调。
     */
    private fun unregisterNetworkCallback() {
        val callback = networkCallback ?: return
        val connectivityManager = context?.getSystemService(ConnectivityManager::class.java)
        if (connectivityManager == null) {
            AppLogger.w("HomeFragment", "ConnectivityManager unavailable; skip network callback unregistration.")
            return
        }
        runCatching { connectivityManager.unregisterNetworkCallback(callback) }
            .onFailure { AppLogger.w("HomeFragment", "Failed to unregister network callback.", it) }
        networkCallback = null
    }

    /**
     * 判断当前是否具备可用网络能力。
     *
     * @param connectivityManager 网络连接管理器。
     * @return `true` 表示存在可访问互联网的活动网络。
     */
    private fun isNetworkAvailable(connectivityManager: ConnectivityManager): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * 展示网络断开提示。
     */
    private fun showNetworkLostMessage() {
        if (!isAdded) return
        if (networkSnackbar?.isShown == true) return
        networkSnackbar = Snackbar.make(
            binding.root,
            getString(R.string.note_detail_network_lost),
            Snackbar.LENGTH_INDEFINITE
        ).also { it.show() }
    }

    private fun scrollCurrentCategoryTabIntoView() {
        val channels = visibleChannelsForCurrentTopTab()
        val currentCategoryId = channelCoordinator.currentCategory?.id ?: return
        val selectedIndex = channels.indexOfFirst { it.id == currentCategoryId }
        if (selectedIndex == -1) return

        val selectedTab = binding.compactCategoryContainer.getChildAt(selectedIndex) ?: return
        binding.categoryScroll.post {
            val scrollView = binding.categoryScroll
            val targetScrollX =
                (selectedTab.left - (scrollView.width - selectedTab.width) / 2).coerceAtLeast(0)
            scrollView.smoothScrollTo(targetScrollX, 0)
        }
    }

    private fun dp(value: Int): Int =
        (value * Resources.getSystem().displayMetrics.density).roundToInt()

    private fun visibleChannelsForCurrentTopTab(): List<DiscoverCategoryItem> {
        val channels = channelCoordinator.myChannels
        if (currentTopTab != TopTab.NEARBY) return channels
        return channels.filterNot { it.id == RECOMMEND_CHANNEL_ID }.ifEmpty { channels }
    }

    private fun visibleChannelsForManager(): List<DiscoverCategoryItem> {
        val channels = channelCoordinator.allChannels
        if (currentTopTab != TopTab.NEARBY) return channels
        return channels.filterNot { it.id == RECOMMEND_CHANNEL_ID }.ifEmpty { channels }
    }

    /**
     * 清理首页视图引用与短期资源。
     */
    override fun onDestroyView() {
        networkSnackbar?.dismiss()
        networkSnackbar = null
        clearDiscoverSwipeState()
        binding.recyclerView.adapter = null
        binding.discoverSwipePreviewRecyclerView.adapter = null
        binding.followingRecyclerView.adapter = null
        followingSectionRenderer = null
        channelRenderer = null
        _binding = null
        super.onDestroyView()
    }

    private data class DrawerRowModel(
        /** 抽屉行左侧图标资源。 */
        val iconRes: Int,
        /** 抽屉行标题文案。 */
        val title: String,
        /** 抽屉行可选角标文案。 */
        val badge: String? = null
    )

    /**
     * 首页顶部一级页签枚举。
     */
    private enum class TopTab {
        /** 关注页签。 */
        FOLLOWING,
        /** 发现页签。 */
        DISCOVER,
        /** 附近页签。 */
        NEARBY
    }

    private data class DiscoverSwipeSession(
        val targetCategory: DiscoverCategoryItem,
        val toNext: Boolean,
        val width: Float
    )

    private companion object {
        private const val RECOMMEND_CHANNEL_ID = "recommend"
        /** 触发分页加载时距离列表尾部的阈值。 */
        private const val LOAD_MORE_THRESHOLD = 4
        private const val HORIZONTAL_SWIPE_DOMINANCE_RATIO = 1.2f
        private const val DISCOVER_SWIPE_COMMIT_THRESHOLD = 0.28f
        private const val DISCOVER_SWIPE_SETTLE_DURATION = 180L
        private const val TOP_BAR_SHOW_DURATION = 260L
        private const val TOP_BAR_HIDE_DURATION = 180L
        private const val TOP_BAR_OVERSHOOT_TENSION = 0.72f
    }
}
