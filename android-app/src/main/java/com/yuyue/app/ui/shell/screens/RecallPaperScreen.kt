package com.yuyue.app.ui.shell.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yuyue.app.data.WelcomeFeed
import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.ui.AppViewModel
import com.yuyue.app.ui.components.ObserveYuyueToasts
import com.yuyue.app.ui.components.YuyueTopToastHost
import com.yuyue.app.ui.paper.PaperEmptyState
import com.yuyue.app.ui.shell.RecallViewModel
import com.yuyue.app.ui.theme.Moss
import com.yuyue.app.util.FuzzyTimeLabel
import com.yuyue.app.util.TimeUtils
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/**
 * Recall: text + voice share FTS path; dated nodes with entryId traceability.
 * Capture dock is owned by ShellScaffold (voice submits via CaptureKernel / AppViewModel).
 */
@Composable
fun RecallPaperScreen(
    appVm: AppViewModel,
    recallVm: RecallViewModel,
    onOpenEntry: (String) -> Unit,
    onOpenPeople: () -> Unit = {},
    onOpenPlaces: () -> Unit = {},
    onOpenTopics: () -> Unit = {},
    onOpenWeekReview: () -> Unit = {},
    onOpenMonthReview: () -> Unit = {},
    onOpenPhotoWall: () -> Unit = {},
    onOpenLocalExtras: () -> Unit = {},
) {
    val query by recallVm.query.collectAsState()
    val recallHits by appVm.recallHits.collectAsState()
    val searchHits by appVm.searchHits.collectAsState()
    val notebookId by appVm.selectedNotebookId.collectAsState()
    val scope = rememberCoroutineScope()
    var localHits by remember { mutableStateOf<List<EntryEntity>>(emptyList()) }
    val zone = remember { ZoneId.of("Asia/Shanghai") }
    val today = remember { LocalDate.now(zone) }
    val snackbar = remember { SnackbarHostState() }

    ObserveYuyueToasts(appVm, snackbar)

    val hits = when {
        localHits.isNotEmpty() -> localHits
        !recallHits.isNullOrEmpty() -> recallHits.orEmpty()
        !searchHits.isNullOrEmpty() -> searchHits.orEmpty()
        else -> emptyList()
    }

    fun runSearch(q: String) {
        scope.launch {
            localHits = recallVm.searchLike(q, notebookId)
            appVm.setSearchQuery(q)
            appVm.searchKeyword(q)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = {},
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text("回忆", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "说说要找的事，或点下面的问法",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = recallVm::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("想起来什么？") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { runSearch(query) }) { Text("搜索") }
                    TextButton(
                        onClick = {
                            localHits = emptyList()
                            recallVm.setQuery("")
                            appVm.dismissRecallHits()
                        },
                    ) { Text("清除") }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("今天", "昨天", "周末").forEach { chip ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                recallVm.setQuery(chip)
                                runSearch(chip)
                            },
                            label = { Text(chip) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                DifferentiatorLinks(
                    onPeople = onOpenPeople,
                    onPlaces = onOpenPlaces,
                    onTopics = onOpenTopics,
                    onWeek = onOpenWeekReview,
                    onMonth = onOpenMonthReview,
                    onPhotos = onOpenPhotoWall,
                    onExtras = onOpenLocalExtras,
                )
                Spacer(Modifier.height(12.dp))
                if (hits.isEmpty()) {
                    PaperEmptyState(
                        title = "还没有连线",
                        body = "输入关键词，或按住下方语音球问一句。命中会带 entryId，可点进条目。",
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(hits, key = { it.id }) { entry ->
                            RecallNode(
                                entry = entry,
                                today = today,
                                zone = zone,
                                onOpen = { onOpenEntry(entry.id) },
                            )
                        }
                    }
                }
            }
        }
        YuyueTopToastHost(snackbar)
    }
}

@Composable
private fun RecallNode(
    entry: EntryEntity,
    today: LocalDate,
    zone: ZoneId,
    onOpen: () -> Unit,
) {
    val day = TimeUtils.millisToLocalDate(entry.startAtMillis, zone)
    val dayLabel = WelcomeFeed.shortDayLabel(day, today)
    val time = FuzzyTimeLabel.forEntry(entry.startAtMillis, entry.rawTranscript, zone)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 8.dp),
    ) {
        Box(
            Modifier
                .width(2.dp)
                .height(52.dp)
                .background(Moss),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "$dayLabel · $time",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(entry.title.ifBlank { "（无标题）" }, style = MaterialTheme.typography.titleLarge)
            Text(
                "entryId · ${entry.id.take(8)}…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
