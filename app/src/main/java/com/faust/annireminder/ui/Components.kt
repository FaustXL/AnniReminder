package com.faust.annireminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * 通用滚轮选择器：点击或滑动选择，选中项居中加粗。
 * 上下 contentPadding 让首尾项也能滚到中心。
 */
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleCount: Int = 5,
    highlight: Color = C.Black,
    textStyle: TextStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold)
) {
    val itemH = 40.dp
    val offset = visibleCount / 2
    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    )
    val scope = rememberCoroutineScope()
    val fling = rememberSnapFlingBehavior(lazyListState = state)

    val centered by remember {
        derivedStateOf { centeredIndex(state) }
    }
    val currentOnSelected by rememberUpdatedState(onSelected)
    LaunchedEffect(state) {
        snapshotFlow { centeredIndex(state) }
            .distinctUntilChanged()
            .collect { idx -> idx?.let(currentOnSelected) }
    }
    LaunchedEffect(selectedIndex, items.size) {
        val target = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        if (centered != target && !state.isScrollInProgress) {
            state.scrollToItem(target)
        }
    }

    Box(
        modifier
            .height(itemH * visibleCount)
            .clip(RoundedCornerShape(16.dp))
            .background(C.Black.copy(alpha = 0.08f))
    ) {
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = itemH * offset),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items.size) { i ->
                Box(
                    Modifier
                        .height(itemH)
                        .fillMaxWidth()
                        .clickable {
                            onSelected(i)
                            scope.launch { state.scrollToItem(i) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        items[i],
                        style = textStyle.copy(
                            color = if (centered == i) textStyle.color else textStyle.color.copy(alpha = 0.38f)
                        )
                    )
                }
            }
        }
        // 中心高亮框
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemH)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Transparent)
                .border(2.dp, highlight, RoundedCornerShape(14.dp))
        )
    }
}

private fun centeredIndex(state: LazyListState): Int? {
    val info: LazyListLayoutInfo = state.layoutInfo
    if (info.visibleItemsInfo.isEmpty()) return null
    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return info.visibleItemsInfo
        .minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - center) }
        ?.index
}

/** 步进器行：标签 [-] 值 单位 [+] */
@Composable
fun StepperRow(
    label: String,
    value: Int,
    unit: String,
    range: IntRange,
    onChange: (Int) -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = C.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Box(Modifier.weight(1f))
        StepBtn(if (value <= range.first) C.Black.copy(alpha = 0.3f) else C.Black, "−") {
            if (value > range.first) onChange(value - 1)
        }
        Box(
            Modifier
                .padding(horizontal = 14.dp)
                .width(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(C.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$value",
                color = C.Paper,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        StepBtn(if (value >= range.last) C.Black.copy(alpha = 0.3f) else C.Black, "+") {
            if (value < range.last) onChange(value + 1)
        }
        Text(" $unit", color = C.Black.copy(alpha = 0.7f), fontSize = 13.sp)
    }
}

@Composable
fun StepBtn(tint: Color, symbol: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(tint)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = C.Paper, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}

/** 分段开关（农历 / 公历） */
@Composable
fun SegToggle(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(C.Black.copy(alpha = 0.10f))
    ) {
        options.forEachIndexed { i, label ->
            val isSel = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (isSel) C.Black else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (isSel) C.Paper else C.Black,
                    fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/** 快捷标签 pill */
@Composable
fun LabelChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) C.Black else C.Black.copy(alpha = 0.08f))
            .border(
                1.dp,
                if (selected) C.Black else C.Black.copy(alpha = 0.35f),
                RoundedCornerShape(999.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            color = if (selected) C.Paper else C.Black.copy(alpha = 0.8f),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold
        )
    }
}
