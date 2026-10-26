package com.yuyue.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yuyue.app.ui.PendingDraft
import com.yuyue.app.util.TimeUtils
import java.time.ZoneId

@Composable
fun VoiceDraftCard(
    text: String,
    listening: Boolean,
    busy: Boolean,
    analyzing: Boolean,
    error: String?,
    clarifyQuestion: String? = null,
    pendingDraft: PendingDraft? = null,
    analyzingHint: String? = null,
    analyzingLabel: String = "稍等片刻",
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
    onSaveNow: (() -> Unit)? = null,
) {
    val canAnalyze = text.isNotBlank() && !listening && !analyzing
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            if (analyzing) {
                AnalyzingBlock(hint = analyzingHint, title = analyzingLabel)
                return@Column
            }

            val heading = when {
                !clarifyQuestion.isNullOrBlank() -> clarifyQuestion
                busy && !listening -> "正在准备识别…"
                listening -> "正在聆听…"
                else -> null
            }
            if (heading != null) {
                Text(
                    heading,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(10.dp))
            }
            if (listening || busy) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                )
                Spacer(Modifier.height(8.dp))
            }
            if (!error.isNullOrBlank()) {
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }
            SlotChips(pendingDraft)
            TranscriptPane(text = text)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) { Text("取消") }
                if (onSaveNow != null && pendingDraft != null) {
                    OutlinedButton(
                        onClick = onSaveNow,
                        enabled = pendingDraft.entries.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                    ) { Text("先记下") }
                }
                Button(
                    onClick = onSubmit,
                    enabled = canAnalyze,
                    modifier = Modifier.weight(1f),
                ) { Text("发送") }
            }
        }
    }
}

@Composable
fun AnalyzingBlock(
    hint: String?,
    title: String = "稍等片刻",
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(36.dp),
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        val body = hint?.trim().orEmpty()
        if (body.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f),
            ) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        } else {
            Spacer(Modifier.height(4.dp))
            Text(
                "原话已保存",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SlotChips(draft: PendingDraft?) {
    if (draft == null) return
    val entry = draft.entries.firstOrNull()
    val zone = ZoneId.of("Asia/Shanghai")
    val time = if (draft.timeExplicit && entry != null) {
        runCatching {
            TimeUtils.formatTime(TimeUtils.parseIso(entry.startAt).toEpochMilli(), zone)
        }.getOrNull()
    } else {
        null
    }
    val place = entry?.place?.takeIf { it.isNotBlank() }
    val people = entry?.people?.filter { it.isNotBlank() }?.joinToString("、")?.takeIf { it.isNotBlank() }
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SlotChip("时间", time)
        SlotChip("地点", place)
        SlotChip("人物", people)
    }
}

@Composable
private fun SlotChip(label: String, value: String?) {
    val filled = !value.isNullOrBlank()
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (filled) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
        },
    ) {
        Text(
            text = if (filled) "$label $value" else label,
            style = MaterialTheme.typography.labelMedium,
            color = if (filled) {
                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun TranscriptPane(text: String) {
    val shape = RoundedCornerShape(8.dp)
    val empty = text.isBlank()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 180.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), shape)
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = if (empty) "先说一句吧" else text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (empty) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
