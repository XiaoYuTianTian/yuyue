package com.yuyue.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yuyue.app.data.capture.isEntryPolishing
import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.speech.SpeechTranscriber
import com.yuyue.app.ui.components.CaptureQueueBar
import com.yuyue.app.ui.components.ObserveYuyueToasts
import com.yuyue.app.ui.components.RecallResultCard
import com.yuyue.app.ui.components.TitleSavedToast
import com.yuyue.app.ui.components.VoiceCaptureDock
import com.yuyue.app.ui.components.YuyueTopToastHost
import com.yuyue.app.ui.theme.YuyueTheme
import com.yuyue.app.ui.theme.done
import com.yuyue.app.ui.theme.pending
import com.yuyue.app.ui.theme.yuyueTopBarColors
import com.yuyue.app.util.TimeUtils
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayTimelineScreen(
    vm: AppViewModel,
    transcriber: SpeechTranscriber,
    day: LocalDate,
    onBack: () -> Unit,
    onEntryClick: (EntryEntity) -> Unit,
) {
    val entries by vm.dayEntries.collectAsState()
    val jobRunning by vm.jobRunning.collectAsState()
    val savedTitle by vm.savedTitle.collectAsState()
    val clarifyQuestion by vm.clarifyQuestion.collectAsState()
    val recallHits by vm.recallHits.collectAsState()
    val recallQuery by vm.recallQuery.collectAsState()
    val pendingDraft by vm.pendingDraft.collectAsState()
    val pendingMutate by vm.pendingMutatePick.collectAsState()
    val voiceNonce by vm.voiceNonce.collectAsState()
    val composerFocusNonce by vm.composerFocusNonce.collectAsState()
    val captureQueue by vm.captureQueue.collectAsState()
    val analyzingLabel by vm.analyzingLabel.collectAsState()
    val mediaByEntryId by vm.mediaByEntryId.collectAsState()
    val zone = ZoneId.of("Asia/Shanghai")
    val snackbar = remember { SnackbarHostState() }
    val weekday = day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINA)
    val sorted = remember(entries) { entries.sortedBy { it.startAtMillis } }

    ObserveYuyueToasts(vm, snackbar)

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {},
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("${day.monthValue}月${day.dayOfMonth}日 · $weekday")
                            Text(
                                if (sorted.isEmpty()) "还空着" else "${sorted.size} 则",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    colors = yuyueTopBarColors(),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                )
            },
            bottomBar = {
                VoiceCaptureDock(
                    transcriber = transcriber,
                    jobRunning = jobRunning,
                    clarifyQuestion = clarifyQuestion,
                    dismissKey = savedTitle ?: recallHits?.let { "recall" }
                        ?: voiceNonce.takeIf { it > 0 }?.let { "v$it" },
                    pendingDraft = pendingDraft,
                    analyzingLabel = analyzingLabel,
                    composerFocusNonce = composerFocusNonce,
                    extraAbove = {
                        CaptureQueueBar(
                            items = captureQueue,
                            onDiscard = { vm.discardCapture(it) },
                            onRetry = { vm.retryCapture(it) },
                            onContinue = { vm.continueCapture(it) },
                            onPause = { vm.pauseCapture(it) },
                            onResume = { id, raw -> vm.resumeCapture(id, raw) },
                        )
                        recallHits?.let { hits ->
                            RecallResultCard(
                                hits = hits,
                                query = recallQuery,
                                headline = if (pendingMutate) "要改哪一则？" else null,
                                onOpen = {
                                    if (vm.applyPendingMutate(it.id)) return@RecallResultCard
                                    vm.dismissRecallHits()
                                    onEntryClick(it)
                                },
                                onDismiss = { vm.dismissRecallHits() },
                            )
                        }
                    },
                    onSubmit = { text, uris, source -> vm.submitStructuredTurn(text, uris, source) },
                    onSaveNow = { vm.commitPendingDraft() },
                    onCancelPending = { vm.dismissPendingDraft() },
                )
            },
        ) { padding ->
            if (sorted.isEmpty()) {
                Box(
                    Modifier.padding(padding).fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("为这一天留下第一笔", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    itemsIndexed(sorted, key = { _, e -> e.id }) { index, e ->
                        TimelineRow(
                            entry = e,
                            zone = zone,
                            index = index,
                            onClick = { onEntryClick(e) },
                            onToggleDone = { vm.setEntryDone(e.id, !e.done) },
                            thumbPath = mediaByEntryId[e.id]?.firstOrNull()?.thumbPath,
                            polishing = captureQueue.isEntryPolishing(e.id),
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }

        YuyueTopToastHost(snackbar)

        savedTitle?.let { title ->
            Box(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = 56.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                TitleSavedToast(title = title, onDismiss = { vm.clearSavedTitle() })
            }
        }
    }
}

@Composable
private fun TimelineRow(
    entry: EntryEntity,
    zone: ZoneId,
    index: Int,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    thumbPath: String? = null,
    polishing: Boolean = false,
) {
    var shown by remember(entry.id) { mutableStateOf(false) }
    LaunchedEffect(entry.id) {
        delay((index.coerceAtMost(30) * 45).toLong())
        shown = true
    }
    val ink = if (entry.done) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(
        alpha = if (entry.done) 0.72f else 1f,
    )
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(220)) +
            scaleIn(tween(220), initialScale = 0.92f) +
            slideInVertically(tween(220)) { it / 6 },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                TimeUtils.formatTime(entry.startAtMillis, zone),
                style = MaterialTheme.typography.labelLarge,
                color = muted,
                modifier = Modifier.width(48.dp),
            )
            if (!thumbPath.isNullOrBlank()) {
                com.yuyue.app.ui.components.LocalFileImage(
                    path = thumbPath,
                    contentDescription = "照片",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (polishing) {
                    Text(
                        "整理中",
                        style = MaterialTheme.typography.labelLarge,
                        color = muted,
                    )
                }
            }
            Box(
                Modifier
                    .requiredSize(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleDone),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (entry.done) MaterialTheme.colorScheme.done else MaterialTheme.colorScheme.pending),
                )
            }
        }
    }
}

@Preview
@Composable
private fun TimelineRowPreview() {
    YuyueTheme {
        TimelineRow(
            entry = EntryEntity(
                id = "1",
                notebookId = "n",
                entryKind = "note",
                status = "active",
                subjectKey = null,
                polarity = null,
                title = "下班喝了咖啡",
                body = "去了街角那家",
                rawTranscript = "下了班去喝了咖啡",
                startAtMillis = System.currentTimeMillis(),
                endAtMillis = null,
                statedAtMillis = System.currentTimeMillis(),
                updatedAtMillis = System.currentTimeMillis(),
                done = true,
            ),
            zone = ZoneId.of("Asia/Shanghai"),
            index = 0,
            onClick = {},
            onToggleDone = {},
        )
    }
}
