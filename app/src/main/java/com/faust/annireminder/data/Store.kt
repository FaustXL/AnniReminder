package com.faust.annireminder.data

import android.content.Context
import com.faust.annireminder.model.CalType
import com.faust.annireminder.model.DateEntry
import com.faust.annireminder.model.Person
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.util.UUID

/** 应用数据：人物列表 + 设置，JSON 文件持久化（零第三方依赖） */
object Store {

    private const val DATA_FILE = "anniversaries.json"
    private const val SETTINGS_FILE = "settings.json"

    // ---------- 数据 ----------

    fun loadPeople(context: Context): MutableList<Person> {
        val f = File(context.filesDir, DATA_FILE)
        if (!f.exists()) return mutableListOf()
        return try {
            val arr = JSONArray(f.readText())
            (0 until arr.length()).mapNotNull { i ->
                runCatching { personFromJson(arr.getJSONObject(i)) }.getOrNull()
            }.toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun savePeople(context: Context, people: List<Person>) {
        val arr = JSONArray()
        people.forEach { arr.put(personToJson(it)) }
        writeFile(context, DATA_FILE, arr.toString())
    }

    // ---------- 设置 ----------

    data class Settings(
        val calendarId: Long = -1L   // -1 = 自动选择
    )

    fun loadSettings(context: Context): Settings {
        val f = File(context.filesDir, SETTINGS_FILE)
        if (!f.exists()) return Settings()
        return try {
            val o = JSONObject(f.readText())
            Settings(calendarId = o.optLong("calendarId", -1L))
        } catch (e: Exception) {
            Settings()
        }
    }

    fun saveSettings(context: Context, s: Settings) {
        writeFile(context, SETTINGS_FILE, JSONObject().put("calendarId", s.calendarId).toString())
    }

    // ---------- JSON 映射 ----------

    private fun personToJson(p: Person): JSONObject {
        val dates = JSONArray()
        p.dates.forEach { dates.put(entryToJson(it)) }
        return JSONObject()
            .put("id", p.id)
            .put("name", p.name)
            .put("note", p.note)
            .put("dates", dates)
    }

    private fun personFromJson(o: JSONObject): Person {
        val dates = ArrayList<DateEntry>()
        val arr = o.optJSONArray("dates") ?: JSONArray()
        for (i in 0 until arr.length()) {
            runCatching { dates.add(entryFromJson(arr.getJSONObject(i))) }
        }
        return Person(
            id = o.getString("id"),
            name = o.getString("name"),
            note = o.optString("note", ""),
            dates = dates
        )
    }

    private fun entryToJson(e: DateEntry): JSONObject {
        val ids = JSONArray()
        e.eventIds.forEach { ids.put(it) }
        return JSONObject()
            .put("id", e.id)
            .put("label", e.label)
            .put("type", e.type.name)
            .put("lunarMonth", e.lunarMonth)
            .put("lunarLeap", e.lunarLeap)
            .put("lunarDay", e.lunarDay)
            .put("solar", e.solarDate.toString())
            .put("repeatYears", e.repeatYears)
            .put("advanceDays", e.advanceDays)
            .put("calendarId", e.calendarId)
            .put("eventIds", ids)
    }

    private fun entryFromJson(o: JSONObject): DateEntry {
        val ids = ArrayList<Long>()
        val arr = o.optJSONArray("eventIds") ?: JSONArray()
        for (i in 0 until arr.length()) ids.add(arr.getLong(i))
        return DateEntry(
            id = o.getString("id"),
            label = o.getString("label"),
            type = CalType.valueOf(o.getString("type")),
            lunarMonth = o.optInt("lunarMonth", 1),
            lunarLeap = o.optBoolean("lunarLeap", false),
            lunarDay = o.optInt("lunarDay", 1),
            solarDate = runCatching { LocalDate.parse(o.getString("solar")) }.getOrDefault(LocalDate.now()),
            repeatYears = o.optInt("repeatYears", 10),
            advanceDays = o.optInt("advanceDays", 3),
            calendarId = o.optLong("calendarId", -1L),
            eventIds = ids
        )
    }

    // ---------- 工具 ----------

    fun newId(): String = UUID.randomUUID().toString()

    private fun writeFile(context: Context, name: String, content: String) {
        val tmp = File(context.filesDir, "$name.tmp")
        tmp.writeText(content)
        val target = File(context.filesDir, name)
        if (target.exists()) target.delete()
        tmp.renameTo(target)
    }
}
