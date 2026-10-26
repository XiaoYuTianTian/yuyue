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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.data.local.NotebookEntity
import com.yuyue.app.ui.AppViewModel
import com.yuyue.app.ui.components.ObserveYuyueToasts
import com.yuyue.app.ui.components.TitleSavedToast
import com.yuyue.app.ui.components.YuyueTopToastHost
import com.yuyue.app.ui.paper.EventCapsule
import com.yuyue.app.ui.paper.PaperEmptyState
import com.yuyue.app.ui.shell.HomeFeedSplit
import com.yuyue.app.util.FuzzyTimeLabel
import java.time.LocalDate
import java.time.ZoneId

/**
 * Rebuild Home: voice orb is owned by ShellScaffold CaptureOverlaySlot;
 * today + few recent; notebooks as secondary filter.
 * No “接下来 / 未完成” default narrative.
 */
@Composable
fun HomePaperScreen(
    appVm: AppViewModel,
    onOpenVault: () -> Unit,
    onOpenTime: () -> Unit,
    onOpenEntry: (String) -> Unit,
) {
    val notebooks by appVm.notebooks.collectAsState()
    val selectedId by appVm.selectedNotebookId.collectAsState()
    val feed by appVm.welcomeFeed.collectAsState()
    val savedTitle by appVm.savedTitle.collectAsState()
    val zone = remember { ZoneId.of("Asia/Shanghai") }
    val today = remember { LocalDate.now(zone) }
    val sections = remember(feed, today) { HomeFeedSplit.split(feed, today, zone) }
    val snackbar = remember { SnackbarHostState() }

    ObserveYuyueToasts(appVm, snackbar)

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = {},
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            LazyColumn(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("语曰", style = MaterialTheme.typography.headlineMedium)
                            Text(
                                HomeFeedSplit.subtitle(sections),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row {
                            TextButton(onClick = onOpenTime) { Text("时间") }
                            TextButton(onClick = onOpenVault) { Text("保险库") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    SecondaryNotebookFilter(
                        notebooks = notebooks,
                        selectedId = selectedId,
                        onSelect = { appVm.selectNotebook(it) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (sections.today.isEmpty() && sections.recent.isEmpty()) {
                    item {
                        PaperEmptyState(
                            title = "还没有落轴的事",
                            body = "按住下方语音球说一句，会先预览再写入今天。",
                        )
                    }
                }
                if (sections.today.isNotEmpty()) {
                    item {
                        Text(
                            "今天",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(sections.today, key = { it.id }) { entry ->
                        EntryCapsuleRow(entry, zone, onOpenEntry)
                    }
                }
                if (sections.recent.isNotEmpty()) {
                    item {
                        Text(
                            "近期",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                    items(sections.recent, key = { it.id }) { entry ->
                        EntryCapsuleRow(entry, zone, onOpenEntry)
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
        YuyueTopToastHost(snackbar)
        savedTitle?.let { title ->
            TitleSavedToast(title = title, onDismiss = { appVm.clearSavedTitle() })
        }
    }
}

@Composable
private fun EntryCapsuleRow(
    entry: EntryEntity,
    zone: ZoneId,
    onOpenEntry: (String) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clickable { onOpenEntry(entry.id) },
    ) {
        EventCapsule(
            timeLabel = FuzzyTimeLabel.forEntry(entry.startAtMillis, entry.rawTranscript, zone),
            title = entry.title.ifBlank { "（无标题）" },
            support = entry.place?.takeIf { it.isNotBlank() }
                ?: entry.people?.takeIf { it.isNotBlank() },
        )
    }
}

@Composable
private fun SecondaryNotebookFilter(
    notebooks: List<NotebookEntity>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    if (notebooks.isEmpty()) return
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        notebooks.forEach { nb ->
            FilterChip(
                selected = nb.id == selectedId,
                onClick = { onSelect(nb.id) },
                label = { Text(nb.name, style = MaterialTheme.typography.labelMedium) },
            )
        }
    }
}
