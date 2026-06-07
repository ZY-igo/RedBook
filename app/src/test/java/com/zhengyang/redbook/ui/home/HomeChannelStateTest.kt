package com.zhengyang.redbook.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeChannelStateTest {

    @Test
    fun `sync initializes all channels and default selections`() {
        val state = HomeChannelState()
        val categories = listOf(
            category(id = "recommend", isDefaultSelected = true),
            category(id = "food"),
            category(id = "travel", isDefaultSelected = true)
        )

        state.sync(categories)

        assertEquals(categories, state.allChannels)
        assertEquals(listOf(categories[0], categories[2]), state.myChannels)
        assertEquals("recommend", state.currentCategory?.id)
    }

    @Test
    fun `sync preserves selected channels when data refreshes`() {
        val state = HomeChannelState()
        val initial = listOf(
            category(id = "recommend", isDefaultSelected = true),
            category(id = "food"),
            category(id = "travel", isDefaultSelected = true)
        )
        state.sync(initial)
        state.addChannel(initial[1])
        state.setCurrentCategory(initial[1])

        val refreshed = listOf(
            category(id = "recommend", title = "推荐", isDefaultSelected = true),
            category(id = "food", title = "美食"),
            category(id = "travel", title = "旅行", isDefaultSelected = true),
            category(id = "live", title = "直播")
        )

        state.sync(refreshed)

        assertEquals(listOf("recommend", "food", "travel"), state.myChannels.map { it.id })
        assertEquals("food", state.currentCategory?.id)
    }

    @Test
    fun `add channel ignores duplicates`() {
        val state = HomeChannelState()
        val recommend = category(id = "recommend", isDefaultSelected = true)
        val food = category(id = "food")
        state.sync(listOf(recommend, food))

        assertTrue(state.addChannel(food))
        assertFalse(state.addChannel(food))
        assertEquals(listOf("recommend", "food"), state.myChannels.map { it.id })
    }

    @Test
    fun `remove current channel falls back to first remaining channel`() {
        val state = HomeChannelState()
        val recommend = category(id = "recommend", isDefaultSelected = true)
        val travel = category(id = "travel", isDefaultSelected = true)
        val food = category(id = "food")
        state.sync(listOf(recommend, travel, food))
        state.addChannel(food)
        state.setCurrentCategory(food)

        assertTrue(state.removeChannel(food))

        assertEquals(listOf("recommend", "travel"), state.myChannels.map { it.id })
        assertEquals("recommend", state.currentCategory?.id)
    }

    @Test
    fun `cannot remove recommend or last remaining channel`() {
        val state = HomeChannelState()
        val recommend = category(id = "recommend", isDefaultSelected = true)
        val food = category(id = "food")
        state.sync(listOf(recommend, food))

        assertFalse(state.canRemoveChannel(recommend))
        assertFalse(state.removeChannel(recommend))

        assertTrue(state.addChannel(food))
        assertTrue(state.removeChannel(food))
        assertFalse(state.canRemoveChannel(recommend))
        assertEquals(listOf("recommend"), state.myChannels.map { it.id })
    }

    @Test
    fun `find nearby fallback returns travel when available`() {
        val state = HomeChannelState()
        val travel = category(id = "travel")
        state.sync(
            listOf(
                category(id = "recommend", isDefaultSelected = true),
                travel,
                category(id = "food")
            )
        )

        assertSame(travel, state.findNearbyFallbackCategory())
    }

    @Test
    fun `find nearby fallback returns null when travel missing`() {
        val state = HomeChannelState()
        state.sync(listOf(category(id = "recommend", isDefaultSelected = true)))

        assertNull(state.findNearbyFallbackCategory())
    }

    private fun category(
        id: String,
        title: String = id,
        isDefaultSelected: Boolean = false
    ): DiscoverCategoryItem {
        return DiscoverCategoryItem(
            id = id,
            title = title,
            bucket = DiscoverCategoryBucket.RECOMMEND,
            usesWaterfall = true,
            isDefaultSelected = isDefaultSelected
        )
    }
}
