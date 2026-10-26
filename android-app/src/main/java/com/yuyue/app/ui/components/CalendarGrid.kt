package com.yuyue.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yuyue.app.ui.theme.done
import com.yuyue.app.ui.theme.pending
import com.yuyue.app.util.DayDot
import androidx.compose.ui.draw.alpha
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

private val Weekdays = listOf("日", "一", "二", "三", "四", "五", "六")

@Composable
fun IosStyleCalendar(
    month: YearMonth,
    dayDots: Map<Int, DayDot>,
    selectedDay: LocalDate?,
    onDayClick: (Int) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now(ZoneId.of("Asia/Shanghai"))
    val firstDow = month.atDay(1).dayOfWeek.value % 7
    val daysInMonth = month.lengthOfMonth()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .alpha(if (enabled) 1f else 0.45f),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Weekdays.forEach { label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                userScrollEnabled = false,
                modifier = Modifier.height(((firstDow + daysInMonth + 6) / 7 * 48).dp),
            ) {
                items(firstDow) { Box(Modifier.aspectRatio(1f)) }
                items(daysInMonth) { i ->
                    val day = i + 1
                    val date = month.atDay(day)
                    val isToday = date == today
                    val isSelected = selectedDay == date
                    val dot = dayDots[day]
                    CalendarDayCell(
                        day = day,
                        isToday = isToday,
                        isSelected = isSelected,
                        dot = dot,
                        enabled = enabled,
                        onClick = { onDayClick(day) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    isToday: Boolean,
    isSelected: Boolean,
    dot: DayDot?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val markColor = when (dot) {
        DayDot.Red -> MaterialTheme.colorScheme.pending
        DayDot.Green -> MaterialTheme.colorScheme.done
        null -> Color.Transparent
    }
    val bg = when {
        isSelected -> primary
        dot != null -> markColor.copy(alpha = 0.18f)
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .semantics {
                contentDescription = buildString {
                    append("日期 $day")
                    if (isToday) append("，今天")
                    if (isSelected) append("，已选中")
                    if (dot == DayDot.Red) append("，有未完成")
                    if (dot == DayDot.Green) append("，有记忆")
                }
            }
            .clip(CircleShape)
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(1.5.dp, primary, CircleShape)
                } else Modifier,
            )
            .background(bg, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$day", style = MaterialTheme.typography.bodyMedium, color = textColor)
            if (dot != null && !isSelected) {
                Box(
                    Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(markColor),
                )
            }
        }
    }
}
