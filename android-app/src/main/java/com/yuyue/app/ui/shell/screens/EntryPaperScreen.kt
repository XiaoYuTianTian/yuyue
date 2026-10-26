package com.yuyue.app.ui.shell.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.yuyue.app.data.WelcomeFeed
import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.data.local.EntryMediaEntity
import com.yuyue.app.data.local.EntryRevisionEntity
import com.yuyue.app.ui.components.LocalFileImage
import com.yuyue.app.ui.theme.PaperRaised
import com.yuyue.app.ui.theme.yuyueTopBarColors
import com.yuyue.app.util.FuzzyTimeLabel
import com.yuyue.app.util.TimeUtils
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Entry detail: raw + polished + revisions + media + source.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryPaperScreen(
    entry: EntryEntity,
    media: List<EntryMediaEntity>,
    revisions: List<EntryRevisionEntity>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val zone = ZoneId.of("Asia/Shanghai")
    val today = LocalDate.now(zone)
    val day = TimeUtils.millisToLocalDate(entry.startAtMillis, zone)
    val dayLabel = WelcomeFeed.shortDayLabel(day, today)
    val fuzzy = FuzzyTimeLabel.forDetail(entry.startAtMillis, entry.rawTranscript, zone)
    val sourceLabel = sourceLabel(entry)

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
            Text(
                "$dayLabel · $fuzzy",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(entry.title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "来源 · $sourceLabel",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (media.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(media, key = { it.id }) { item ->
                        LocalFileImage(
                            path = item.localPath,
                            contentDescription = "日记照片",
                            modifier = Modifier
                                .width(240.dp)
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("整理", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                entry.body.ifBlank { "（尚未整理）" },
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!entry.rawTranscript.isNullOrBlank()) {
                Spacer(Modifier.height(20.dp))
                Text("原话", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = PaperRaised,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        entry.rawTranscript.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }
            if (revisions.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("修订", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                revisions.forEach { rev ->
                    val whenLabel = InstantFormat.format(rev.createdAtMillis, zone)
                    Text(
                        "$whenLabel · ${rev.reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun sourceLabel(entry: EntryEntity): String {
    val kind = when (entry.entryKind.lowercase()) {
        "voice", "speech", "utterance" -> "语音"
        "share", "shared" -> "分享"
        "photo", "image" -> "照片"
        "text", "typed" -> "文字"
        else -> entry.entryKind.ifBlank { "本机" }
    }
    val via = if (!entry.rawTranscript.isNullOrBlank()) "含原话" else null
    return listOfNotNull(kind, via, entry.utteranceId?.let { "批次 ${it.take(6)}" })
        .joinToString(" · ")
}

private object InstantFormat {
    private val fmt = DateTimeFormatter.ofPattern("M月d日 HH:mm")
    fun format(millis: Long, zone: ZoneId): String =
        java.time.Instant.ofEpochMilli(millis).atZone(zone).format(fmt)
}
