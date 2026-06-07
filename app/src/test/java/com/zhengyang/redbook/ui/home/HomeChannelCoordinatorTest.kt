package com.zhengyang.redbook.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeChannelCoordinatorTest {

    @Test
    fun `toggle edit mode flips and exit clears it`() {
        val coordinator = HomeChannelCoordinator()

        assertFalse(coordinator.isEditMode)
        assertTrue(coordinator.toggleEditMode())
        assertTrue(coordinator.isEditMode)

        coordinator.exitEditMode()

        assertFalse(coordinator.isEditMode)
    }

    @Test
    fun `remove current channel reports fallback category`() {
        val coordinator = HomeChannelCoordinator()
        val recommend = category("recommend", defaultSelected = true)
        val travel = category("travel", defaultSelected = true)
        val food = category("food")

        coordinator.sync(listOf(recommend, travel, food))
        coordinator.addChannel(food)
        coordinator.setCurrentCategory(food)

        val result = coordinator.removeChannel(food)

        assertEquals(
            HomeChannelCoordinator.ChannelRemovalResult.CurrentCategoryChanged(recommend),
            result
        )
        assertEquals("recommend", coordinator.currentCategory?.id)
    }

    @Test
    fun `remove non current channel keeps current selection`() {
        val coordinator = HomeChannelCoordinator()
        val recommend = category("recommend", defaultSelected = true)
        val travel = category("travel", defaultSelected = true)
        val food = category("food")

        coordinator.sync(listOf(recommend, travel, food))
        coordinator.addChannel(food)
        coordinator.setCurrentCategory(recommend)

        val result = coordinator.removeChannel(food)

        assertEquals(HomeChannelCoordinator.ChannelRemovalResult.Removed, result)
        assertEquals("recommend", coordinator.currentCategory?.id)
    }

    @Test
    fun `remove blocked channel reports unchanged`() {
        val coordinator = HomeChannelCoordinator()
        val recommend = category("recommend", defaultSelected = true)

        coordinator.sync(listOf(recommend))

        assertEquals(
            HomeChannelCoordinator.ChannelRemovalResult.Unchanged,
            coordinator.removeChannel(recommend)
        )
    }

    private fun category(
        id: String,
        defaultSelected: Boolean = false
    ): DiscoverCategoryItem {
        return DiscoverCategoryItem(
            id = id,
            title = id,
            bucket = DiscoverCategoryBucket.RECOMMEND,
            usesWaterfall = true,
            isDefaultSelected = defaultSelected
        )
    }
}
