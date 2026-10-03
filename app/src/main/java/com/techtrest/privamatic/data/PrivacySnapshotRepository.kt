package com.techtrest.privamatic.data

import android.content.Context
import com.techtrest.privamatic.data.model.CheckDeduction
import com.techtrest.privamatic.data.model.PrivacySnapshot
import java.util.Calendar

enum class HistoryFilter { WEEK, MONTH, THREE_MONTHS, SIX_MONTHS, YEAR, ALL }

class PrivacySnapshotRepository(context: Context) {
    private val dao = PrivacyDatabase.getInstance(context).snapshotDao()

    /** Records a snapshot — at most one per calendar day. Skips if one already exists today. */
    suspend fun recordSnapshot(score: Int, deductions: List<CheckDeduction>) {
        val now = System.currentTimeMillis()
        val dayStart = midnightOf(now)
        val dayEnd = dayStart + DAY_MS
        if (dao.countSnapshotsForDay(dayStart, dayEnd) == 0) {
            dao.insert(
                PrivacySnapshot(
                    timestamp = now,
                    score = score,
                    deductionsJson = CheckDeduction.encode(deductions)
                )
            )
        }
    }

    suspend fun getSnapshots(filter: HistoryFilter): List<PrivacySnapshot> {
        if (filter == HistoryFilter.ALL) return dao.getAllSnapshots()
        val since = System.currentTimeMillis() - filterDays(filter) * DAY_MS
        return dao.getSnapshotsSince(since)
    }

    suspend fun getEarliestTimestamp(): Long? = dao.getEarliestTimestamp()

    /** Any snapshot at all, whatever the selected range. */
    suspend fun hasAnySnapshot(): Boolean = dao.getEarliestTimestamp() != null

    suspend fun clearAll() = dao.deleteAll()

    private fun filterDays(filter: HistoryFilter): Long = when (filter) {
        HistoryFilter.WEEK -> 7L
        HistoryFilter.MONTH -> 30L
        HistoryFilter.THREE_MONTHS -> 90L
        HistoryFilter.SIX_MONTHS -> 180L
        HistoryFilter.YEAR -> 365L
        HistoryFilter.ALL -> 0L
    }

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000

        private fun midnightOf(ms: Long): Long = Calendar.getInstance().apply {
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
