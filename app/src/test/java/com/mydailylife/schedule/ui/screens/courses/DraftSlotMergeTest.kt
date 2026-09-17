package com.mydailylife.schedule.ui.screens.courses

import org.junit.Assert.assertEquals
import org.junit.Test

class DraftSlotMergeTest {
    @Test
    fun mergesAdjacentSameDay() {
        val first = listOf(DraftSlot(weekday = 1, startSlot = 1, endSlot = 2))
        val merged = mergeDraftSlot(first, DraftSlot(weekday = 1, startSlot = 3, endSlot = 4))
        assertEquals(listOf(DraftSlot(1, 1, 4)), merged)
    }

    @Test
    fun keepsSeparateDays() {
        val first = listOf(DraftSlot(weekday = 1, startSlot = 1, endSlot = 2))
        val merged = mergeDraftSlot(first, DraftSlot(weekday = 2, startSlot = 1, endSlot = 2))
        assertEquals(2, merged.size)
    }
}
