package com.zhengyang.redbook.ui.home

import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.zhengyang.redbook.R
import com.zhengyang.redbook.databinding.FragmentHomeBinding
import com.zhengyang.redbook.ui.note.NoteDetailActivity
import com.zhengyang.redbook.ui.search.SearchActivity
import com.zhengyang.redbook.utils.AppLogger
import com.zhengyang.redbook.utils.dpToPx
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private val adapter = HomeAdapter()
    private val followingAdapter = HomeAdapter()
    private val channelCoordinator = HomeChannelCoordinator()
    private var networkSnackbar: Snackbar? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private var followingSectionRenderer: HomeFollowingSectionRenderer? = null
    private var channelRenderer: HomeChannelRenderer? = null

    private var isCategoryExpanded = false
    private var currentTopTab = TopTab.DISCOVER

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)
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
            return
        }

        val targetCategory = when (tab) {
            TopTab.NEARBY -> channelCoordinator.findNearbyFallbackCategory()
            else -> channelCoordinator.currentCategory
        }
        targetCategory?.let(::renderCategory)
    }

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

    private fun setupFollowingPage() {
        binding.followingRefreshLayout.setOnRefreshListener {
            viewModel.refreshFollowing(force = true)
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
            channelCoordinator.toggleEditMode()
            renderChannelManager()
        }
        binding.panelCollapse.rotation = 180f
        renderCompactCategoryTabs()
        renderChannelManager()
        setCategoryExpanded(false, animate = false)
    }

    private fun renderCategory(category: DiscoverCategoryItem) {
        channelCoordinator.setCurrentCategory(category)
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect(::handleUiEvent)
            }
        }
    }

    private fun renderHomeState(state: HomeUiState) {
        if (!state.isDiscoverRefreshing) {
            binding.discoverRefreshLayout.setRefreshing(false)
        }
        if (!state.isFollowingRefreshing) {
            binding.followingRefreshLayout.setRefreshing(false)
        }
        syncCategories(state.categories)
        adapter.submitList(state.discoverItems)
        followingSectionRenderer?.render(state, followingAdapter)
    }

    private fun handleUiEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.ShowMessage -> {
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun syncCategories(categories: List<DiscoverCategoryItem>) {
        channelCoordinator.sync(categories)
        renderCompactCategoryTabs()
        renderChannelManager()
    }

    private fun renderCompactCategoryTabs() {
        channelRenderer?.renderCompactTabs(
            myChannels = channelCoordinator.myChannels,
            currentCategoryId = channelCoordinator.currentCategory?.id,
            onCategorySelected = ::renderCategory
        )
    }

    private fun renderChannelManager() {
        channelRenderer?.renderManager(
            allChannels = channelCoordinator.allChannels,
            myChannels = channelCoordinator.myChannels,
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

    private fun createLayoutManager(category: DiscoverCategoryItem): RecyclerView.LayoutManager {
        return if (category.usesWaterfall) {
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL).apply {
                gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
            }
        } else {
            GridLayoutManager(requireContext(), 2)
        }
    }

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

    private fun registerNetworkCallback() {
        if (networkCallback != null) return
        val connectivityManager = context?.getSystemService(ConnectivityManager::class.java)
        if (connectivityManager == null) {
            AppLogger.w("HomeFragment", "ConnectivityManager unavailable; skip network callback registration.")
            return
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                binding.root.post {
                    networkSnackbar?.dismiss()
                    networkSnackbar = null
                }
            }

            override fun onLost(network: Network) {
                binding.root.post {
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

    private fun isNetworkAvailable(connectivityManager: ConnectivityManager): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showNetworkLostMessage() {
        if (!isAdded) return
        if (networkSnackbar?.isShown == true) return
        networkSnackbar = Snackbar.make(
            binding.root,
            getString(R.string.note_detail_network_lost),
            Snackbar.LENGTH_INDEFINITE
        ).also { it.show() }
    }

    private fun dp(value: Int): Int = value.dpToPx()

    override fun onDestroyView() {
        networkSnackbar?.dismiss()
        networkSnackbar = null
        binding.recyclerView.adapter = null
        binding.followingRecyclerView.adapter = null
        followingSectionRenderer = null
        channelRenderer = null
        _binding = null
        super.onDestroyView()
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

    private companion object {
        private const val LOAD_MORE_THRESHOLD = 4
    }
}
