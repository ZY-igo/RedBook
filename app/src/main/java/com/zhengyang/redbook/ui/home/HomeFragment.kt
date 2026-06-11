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
 * 首页主界面 Fragment。
 *
 * 这个类是首页模块的总协调者，主要承担以下职责：
 * 1. 初始化首页 ViewBinding、Adapter、Renderer 与交互监听。
 * 2. 订阅 [HomeViewModel] 的状态流与事件流，并把状态渲染到界面上。
 * 3. 管理顶部一级 tab、分类栏、频道管理面板、抽屉、列表分页等首页交互。
 * 4. 处理发现流的横向切分类手势动画，以及网络断开时的提示逻辑。
 */
@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    /** 持有 Fragment 对应的 ViewBinding；在 `onDestroyView` 中必须置空，避免泄漏 View。 */
    private var _binding: FragmentHomeBinding? = null
    /** 对 `_binding` 的非空快捷访问；仅能在视图已创建且尚未销毁时调用。 */
    private val binding get() = _binding!!
    /** 首页对应的 ViewModel，负责提供 UI 状态、一次性事件以及业务操作入口。 */
    private val viewModel: HomeViewModel by viewModels()

    @Inject
    /** 由 Hilt 注入的图片加载器，供首页卡片封面、头像等图片统一复用。 */
    lateinit var imageLoader: ImageLoader

    /** 发现流主列表适配器。 */
    private lateinit var adapter: HomeAdapter
    /** 横向切换分类时，右侧/左侧预览列表使用的适配器。 */
    private lateinit var previewAdapter: HomeAdapter
    /** 关注流主列表适配器。 */
    private lateinit var followingAdapter: HomeAdapter
    /** 发现流底部的“加载中 / 没有更多 / 离线缓存”提示适配器。 */
    private lateinit var discoverFooterAdapter: HomeFeedFooterAdapter
    /** 关注流底部的状态提示适配器。 */
    private lateinit var followingFooterAdapter: HomeFeedFooterAdapter
    /** 管理首页频道数据、当前选中频道、编辑模式和频道增删逻辑。 */
    private val channelCoordinator = HomeChannelCoordinator()
    /** 当前显示中的网络断开提示条；为空表示未显示。 */
    private var networkSnackbar: Snackbar? = null
    /** 注册到系统的网络回调；仅在页面可见期间持有。 */
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /** 负责渲染关注页顶部推荐用户等复合区块的辅助渲染器。 */
    private var followingSectionRenderer: HomeFollowingSectionRenderer? = null
    /** 负责渲染紧凑分类栏和频道管理面板的辅助渲染器。 */
    private var channelRenderer: HomeChannelRenderer? = null

    /** 标记频道管理面板当前是否处于展开状态。 */
    private var isCategoryExpanded = false
    /** 记录当前激活的首页一级 tab，默认进入“发现”。 */
    private var currentTopTab = TopTab.DISCOVER
    /** 标记顶部分类区是否处于显示状态，用于滚动时的显隐控制。 */
    private var isCategorySectionVisible = true
    /** 标记发现流横向切分类动画是否正在进行，避免并发触发新的切换。 */
    private var isDiscoverTransitionAnimating = false
    /** 当前一次发现流横向切换过程的上下文信息；为空表示未在切换中。 */
    private var activeDiscoverSwipe: DiscoverSwipeSession? = null

    /**
     * 在 Fragment 的根视图创建完成后初始化首页。
     *
     * 这里会完成几类工作：
     * 1. 绑定 ViewBinding。
     * 2. 创建 Adapter、Renderer 等界面协作者。
     * 3. 注册各种点击、滚动、刷新、手势监听。
     * 4. 开始收集 ViewModel 状态并渲染初始页面。
     *
     * @param view 当前 Fragment 对应的根视图。
     * @param savedInstanceState 系统在重建 Fragment 时传入的恢复状态；当前未直接使用。
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 先调用父类实现，保持 Fragment 生命周期链完整。
        super.onViewCreated(view, savedInstanceState)
        // 把传入的根视图绑定成类型安全的 ViewBinding。
        _binding = FragmentHomeBinding.bind(view)
        // 创建发现流主列表适配器。
        adapter = HomeAdapter(imageLoader)
        // 创建发现流横向切换时的预览列表适配器。
        previewAdapter = HomeAdapter(imageLoader)
        // 创建关注流适配器。
        followingAdapter = HomeAdapter(imageLoader)
        // 创建发现流底部状态行适配器。
        discoverFooterAdapter = HomeFeedFooterAdapter()
        // 创建关注流底部状态行适配器。
        followingFooterAdapter = HomeFeedFooterAdapter()
        // 创建关注页顶部区块渲染器，把关注/不感兴趣操作回调给 ViewModel。
        followingSectionRenderer = HomeFollowingSectionRenderer(
            context = requireContext(),
            binding = binding,
            onFollowUser = viewModel::followUser,
            onDismissSuggestion = viewModel::dismissSuggestion
        )
        // 创建频道相关渲染器，负责紧凑分类栏和频道管理面板。
        channelRenderer = HomeChannelRenderer(
            context = requireContext(),
            binding = binding
        )

        // 处理状态栏、导航栏对布局的侵入。
        applySystemBarInsets()
        // 初始化顶部一级 tab。
        setupTopTabs()
        // 初始化左侧抽屉内容及监听。
        setupDrawer()
        // 初始化首页的几个 RecyclerView。
        setupRecyclerViews()
        // 注册分页加载逻辑。
        setupPagination()
        // 注册顶部分类栏随滚动隐藏/显示的逻辑。
        setupCategorySectionScrollBehavior()
        // 初始化顶部紧凑分类栏与频道管理面板。
        setupCategoryTabs()
        // 初始化关注页下拉刷新。
        setupFollowingPage()
        // 开始收集 ViewModel 状态与事件。
        collectUiState()

        // 发现流下拉刷新时，只刷新当前选中的分类。
        binding.discoverRefreshLayout.setOnRefreshListener {
            channelCoordinator.currentCategory?.let(viewModel::refreshDiscover)
        }
        // 点击左上角菜单按钮时展开抽屉。
        binding.buttonMenu.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        // 点击搜索按钮时跳转搜索页。
        binding.buttonSearch.setOnClickListener {
            startActivity(Intent(requireContext(), SearchActivity::class.java))
        }

        // 根据默认 tab 渲染首屏内容。
        renderTopTab(currentTopTab)
        // 首次进入时直接显示分类区，不做动画。
        showCategorySection(animate = false)
    }

    /**
     * 处理系统栏和抽屉区域的安全内边距。
     */
    private fun applySystemBarInsets() {
        // 记录顶部栏原始外边距，后续要在这个基础上叠加状态栏高度。
        val baseTopMargin = (binding.topBar.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        // 给顶部栏注册 Insets 监听，用于避开状态栏。
        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { topBar, insets ->
            // 读取状态栏 Insets。
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            // 把状态栏高度加到顶部栏的 topMargin 上。
            topBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = baseTopMargin + statusBarInsets.top
            }
            // 返回原始 Insets，继续向下分发。
            insets
        }

        // 记录抽屉内容区域原始顶部内边距。
        val drawerTopPadding = binding.drawerContent.paddingTop
        // 抽屉内容需要避开系统栏，因此在原有 paddingTop 上叠加系统栏高度。
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

        // 记录抽屉底部工具栏原始底部内边距。
        val footerBottomPadding = binding.drawerFooter.paddingBottom
        // 抽屉底部区域需要避开导航栏，因此增加 paddingBottom。
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

        // 视图 attach 后主动请求一次 Insets 分发，确保首次布局也能拿到正确系统栏尺寸。
        binding.topBar.doOnAttach { ViewCompat.requestApplyInsets(it) }
        binding.drawerContent.doOnAttach { ViewCompat.requestApplyInsets(it) }
        binding.drawerFooter.doOnAttach { ViewCompat.requestApplyInsets(it) }
    }

    /**
     * 初始化顶部页签文案与点击事件。
     */
    private fun setupTopTabs() {
        // 为三个一级 tab 设置显示文案。
        binding.tabFollowing.text = getString(R.string.tab_following)
        binding.tabDiscover.text = getString(R.string.tab_discover)
        binding.tabNearby.text = getString(R.string.tab_nearby)

        // 点击不同 tab 时，统一交给 renderTopTab 切换页面。
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
        // 保存当前 tab 状态，供后续滚动、分类过滤等逻辑判断。
        currentTopTab = tab
        // 更新三个 tab 的选中态，驱动文字/样式变化。
        binding.tabFollowing.isSelected = tab == TopTab.FOLLOWING
        binding.tabDiscover.isSelected = tab == TopTab.DISCOVER
        binding.tabNearby.isSelected = tab == TopTab.NEARBY
        // 每次切 tab 后都先把分类区显示出来，避免用户切换后看不到分类栏。
        showCategorySection(animate = false)

        // 只有“关注”tab 展示 followingContent，其余 tab 都复用 discoverContent。
        val showDiscoverContent = tab != TopTab.FOLLOWING
        binding.discoverContentContainer.visibility = if (showDiscoverContent) View.VISIBLE else View.GONE
        binding.followingContentContainer.visibility = if (showDiscoverContent) View.GONE else View.VISIBLE

        // 如果切到“关注”，不需要频道管理面板，直接收起后返回。
        if (!showDiscoverContent) {
            setCategoryExpanded(false, animate = false)
            return
        }

        // “附近”tab 可能会过滤掉推荐频道，因此需要取一个回退分类；
        // “发现”tab 则直接使用当前选中的分类。
        val targetCategory = when (tab) {
            TopTab.NEARBY -> channelCoordinator.findNearbyFallbackCategory()
            else -> channelCoordinator.currentCategory
        }
        // 找到目标分类后立即渲染，确保列表布局与选中态同步。
        targetCategory?.let(::renderCategory)
    }

    /**
     * 初始化抽屉内容与抽屉状态监听。
     */
    private fun setupDrawer() {
        // 设置抽屉打开时覆盖主内容的蒙层颜色。
        binding.drawerLayout.setScrimColor(requireContext().getColor(R.color.xhs_scrim))
        // 监听抽屉开合，动态切换系统栏底色和明暗样式。
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                updateSystemBarsForDrawer(isDrawerOpen = true)
            }

            override fun onDrawerClosed(drawerView: View) {
                updateSystemBarsForDrawer(isDrawerOpen = false)
            }
        })
        // 配置“添加朋友”分组中的单行内容。
        configureDrawerSection(
            binding.root.findViewById<View>(R.id.drawerAddFriends).parent as ViewGroup,
            listOf(DrawerRowModel(R.drawable.ic_xhs_user_add, getString(R.string.drawer_add_friends)))
        )
        // 配置“创作中心”分组。
        configureDrawerSection(
            binding.root.findViewById<View>(R.id.drawerCreatorCenter).parent as ViewGroup,
            listOf(DrawerRowModel(R.drawable.ic_xhs_flash, getString(R.string.drawer_creator_center)))
        )
        // 配置“草稿/活动/历史/下载”等分组。
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
        // 配置“订单/购物车/钱包”分组。
        configureDrawerSection(
            binding.root.findViewById<View>(R.id.drawerOrders).parent as ViewGroup,
            listOf(
                DrawerRowModel(R.drawable.ic_xhs_order, getString(R.string.drawer_orders)),
                DrawerRowModel(R.drawable.ic_xhs_cart, getString(R.string.drawer_cart)),
                DrawerRowModel(R.drawable.ic_xhs_wallet, getString(R.string.drawer_wallet))
            )
        )
        // 配置“小程序/拍一拍”等分组。
        configureDrawerSection(
            binding.root.findViewById<View>(R.id.drawerMiniApps).parent as ViewGroup,
            listOf(
                DrawerRowModel(R.drawable.ic_xhs_link, getString(R.string.drawer_mini_apps)),
                DrawerRowModel(R.drawable.ic_xhs_camera, getString(R.string.drawer_moments))
            )
        )
        // 配置底部三个快捷入口。
        configureFooter()
        // 首次进入时按抽屉关闭状态设置系统栏。
        updateSystemBarsForDrawer(isDrawerOpen = false)
    }

    /**
     * 根据抽屉开合状态调整系统栏外观。
     *
     * @param isDrawerOpen 当前抽屉是否处于打开状态。
     */
    private fun updateSystemBarsForDrawer(isDrawerOpen: Boolean) {
        // Fragment 可能尚未附着 Activity，此时无法操作 Window，直接返回。
        val window = activity?.window ?: return
        // 抽屉打开时使用抽屉背景色；关闭时恢复首页背景色。
        val bgColor = requireContext().getColor(if (isDrawerOpen) R.color.xhs_drawer_bg else R.color.xhs_bg)
        // 同步状态栏、导航栏背景色。
        window.statusBarColor = bgColor
        window.navigationBarColor = bgColor
        // 控制系统栏图标明暗：抽屉打开时一般是浅色背景配深色图标的反向状态。
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !isDrawerOpen
            isAppearanceLightNavigationBars = !isDrawerOpen
        }
    }

    private fun configureDrawerSection(container: ViewGroup, rows: List<DrawerRowModel>) {
        // 按数据顺序把图标、标题、角标填充到分组中的每一行视图。
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
        // 底部三个快捷项的文案。
        val labels = listOf(
            getString(R.string.drawer_scan),
            getString(R.string.drawer_help),
            getString(R.string.drawer_settings)
        )
        // 与文案一一对应的图标。
        val icons = listOf(
            R.drawable.ic_xhs_scan,
            R.drawable.ic_xhs_headset,
            R.drawable.ic_xhs_settings
        )
        // 顺序写入抽屉底部的每一个 item。
        labels.indices.forEach { index ->
            val item = binding.drawerFooter.getChildAt(index) as ViewGroup
            item.findViewById<ImageView>(R.id.footerIcon).setImageResource(icons[index])
            item.findViewById<TextView>(R.id.footerLabel).text = labels[index]
        }
    }

    /**
     * 初始化首页三个 RecyclerView：
     * 发现流主列表、发现流切分类预览列表、关注流主列表。
     */
    private fun setupRecyclerViews() {
        // 点击发现流卡片时跳转笔记详情页。
        adapter.onItemClick = { item ->
            startActivity(NoteDetailActivity.createIntent(requireContext(), item))
        }
        // 发现流列表由内容适配器 + 底部状态适配器拼接而成。
        binding.recyclerView.adapter = ConcatAdapter(adapter, discoverFooterAdapter)
        // 首页卡片高度可能变化，不固定尺寸。
        binding.recyclerView.setHasFixedSize(false)
        // 关闭默认 item 动画，减少刷新/切换时的闪烁。
        binding.recyclerView.itemAnimator = null
        // 避免重复添加列表间距装饰。
        if (binding.recyclerView.itemDecorationCount == 0) {
            binding.recyclerView.addItemDecoration(HomeSpacingDecoration())
        }

        // 预览列表点击后同样进入笔记详情。
        previewAdapter.onItemClick = { item ->
            startActivity(NoteDetailActivity.createIntent(requireContext(), item))
        }
        // 预览层只显示切换目标分类的内容，不拼接 footer。
        binding.discoverSwipePreviewRecyclerView.adapter = previewAdapter
        binding.discoverSwipePreviewRecyclerView.setHasFixedSize(false)
        binding.discoverSwipePreviewRecyclerView.itemAnimator = null
        if (binding.discoverSwipePreviewRecyclerView.itemDecorationCount == 0) {
            binding.discoverSwipePreviewRecyclerView.addItemDecoration(HomeSpacingDecoration())
        }

        // 关注流卡片点击逻辑。
        followingAdapter.onItemClick = { item ->
            startActivity(NoteDetailActivity.createIntent(requireContext(), item))
        }
        // 关注流同样拼接一个 footer 展示加载状态。
        binding.followingRecyclerView.adapter = ConcatAdapter(followingAdapter, followingFooterAdapter)
        binding.followingRecyclerView.setHasFixedSize(false)
        binding.followingRecyclerView.itemAnimator = null
        // 关注流固定使用两列普通网格布局。
        binding.followingRecyclerView.layoutManager = createGridLayoutManager(
            spanCount = 2,
            adapterProvider = { binding.followingRecyclerView.adapter }
        )
        if (binding.followingRecyclerView.itemDecorationCount == 0) {
            binding.followingRecyclerView.addItemDecoration(HomeSpacingDecoration())
        }

        // 最后注册发现流的横向切分类手势。
        setupDiscoverSwipeNavigation()
    }

    /** 为发现流和关注流注册分页加载监听。 */
    private fun setupPagination() {
        // 发现流向下滚动时，如果接近底部则尝试加载当前分类的下一页。
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                // 只有向下滚动才需要考虑加载更多；上滑回看内容时不触发。
                if (dy <= 0) return
                // 当最后可见项接近尾部时，通知 ViewModel 加载更多。
                if (shouldLoadMore(recyclerView)) {
                    channelCoordinator.currentCategory?.let(viewModel::loadMoreDiscover)
                }
            }
        })
        // 关注流分页逻辑与发现流一致，只是数据源固定为 following。
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
        // 发现流和关注流都共享同一套顶部分类区显隐规则。
        binding.recyclerView.addOnScrollListener(createCategorySectionScrollListener())
        binding.followingRecyclerView.addOnScrollListener(createCategorySectionScrollListener())
    }

    private fun createCategorySectionScrollListener(): RecyclerView.OnScrollListener {
        return object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                // 管理面板展开时，保持分类区固定显示，不参与滚动隐藏。
                if (isCategoryExpanded) return
                // 已经滚回顶部时，把分类区显示出来。
                if (!recyclerView.canScrollVertically(-1)) {
                    showCategorySection()
                    return
                }
                // 关注页不在这里主动隐藏分类区，保持顶部结构稳定。
                if (currentTopTab == TopTab.FOLLOWING) {
                    return
                }
                // 向下浏览内容时收起分类区，给列表更多可视空间。
                if (dy > 0) {
                    hideCategorySection()
                }
            }
        }
    }

    private fun setupFollowingPage() {
        // 关注页下拉刷新时强制拉取最新数据。
        binding.followingRefreshLayout.setOnRefreshListener {
            viewModel.refreshFollowing(force = true)
        }
    }

    /**
     * 初始化发现流横向切分类手势。
     *
     * 手势成功启动后，会把当前列表和目标分类预览列表同时加入平移动画。
     */
    private fun setupDiscoverSwipeNavigation() {
        // 系统定义的最小滑动距离，用来过滤掉轻微抖动。
        val touchSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop
        // 在发现流列表上注册 item touch listener，识别横向切分类手势。
        binding.recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            // 手指按下时的 X 坐标。
            private var downX = 0f
            // 手指按下时的 Y 坐标。
            private var downY = 0f
            // 当前是否已经正式进入“横向拖拽分类”的处理流程。
            private var isDragging = false

            override fun onInterceptTouchEvent(
                recyclerView: RecyclerView,
                event: MotionEvent
            ): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        // 记录按下位置，供后续计算水平/垂直位移。
                        downX = event.x
                        downY = event.y
                        // 每次新的触摸序列都重置拖拽状态。
                        isDragging = false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        // 当前页面状态不允许切分类时，直接放行给 RecyclerView 自己处理。
                        if (!canHandleDiscoverSwipe()) return false

                        // 计算手指相对按下点的水平和垂直位移。
                        val deltaX = event.x - downX
                        val deltaY = event.y - downY
                        val absDeltaX = kotlin.math.abs(deltaX)
                        val absDeltaY = kotlin.math.abs(deltaY)
                        // 只有“水平位移足够大且明显大于垂直位移”时，才认定为切分类手势。
                        val isHorizontalSwipe =
                            absDeltaX > touchSlop &&
                                absDeltaX > absDeltaY * HORIZONTAL_SWIPE_DOMINANCE_RATIO
                        if (!isHorizontalSwipe) return false

                        // 第一次确认水平拖拽时，正式启动一次切换会话。
                        if (!isDragging) {
                            isDragging = startDiscoverSwipe(toNext = deltaX < 0f)
                        }
                        // 如果成功进入拖拽状态，就持续刷新当前列表和预览列表的位置。
                        if (isDragging) {
                            updateDiscoverSwipe(deltaX)
                        }
                        // 返回 true 表示后续事件由当前监听器继续消费。
                        return isDragging
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        // 手势结束时，根据拖拽距离决定是提交切换还是回弹。
                        if (isDragging) {
                            finishDiscoverSwipe(commit = shouldCommitDiscoverSwipe(event.x - downX))
                        }
                        // 结束当前触摸序列。
                        isDragging = false
                    }
                }
                // 如果已经在拖拽中，则继续拦截事件。
                return isDragging
            }

            override fun onTouchEvent(recyclerView: RecyclerView, event: MotionEvent) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_MOVE -> {
                        // 在已经拦截的前提下，持续同步拖拽位移。
                        if (isDragging) {
                            updateDiscoverSwipe(event.x - downX)
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        // 抬手时按阈值提交或回弹。
                        if (isDragging) {
                            finishDiscoverSwipe(commit = shouldCommitDiscoverSwipe(event.x - downX))
                            isDragging = false
                        }
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        // 事件被系统取消时，一律回弹，不提交切换。
                        if (isDragging) {
                            finishDiscoverSwipe(commit = false)
                            isDragging = false
                        }
                    }
                }
            }
        })
    }

    /** 初始化顶部紧凑分类栏和频道管理面板。 */
    private fun setupCategoryTabs() {
        // 点击右侧箭头时在“展开 / 收起”之间切换。
        binding.categoryExpand.setOnClickListener {
            setCategoryExpanded(!isCategoryExpanded)
        }
        // 点击面板顶部折叠按钮时收起管理面板。
        binding.panelCollapse.setOnClickListener {
            setCategoryExpanded(false)
        }
        // 点击“编辑”时切换频道管理器的编辑模式，并重绘面板。
        binding.panelEditButton.setOnClickListener {
            channelCoordinator.toggleEditMode()
            renderChannelManager()
        }
        // 折叠按钮图标默认旋转 180 度，用于显示“向上收起”的视觉方向。
        binding.panelCollapse.rotation = 180f
        // 渲染紧凑分类栏。
        renderCompactCategoryTabs()
        // 渲染频道管理面板。
        renderChannelManager()
        // 初始状态默认收起管理面板。
        setCategoryExpanded(false, animate = false)
    }

    /** 用户显式选择某个分类时，同时更新本地状态并触发加载。 */
    private fun renderCategory(category: DiscoverCategoryItem) {
        // 先立即更新本地 UI，给用户即时反馈。
        applySelectedCategory(category)
        // 再通知 ViewModel 切换分类并拉取对应数据。
        viewModel.selectDiscoverCategory(category)
    }

    /** 只在本地应用分类选择结果，不直接触发网络请求。 */
    private fun applySelectedCategory(category: DiscoverCategoryItem) {
        // 更新协调器中的当前频道记录。
        channelCoordinator.setCurrentCategory(category)
        // 刷新顶部紧凑分类栏选中态。
        renderCompactCategoryTabs()
        // 刷新频道管理面板选中态。
        renderChannelManager()
        // 根据分类决定发现流使用普通网格还是瀑布流布局。
        binding.recyclerView.layoutManager = createLayoutManager(category)
    }

    /** 判断当前条件下是否允许用横向手势切换发现流分类。 */
    private fun canHandleDiscoverSwipe(): Boolean {
        // 仅“发现”页允许横向切分类；面板展开、动画进行中或只有一个频道时都禁用。
        return currentTopTab == TopTab.DISCOVER &&
            !isCategoryExpanded &&
            !isDiscoverTransitionAnimating &&
            channelCoordinator.myChannels.size > 1
    }

    /** 启动一次发现流横向切换会话，并准备目标分类预览内容。 */
    private fun startDiscoverSwipe(toNext: Boolean): Boolean {
        // 拿到当前“我的频道”列表，用于寻找前一个/后一个分类。
        val channels = channelCoordinator.myChannels
        // 当前没有选中分类时无法切换。
        val currentCategory = channelCoordinator.currentCategory ?: return false
        // 找到当前分类在频道数组中的位置。
        val currentIndex = channels.indexOfFirst { it.id == currentCategory.id }
        if (currentIndex == -1) return false

        // 根据滑动方向决定要切换到前一个还是后一个频道。
        val targetIndex = if (toNext) currentIndex + 1 else currentIndex - 1
        // 越界说明已经滑到头/尾，没有目标分类可切换。
        val targetCategory = channels.getOrNull(targetIndex) ?: return false
        // 取内容区宽度作为横向切换动画的总距离；宽度无效时终止。
        val contentWidth = binding.discoverContentContainer.width
            .takeIf { it > 0 }
            ?: binding.root.width
            .takeIf { it > 0 }
            ?: return false
        // 先把目标分类的预览内容绑定到预览列表。
        bindDiscoverSwipePreview(targetCategory)
        // 记录本次切换会话的目标分类、方向和宽度。
        activeDiscoverSwipe = DiscoverSwipeSession(
            targetCategory = targetCategory,
            toNext = toNext,
            width = contentWidth.toFloat()
        )
        // 标记动画中，防止并发开启第二次切换。
        isDiscoverTransitionAnimating = true
        // 停止主列表与预览列表当前可能存在的惯性滚动。
        binding.recyclerView.stopScroll()
        binding.discoverSwipePreviewRecyclerView.stopScroll()
        // 显示预览容器，让目标分类列表进入屏幕外待命。
        binding.discoverSwipePreviewContainer.visibility = View.VISIBLE
        // 把当前列表和预览列表重置到切换起始位置。
        resetDiscoverSwipeTranslations()
        return true
    }

    /** 根据手势位移更新当前列表和预览列表的平移位置。 */
    private fun updateDiscoverSwipe(deltaX: Float) {
        // 没有会话时无需处理。
        val session = activeDiscoverSwipe ?: return
        val width = session.width
        // 把拖拽位移限制在一个屏宽以内，避免拉得过远。
        val constrainedDelta = deltaX.coerceIn(-width, width)
        // 如果实际位移和预定切换方向相反，则不更新，防止反向拖动造成错乱。
        if (session.toNext && constrainedDelta > 0f) return
        if (!session.toNext && constrainedDelta < 0f) return

        // 预览列表初始时位于屏幕右侧或左侧一个整屏的位置。
        val previewBaseTranslation = if (session.toNext) width else -width
        // 当前列表和预览列表按同样的拖拽距离同步平移，形成并排切换效果。
        setDiscoverSwipeTranslations(
            currentTranslation = constrainedDelta,
            previewTranslation = previewBaseTranslation + constrainedDelta
        )
    }

    /** 判断当前手势位移是否达到正式切换分类的阈值。 */
    private fun shouldCommitDiscoverSwipe(deltaX: Float): Boolean {
        val session = activeDiscoverSwipe ?: return false
        // 当水平位移达到屏宽一定比例后，认为用户意图明确，提交切换。
        return kotlin.math.abs(deltaX) >= session.width * DISCOVER_SWIPE_COMMIT_THRESHOLD
    }

    /** 结束一次横向切换会话，并决定回弹还是提交目标分类。 */
    private fun finishDiscoverSwipe(commit: Boolean) {
        // 会话不存在时无事可做。
        val session = activeDiscoverSwipe ?: return
        val width = session.width
        // 提交切换时，当前列表滑出屏幕；取消时回到原位。
        val currentTarget = if (commit) {
            if (session.toNext) -width else width
        } else {
            0f
        }
        // 预览列表的起始位置取决于切换方向。
        val previewStart = if (session.toNext) width else -width
        // 提交时预览列表进入原位；取消时回到屏幕外。
        val previewTarget = if (commit) 0f else previewStart

        // 需要一起参与切换动画的主内容视图。
        val animatedViews = discoverSwipeAnimatedViews()
        // 单独拿出预览容器，稍后与主内容同步动画。
        val animatedPreview = binding.discoverSwipePreviewContainer
        // 用计数器等待所有动画都完成后再统一收尾。
        var remainingAnimations = animatedViews.size + 1
        val onAnimationEnd = {
            remainingAnimations -= 1
            if (remainingAnimations == 0) {
                // 如果用户确认切换，则在动画结束后真正提交分类切换。
                if (commit) {
                    applySelectedCategory(session.targetCategory)
                    viewModel.selectDiscoverCategory(session.targetCategory)
                }
                // 无论提交还是取消，都要恢复临时状态。
                clearDiscoverSwipeState()
            }
        }

        // 让当前内容视图滑向目标位置。
        animatedViews.forEach { view ->
            view.animate()
                .translationX(currentTarget)
                .setDuration(DISCOVER_SWIPE_SETTLE_DURATION)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction(onAnimationEnd)
                .start()
        }
        // 让预览列表同步滑入或滑回。
        animatedPreview.animate()
            .translationX(previewTarget)
            .setDuration(DISCOVER_SWIPE_SETTLE_DURATION)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction(onAnimationEnd)
            .start()
    }

    /** 为目标分类绑定切换预览内容，优先使用缓存，否则退回骨架屏。 */
    private fun bindDiscoverSwipePreview(category: DiscoverCategoryItem) {
        // 预览列表的布局类型要和目标分类一致，避免切换时布局突变。
        binding.discoverSwipePreviewRecyclerView.layoutManager = createLayoutManager(category)
        // 如果目标分类已有缓存数据，直接展示；否则用骨架屏占位。
        previewAdapter.submitList(viewModel.peekDiscoverItems(category.id) ?: HomeSkeletonFactory.discover())
    }

    private fun resetDiscoverSwipeTranslations() {
        // 没有会话时不需要重置。
        val session = activeDiscoverSwipe ?: return
        // 预览列表初始化到屏幕右侧或左侧整屏外。
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
        // 先取消现有属性动画，避免和手势拖拽实时赋值打架。
        discoverSwipeAnimatedViews().forEach { view ->
            view.animate().cancel()
            view.translationX = currentTranslation
        }
        // 同样取消预览容器动画后再直接设置平移值。
        binding.discoverSwipePreviewContainer.animate().cancel()
        binding.discoverSwipePreviewContainer.translationX = previewTranslation
    }

    private fun discoverSwipeAnimatedViews(): List<View> {
        // 当前发现流实际参与水平切换的内容包括刷新容器和空态容器。
        return listOf(binding.discoverRefreshLayout, binding.discoverEmptyContainer)
    }

    /** 清理横向切换相关动画状态，并恢复发现流正常显示。 */
    private fun clearDiscoverSwipeState() {
        // 取消并清空主内容视图的平移状态。
        discoverSwipeAnimatedViews().forEach { view ->
            view.animate().cancel()
            view.translationX = 0f
        }
        // 取消预览容器动画并恢复初始位置。
        binding.discoverSwipePreviewContainer.animate().cancel()
        binding.discoverSwipePreviewContainer.translationX = 0f
        // 隐藏预览层。
        binding.discoverSwipePreviewContainer.visibility = View.GONE
        // 清空预览列表，避免旧数据残留。
        previewAdapter.submitList(emptyList())
        // 结束本次切换会话。
        activeDiscoverSwipe = null
        isDiscoverTransitionAnimating = false
        // 重新用最新状态渲染发现流，确保主列表内容与 footer/空态完全正确。
        renderDiscoverState(viewModel.uiState.value)
    }

    /**
     * 收集首页状态流与一次性事件流。
     */
    private fun collectUiState() {
        // 在视图生命周期处于 STARTED 期间收集持续性的 UI 状态。
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::renderHomeState)
            }
        }
        // 同样在 STARTED 期间收集一次性事件，例如 toast 提示。
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect(::handleUiEvent)
            }
        }
    }

    /**
     * 根据最新首页状态刷新页面内容。
     *
     * 会同步处理刷新控件、分类数据、发现流、关注流和 footer。
     *
     * @param state ViewModel 当前发出的最新首页状态。
     */
    private fun renderHomeState(state: HomeUiState) {
        // 视图已销毁时不再继续渲染。
        val binding = _binding ?: return
        // 当刷新结束时主动关闭发现流的下拉刷新动画。
        if (!state.isDiscoverRefreshing) {
            binding.discoverRefreshLayout.setRefreshing(false)
        }
        // 当刷新结束时主动关闭关注流的下拉刷新动画。
        if (!state.isFollowingRefreshing) {
            binding.followingRefreshLayout.setRefreshing(false)
        }
        // 先同步频道数据，确保分类栏和频道管理面板使用的是最新分类集合。
        syncCategories(state.categories)
        // 正在做横向切换动画时，暂时不覆盖发现流内容，避免闪动。
        if (activeDiscoverSwipe == null) {
            renderDiscoverState(state)
        }
        // 渲染关注页顶部特殊区块。
        followingSectionRenderer?.render(state, followingAdapter)
        // 单独刷新关注流 footer。
        renderFollowingFooter(state)
    }

    private fun renderDiscoverState(state: HomeUiState) {
        // 视图已被销毁时直接返回。
        if (_binding == null) return
        // 提交发现流卡片数据给主适配器。
        adapter.submitList(state.discoverItems)
        // 根据数据和错误状态决定是否显示空态。
        renderDiscoverEmptyState(state)
        // 更新发现流 footer。
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
                // 以短 Toast 的形式把一次性提示展示给用户。
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 把分类数据同步给频道协调器。
     *
     * 如果当前还没有建立发现流初始选中状态，会在这里补发首次选择请求。
     */
    private fun syncCategories(categories: List<DiscoverCategoryItem>) {
        // 记录同步前的当前分类 id，用于判断本次是否发生了分类切换。
        val previousCategoryId = channelCoordinator.currentCategory?.id
        // 把最新分类集合交给协调器统一管理。
        channelCoordinator.sync(categories)
        // 分类数据更新后，顶部分类栏和管理面板都需要重绘。
        renderCompactCategoryTabs()
        renderChannelManager()

        // 根据当前一级 tab 决定此刻应该使用哪个分类。
        val currentCategory = when (currentTopTab) {
            TopTab.NEARBY -> channelCoordinator.findNearbyFallbackCategory()
            else -> channelCoordinator.currentCategory
        } ?: return
        // 关注页不使用发现流分类，直接返回。
        if (currentTopTab == TopTab.FOLLOWING) return

        // 以下几种情况需要重新建立当前分类的本地选中态与布局：
        // 1. 之前还没有当前分类。
        // 2. 列表布局管理器尚未初始化。
        // 3. 当前分类 id 发生了变化。
        val shouldBootstrapSelection =
            previousCategoryId == null ||
                binding.recyclerView.layoutManager == null ||
                previousCategoryId != currentCategory.id
        if (shouldBootstrapSelection) {
            applySelectedCategory(currentCategory)
        }

        // 若发现流当前没有任何数据，且也不在刷新/加载更多中，则补发一次初始加载请求。
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
        // 仅在首屏加载结束、未刷新、列表为空且确实存在错误信息时显示空态。
        val shouldShow = !state.isInitialLoading &&
            !state.isDiscoverRefreshing &&
            state.discoverItems.isEmpty() &&
            !state.discoverErrorMessage.isNullOrBlank()
        binding.discoverEmptyContainer.visibility = if (shouldShow) View.VISIBLE else View.GONE
        if (!shouldShow) return
        // 空态文案由资源文件提供，便于统一管理与本地化。
        binding.discoverEmptyTitle.text = getString(R.string.home_discover_empty_title)
        binding.discoverEmptySubtitle.text = getString(R.string.home_discover_empty_subtitle)
    }

    private fun renderDiscoverFooter(state: HomeUiState) {
        if (_binding == null) return
        // 根据加载状态、错误状态和是否到底来决定 footer 展示的文案。
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
        // 关注流 footer 的规则与发现流类似，但文案略有区别。
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
        // 渲染顶部紧凑分类栏，并把点击事件回调到 renderCategory。
        channelRenderer?.renderCompactTabs(
            myChannels = visibleChannelsForCurrentTopTab(),
            currentCategoryId = channelCoordinator.currentCategory?.id,
            onCategorySelected = ::renderCategory
        )
        // 渲染后尽量把当前选中的分类滚动到可视区域中间附近。
        scrollCurrentCategoryTabIntoView()
    }

    private fun renderChannelManager() {
        // 当前一级 tab 可见的“我的频道”集合。
        val visibleChannels = visibleChannelsForCurrentTopTab()
        // 把完整频道数据、我的频道、当前分类和各种操作回调交给渲染器。
        channelRenderer?.renderManager(
            allChannels = visibleChannelsForManager(),
            myChannels = visibleChannels,
            currentCategoryId = channelCoordinator.currentCategory?.id,
            isEditMode = channelCoordinator.isEditMode,
            callbacks = HomeChannelRenderer.ChannelCallbacks(
                onMyChannelSelected = {
                    // 点击“我的频道”时切换分类，并关闭管理面板。
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
        // 添加失败通常表示该频道已存在，此时不重复刷新 UI。
        if (!channelCoordinator.addChannel(category)) return
        // 添加成功后刷新分类栏和管理面板。
        renderCompactCategoryTabs()
        renderChannelManager()
    }

    private fun removeChannel(category: DiscoverCategoryItem) {
        // 删除频道后，根据不同结果决定是否重绘 UI 或切换当前分类。
        when (val result = channelCoordinator.removeChannel(category)) {
            HomeChannelCoordinator.ChannelRemovalResult.Unchanged -> return
            HomeChannelCoordinator.ChannelRemovalResult.Removed -> {
                renderCompactCategoryTabs()
                renderChannelManager()
            }
            is HomeChannelCoordinator.ChannelRemovalResult.CurrentCategoryChanged -> {
                // 如果删除的是当前分类，则切到协调器指定的新当前分类。
                result.category?.let(::renderCategory)
            }
        }
    }

    private fun canRemoveChannel(category: DiscoverCategoryItem): Boolean {
        // 是否允许删除由协调器统一决定，例如可能保留最少频道数量限制。
        return channelCoordinator.canRemoveChannel(category)
    }

    /** 控制频道管理面板的展开/收起状态以及对应动画。 */
    private fun setCategoryExpanded(expanded: Boolean, animate: Boolean = true) {
        // 同时比较逻辑状态和实际可见状态，避免重复执行同一轮动画。
        val panelVisible = binding.categoryManagerPanel.visibility == View.VISIBLE
        if (expanded == isCategoryExpanded && panelVisible == expanded) return

        // 更新内部展开标记。
        isCategoryExpanded = expanded
        // 面板收起时，如果还处在编辑模式，顺便退出编辑并刷新 UI。
        if (!expanded && channelCoordinator.isEditMode) {
            channelCoordinator.exitEditMode()
            renderChannelManager()
        }

        // 先取消可能残留的旋转/位移动画，避免动画打架。
        binding.categoryExpand.animate().cancel()
        binding.categoryManagerPanel.animate().cancel()

        // 展开时箭头旋转到 180 度，收起时恢复 0 度。
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
            // 展开前先把面板放到上方一点并设为透明，作为进入动画起点。
            binding.categoryManagerPanel.alpha = 0f
            binding.categoryManagerPanel.translationY = -dp(10).toFloat()
            binding.categoryManagerPanel.visibility = View.VISIBLE
            if (animate) {
                // 执行淡入 + 下移到原位的展开动画。
                binding.categoryManagerPanel.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(180L)
                    .start()
            } else {
                // 不需要动画时，直接显示在最终状态。
                binding.categoryManagerPanel.alpha = 1f
                binding.categoryManagerPanel.translationY = 0f
            }
        } else if (animate) {
            // 收起时执行淡出 + 上移，并在结束后把面板设为 GONE。
            binding.categoryManagerPanel.animate()
                .alpha(0f)
                .translationY(-dp(10).toFloat())
                .setDuration(160L)
                .withEndAction {
                    binding.categoryManagerPanel.visibility = View.GONE
                }
                .start()
        } else {
            // 不走动画时，直接切到收起终态。
            binding.categoryManagerPanel.alpha = 0f
            binding.categoryManagerPanel.translationY = -dp(10).toFloat()
            binding.categoryManagerPanel.visibility = View.GONE
        }
    }

    private fun showCategorySection(animate: Boolean = true) {
        // 顶部分类区视图引用。
        val section = binding.categorySection
        // 取得隐藏时需要上移的距离，一般就是整个分类区高度。
        val hideOffset = categorySectionHideOffset()
        // 开始新动画前先取消旧动画。
        section.animate().cancel()
        // 更新可见标记。
        isCategorySectionVisible = true

        // 关注页不做复杂动画，始终保持分类区可见。
        if (currentTopTab == TopTab.FOLLOWING) {
            section.visibility = View.VISIBLE
            section.alpha = 1f
            section.translationY = 0f
            return
        }

        // 不需要动画或高度还没测量出来时，直接展示。
        if (!animate || hideOffset <= 0f) {
            section.visibility = View.VISIBLE
            section.alpha = 1f
            section.translationY = 0f
            return
        }

        // 如果此前是 GONE，先把起始状态设置到屏幕上方并透明。
        if (section.visibility != View.VISIBLE) {
            section.visibility = View.VISIBLE
            section.alpha = 0f
            section.translationY = -hideOffset
        }
        // 执行带轻微回弹感的显示动画。
        section.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(TOP_BAR_SHOW_DURATION)
            .setInterpolator(OvershootInterpolator(TOP_BAR_OVERSHOOT_TENSION))
            .start()
    }

    private fun hideCategorySection(animate: Boolean = true) {
        // 顶部分类区视图引用。
        val section = binding.categorySection
        // 计算收起时要上移的距离。
        val hideOffset = categorySectionHideOffset()
        // 已经隐藏且视图也不可见时，不重复处理。
        if (!isCategorySectionVisible && section.visibility != View.VISIBLE) return
        // 管理面板展开时禁止隐藏分类区。
        if (isCategoryExpanded) return

        // 取消现有动画并更新标记。
        section.animate().cancel()
        isCategorySectionVisible = false

        // 不做动画时直接设置到隐藏终态。
        if (!animate || hideOffset <= 0f) {
            section.translationY = -hideOffset
            section.alpha = 0f
            section.visibility = View.GONE
            return
        }

        // 执行向上滑出并淡出的隐藏动画。
        section.animate()
            .translationY(-hideOffset)
            .alpha(0f)
            .setDuration(TOP_BAR_HIDE_DURATION)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                // 动画结束后再次确认状态，避免中途又被 showCategorySection 改回可见。
                if (!isCategorySectionVisible) {
                    section.visibility = View.GONE
                }
            }
            .start()
    }

    private fun categorySectionHideOffset(): Float {
        // 高度已测量时直接使用高度；未测量完成时返回 0，调用方会跳过动画。
        val section = binding.categorySection
        return if (section.height > 0) section.height.toFloat() else 0f
    }

    /** 根据分类配置选择普通网格或瀑布流布局。 */
    private fun createLayoutManager(category: DiscoverCategoryItem): RecyclerView.LayoutManager {
        // 某些分类需要瀑布流卡片效果，其余分类使用普通两列网格。
        return if (category.usesWaterfall) {
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL).apply {
                // 允许瀑布流在出现空隙时自动调整 item 跨列位置。
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
            // 自定义跨度规则：普通内容占 1 列，尾部 footer 占满整行。
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val adapter = adapterProvider()
                    val itemCount = adapter?.itemCount ?: return 1
                    // ConcatAdapter 的最后一个位置就是 footer，让它横跨全部列数。
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
        // 没有布局管理器或适配器时无法计算分页阈值。
        val layoutManager = recyclerView.layoutManager ?: return false
        val itemCount = recyclerView.adapter?.itemCount ?: return false
        // 兼容普通网格和瀑布流两种布局，取最后一个可见 item 的位置。
        val lastVisible = when (layoutManager) {
            is GridLayoutManager -> layoutManager.findLastVisibleItemPosition()
            is StaggeredGridLayoutManager -> layoutManager.findLastVisibleItemPositions(null).maxOrNull() ?: 0
            else -> return false
        }
        // 当最后可见位置距离尾部小于阈值时，触发下一页加载。
        return itemCount > 0 && lastVisible >= itemCount - LOAD_MORE_THRESHOLD
    }

    /** 页面开始可见时注册网络监听。 */
    override fun onStart() {
        // 先走 Fragment 标准生命周期，再注册首页所需的网络监听。
        super.onStart()
        registerNetworkCallback()
    }

    /** 页面不再可见时移除网络监听。 */
    override fun onStop() {
        // 页面离开前先注销网络监听，避免后台继续收到回调。
        unregisterNetworkCallback()
        super.onStop()
    }

    /**
     * 注册默认网络回调。
     */
    private fun registerNetworkCallback() {
        // 已经注册过则不重复注册。
        if (networkCallback != null) return
        // 通过系统服务获取网络管理器。
        val connectivityManager = context?.getSystemService(ConnectivityManager::class.java)
        if (connectivityManager == null) {
            AppLogger.w("HomeFragment", "ConnectivityManager unavailable; skip network callback registration.")
            return
        }
        // 创建网络状态回调，用于监听联网与断网变化。
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // 网络恢复后，在主线程安全地关闭断网提示条。
                _binding?.root?.post {
                    networkSnackbar?.dismiss()
                    networkSnackbar = null
                }
            }

            override fun onLost(network: Network) {
                // 网络丢失后，异步回到主线程展示断网提示。
                _binding?.root?.post {
                    showNetworkLostMessage()
                }
            }
        }
        // 先把回调保存下来，供后续注销时使用。
        networkCallback = callback
        // 注册默认网络回调；失败时记录日志，但不让页面崩溃。
        runCatching { connectivityManager.registerDefaultNetworkCallback(callback) }
            .onFailure { AppLogger.w("HomeFragment", "Failed to register network callback.", it) }
        // 如果注册时当前本身就处于断网状态，主动立即展示提示。
        if (!isNetworkAvailable(connectivityManager)) {
            showNetworkLostMessage()
        }
    }

    /**
     * 注销默认网络回调。
     */
    private fun unregisterNetworkCallback() {
        // 没有已注册回调时无需处理。
        val callback = networkCallback ?: return
        // 重新获取系统网络管理器。
        val connectivityManager = context?.getSystemService(ConnectivityManager::class.java)
        if (connectivityManager == null) {
            AppLogger.w("HomeFragment", "ConnectivityManager unavailable; skip network callback unregistration.")
            return
        }
        // 尝试注销网络回调，失败时只记日志。
        runCatching { connectivityManager.unregisterNetworkCallback(callback) }
            .onFailure { AppLogger.w("HomeFragment", "Failed to unregister network callback.", it) }
        // 清空本地引用，表示当前已不再监听网络。
        networkCallback = null
    }

    /**
     * 判断当前是否具备可用网络能力。
     *
     * @param connectivityManager 网络连接管理器。
     * @return `true` 表示存在可访问互联网的活动网络。
     */
    private fun isNetworkAvailable(connectivityManager: ConnectivityManager): Boolean {
        // 没有活动网络时直接判定为不可用。
        val network = connectivityManager.activeNetwork ?: return false
        // 没有网络能力信息时同样视为不可用。
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        // 仅检查是否具备 Internet 能力，不额外判断是否已真正联网验证。
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * 展示网络断开提示。
     */
    private fun showNetworkLostMessage() {
        // Fragment 未附着时不能安全访问资源和 View。
        if (!isAdded) return
        // 提示条已显示时，不重复弹出第二个。
        if (networkSnackbar?.isShown == true) return
        // 创建一个常驻 Snackbar 提示当前网络已断开。
        networkSnackbar = Snackbar.make(
            binding.root,
            getString(R.string.note_detail_network_lost),
            Snackbar.LENGTH_INDEFINITE
        ).also { it.show() }
    }

    /** 把当前选中的紧凑分类 tab 滚动到可见区域中心附近。 */
    private fun scrollCurrentCategoryTabIntoView() {
        // 当前 tab 下实际可见的频道集合。
        val channels = visibleChannelsForCurrentTopTab()
        // 没有当前选中分类时不滚动。
        val currentCategoryId = channelCoordinator.currentCategory?.id ?: return
        // 找到当前选中分类在紧凑 tab 列表中的位置。
        val selectedIndex = channels.indexOfFirst { it.id == currentCategoryId }
        if (selectedIndex == -1) return

        // 根据位置拿到对应的 tab View。
        val selectedTab = binding.compactCategoryContainer.getChildAt(selectedIndex) ?: return
        binding.categoryScroll.post {
            // 让当前选中 tab 尽量位于水平滚动区域的中间位置。
            val scrollView = binding.categoryScroll
            val targetScrollX =
                (selectedTab.left - (scrollView.width - selectedTab.width) / 2).coerceAtLeast(0)
            scrollView.smoothScrollTo(targetScrollX, 0)
        }
    }

    private fun dp(value: Int): Int =
        // 把 dp 单位换算成当前屏幕密度下的像素值。
        (value * Resources.getSystem().displayMetrics.density).roundToInt()

    private fun visibleChannelsForCurrentTopTab(): List<DiscoverCategoryItem> {
        // 默认返回“我的频道”全集。
        val channels = channelCoordinator.myChannels
        // 只有“附近”tab 需要过滤掉 recommend 频道，避免出现不合语义的分类。
        if (currentTopTab != TopTab.NEARBY) return channels
        // 如果过滤后为空，则回退到原始列表，避免 UI 完全没有频道可展示。
        return channels.filterNot { it.id == RECOMMEND_CHANNEL_ID }.ifEmpty { channels }
    }

    private fun visibleChannelsForManager(): List<DiscoverCategoryItem> {
        // 频道管理面板默认展示全部可管理频道。
        val channels = channelCoordinator.allChannels
        // “附近”tab 下同样隐藏 recommend 频道。
        if (currentTopTab != TopTab.NEARBY) return channels
        return channels.filterNot { it.id == RECOMMEND_CHANNEL_ID }.ifEmpty { channels }
    }

    /**
     * 销毁视图时释放适配器、渲染器和 binding 引用。
     *
     * Fragment 实例可能被保留，但其 View 会被销毁重建，因此这里要主动断开所有
     * 与旧 View 树相关的引用，避免内存泄漏或后续误用失效视图。
     */
    override fun onDestroyView() {
        // 页面销毁时关闭可能还在显示的网络提示。
        networkSnackbar?.dismiss()
        networkSnackbar = null
        // 清理发现流横向切换过程中的临时状态。
        clearDiscoverSwipeState()
        // 解除 RecyclerView 与 Adapter 的绑定，帮助旧 View 树被及时回收。
        binding.recyclerView.adapter = null
        binding.discoverSwipePreviewRecyclerView.adapter = null
        binding.followingRecyclerView.adapter = null
        // 释放依赖旧 binding 的渲染器引用。
        followingSectionRenderer = null
        channelRenderer = null
        // 置空 binding，标记 View 生命周期结束。
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
        /** 本次横向切换要进入的目标分类。 */
        val targetCategory: DiscoverCategoryItem,
        /** `true` 表示切到后一个分类，`false` 表示切到前一个分类。 */
        val toNext: Boolean,
        /** 参与切换动画的基准宽度，通常等于内容区宽度。 */
        val width: Float
    )

    private companion object {
        /** “推荐”频道的固定 id，在“附近”tab 下会被过滤掉。 */
        private const val RECOMMEND_CHANNEL_ID = "recommend"
        /** 触发分页加载时距离列表尾部的阈值。 */
        private const val LOAD_MORE_THRESHOLD = 4
        /** 判定“横向意图明显强于纵向意图”时使用的比例阈值。 */
        private const val HORIZONTAL_SWIPE_DOMINANCE_RATIO = 1.2f
        /** 横向拖拽达到屏宽该比例时，认为应提交分类切换。 */
        private const val DISCOVER_SWIPE_COMMIT_THRESHOLD = 0.28f
        /** 横向切换收尾动画时长。 */
        private const val DISCOVER_SWIPE_SETTLE_DURATION = 180L
        /** 顶部分类区显示动画时长。 */
        private const val TOP_BAR_SHOW_DURATION = 260L
        /** 顶部分类区隐藏动画时长。 */
        private const val TOP_BAR_HIDE_DURATION = 180L
        /** 顶部分类区显示时回弹插值器的张力参数。 */
        private const val TOP_BAR_OVERSHOOT_TENSION = 0.72f
    }
}
