package com.faust.annireminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faust.annireminder.calendar.CalendarHelper
import com.faust.annireminder.lunar.DateExpander
import com.faust.annireminder.model.CalType
import com.faust.annireminder.model.Person
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun HomeScreen(vm: AppViewModel, hasPermission: Boolean, onRequestPermission: () -> Unit) {
    var deleteTarget by remember { mutableStateOf<Person?>(null) }

    Box(
        Modifier
            .fillMaxSize()
            .background(C.Black)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── 橙色大标题卡 ──
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(30.dp))
                    .background(C.Orange)
                    .padding(24.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text("纪念日", color = C.Black, fontSize = 42.sp, fontWeight = FontWeight.Black, lineHeight = 44.sp)
                            Text("提醒", color = C.Black, fontSize = 42.sp, fontWeight = FontWeight.Black, lineHeight = 44.sp)
                        }
                        Box(
                            Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(C.Black)
                                .clickable { vm.screen = Screen.Settings },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚙", color = C.Paper, fontSize = 20.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    val eventCount = vm.people.sumOf { it.dates.size }
                    Text(
                        "${vm.people.size} PEOPLE · $eventCount DATES · BIRTHDAY & ANNIVERSARY",
                        style = Mono,
                        color = C.Black.copy(alpha = 0.8f)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            if (!hasPermission) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(C.CardBlack)
                        .clickable(onClick = onRequestPermission)
                        .padding(18.dp)
                ) {
                    Text(
                        "需要日历权限才能写入日程，点击授权 →",
                        color = C.Orange, fontWeight = FontWeight.Bold, fontSize = 14.sp
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            // ── 人物卡片 ──
            vm.people.forEach { p ->
                PersonCard(
                    person = p,
                    onEdit = { vm.screen = Screen.Edit(p.id) },
                    onDelete = { deleteTarget = p },
                    onQuickDeleteEntry = { eid -> vm.deleteEntry(p, eid) }
                )
                Spacer(Modifier.height(14.dp))
            }

            if (vm.people.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(C.CardBlack)
                        .padding(vertical = 46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "还没有人\n点击右下角 ＋ 添加第一位",
                        color = C.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        lineHeight = 26.sp
                    )
                }
            }
            Spacer(Modifier.height(90.dp))
        }

        // ── FAB ──
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(22.dp)
                .size(66.dp)
                .clip(CircleShape)
                .background(C.Orange)
                .clickable { vm.screen = Screen.Edit(null) },
            contentAlignment = Alignment.Center
        ) {
            Text("＋", color = C.Black, fontSize = 32.sp, fontWeight = FontWeight.Black)
        }
    }

    // 删除确认
    deleteTarget?.let { p ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = C.CardBlack,
            title = { Text("删除「${p.name}」？", color = C.Paper, fontWeight = FontWeight.Black) },
            text = { Text("将同时删除已写入系统日历的全部 ${p.dates.sumOf { it.eventIds.size }} 条日程。", color = C.Gray) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deletePerson(p)
                    deleteTarget = null
                }) { Text("删除", color = C.Orange, fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消", color = C.Gray) }
            }
        )
    }
}

@Composable
private fun PersonCard(
    person: Person,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onQuickDeleteEntry: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(C.GrayCard)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                person.name.ifEmpty { "未命名" },
                color = C.Black, fontSize = 28.sp, fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Text(
                "编辑",
                color = C.Black, fontWeight = FontWeight.Black, fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(C.Black.copy(alpha = 0.12f))
                    .clickable(onClick = onEdit)
                    .padding(horizontal = 13.dp, vertical = 6.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "删除",
                color = C.Black, fontWeight = FontWeight.Black, fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(C.Black)
                    .clickable(onClick = onDelete)
                    .padding(horizontal = 13.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        if (person.dates.isEmpty()) {
            Text("（暂无日子，点编辑添加）", color = C.Black.copy(alpha = 0.55f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        person.dates.forEach { e ->
            DateRow(e, onQuickDeleteEntry)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DateRow(entry: com.faust.annireminder.model.DateEntry, onDelete: (String) -> Unit) {
    val next = DateExpander.next(entry)
    val days = next?.let { ChronoUnit.DAYS.between(LocalDate.now(), it) }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(C.Orange)
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${emojiOf(entry.label)} ${entry.label}",
                    color = C.Black, fontSize = 17.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f), maxLines = 1
                )
                Text(
                    if (entry.type == CalType.LUNAR) "农历" else "公历",
                    style = Mono,
                    color = C.Black,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(C.Black.copy(alpha = 0.14f))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "×",
                    color = C.Black.copy(alpha = 0.6f), fontSize = 16.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onDelete(entry.id) }
                        .padding(horizontal = 6.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    CalendarHelper.entryLine(entry),
                    style = Mono,
                    color = C.Black.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                if (days != null && days <= 7) {
                    Text(
                        if (days == 0L) "今天!" else "还有${days}天",
                        color = C.Paper,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(C.Black)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private fun emojiOf(label: String): String = when {
    label.contains("生日") -> "🎂"
    label.contains("恋爱") -> "❤"
    label.contains("结婚") || label.contains("领证") -> "💍"
    label.contains("周年") -> "🎉"
    else -> "💐"
}
