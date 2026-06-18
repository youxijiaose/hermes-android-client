package com.hermes.client.util

import android.content.Context
import com.hermes.client.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * 相对时间格式化
 * 例如: "2 分钟前", "1 小时前", "昨天", "2024-01-15"
 */
object TimeUtils {

    private val SECOND_MILLIS = 1000
    private val MINUTE_MILLIS = 60 * SECOND_MILLIS
    private val HOUR_MILLIS = 60 * MINUTE_MILLIS
    private val DAY_MILLIS = 24 * HOUR_MILLIS

    private val shortDateFormat = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
    private val longDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
    private val yesterdayFormat = SimpleDateFormat("昨天 HH:mm", Locale.getDefault())

    /**
     * 格式化时间戳为相对时间
     */
    fun formatRelativeTime(context: Context, timestamp: Long): String {
        val now = System.currentTimeMillis()
        val delta = now - timestamp

        return when {
            delta < MINUTE_MILLIS -> {
                "刚刚"
            }
            delta < 2 * MINUTE_MILLIS -> {
                "1 分钟前"
            }
            delta < HOUR_MILLIS -> {
                "${delta / MINUTE_MILLIS} 分钟前"
            }
            delta < 2 * HOUR_MILLIS -> {
                "1 小时前"
            }
            delta < DAY_MILLIS -> {
                "${delta / HOUR_MILLIS} 小时前"
            }
            delta < 2 * DAY_MILLIS -> {
                yesterdayFormat.format(Date(timestamp))
            }
            timestamp < now - 7 * DAY_MILLIS -> {
                longDateFormat.format(Date(timestamp))
            }
            else -> {
                shortDateFormat.format(Date(timestamp))
            }
        }
    }

    /**
     * 格式化时间戳为完整日期
     */
    fun formatFullTime(timestamp: Long): String {
        return longDateFormat.format(Date(timestamp))
    }

    /**
     * 格式化时间戳为短日期
     */
    fun formatShortTime(timestamp: Long): String {
        return shortDateFormat.format(Date(timestamp))
    }

    /**
     * 检查是否是今天
     */
    fun isToday(timestamp: Long): Boolean {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return timestamp >= today
    }

    /**
     * 检查是否是昨天
     */
    fun isYesterday(timestamp: Long): Boolean {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return timestamp >= yesterday && timestamp < yesterday + DAY_MILLIS
    }
}
