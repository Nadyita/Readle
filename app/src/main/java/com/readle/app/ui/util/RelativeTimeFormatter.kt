package com.readle.app.ui.util

import android.content.Context
import com.readle.app.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Formats a timestamp into a relative time string.
 * - "Heute, HH:mm" for today
 * - "Gestern" for yesterday
 * - "vor X Tagen" for 2-6 days ago
 * - "16. März" for this year
 * - "16. März 2025" for previous years
 */
object RelativeTimeFormatter {

    /**
     * Formats a timestamp into a localized relative time string.
     *
     * @param timestamp The timestamp in milliseconds
     * @param currentTimeMillis The current time in milliseconds (for testing)
     * @param context Android context for string resources
     * @return Formatted relative time string
     */
    fun format(
        timestamp: Long,
        currentTimeMillis: Long = System.currentTimeMillis(),
        context: Context
    ): String {
        if (timestamp <= 0) {
            return context.getString(R.string.abs_never_synced)
        }

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTimeMillis

        // Get today's date at midnight
        val todayMidnight = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Get yesterday's date at midnight
        val yesterdayMidnight = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Get sync date
        val syncCalendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }

        // Calculate days difference
        val diffMillis = currentTimeMillis - timestamp
        val diffDays = diffMillis / (1000 * 60 * 60 * 24)

        return when {
            timestamp >= todayMidnight -> {
                // Today: show "Heute, HH:mm" / "Today, HH:mm"
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                context.getString(R.string.abs_sync_today, timeFormat.format(Date(timestamp)))
            }
            timestamp >= yesterdayMidnight -> {
                // Yesterday
                context.getString(R.string.abs_sync_yesterday)
            }
            diffDays < 7 -> {
                // Within a week: show "vor X Tagen" / "X days ago"
                context.getString(R.string.abs_sync_days_ago, diffDays.toInt())
            }
            syncCalendar.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) -> {
                // This year: show "16. März" / "March 16"
                val dateFormat = SimpleDateFormat("d. MMMM", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
            else -> {
                // Older: show "16. März 2025" / "March 16, 2025"
                val dateFormat = SimpleDateFormat("d. MMMM yyyy", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
        }
    }
}

