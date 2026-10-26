package com.yuyue.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.speech.SpeechTranscriber
import com.yuyue.app.ui.components.CaptureQueueBar
import com.yuyue.app.ui.components.IosStyleCalendar
import com.yuyue.app.ui.components.ObserveYuyueToasts
import com.yuyue.app.ui.components.RecallResultCard
import com.yuyue.app.ui.components.TitleSavedToast
import com.yuyue.app.ui.components.VoiceCaptureDock
import com.yuyue.app.ui.components.YuyueTopToastHost
import com.yuyue.app.ui.theme.yuyueTopBarColors
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    vm: AppViewModel,
    transcriber: SpeechTranscriber,
    onBack: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onOpenEntry: (EntryEntity) -> Unit,
) {
    val month by vm.month.collectAsState()
    val dayDots by vm.dayDots.collectAsState()
    val focusedDay by vm.focusedDay.collectAsState()
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
    val snackbar = remember { SnackbarHostState() }
    val calendarEnabled = !jobRunning

    ObserveYuyueToasts(vm, snackbar)

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = {},
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("月历") },
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
                                    onOpenEntry(it)
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
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                MonthHeader(
                    month = month,
                    enabled = calendarEnabled,
                    onPrev = { vm.changeMonth(-1) },
                    onNext = { vm.changeMonth(1) },
                )
                IosStyleCalendar(
                    month = month,
                    dayDots = dayDots,
                    selectedDay = focusedDay,
                    enabled = calendarEnabled,
                    onDayClick = { day ->
                        if (calendarEnabled) {
                            onOpenDay(month.atDay(day))
                        }
                    },
                )
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
fun MonthHeader(
    month: YearMonth,
    enabled: Boolean = true,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev, enabled = enabled) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上月")
        }
        Text("${month.year}年${month.monthValue}月", style = MaterialTheme.typography.headlineMedium)
        IconButton(onClick = onNext, enabled = enabled) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下月")
        }
    }
}
