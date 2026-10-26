package com.yuyue.app.ui.components

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.yuyue.app.data.capture.CaptureIngress
import com.yuyue.app.data.media.EntryMediaStore
import com.yuyue.app.data.media.ImageCompressor
import com.yuyue.app.speech.SpeechTranscriber
import com.yuyue.app.speech.joinTranscript
import com.yuyue.app.ui.MicPermissionGate
import com.yuyue.app.ui.PendingDraft
import com.yuyue.app.ui.VoiceEmptyPolicy
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private enum class CaptureMode { Voice, Text, Collapsing }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceCaptureDock(
    transcriber: SpeechTranscriber,
    jobRunning: Boolean,
    clarifyQuestion: String?,
    dismissKey: String? = null,
    pendingDraft: PendingDraft? = null,
    analyzingLabel: String = "稍等片刻",
    composerFocusNonce: Int = 0,
    /** When false, skip nav-bar inset (e.g. shell dock sits above [NavigationBar]). */
    consumeNavigationBars: Boolean = true,
    extraAbove: @Composable () -> Unit = {},
    onSubmit: (String, List<Uri>, CaptureIngress) -> Boolean,
    onSaveNow: () -> Unit = {},
    onCancelPending: () -> Unit = {},
) {
    var mode by remember { mutableStateOf(CaptureMode.Voice) }
    var showVoice by remember { mutableStateOf(false) }
    var committed by remember { mutableStateOf("") }
    var typed by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf<String?>(null) }
    var analyzingHint by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val imeVisible = WindowInsets.isImeVisible
    val context = LocalContext.current
    var attached by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showPhotoSource by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val albumPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(ImageCompressor.MAX_COUNT),
    ) { uris ->
        if (uris.isNotEmpty()) {
            attached = (attached + uris).distinct().take(ImageCompressor.MAX_COUNT)
        }
    }
    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { ok ->
        val uri = cameraUri
        if (ok && uri != null) {
            attached = (attached + uri).distinct().take(ImageCompressor.MAX_COUNT)
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val file = EntryMediaStore(context).cameraCacheFile()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraUri = uri
            takePicture.launch(uri)
        }
    }
    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) transcriber.start()
        else hint = "未授予麦克风权限，请在系统设置中允许「语曰」录音"
    }

    fun openAlbum() {
        albumPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun openCamera() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (granted) {
            val file = EntryMediaStore(context).cameraCacheFile()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraUri = uri
            takePicture.launch(uri)
        } else {
            cameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    fun trySubmit(text: String): Boolean {
        if (text.isBlank() && attached.isEmpty()) return false
        analyzingHint = text.ifBlank { "（照片）" }
        val ingress = when {
            attached.isNotEmpty() && text.isBlank() -> CaptureIngress.Photo
            mode == CaptureMode.Text -> CaptureIngress.Keyboard
            else -> CaptureIngress.Voice
        }
        return if (onSubmit(text, attached, ingress)) {
            attached = emptyList()
            true
        } else {
            false
        }
    }

    val listening by transcriber.listening.collectAsState()
    val busy by transcriber.busy.collectAsState()
    val live by transcriber.partial.collectAsState()
    val speechError by transcriber.error.collectAsState()

    fun hideIme() {
        keyboard?.hide()
        focusManager.clearFocus(force = true)
    }

    fun clearVoiceDraft() {
        committed = ""
        transcriber.clearLive()
        hint = null
    }

    LaunchedEffect(dismissKey) {
        if (dismissKey != null) {
            analyzingHint = null
            clearVoiceDraft()
            typed = ""
            attached = emptyList()
            showVoice = false
            if (mode == CaptureMode.Collapsing) mode = CaptureMode.Voice
            hideIme()
        }
    }

    LaunchedEffect(jobRunning) {
        if (jobRunning) return@LaunchedEffect
        // 让 dismissKey（入队成功/失败后清空）先跑完，再决定要不要把原文放回
        delay(1)
        val keep = analyzingHint?.trim().orEmpty()
        if (keep.isEmpty()) return@LaunchedEffect
        if (mode == CaptureMode.Text) {
            typed = keep
        } else {
            committed = keep
            showVoice = true
        }
    }

    fun switchToText() {
        if (jobRunning) return
        if (transcriber.listening.value) transcriber.stop()
        clearVoiceDraft()
        showVoice = false
        mode = CaptureMode.Text
    }

    LaunchedEffect(composerFocusNonce) {
        if (composerFocusNonce > 0) switchToText()
    }

    fun switchToVoice() {
        if (jobRunning) return
        // Fold the text bar first; only show the orb after IME has settled.
        hideIme()
        mode = CaptureMode.Collapsing
    }

    LaunchedEffect(mode) {
        if (mode != CaptureMode.Collapsing) return@LaunchedEffect
        // Let the text bar finish fading out before the orb appears.
        delay(140)
        withTimeoutOrNull(500) {
            snapshotFlow { imeVisible }.first { !it }
        }
        // Brief settle so imePadding finishes collapsing before the tall orb mounts.
        delay(60)
        mode = CaptureMode.Voice
    }

    var wasListening by remember { mutableStateOf(false) }
    val draftText = joinTranscript(committed, live)

    LaunchedEffect(listening, live) {
        if (wasListening && !listening) {
            committed = joinTranscript(committed, live)
            transcriber.clearLive()
            // 松开后仍无文字：不展开文稿区
            if (committed.isBlank() &&
                clarifyQuestion.isNullOrBlank() &&
                !jobRunning
            ) {
                showVoice = false
                hint = VoiceEmptyPolicy.message(committed, framesHeard = wasListening)
            }
        }
        wasListening = listening
    }
    // 识别到真实内容后再展开文稿区
    LaunchedEffect(draftText) {
        if (draftText.isNotBlank()) showVoice = true
    }
    LaunchedEffect(clarifyQuestion) {
        if (!clarifyQuestion.isNullOrBlank()) {
            analyzingHint = null
            clearVoiceDraft()
            if (mode == CaptureMode.Voice) {
                showVoice = true
            }
            hideIme()
        }
    }

    val dockModifier = Modifier
        .fillMaxWidth()
        .then(if (consumeNavigationBars) Modifier.navigationBarsPadding() else Modifier)
        .imePadding()
    Column(dockModifier) {
        extraAbove()
        if (showPhotoSource) {
            AlertDialog(
                onDismissRequest = { showPhotoSource = false },
                title = { Text("添加照片") },
                text = { Text("拍一张，或从相册选，最多 ${ImageCompressor.MAX_COUNT} 张。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showPhotoSource = false
                            openCamera()
                        },
                    ) { Text("拍照") }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showPhotoSource = false
                            openAlbum()
                        },
                    ) { Text("相册") }
                },
            )
        }
        CaptureAttachStrip(
            uris = attached,
            onRemove = { uri -> attached = attached.filterNot { it == uri } },
            onSendPhotosOnly = {
                if (!jobRunning && trySubmit("")) {
                    clearVoiceDraft()
                    typed = ""
                    hideIme()
                }
            },
        )
        if (mode == CaptureMode.Voice && showVoice) {
            VoiceDraftCard(
                text = draftText,
                listening = listening,
                busy = busy,
                analyzing = jobRunning,
                error = speechError ?: hint,
                clarifyQuestion = clarifyQuestion,
                pendingDraft = pendingDraft,
                analyzingHint = analyzingHint,
                analyzingLabel = analyzingLabel,
                onDismiss = {
                    if (!jobRunning) {
                        if (transcriber.listening.value) transcriber.stop()
                        clearVoiceDraft()
                        showVoice = false
                        hideIme()
                        onCancelPending()
                    }
                },
                onSubmit = {
                    val t = joinTranscript(committed, live).trim()
                    if (t.isBlank() && attached.isEmpty()) {
                        hint = "还没有可发送的内容"
                    } else {
                        if (transcriber.listening.value) transcriber.stop()
                        hideIme()
                        if (trySubmit(t)) {
                            clearVoiceDraft()
                        }
                    }
                },
                onSaveNow = {
                    if (!jobRunning) {
                        hideIme()
                        onSaveNow()
                    }
                },
            )
        }
        AnimatedContent(
            targetState = mode,
            transitionSpec = {
                when {
                    // Text → Collapsing: only fold the composer away.
                    initialState == CaptureMode.Text && targetState == CaptureMode.Collapsing ->
                        fadeIn(tween(0)) togetherWith fadeOut(tween(120))
                    // Collapsing → Voice: orb appears after the fold.
                    initialState == CaptureMode.Collapsing && targetState == CaptureMode.Voice ->
                        fadeIn(tween(160)) togetherWith fadeOut(tween(80))
                    else ->
                        fadeIn(tween(140)) togetherWith fadeOut(tween(100))
                } using SizeTransform(clip = true) { _, _ -> tween(160) }
            },
            label = "captureMode",
        ) { current ->
            when (current) {
                CaptureMode.Voice -> VoiceOrbBar(
                    listening = listening,
                    busy = busy,
                    enabled = !jobRunning,
                    statusMessage = if (!listening && !busy) (speechError ?: hint) else null,
                    onHoldStart = {
                        if (!jobRunning) {
                            hideIme()
                            hint = null
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO,
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            if (MicPermissionGate.shouldPrompt(granted)) {
                                micPermission.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                transcriber.start()
                            }
                        }
                    },
                    onHoldEnd = { transcriber.stop() },
                    onSwitchToText = { switchToText() },
                    onAttachPhoto = { if (!jobRunning) showPhotoSource = true },
                )
                CaptureMode.Text -> TextComposerBar(
                    text = typed,
                    onTextChange = { typed = it },
                    enabled = !jobRunning,
                    analyzing = jobRunning,
                    clarifyQuestion = clarifyQuestion,
                    pendingDraft = pendingDraft,
                    analyzingHint = analyzingHint,
                    analyzingLabel = analyzingLabel,
                    requestFocus = !jobRunning,
                    onSwitchToVoice = { switchToVoice() },
                    onAttachPhoto = { if (!jobRunning) showPhotoSource = true },
                    hasImages = attached.isNotEmpty(),
                    onSubmit = {
                        val t = typed.trim()
                        if ((t.isBlank() && attached.isEmpty()) || jobRunning) return@TextComposerBar
                        hideIme()
                        if (trySubmit(t)) {
                            typed = ""
                        }
                    },
                    onSaveNow = { if (!jobRunning) onSaveNow() },
                    onCancelPending = {
                        if (!jobRunning) {
                            typed = ""
                            hideIme()
                            onCancelPending()
                        }
                    },
                )
                CaptureMode.Collapsing -> {
                    // Empty while IME collapses; orb mounts only after settle.
                    Box(Modifier.fillMaxWidth().height(0.dp))
                }
            }
        }
    }
}
