package com.yuyue.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yuyue.app.data.capture.CaptureQueueItem
import com.yuyue.app.data.capture.CaptureStatus
import com.yuyue.app.data.capture.userLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CaptureQueueBar(
    items: List<CaptureQueueItem>,
    modifier: Modifier = Modifier,
    onDiscard: (String) -> Unit = {},
    onRetry: (String) -> Unit = {},
    onContinue: (String) -> Unit = {},
    onPause: (String) -> Unit = {},
    onResume: (String, String?) -> Unit = { _, _ -> },
) {
    if (items.isEmpty()) return
    var showDetail by remember { mutableStateOf(false) }
    val summary = remember(items) { queueSummary(items) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { showDetail = true },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    items.first().rawTranscript.trim().ifBlank {
                        if (items.first().hasMedia) "（照片）" else "（无原文）"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "详情",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }

    if (showDetail) {
        YuyueAlertDialog(
            onDismissRequest = { showDetail = false },
            title = "待处理",
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(items, key = { it.requestId }) { item ->
                        QueueItemDetail(
                            item = item,
                            itemCount = items.size,
                            onDiscard = onDiscard,
                            onRetry = onRetry,
                            onContinue = onContinue,
                            onPause = onPause,
                            onResume = onResume,
                            onCloseDetail = { showDetail = false },
                        )
                    }
                }
            },
            buttons = {
                TextButton(onClick = { showDetail = false }) { Text("关闭") }
            },
        )
    }
}

@Composable
private fun QueueItemDetail(
    item: CaptureQueueItem,
    itemCount: Int,
    onDiscard: (String) -> Unit,
    onRetry: (String) -> Unit,
    onContinue: (String) -> Unit,
    onPause: (String) -> Unit,
    onResume: (String, String?) -> Unit,
    onCloseDetail: () -> Unit,
) {
    var rawDraft by remember(item.requestId, item.rawTranscript) {
        mutableStateOf(item.rawTranscript)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            item.status.userLabel(item.lastError),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (item.status == CaptureStatus.Paused) {
            OutlinedTextField(
                value = rawDraft,
                onValueChange = { rawDraft = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 6,
            )
        } else {
            Text(
                item.rawTranscript.trim().ifBlank { if (item.hasMedia) "（照片）" else "（无原文）" },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            formatCapturedAt(item.capturedAtMillis),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            when (item.status) {
                CaptureStatus.Processing,
                CaptureStatus.Pending,
                CaptureStatus.Retryable,
                -> {
                    TextButton(onClick = {
                        onPause(item.requestId)
                        onCloseDetail()
                    }) { Text("暂停") }
                    if (item.status == CaptureStatus.Retryable) {
                        TextButton(onClick = {
                            onRetry(item.requestId)
                            onCloseDetail()
                        }) { Text("重试") }
                    }
                }
                CaptureStatus.Paused -> TextButton(onClick = {
                    onResume(item.requestId, rawDraft)
                    onCloseDetail()
                }) { Text("保存并继续") }
                CaptureStatus.ForegroundRequired -> {
                    TextButton(onClick = {
                        onContinue(item.requestId)
                        onCloseDetail()
                    }) { Text("继续") }
                    TextButton(onClick = {
                        onPause(item.requestId)
                        onCloseDetail()
                    }) { Text("暂停") }
                }
                CaptureStatus.Completed -> Unit
            }
            TextButton(onClick = {
                onDiscard(item.requestId)
                if (itemCount <= 1) onCloseDetail()
            }) { Text("丢弃") }
        }
    }
}

private fun queueSummary(items: List<CaptureQueueItem>): String {
    val processing = items.count {
        it.status == CaptureStatus.Processing || it.status == CaptureStatus.Pending
    }
    val retry = items.count { it.status == CaptureStatus.Retryable }
    val needYou = items.count { it.status == CaptureStatus.ForegroundRequired }
    return when {
        items.size == 1 -> items.first().status.userLabel(items.first().lastError)
        processing > 0 && retry == 0 && needYou == 0 -> "${items.size} 条正在处理"
        retry > 0 && processing == 0 -> "${items.size} 条等待重试"
        needYou > 0 && processing == 0 && retry == 0 -> "${items.size} 条需要你确认"
        else -> "${items.size} 条待处理"
    }
}

private fun formatCapturedAt(millis: Long): String =
    SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(millis))
