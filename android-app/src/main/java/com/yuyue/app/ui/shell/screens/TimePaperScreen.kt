package com.yuyue.app.ui.shell.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.ui.AppViewModel
import com.yuyue.app.ui.components.IosStyleCalendar
import com.yuyue.app.ui.components.ObserveYuyueToasts
import com.yuyue.app.ui.components.YuyueTopToastHost
import com.yuyue.app.ui.paper.EventCapsule
import com.yuyue.app.ui.paper.PaperEmptyState
import com.yuyue.app.ui.theme.Moss
import com.yuyue.app.util.FuzzyTimeLabel
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Time tab: month memory map → day capsules (fuzzy labels) → entry detail.
 * Capture dock is owned by ShellScaffold.
 */
@Composable
fun TimePaperScreen(
    appVm: AppViewModel,
    onOpenEntry: (String) -> Unit,
    onOpenPeople: () -> Unit = {},
    onOpenPlaces: () -> Unit = {},
    onOpenTopics: () -> Unit = {},
    onOpenWeekReview: () -> Unit = {},
    onOpenMonthReview: () -> Unit = {},
    onOpenPhotoWall: () -> Unit = {},
    onOpenLocalExtras: () -> Unit = {},
) {
    val month by appVm.month.collectAsState()
    val dayDots by appVm.dayDots.collectAsState()
    val selectedDay by appVm.selectedDay.collectAsState()
    val dayEntries by appVm.dayEntries.collectAsState()
    val jobRunning by appVm.jobRunning.collectAsState()
    val zone = remember { ZoneId.of("Asia/Shanghai") }
    val snackbar = remember { SnackbarHostState() }

    ObserveYuyueToasts(appVm, snackbar)

    LaunchedEffect(Unit) { appVm.refreshMonth() }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = {},
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 8.dp),
            ) {
                MonthHeader(
                    month = month,
                    onPrev = { appVm.changeMonth(-1) },
                    onNext = { appVm.changeMonth(1) },
                )
                Text(
                    "点有记忆的日子",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(8.dp))
                Column(Modifier.padding(horizontal = 20.dp)) {
                    DifferentiatorLinks(
                        onPeople = onOpenPeople,
                        onPlaces = onOpenPlaces,
                        onTopics = onOpenTopics,
                        onWeek = onOpenWeekReview,
                        onMonth = onOpenMonthReview,
                        onPhotos = onOpenPhotoWall,
                        onExtras = onOpenLocalExtras,
                    )
                }
                Spacer(Modifier.height(8.dp))
                IosStyleCalendar(
                    month = month,
                    dayDots = dayDots,
                    selectedDay = selectedDay,
                    onDayClick = { dayOfMonth ->
                        val day = month.atDay(dayOfMonth)
                        if (dayDots.containsKey(dayOfMonth)) {
                            appVm.openDay(day)
                        }
                    },
                    enabled = !jobRunning,
                )
                val day = selectedDay
                if (day != null) {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = Moss.copy(alpha = 0.35f),
                        thickness = 2.dp,
                    )
                    Text(
                        "${day.monthValue} 月 ${day.dayOfMonth} 日",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                    if (dayEntries.isEmpty()) {
                        PaperEmptyState(
                            title = "这一天还空着",
                            body = "有记忆的日子才会出现在月历上。",
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    } else {
                        dayEntries.forEach { entry ->
                            DayCapsule(entry, zone, onOpenEntry)
                        }
                    }
                }
            }
        }
        YuyueTopToastHost(snackbar)
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上月")
        }
        Text(
            "${month.month.getDisplayName(TextStyle.FULL, Locale.CHINA).trimEnd('月')}月",
            style = MaterialTheme.typography.headlineMedium,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下月")
        }
    }
}

@Composable
private fun DayCapsule(
    entry: EntryEntity,
    zone: ZoneId,
    onOpenEntry: (String) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable { onOpenEntry(entry.id) },
    ) {
        EventCapsule(
            timeLabel = FuzzyTimeLabel.forEntry(entry.startAtMillis, entry.rawTranscript, zone),
            title = entry.title.ifBlank { "（无标题）" },
            support = listOfNotNull(
                entry.people?.takeIf { it.isNotBlank() },
                entry.place?.takeIf { it.isNotBlank() },
            ).joinToString(" · ").ifBlank { null },
        )
    }
}
