package com.faust.annireminder.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.faust.annireminder.lunar.DateExpander
import com.faust.annireminder.model.DateEntry
import com.faust.annireminder.model.Person
import com.faust.annireminder.model.LunarText
import java.time.LocalDate
import java.time.ZoneId

/** 系统日历 Provider 读写：查/建日历、写入日程（当天 + 提前N天）、按 id 删除 */
object CalendarHelper {

    const val LOCAL_ACCOUNT_NAME = "纪念日提醒"

    data class CalInfo(
        val id: Long,
        val name: String,
        val account: String,
        val isLocal: Boolean
    )

    // ---------- 日历查询 ----------

    fun listWritableCalendars(context: Context): List<CalInfo> {
        val cr = context.contentResolver
        val uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )
        val sel = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val args = arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString())
        val out = ArrayList<CalInfo>()
        try {
            cr.query(uri, projection, sel, args, null)?.use { c ->
                while (c.moveToNext()) {
                    val type = c.getString(3) ?: ""
                    out.add(
                        CalInfo(
                            id = c.getLong(0),
                            name = c.getString(1) ?: "日历",
                            account = c.getString(2) ?: "",
                            isLocal = type == CalendarContract.ACCOUNT_TYPE_LOCAL
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // 无权限等
        }
        return out
    }

    /**
     * 确定写入目标日历：优先用户选择的；其次本机已有可写日历；都没有则创建本地日历。
     */
    fun resolveTargetCalendarId(context: Context, preferredId: Long): Long {
        val list = listWritableCalendars(context)
        list.firstOrNull { it.id == preferredId }?.let { return it.id }
        list.firstOrNull { it.isLocal }?.let { return it.id }
        return list.firstOrNull()?.id ?: createLocalCalendar(context)
    }

    /** 创建本地日历（无任何可写日历时的兜底） */
    fun createLocalCalendar(context: Context): Long {
        val cr = context.contentResolver
        val uri = CalendarContract.Calendars.CONTENT_URI
            .buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, LOCAL_ACCOUNT_NAME)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, LOCAL_ACCOUNT_NAME)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, LOCAL_ACCOUNT_NAME)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "纪念日")
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFFFF4B00.toInt())
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, LOCAL_ACCOUNT_NAME)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
        }
        val newUri = cr.insert(uri, values) ?: error("创建本地日历失败")
        return ContentUris.parseId(newUri)
    }

    // ---------- 日程写入 ----------

    /** 把一条 DateEntry 展开写入：每个日期 1 条当天日程 + 提前N天 1 条提醒日程 */
    fun writeEntryEvents(context: Context, calendarId: Long, person: Person, entry: DateEntry): List<Long> {
        val today = LocalDate.now()
        val dates = DateExpander.expand(entry, today = today)
        val ids = ArrayList<Long>(dates.size * 2)
        val anchor = entry.summary()
        val desc = "$anchor · 连续${entry.repeatYears}年 · 由纪念日提醒创建"
        for (d in dates) {
            ids += insertEvent(context, calendarId, "🎂 ${person.name} · ${entry.label}", d, desc)
            if (entry.advanceDays > 0) {
                val rd = d.minusDays(entry.advanceDays.toLong())
                if (!rd.isBefore(today)) {
                    ids += insertEvent(
                        context, calendarId,
                        "⏰ 还有${entry.advanceDays}天：${person.name} · ${entry.label}", rd, desc
                    )
                }
            }
        }
        return ids
    }

    private fun insertEvent(context: Context, calendarId: Long, title: String, date: LocalDate, desc: String): Long {
        val cr = context.contentResolver
        val zone = ZoneId.systemDefault()
        val start = date.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val end = start + 15 * 60 * 1000L
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DESCRIPTION, desc)
            put(CalendarContract.Events.DTSTART, start)
            put(CalendarContract.Events.DTEND, end)
            put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
            put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
        }
        val uri = cr.insert(CalendarContract.Events.CONTENT_URI, values) ?: error("写入日程失败: $title")
        val eventId = ContentUris.parseId(uri)
        // 上午 9 点提醒（事件本身定在 9:00，minutes=0 即准点响铃）
        runCatching {
            val rv = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, 0)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            cr.insert(CalendarContract.Reminders.CONTENT_URI, rv)
        }
        return eventId
    }

    // ---------- 删除 ----------

    fun deleteEvents(context: Context, eventIds: List<Long>) {
        if (eventIds.isEmpty()) return
        val cr = context.contentResolver
        for (id in eventIds) {
            try {
                cr.delete(
                    CalendarContract.Events.CONTENT_URI,
                    "${CalendarContract.Events._ID}=?",
                    arrayOf(id.toString())
                )
            } catch (e: Exception) {
                // 忽略单条失败
            }
        }
    }

    // ---------- 展示辅助 ----------

    fun entryLine(entry: DateEntry): String {
        val next = DateExpander.next(entry) ?: return entry.summary()
        val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), next)
        val when0 = if (days == 0L) "今天" else "还有${days}天"
        return "${entry.summary()} · $next · $when0"
    }
}
