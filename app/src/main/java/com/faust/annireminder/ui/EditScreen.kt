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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.faust.annireminder.data.Store
import com.faust.annireminder.model.CalType
import com.faust.annireminder.model.DateEntry
import com.faust.annireminder.model.LunarText
import com.faust.annireminder.model.Person
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val LABEL_PRESETS = listOf("生日", "纪念日", "恋爱纪念日")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(vm: AppViewModel, personId: String?) {
    val editing = personId?.let { pid -> vm.people.find { it.id == pid } }
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var dates by remember {
        mutableStateOf(
            editing?.dates?.toList() ?: listOf(
                DateEntry(
                    id = Store.newId(),
                    label = "生日",
                    type = CalType.LUNAR,
                    lunarMonth = 1,
                    lunarDay = 1
                )
            )
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val n = name.trim()
        if (n.isEmpty()) {
            error = "请先填写姓名"
            return
        }
        val person = Person(id = editing?.id ?: Store.newId(), name = n, dates = dates)
        vm.savePerson(person) { ok, msg ->
            vm.message = msg
            if (ok) vm.screen = Screen.Home
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(C.Black)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 标题行
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
            Text(
                if (editing == null) "添加人物" else "编辑人物",
                color = C.Orange,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(Modifier.height(18.dp))

        // 姓名
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(C.GrayCard)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("姓名（如：妈妈、老伴…）", color = C.Black.copy(alpha = 0.45f), fontWeight = FontWeight.Bold) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = C.Black,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = C.Black,
                    unfocusedBorderColor = C.Black.copy(alpha = 0.4f),
                    cursorColor = C.Black,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        }
        Spacer(Modifier.height(16.dp))

        // 每条日子
        dates.forEachIndexed { idx, e ->
            EntryCard(
                entry = e,
                onUpdate = { ne -> dates = dates.toMutableList().also { it[idx] = ne } },
                onDelete = { dates = dates.filterIndexed { i, _ -> i != idx } }
            )
            Spacer(Modifier.height(14.dp))
        }

        // 添加日子
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(C.CardBlack)
                .clickable {
                    dates = dates + DateEntry(
                        id = Store.newId(),
                        label = "纪念日",
                        type = CalType.SOLAR,
                        solarDate = LocalDate.now()
                    )
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("＋ 添加日子", color = C.Orange, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
        Spacer(Modifier.height(10.dp))

        if (error != null) {
            Text(error!!, color = C.Orange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
        }

        // 保存
        Button(
            onClick = { submit() },
            enabled = !vm.busy,
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(containerColor = C.Orange, contentColor = C.Black),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            if (vm.busy) {
                CircularProgressIndicator(color = C.Black, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
            } else {
                Text("保存并写入日历", fontWeight = FontWeight.Black, fontSize = 18.sp)
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryCard(
    entry: DateEntry,
    onUpdate: (DateEntry) -> Unit,
    onDelete: () -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val typeIdx = if (entry.type == CalType.LUNAR) 0 else 1

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(C.Orange)
            .padding(18.dp)
    ) {
        // 快捷标签
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LABEL_PRESETS.forEach { p ->
                LabelChip(p, entry.label == p) { onUpdate(entry.copy(label = p)) }
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = entry.label,
            onValueChange = { onUpdate(entry.copy(label = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("称呼（生日 / 纪念日 / 自定义）", color = C.Black.copy(alpha = 0.6f), fontSize = 13.sp) },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = C.Black, fontSize = 17.sp, fontWeight = FontWeight.Black
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = C.Black,
                unfocusedBorderColor = C.Black.copy(alpha = 0.5f),
                cursorColor = C.Black,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )
        Spacer(Modifier.height(14.dp))

        // 农历 / 公历
        SegToggle(listOf("农历", "公历"), typeIdx) { i ->
            onUpdate(entry.copy(type = if (i == 0) CalType.LUNAR else CalType.SOLAR))
        }
        Spacer(Modifier.height(14.dp))

        if (entry.type == CalType.LUNAR) {
            LunarPickers(entry, onUpdate)
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(C.Black)
                    .clickable { showPicker = true }
                    .padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "日期：${entry.solarDate.year}年${entry.solarDate.monthValue}月${entry.solarDate.dayOfMonth}日（点击选择）",
                    color = C.Paper, fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        StepperRow("连续写入", entry.repeatYears, "年", 1..30) {
            onUpdate(entry.copy(repeatYears = it))
        }
        Spacer(Modifier.height(10.dp))
        StepperRow(
            "提前提醒",
            entry.advanceDays,
            "天",
            0..30,
            tip = "提前 N 天额外写入一条提醒日程，设为 0 则不写入"
        ) {
            onUpdate(entry.copy(advanceDays = it))
        }
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "约 ${entry.repeatYears * (if (entry.advanceDays > 0) 2 else 1)} 条日程",
                style = Mono,
                color = C.Black.copy(alpha = 0.65f)
            )
            Box(Modifier.weight(1f))
            Text(
                "删除这条",
                color = C.Black,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(C.Black.copy(alpha = 0.14f))
                    .clickable(onClick = onDelete)
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            )
        }
    }

    if (showPicker) {
        val initMillis = entry.solarDate
            .atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = initMillis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneId.of("UTC")).toLocalDate()
                        onUpdate(entry.copy(solarDate = d))
                    }
                    showPicker = false
                }) { Text("确定", color = C.Orange, fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("取消", color = C.Gray) }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun LunarPickers(entry: DateEntry, onUpdate: (DateEntry) -> Unit) {
    val months = (1..12).map { LunarText.month(it) }
    val days = (1..30).map { LunarText.day(it) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("月份", style = Mono, color = C.Black.copy(alpha = 0.7f))
            Spacer(Modifier.width(10.dp))
            // 闰月开关
            val leap = entry.lunarLeap
            Box(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (leap) C.Black else C.Black.copy(alpha = 0.10f))
                    .clickable { onUpdate(entry.copy(lunarLeap = !leap)) }
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    "闰月",
                    color = if (leap) C.Paper else C.Black.copy(alpha = 0.75f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WheelPicker(
                items = months,
                selectedIndex = (entry.lunarMonth - 1).coerceIn(0, 11),
                onSelected = { i -> onUpdate(entry.copy(lunarMonth = i + 1)) },
                modifier = Modifier.weight(1f)
            )
            WheelPicker(
                items = days,
                selectedIndex = (entry.lunarDay - 1).coerceIn(0, 29),
                onSelected = { i -> onUpdate(entry.copy(lunarDay = i + 1)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
