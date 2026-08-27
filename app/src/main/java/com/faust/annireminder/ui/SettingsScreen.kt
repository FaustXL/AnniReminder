package com.faust.annireminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(vm: AppViewModel) {
    LaunchedEffect(Unit) { vm.refreshCalendars() }
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Black)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(C.CardBlack)
                    .clickable { vm.screen = Screen.Home },
                contentAlignment = Alignment.Center
            ) {
                Text("←", color = C.Paper, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(14.dp))
            Text("设置", color = C.Orange, fontSize = 30.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(18.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(C.GrayCard)
                .padding(18.dp)
        ) {
            Column {
                Text("写入目标日历", color = C.Black, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                Text(
                    "日程会写入下面选中的日历账号",
                    style = Mono, color = C.Black.copy(alpha = 0.6f), fontSize = 11.sp
                )
                Spacer(Modifier.height(14.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (vm.settings.calendarId == -1L) C.Black else C.Black.copy(alpha = 0.10f))
                        .clickable { vm.selectCalendar(-1L) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val auto = vm.settings.calendarId == -1L
                    Text(
                        "自动选择（推荐）",
                        color = if (auto) C.Paper else C.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                    RadioDot(auto)
                }
                Spacer(Modifier.height(8.dp))

                if (vm.calendars.isEmpty()) {
                    Text(
                        "未发现可写日历，保存时会自动创建本地日历「纪念日」",
                        color = C.Black.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.Bold
                    )
                }
                vm.calendars.forEach { cal ->
                    val sel = vm.settings.calendarId == cal.id
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (sel) C.Black else C.Black.copy(alpha = 0.10f))
                            .clickable { vm.selectCalendar(cal.id) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                cal.name,
                                color = if (sel) C.Paper else C.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                            Text(
                                cal.account,
                                style = Mono,
                                color = (if (sel) C.Paper else C.Black).copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                        }
                        RadioDot(sel)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(C.CardBlack)
                .padding(18.dp)
        ) {
            Column {
                Text("说明", color = C.Orange, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "· 每个日子会在日历生成两条日程：当天 09:00 的生日/纪念日，以及提前 N 天 09:00 的提醒日程\n" +
                        "· 编辑保存时会自动删除旧日程并重新写入\n" +
                        "· 删除人物或某条日子时会同步删除日历中对应日程\n" +
                        "· 提醒铃响由系统日历应用负责，请保持其通知权限开启",
                    color = C.Paper.copy(alpha = 0.85f), fontSize = 13.sp, lineHeight = 21.sp
                )
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (selected) C.Orange else C.Black.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(C.Black)
            )
        }
    }
}
