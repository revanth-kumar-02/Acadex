package com.acadex.app.core.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeUtils {

    fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun getWeeklyDates(): List<Date> {
        val list = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3)
        for (i in 0..14) {
            list.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return list
    }

    fun formatDate(timeMillis: Long): String {
        val formatter = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        return formatter.format(Date(timeMillis))
    }

    fun formatDateTime(timeMillis: Long): String {
        val formatter = SimpleDateFormat("dd MMM, yyyy - hh:mm a", Locale.getDefault())
        return formatter.format(Date(timeMillis))
    }

    fun formatDeadline(dueTimeMillis: Long): String {
        val formatter = SimpleDateFormat("d MMMM", Locale.getDefault())
        val dueDateStr = formatter.format(Date(dueTimeMillis))
        
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dueCal = Calendar.getInstance().apply {
            timeInMillis = dueTimeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val diffMs = dueCal.timeInMillis - todayCal.timeInMillis
        val diffDays = (diffMs / 86400000L).toInt()
        
        val relativeStr = when {
            diffDays < 0 -> "Overdue"
            diffDays == 0 -> "Due Today"
            diffDays == 1 -> "1 day remaining"
            else -> "$diffDays days remaining"
        }
        
        return "$dueDateStr | $relativeStr"
    }

    fun parseIsoTimestamp(isoStr: String?): Long {
        if (isoStr == null) return System.currentTimeMillis()
        return runCatching {
            val clean = isoStr.substringBefore(".")
            val format = if (clean.endsWith("Z")) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
            } else {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            }
            format.parse(clean.replace("Z", "'Z'"))?.time ?: System.currentTimeMillis()
        }.getOrElse {
            runCatching {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(isoStr)?.time
            }.getOrNull() ?: System.currentTimeMillis()
        }
    }
}
