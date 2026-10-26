package com.yuyue.app.ui.shell.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.yuyue.app.speech.SpeechTranscriber
import com.yuyue.app.ui.MicPermissionGate
import com.yuyue.app.ui.paper.PaperScaffold
import com.yuyue.app.ui.shell.OnboardingEvent
import com.yuyue.app.ui.shell.OnboardingFlow
import com.yuyue.app.ui.shell.OnboardingPhase
import com.yuyue.app.ui.shell.OnboardingState
import com.yuyue.app.ui.theme.Moss
import com.yuyue.app.ui.theme.MossDeep
import com.yuyue.app.ui.theme.PaperRaised
import com.yuyue.app.util.FuzzyTimeLabel

/**
 * Real hold-to-speak onboarding: permission explained on action → preview → first entry.
 */
@Composable
fun OnboardingScreen(
    transcriber: SpeechTranscriber,
    onCommitFirstEntry: (String) -> Boolean,
    onFinished: () -> Unit,
) {
    var state by remember { mutableStateOf(OnboardingState()) }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val listening by transcriber.listening.collectAsState()
    val busy by transcriber.busy.collectAsState()
    val partial by transcriber.partial.collectAsState()
    val speechError by transcriber.error.collectAsState()
    var showMicExplainer by remember { mutableStateOf(false) }
    var pendingHoldAfterGrant by remember { mutableStateOf(false) }
    var awaitingFinal by remember { mutableStateOf(false) }

    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            pendingHoldAfterGrant = true
        } else {
            state = OnboardingFlow.reduce(
                state,
                OnboardingEvent.SpeakFailed("需要麦克风才能按住说话。可在系统设置中开启。"),
            )
        }
    }

    LaunchedEffect(pendingHoldAfterGrant) {
        if (pendingHoldAfterGrant) {
            pendingHoldAfterGrant = false
            transcriber.start()
        }
    }

    LaunchedEffect(speechError) {
        val err = speechError ?: return@LaunchedEffect
        if (state.phase == OnboardingPhase.Speak && !awaitingFinal) {
            state = OnboardingFlow.reduce(state, OnboardingEvent.SpeakFailed(err))
        }
    }

    LaunchedEffect(awaitingFinal, listening, busy, partial) {
        if (!awaitingFinal) return@LaunchedEffect
        if (listening || busy) return@LaunchedEffect
        awaitingFinal = false
        val text = partial.trim()
        state = OnboardingFlow.reduce(state, OnboardingEvent.PreviewReady(text))
    }

    LaunchedEffect(state.phase) {
        if (state.phase == OnboardingPhase.Finished) onFinished()
    }

    fun dispatch(event: OnboardingEvent) {
        state = OnboardingFlow.reduce(state, event)
    }

    fun beginHold() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (MicPermissionGate.shouldPrompt(granted)) {
            if (!state.micExplainerShown) {
                showMicExplainer = true
            } else {
                micPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        } else {
            transcriber.start()
        }
    }

    fun endHold() {
        if (state.phase != OnboardingPhase.Speak) return
        awaitingFinal = true
        transcriber.stop()
    }

    if (showMicExplainer) {
        AlertDialog(
            onDismissRequest = { showMicExplainer = false },
            title = { Text("需要麦克风") },
            text = {
                Text("按住说话时，语曰只在本机听写你的话，不会把原声上传。接下来系统会询问录音权限。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMicExplainer = false
                        dispatch(OnboardingEvent.MicExplainerAcknowledged)
                        micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    },
                ) { Text("继续") }
            },
            dismissButton = {
                TextButton(onClick = { showMicExplainer = false }) { Text("稍后") }
            },
        )
    }

    when (state.phase) {
        OnboardingPhase.Intro -> {
            PaperScaffold(
                title = null,
                subtitle = null,
            ) {
                Spacer(Modifier.weight(0.35f))
                Text(
                    "语曰",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "用说话写的私人时间日记",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "按住说一句真实的话。我们会先预览，再落到今天。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(0.45f))
                Button(
                    onClick = { dispatch(OnboardingEvent.StartHoldFlow) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("按住试说一句") }
            }
        }
        OnboardingPhase.Speak, OnboardingPhase.Preview, OnboardingPhase.Committing -> {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Text("今天", style = MaterialTheme.typography.headlineMedium)
                Text(
                    if (state.phase == OnboardingPhase.Speak) "按住球体说话，松手预览" else "确认后写入本机",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(32.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    HoldOrb(
                        listening = listening,
                        enabled = state.phase == OnboardingPhase.Speak,
                        onHoldStart = { beginHold() },
                        onHoldEnd = { endHold() },
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    when {
                        listening -> "继续按住…"
                        state.phase == OnboardingPhase.Speak -> "轻按不生效 · 按住才算"
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                state.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                if (state.phase == OnboardingPhase.Preview || state.phase == OnboardingPhase.Committing) {
                    Spacer(Modifier.height(20.dp))
                    Surface(
                        color = PaperRaised,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "${FuzzyTimeLabel.forEntry(System.currentTimeMillis(), state.previewText)} · 今天",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                state.previewText,
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "原话会保留。确认后写入本机。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        dispatch(OnboardingEvent.ConfirmWrite)
                                        val ok = onCommitFirstEntry(state.previewText)
                                        dispatch(
                                            if (ok) OnboardingEvent.WriteSucceeded
                                            else OnboardingEvent.WriteFailed("未能写入，请稍后重试"),
                                        )
                                    },
                                    enabled = state.phase == OnboardingPhase.Preview,
                                    modifier = Modifier.weight(1f),
                                ) { Text("写入今天") }
                                OutlinedButton(
                                    onClick = { dispatch(OnboardingEvent.Redo) },
                                    enabled = state.phase == OnboardingPhase.Preview,
                                    modifier = Modifier.weight(1f),
                                ) { Text("重说") }
                            }
                        }
                    }
                }
                if (listening && partial.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(partial, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        OnboardingPhase.Finished -> {
            Box(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun HoldOrb(
    listening: Boolean,
    enabled: Boolean,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
) {
    val scale = if (listening) 1.06f else 1f
    Surface(
        modifier = Modifier
            .size(112.dp)
            .scale(scale)
            .semantics { contentDescription = if (listening) "松开结束" else "按住说话" }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        onHoldStart()
                        try {
                            tryAwaitRelease()
                        } finally {
                            onHoldEnd()
                        }
                    },
                )
            },
        shape = CircleShape,
        color = if (listening) MossDeep else Moss,
        shadowElevation = 6.dp,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                if (listening) "聆听" else "按住",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
