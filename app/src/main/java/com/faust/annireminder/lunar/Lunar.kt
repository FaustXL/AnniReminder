package com.faust.annireminder.lunar

import android.icu.util.Calendar
import android.icu.util.ChineseCalendar
import android.icu.util.TimeZone
import com.faust.annireminder.model.CalType
import com.faust.annireminder.model.DateEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 农历/公历日期展开：把一条 DateEntry 换算成未来 N 个具体公历日期。
 * 农历换算使用系统内置 ICU ChineseCalendar（API 24+），无需第三方库。
 */
object DateExpander {

    private val SHANGHAI = ZoneId.of("Asia/Shanghai")
    private val TZ = TimeZone.getTimeZone("Asia/Shanghai")

    /**
     * 农历(扩展年/月/日/闰月标志) -> 公历日期。
     * 该年没有对应闰月时回退为平月；日数超出月长时钳制到月末。
     */
    fun lunarToSolar(extYear: Int, month1based: Int, isLeap: Boolean, day: Int): LocalDate {
        var cc = resolve(extYear, month1based, isLeap)
        val wantMonth = month1based - 1
        val resolvedMonth = cc.get(Calendar.MONTH)
        if (isLeap && resolvedMonth != wantMonth) {
            // ICU 把闰月解析成了别的月份（该年无此闰月），回退为平月
            cc = resolve(extYear, month1based, false)
        }
        cc.set(Calendar.DAY_OF_MONTH, 1)
        val maxDay = cc.getActualMaximum(Calendar.DAY_OF_MONTH)
        cc.set(Calendar.DAY_OF_MONTH, day.coerceAtMost(maxDay))
        return toLocalDate(cc)
    }

    private fun resolve(extYear: Int, month1based: Int, isLeap: Boolean): ChineseCalendar {
        val cc = ChineseCalendar()
        cc.timeZone = TZ
        cc.clear()
        cc.set(Calendar.EXTENDED_YEAR, extYear)
        cc.set(Calendar.MONTH, month1based - 1)
        cc.set(ChineseCalendar.IS_LEAP_MONTH, if (isLeap) 1 else 0)
        // 触发字段解析
        cc.get(Calendar.MONTH)
        return cc
    }

    /** 以今天为基准，取该农历月日的未来 count 个公历日期 */
    fun lunarOccurrences(month1based: Int, isLeap: Boolean, day: Int, count: Int, today: LocalDate): List<LocalDate> {
        val base = newChineseCalendarNow().get(Calendar.EXTENDED_YEAR)
        val out = ArrayList<LocalDate>(count)
        var k = -1
        while (out.size < count && k <= count + 2) {
            val d = lunarToSolar(base + k, month1based, isLeap, day)
            if (!d.isBefore(today)) out.add(d)
            k++
        }
        return out
    }

    /** 公历(月/日) 未来 count 个日期；2/29 在平年顺延为 3/1 */
    fun solarOccurrences(anchor: LocalDate, count: Int, today: LocalDate): List<LocalDate> {
        val y0 = today.year
        val out = ArrayList<LocalDate>(count)
        var year = y0 - 1
        while (out.size < count && year <= y0 + count + 1) {
            val d = inYear(anchor, year)
            if (!d.isBefore(today)) out.add(d)
            year++
        }
        return out
    }

    fun inYear(anchor: LocalDate, year: Int): LocalDate {
        return try {
            anchor.withYear(year)
        } catch (e: Exception) {
            // 2/29 -> 平年顺延 3/1
            LocalDate.of(year, 3, 1)
        }
    }

    /** 展开一条配置为 N 个"当天"日期 */
    fun expand(entry: DateEntry, count: Int = entry.repeatYears, today: LocalDate = LocalDate.now()): List<LocalDate> {
        return if (entry.type == CalType.LUNAR) {
            lunarOccurrences(entry.lunarMonth, entry.lunarLeap, entry.lunarDay, count, today)
        } else {
            solarOccurrences(entry.solarDate, count, today)
        }
    }

    /** 下一次（含今天）发生日 */
    fun next(entry: DateEntry, today: LocalDate = LocalDate.now()): LocalDate? =
        expand(entry, 1, today).firstOrNull()

    private fun newChineseCalendarNow(): ChineseCalendar {
        val cc = ChineseCalendar()
        cc.timeZone = TZ
        return cc
    }

    private fun toLocalDate(cc: ChineseCalendar): LocalDate =
        Instant.ofEpochMilli(cc.timeInMillis).atZone(SHANGHAI).toLocalDate()
}
