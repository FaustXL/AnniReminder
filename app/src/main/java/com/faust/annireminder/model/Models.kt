package com.faust.annireminder.model

import java.time.LocalDate

/** 农历或公历 */
enum class CalType { LUNAR, SOLAR }

/**
 * 一条纪念日配置（例如某人的生日）。
 * 写入系统日历后，产生的每条日程 id 记录在 eventIds，用于编辑时重建、删除时清理。
 */
data class DateEntry(
    val id: String,
    val label: String,              // 生日 / 纪念日 / 恋爱纪念日 / 自定义文本
    val type: CalType,              // 农历或公历
    // 农历锚点：month 1-12，isLeapMonth 是否闰该月，day 1-30
    val lunarMonth: Int = 1,
    val lunarLeap: Boolean = false,
    val lunarDay: Int = 1,
    // 公历锚点（只使用月/日循环，year 仅作记录）
    val solarDate: LocalDate = LocalDate.now(),
    val repeatYears: Int = 10,      // 连续写入几年
    val advanceDays: Int = 3,       // 提前几天写入提醒日程，0 表示不提前
    val calendarId: Long = -1L,     // 写入时使用的系统日历
    val eventIds: List<Long> = emptyList()
) {
    fun summary(): String = if (type == CalType.LUNAR) {
        "农历" + (if (lunarLeap) "闰" else "") + LunarText.month(lunarMonth) + LunarText.day(lunarDay)
    } else {
        "${solarDate.monthValue}月${solarDate.dayOfMonth}日"
    }
}

data class Person(
    val id: String,
    val name: String,
    val note: String = "",
    val dates: List<DateEntry> = emptyList()
)

object LunarText {
    private val MONTHS = listOf(
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "冬月", "腊月"
    )
    private val DAY_PREFIX = listOf("初", "十", "廿", "三")
    private val DAY_SUFFIX = listOf("一", "二", "三", "四", "五", "六", "七", "八", "九", "十")

    fun month(m: Int): String = MONTHS[(m - 1).coerceIn(0, 11)]

    fun day(d: Int): String = when {
        d <= 10 -> DAY_PREFIX[0] + DAY_SUFFIX[d - 1]
        d < 20 -> DAY_PREFIX[1] + DAY_SUFFIX[d - 11]
        d == 20 -> "二十"
        d < 30 -> DAY_PREFIX[2] + DAY_SUFFIX[d - 21]
        else -> "三十"
    }
}
