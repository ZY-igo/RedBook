package com.zhengyang.redbook.ui.home

import com.zhengyang.redbook.data.mapper.HomeMapper
import com.zhengyang.redbook.data.model.DiscoverCategory
import com.zhengyang.redbook.data.model.FollowingUser
import com.zhengyang.redbook.data.model.HomeDiscoverItem
import com.zhengyang.redbook.data.model.MediaType
import com.zhengyang.redbook.data.repository.HomeRepository
import com.zhengyang.redbook.usecase.HomeDiscoverItemDisplayPolicy
import com.zhengyang.redbook.usecase.LoadHomeCategoriesUseCase
import com.zhengyang.redbook.usecase.LoadHomeDiscoverItemsUseCase
import com.zhengyang.redbook.usecase.LoadHomeFollowingSeedUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `init loads categories discover feed and following seed`() = runTest {
        val repository = FakeHomeRepository(
            categories = listOf(category("recommend", defaultSelected = true)),
            discoverItemsByCategory = mapOf(
                "recommend" to listOf(discoverItem(id = "note-1"))
            ),
            suggestedUsers = listOf(suggestedUser("user-1")),
            followingFeedItems = listOf(discoverItem(id = "follow-note"))
        )

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isInitialLoading)
        assertEquals(1, state.categories.size)
        assertEquals("recommend", state.categories.first().id)
        assertEquals(listOf("note-1"), state.discoverItems.map { it.id })
        assertEquals(listOf("user-1"), state.suggestedUsers.map { it.id })
        assertEquals(listOf("follow-note"), state.followingFeedItems.map { it.id })
        assertFalse(state.isDiscoverRefreshing)
        assertFalse(state.isFollowingRefreshing)
    }

    @Test
    fun `refresh discover failure exposes error state and message event`() = runTest {
        val repository = FakeHomeRepository(
            categories = listOf(category("recommend", defaultSelected = true)),
            discoverItemsByCategory = mapOf(
                "recommend" to listOf(discoverItem(id = "note-1"))
            ),
            suggestedUsers = listOf(suggestedUser("user-1")),
            followingFeedItems = listOf(discoverItem(id = "follow-note"))
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        repository.discoverError = IllegalStateException("discover failed")
        val eventDeferred = backgroundScope.async { viewModel.events.first() }

        viewModel.refreshDiscover(
            DiscoverCategoryItem(
                id = "recommend",
                title = "recommend",
                bucket = DiscoverCategoryBucket.RECOMMEND,
                usesWaterfall = true,
                isDefaultSelected = true
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("discover failed", state.discoverErrorMessage)
        assertFalse(state.isDiscoverRefreshing)
        assertEquals(
            HomeUiEvent.ShowMessage("discover failed"),
            eventDeferred.await()
        )
    }

    @Test
    fun `refresh following without force skips reload when data already exists`() = runTest {
        val repository = FakeHomeRepository(
            categories = listOf(category("recommend", defaultSelected = true)),
            discoverItemsByCategory = mapOf(
                "recommend" to listOf(discoverItem(id = "note-1"))
            ),
            suggestedUsers = listOf(suggestedUser("user-1")),
            followingFeedItems = listOf(discoverItem(id = "follow-note"))
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        assertEquals(1, repository.followingSeedLoadCount)

        viewModel.refreshFollowing(force = false)
        advanceUntilIdle()

        assertEquals(1, repository.followingSeedLoadCount)
        assertFalse(viewModel.uiState.value.isFollowingRefreshing)
    }

    @Test
    fun `follow user moves suggestion once and avoids duplicates`() = runTest {
        val repository = FakeHomeRepository(
            categories = listOf(category("recommend", defaultSelected = true)),
            discoverItemsByCategory = mapOf(
                "recommend" to listOf(discoverItem(id = "note-1"))
            ),
            suggestedUsers = listOf(suggestedUser("user-1")),
            followingFeedItems = listOf(discoverItem(id = "follow-note"))
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.followUser("user-1")
        viewModel.followUser("user-1")

        val state = viewModel.uiState.value
        assertTrue(state.suggestedUsers.isEmpty())
        assertEquals(listOf("user-1"), state.followingUsers.map { it.id })
    }

    private fun createViewModel(repository: FakeHomeRepository): HomeViewModel {
        val mapper = HomeMapper()
        val displayPolicy = HomeDiscoverItemDisplayPolicy()
        return HomeViewModel(
            loadHomeCategories = LoadHomeCategoriesUseCase(repository, mapper),
            loadHomeDiscoverItems = LoadHomeDiscoverItemsUseCase(repository, mapper, displayPolicy),
            loadHomeFollowingSeed = LoadHomeFollowingSeedUseCase(repository, mapper, displayPolicy)
        )
    }

    private fun category(id: String, defaultSelected: Boolean = false): DiscoverCategory {
        return DiscoverCategory(
            id = id,
            title = id,
            bucket = DiscoverCategoryBucket.RECOMMEND.name,
            usesWaterfall = true,
            isDefaultSelected = defaultSelected
        )
    }

    private fun suggestedUser(id: String): FollowingUser {
        return FollowingUser(
            id = id,
            name = id,
            subtitle = "subtitle-$id",
            avatarColorHex = "#123456"
        )
    }

    private fun discoverItem(id: String): HomeDiscoverItem {
        return HomeDiscoverItem(
            id = id,
            title = "title-$id",
            author = "author-$id",
            likeCount = "8",
            badge = "badge",
            coverLabel = "label",
            coverHeightDp = 220,
            mediaType = MediaType.IMAGE,
            imageUrls = listOf("https://example.com/$id.jpg"),
            imageUrl = "https://example.com/$id.jpg",
            startColorHex = "#111111",
            endColorHex = "#222222",
            avatarColorHex = "#333333"
        )
    }

    private class FakeHomeRepository(
        private val categories: List<DiscoverCategory>,
        private val discoverItemsByCategory: Map<String, List<HomeDiscoverItem>>,
        private val suggestedUsers: List<FollowingUser>,
        private val followingFeedItems: List<HomeDiscoverItem>
    ) : HomeRepository {
        var discoverError: Throwable? = null
        var followingSeedLoadCount: Int = 0

        override suspend fun getCategories(): List<DiscoverCategory> = categories

        override suspend fun getDiscoverItems(categoryId: String): List<HomeDiscoverItem> {
            discoverError?.let { throw it }
            return discoverItemsByCategory[categoryId].orEmpty()
        }

        override suspend fun getSuggestedFollowingUsers(): List<FollowingUser> {
            followingSeedLoadCount += 1
            return suggestedUsers
        }

        override suspend fun getFollowingFeedItems(): List<HomeDiscoverItem> {
            return followingFeedItems
        }
    }
}
