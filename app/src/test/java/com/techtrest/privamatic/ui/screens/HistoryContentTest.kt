package com.techtrest.privamatic.ui.screens

import com.techtrest.privamatic.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryContentTest {

    @Test
    fun `no history at all shows the first-point message and cannot clear`() {
        val content = historyContent(pointsInRange = 0, hasAnyHistory = false)
        assertEquals(R.string.label_history_no_data, content.emptyMessage)
        assertFalse(content.canClear)
    }

    @Test
    fun `empty range with older history says so and can still clear`() {
        val content = historyContent(pointsInRange = 0, hasAnyHistory = true)
        assertEquals(R.string.label_history_no_data_in_range, content.emptyMessage)
        assertTrue(content.canClear)
    }

    @Test
    fun `points in range show the chart`() {
        val content = historyContent(pointsInRange = 3, hasAnyHistory = true)
        assertNull(content.emptyMessage)
        assertTrue(content.canClear)
    }
}
