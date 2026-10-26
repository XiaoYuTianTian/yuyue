package com.yuyue.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.yuyue.app.data.local.EntryMediaEntity
import com.yuyue.app.ui.components.LocalFileImage
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yuyue.app.data.WelcomeFeed
import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.ui.theme.YuyueTheme
import com.yuyue.app.ui.theme.done
import com.yuyue.app.ui.theme.pending
import com.yuyue.app.ui.theme.yuyueTopBarColors
import com.yuyue.app.util.TimeUtils
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    entry: EntryEntity,
    media: List<EntryMediaEntity> = emptyList(),
    onBack: () -> Unit,
    onToggleDone: (Boolean) -> Unit,
    onEdit: () -> Unit,
) {
    val zone = ZoneId.of("Asia/Shanghai")
    val today = remember { LocalDate.now(zone) }
    var showRaw by remember(entry.id) { mutableStateOf(false) }
    val accent = if (entry.done) MaterialTheme.colorScheme.done else MaterialTheme.colorScheme.pending
    val day = TimeUtils.millisToLocalDate(entry.startAtMillis, zone)
    val dayLabel = WelcomeFeed.shortDayLabel(day, today)
    val time = TimeUtils.formatTime(entry.startAtMillis, zone)
    val subtitle = listOfNotNull(
        time,
        entry.place?.takeIf { it.isNotBlank() },
        entry.people?.takeIf { it.isNotBlank() },
        diaryTypeLabel(entry.diaryType),
    ).joinToString(" · ")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(dayLabel) },
                colors = yuyueTopBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = onEdit) { Text("编辑") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(accent),
            )
            if (media.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(media.size) { idx ->
                        val item = media[idx]
                        LocalFileImage(
                            path = item.localPath,
                            contentDescription = "日记照片",
                            modifier = Modifier
                                .width(260.dp)
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                entry.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                entry.body.ifBlank { "（没有正文）" },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (!entry.rawTranscript.isNullOrBlank()) {
                Spacer(Modifier.height(20.dp))
                TextButton(onClick = { showRaw = !showRaw }) {
                    Text(if (showRaw) "收起原话" else "原话")
                }
                AnimatedVisibility(showRaw) {
                    Text(
                        entry.rawTranscript.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (entry.done) "已完成" else "未完成",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (entry.done) {
                        MaterialTheme.colorScheme.done
                    } else {
                        MaterialTheme.colorScheme.pending
                    },
                )
                Switch(
                    checked = entry.done,
                    onCheckedChange = onToggleDone,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.done,
                        uncheckedThumbColor = MaterialTheme.colorScheme.pending,
                        uncheckedTrackColor = MaterialTheme.colorScheme.pending.copy(alpha = 0.38f),
                        uncheckedBorderColor = MaterialTheme.colorScheme.pending,
                    ),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun diaryTypeLabel(type: String?): String? = when (type) {
    "life" -> "生活"
    "work" -> "工作"
    "food" -> "饮食"
    "meeting" -> "会面"
    "travel" -> "出行"
    "health" -> "健康"
    "other" -> "其他"
    null, "" -> null
    else -> type
}

@Preview
@Composable
private fun EntryDetailDonePreview() {
    YuyueTheme {
        EntryDetailScreen(
            entry = sampleEntry(done = true, title = "下班喝了咖啡"),
            onBack = {},
            onToggleDone = {},
            onEdit = {},
        )
    }
}

@Preview
@Composable
private fun EntryDetailPendingPreview() {
    YuyueTheme {
        EntryDetailScreen(
            entry = sampleEntry(done = false, title = "明天下午见客户"),
            onBack = {},
            onToggleDone = {},
            onEdit = {},
        )
    }
}

private fun sampleEntry(done: Boolean, title: String) = EntryEntity(
    id = "p",
    notebookId = "n",
    entryKind = "note",
    status = "active",
    subjectKey = null,
    polarity = null,
    title = title,
    body = "整理后的日记正文。",
    rawTranscript = "用户原话在这里",
    startAtMillis = System.currentTimeMillis(),
    endAtMillis = null,
    statedAtMillis = System.currentTimeMillis(),
    updatedAtMillis = System.currentTimeMillis(),
    done = done,
    place = "街角",
    people = null,
    diaryType = "food",
)
