package com.faust.annireminder.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faust.annireminder.calendar.CalendarHelper
import com.faust.annireminder.data.Store
import com.faust.annireminder.model.Person
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object Home : Screen
    data class Edit(val personId: String?) : Screen
    data object Settings : Screen
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    var people by mutableStateOf<List<Person>>(emptyList())
        private set
    var settings by mutableStateOf(Store.Settings())
        private set
    var calendars by mutableStateOf<List<CalendarHelper.CalInfo>>(emptyList())
        private set
    var screen by mutableStateOf<Screen>(Screen.Home)
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    private val context get() = getApplication<Application>()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch(Dispatchers.IO) {
            val p = Store.loadPeople(context)
            val s = Store.loadSettings(context)
            withContext(Dispatchers.Main) {
                people = p
                settings = s
            }
            refreshCalendars()
        }
    }

    fun refreshCalendars() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = CalendarHelper.listWritableCalendars(context)
            withContext(Dispatchers.Main) { calendars = list }
        }
    }

    fun selectCalendar(id: Long) {
        settings = settings.copy(calendarId = id)
        viewModelScope.launch(Dispatchers.IO) { Store.saveSettings(context, settings) }
    }

    /** 实际用于写入的日历 id（并记住选择） */
    private fun resolveCalendarId(): Long {
        val id = CalendarHelper.resolveTargetCalendarId(context, settings.calendarId)
        if (id != settings.calendarId) {
            settings = settings.copy(calendarId = id)
            Store.saveSettings(context, settings)
        }
        return id
    }

    fun savePerson(person: Person, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            busy = true
            try {
                val cr = context.contentResolver
                // 重建：先删除旧日程
                people.find { it.id == person.id }?.dates?.forEach {
                    CalendarHelper.deleteEvents(context, it.eventIds)
                }
                val calId = resolveCalendarId()
                val newDates = person.dates.map { e ->
                    val ids = CalendarHelper.writeEntryEvents(context, calId, person, e)
                    e.copy(calendarId = calId, eventIds = ids)
                }
                val updated = person.copy(dates = newDates)
                val list = people.toMutableList()
                val idx = list.indexOfFirst { it.id == person.id }
                if (idx >= 0) list[idx] = updated else list.add(updated)
                Store.savePeople(context, list)
                val total = newDates.sumOf { it.eventIds.size }
                withContext(Dispatchers.Main) {
                    people = list
                    onResult(true, "已写入 $total 条日程")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "写入失败：${e.message}") }
            } finally {
                withContext(Dispatchers.Main) { busy = false }
            }
        }
    }

    fun deletePerson(person: Person) {
        viewModelScope.launch(Dispatchers.IO) {
            CalendarHelper.deleteEvents(context, person.dates.flatMap { it.eventIds })
            val list = people.filterNot { it.id == person.id }
            Store.savePeople(context, list)
            withContext(Dispatchers.Main) { people = list }
        }
    }

    fun deleteEntry(person: Person, entryId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entry = person.dates.find { it.id == entryId }
            entry?.let { CalendarHelper.deleteEvents(context, it.eventIds) }
            val updated = person.copy(dates = person.dates.filterNot { it.id == entryId })
            val list = if (updated.dates.isEmpty()) {
                people.filterNot { it.id == person.id }
            } else {
                people.map { if (it.id == person.id) updated else it }
            }
            Store.savePeople(context, list)
            withContext(Dispatchers.Main) { people = list }
        }
    }
}
