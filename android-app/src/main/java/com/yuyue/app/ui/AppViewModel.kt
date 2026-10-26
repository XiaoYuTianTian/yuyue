package com.yuyue.app.ui

import android.app.Application
import android.net.Uri
import com.yuyue.app.YuyueApplication
import com.yuyue.app.data.BackupImport
import com.yuyue.app.data.BackupIntegrity
import com.yuyue.app.data.ImportPolicy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yuyue.app.data.AiConfigStore
import com.yuyue.app.data.LlmCatalog
import com.yuyue.app.data.ProfileStore
import com.yuyue.app.data.UserProfile
import com.yuyue.app.data.YuyueRepository
import com.yuyue.app.data.capture.CaptureCommand
import com.yuyue.app.data.capture.CaptureCompletedNotice
import com.yuyue.app.data.capture.CaptureEvents
import com.yuyue.app.data.capture.CaptureIngress
import com.yuyue.app.data.capture.CaptureKernel
import com.yuyue.app.data.capture.CaptureKernelStatus
import com.yuyue.app.data.capture.CaptureLedgerService
import com.yuyue.app.data.capture.CaptureQueueItem
import com.yuyue.app.data.capture.CaptureQueueWork
import com.yuyue.app.data.capture.CaptureSession
import com.yuyue.app.data.capture.CaptureStatus
import com.yuyue.app.data.capture.InteractiveKind
import com.yuyue.app.data.capture.SubmitAction
import com.yuyue.app.llm.RouteKind
import com.yuyue.app.data.media.EntryMediaStore
import com.yuyue.app.data.local.EntryMediaEntity
import com.yuyue.app.data.local.EntryRevisionEntity
import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.data.local.NotebookEntity
import com.yuyue.app.data.remote.EntryDto
import com.yuyue.app.data.remote.ExportResponse
import com.yuyue.app.llm.LockedPolishEntry
import com.yuyue.app.llm.StructuredEntry
import com.yuyue.app.llm.StructuredTurn
import com.yuyue.app.agent.ChatMessage
import com.yuyue.app.util.DayDot
import com.yuyue.app.util.TimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class AppViewModel(
    private val repository: YuyueRepository,
    private val aiConfig: AiConfigStore,
    private val profileStore: ProfileStore,
    private val captureLedger: CaptureLedgerService,
    private val app: Application,
    private val captureKernel: CaptureKernel? = null,
) : ViewModel() {

    /** 本地优先：无需账号即可使用。 */
    val isLoggedIn = MutableStateFlow(true)

    private val _notebooks = MutableStateFlow<List<NotebookEntity>>(emptyList())
    val notebooks: StateFlow<List<NotebookEntity>> = _notebooks

    private val _selectedNotebookId = MutableStateFlow<String?>(null)
    val selectedNotebookId: StateFlow<String?> = _selectedNotebookId

    private val zone = ZoneId.of("Asia/Shanghai")

    private val _month = MutableStateFlow(YearMonth.from(LocalDate.now(zone)))
    val month: StateFlow<YearMonth> = _month

    private val _dayDots = MutableStateFlow<Map<Int, DayDot>>(emptyMap())
    val dayDots: StateFlow<Map<Int, DayDot>> = _dayDots

    /** 日历高亮，默认今天；与是否打开当日详情无关。 */
    private val _focusedDay = MutableStateFlow(LocalDate.now(zone))
    val focusedDay: StateFlow<LocalDate> = _focusedDay

    private val _selectedDay = MutableStateFlow<LocalDate?>(null)
    val selectedDay: StateFlow<LocalDate?> = _selectedDay

    private val _dayEntries = MutableStateFlow<List<EntryEntity>>(emptyList())
    val dayEntries: StateFlow<List<EntryEntity>> = _dayEntries

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _toastAction = MutableStateFlow<String?>(null)
    val toastAction: StateFlow<String?> = _toastAction

    private var toastKind: ToastKind = ToastKind.None
    private var pendingOpenEntryId: String? = null

    private val _openEntryReady = MutableSharedFlow<EntryEntity>(extraBufferCapacity = 8)
    val openEntryReady: SharedFlow<EntryEntity> = _openEntryReady.asSharedFlow()

    private var lastUndo: UndoAction? =
        (app as? YuyueApplication)?.container?.undoStore?.peek()

    private fun rememberUndo(action: UndoAction) {
        lastUndo = action
        (app as? YuyueApplication)?.container?.undoStore?.save(action)
    }

    private fun clearRememberedUndo() {
        lastUndo = null
        (app as? YuyueApplication)?.container?.undoStore?.clear()
    }

    private val _jobRunning = MutableStateFlow(false)
    val jobRunning: StateFlow<Boolean> = _jobRunning

    private val _analyzingLabel = MutableStateFlow("稍等片刻")
    val analyzingLabel: StateFlow<String> = _analyzingLabel

    private val _savedTitle = MutableStateFlow<String?>(null)
    val savedTitle: StateFlow<String?> = _savedTitle

    private val _clarifyQuestion = MutableStateFlow<String?>(null)
    val clarifyQuestion: StateFlow<String?> = _clarifyQuestion

    private val _pendingDraft = MutableStateFlow<PendingDraft?>(null)
    val pendingDraft: StateFlow<PendingDraft?> = _pendingDraft

    private var pendingEntries: List<StructuredEntry> = emptyList()
    private var pendingTimeExplicit: Boolean = false
    private var pendingCaptureRequestId: String? = null
    private var pendingRawTranscript: String? = null
    private var clarifyRounds: Int = 0
    private var recallClarifyRounds: Int = 0

    private val _recallHits = MutableStateFlow<List<EntryEntity>?>(null)
    val recallHits: StateFlow<List<EntryEntity>?> = _recallHits

    private val _pendingMutatePick = MutableStateFlow(false)
    val pendingMutatePick: StateFlow<Boolean> = _pendingMutatePick

    private var pendingMutateText: String? = null
    private var pendingMutateAction: String? = null
    private var pendingMutateTurn: StructuredTurn? = null
    private var pendingMutateRequestId: String? = null

    private val _importPreview = MutableStateFlow<ImportPreview?>(null)
    val importPreview: StateFlow<ImportPreview?> = _importPreview

    private val importJson = Json { ignoreUnknownKeys = true }

    private val turnHistory = mutableListOf<ChatMessage>()

    private var dayJob: Job? = null

    private val _welcomeFeed = MutableStateFlow<List<EntryEntity>>(emptyList())
    val welcomeFeed: StateFlow<List<EntryEntity>> = _welcomeFeed

    private val _onThisDay = MutableStateFlow<List<EntryEntity>>(emptyList())
    val onThisDay: StateFlow<List<EntryEntity>> = _onThisDay

    private val _recallQuery = MutableStateFlow("")
    val recallQuery: StateFlow<String> = _recallQuery

    private val _revisions = MutableStateFlow<List<EntryRevisionEntity>>(emptyList())
    val revisions: StateFlow<List<EntryRevisionEntity>> = _revisions

    private var lastUtteranceId: String? = null

    private val _trashEntries = MutableStateFlow<List<EntryEntity>>(emptyList())
    val trashEntries: StateFlow<List<EntryEntity>> = _trashEntries

    private val _aiKeyConfigured = MutableStateFlow(aiConfig.hasApiKey())
    val aiKeyConfigured: StateFlow<Boolean> = _aiKeyConfigured

    private val _profile = MutableStateFlow(profileStore.load())
    val profile: StateFlow<UserProfile> = _profile

    private val _backupNudge = MutableStateFlow(false)
    val backupNudge: StateFlow<Boolean> = _backupNudge

    private val _welcomeSeen = MutableStateFlow(profileStore.isWelcomeSeen())
    val welcomeSeen: StateFlow<Boolean> = _welcomeSeen

    private val _saveStatus = MutableStateFlow("已保存")
    val saveStatus: StateFlow<String> = _saveStatus

    private val _themeMode = MutableStateFlow((app as YuyueApplication).container.themeStore.mode)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private val _searchScopeAll = MutableStateFlow(false)
    val searchScopeAll: StateFlow<Boolean> = _searchScopeAll

    private val _searchHits = MutableStateFlow<List<EntryEntity>?>(null)
    val searchHits: StateFlow<List<EntryEntity>?> = _searchHits

    private val _searchOpen = MutableStateFlow(false)
    val searchOpen: StateFlow<Boolean> = _searchOpen

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _voiceNonce = MutableStateFlow(0)
    val voiceNonce: StateFlow<Int> = _voiceNonce

    private val _composerFocusNonce = MutableStateFlow(0)
    val composerFocusNonce: StateFlow<Int> = _composerFocusNonce

    fun requestComposerFocus() {
        _composerFocusNonce.value += 1
    }

    /** Desktop shortcut / widget: open voice ball (not keyboard). */
    fun requestVoiceCapture() {
        _voiceNonce.value += 1
    }

    private val _captureQueue = MutableStateFlow<List<CaptureQueueItem>>(emptyList())
    val captureQueue: StateFlow<List<CaptureQueueItem>> = _captureQueue

    private var workspaceEntriesJob: Job? = null
    private val _workspaceEntries = MutableStateFlow<List<EntryEntity>>(emptyList())
    val workspaceEntries: StateFlow<List<EntryEntity>> = _workspaceEntries

    private var heldSharedText: String? = null

    private val mediaStore = EntryMediaStore(app)
    private val _mediaByEntryId = MutableStateFlow<Map<String, List<EntryMediaEntity>>>(emptyMap())
    val mediaByEntryId: StateFlow<Map<String, List<EntryMediaEntity>>> = _mediaByEntryId

    init {
        viewModelScope.launch {
            runCatching { repository.ensureLocalReady() }
                .onFailure { emitToast(it.message ?: "无法打开记事本") }
            maybeOfferBackupNudge()
            repository.notebooks.collect { list ->
                _notebooks.value = list
                if (_selectedNotebookId.value == null && list.isNotEmpty()) {
                    _selectedNotebookId.value = list.find { it.isDefault }?.id ?: list.first().id
                    refreshMonth()
                }
                flushHeldSharedText()
                flushHeldSharedMedia()
            }
        }
        viewModelScope.launch {
            runCatching { repository.backfillMissingEmbeddings(40) }
        }
        viewModelScope.launch {
            captureLedger.observeActiveQueue().collect { items ->
                _captureQueue.value = items
                maybeRestoreInteractive(items)
            }
        }
        viewModelScope.launch {
            repository.observeAllMedia().collect { rows ->
                _mediaByEntryId.value = rows.groupBy { it.entryId }
            }
        }
        viewModelScope.launch {
            CaptureEvents.completed.collect { notice ->
                refreshMonth()
                emitToast(
                    "已记下：${notice.preview}",
                    actionLabel = if (notice.entryId != null) "查看" else null,
                    kind = if (notice.entryId != null) ToastKind.OpenEntry else ToastKind.None,
                    openEntryId = notice.entryId,
                )
                _voiceNonce.value += 1
            }
        }
        viewModelScope.launch {
            captureLedger.pendingRequestIds()
                .forEach { requestId -> CaptureQueueWork.enqueue(app, requestId) }
        }
    }

    fun refreshAiConfigState() {
        _aiKeyConfigured.value = aiConfig.hasApiKey()
    }

    fun selectLlmModel(optionId: String) {
        val option = LlmCatalog.byId(optionId)
        aiConfig.baseUrl = option.baseUrl
        aiConfig.model = option.model
        aiConfig.embeddingModel = option.embeddingModel
        refreshAiConfigState()
        emitToast("已选用 ${option.title}")
        viewModelScope.launch {
            captureLedger.pendingRequestIds()
                .forEach { requestId -> CaptureQueueWork.enqueue(app, requestId) }
        }
    }

    fun selectFastLlmModel(optionId: String) {
        val option = LlmCatalog.byIdFast(optionId)
        aiConfig.fastBaseUrl = option.baseUrl
        aiConfig.fastModel = option.model
        emitToast("已选用 ${option.title}")
    }

    fun saveProfile(displayName: String, gender: String, phone: String, age: String) {
        val ageTrim = age.trim()
        if (ageTrim.isNotEmpty()) {
            val n = ageTrim.toIntOrNull()
            if (n == null || n !in 1..120) {
                emitToast("年龄请填 1 到 120 的数字")
                return
            }
        }
        val current = profileStore.load()
        profileStore.save(
            UserProfile(
                displayName = displayName,
                gender = gender,
                phone = phone,
                age = ageTrim,
                avatarUri = current.avatarUri,
            ),
        )
        _profile.value = profileStore.load()
        emitToast("个人资料已保存")
    }

    fun setAvatarUri(uri: Uri?) {
        if (uri != null) {
            runCatching {
                app.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
        profileStore.setAvatarUri(uri?.toString())
        _profile.value = profileStore.load()
        emitToast(if (uri == null) "已恢复默认头像" else "头像已更新")
    }

    /** Copy gallery pick into app files so we don't depend on persistable SAF grants. */
    fun setAvatarFromGallery(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val dir = java.io.File(app.filesDir, "avatars").also { it.mkdirs() }
                val dest = java.io.File(dir, "profile.jpg")
                app.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                } ?: error("无法读取所选图片")
                profileStore.setAvatarUri(dest.absolutePath)
                _profile.value = profileStore.load()
                emitToast("头像已更新")
            }.onFailure { emitToast(it.message ?: "更换头像失败") }
        }
    }

    fun clearAvatar() {
        runCatching {
            java.io.File(app.filesDir, "avatars/profile.jpg").delete()
        }
        profileStore.setAvatarUri(null)
        _profile.value = profileStore.load()
        emitToast("已恢复默认头像")
    }
    fun currentAiConfig(): AiConfigSnapshot = AiConfigSnapshot(
        apiKey = aiConfig.apiKey,
        baseUrl = aiConfig.baseUrl,
        model = aiConfig.model,
        embeddingModel = aiConfig.embeddingModel,
        fastModel = aiConfig.fastModel,
        fastBaseUrl = aiConfig.fastBaseUrl,
    )

    fun selectNotebook(id: String) {
        if (_selectedNotebookId.value != id) {
            val capId = pendingCaptureRequestId
            val mutId = pendingMutateRequestId
            clearPending()
            pendingMutateRequestId = null
            _pendingMutatePick.value = false
            pendingMutateText = null
            pendingMutateAction = null
            pendingMutateTurn = null
            _recallHits.value = null
            _recallQuery.value = ""
            viewModelScope.launch {
                capId?.let { captureLedger.discard(it) }
                mutId?.let { captureLedger.discard(it) }
            }
        }
        _selectedNotebookId.value = id
        refreshMonth()
    }

    fun changeMonth(delta: Int) {
        _month.value = _month.value.plusMonths(delta.toLong())
        refreshMonth()
    }

    fun refreshMonth() {
        val nb = _selectedNotebookId.value ?: return
        viewModelScope.launch {
            runCatching {
                _dayDots.value = repository.daysWithDots(nb, _month.value)
                _welcomeFeed.value = repository.welcomeFeed(nb)
                _onThisDay.value = repository.onThisDay(nb)
            }.onFailure { emitToast(it.message ?: "无法刷新") }
        }
    }

    fun openDay(day: LocalDate) {
        _focusedDay.value = day
        _selectedDay.value = day
        val nb = _selectedNotebookId.value ?: return
        dayJob?.cancel()
        dayJob = viewModelScope.launch {
            repository.observeDay(nb, day).collect { _dayEntries.value = it }
        }
    }

    fun closeDay() {
        dayJob?.cancel()
        _selectedDay.value = null
    }

    fun clearSavedTitle() {
        _savedTitle.value = null
    }

    fun dismissRecallHits() {
        val mutateId = pendingMutateRequestId
        _recallHits.value = null
        _recallQuery.value = ""
        _pendingMutatePick.value = false
        pendingMutateText = null
        pendingMutateAction = null
        pendingMutateTurn = null
        pendingMutateRequestId = null
        if (mutateId != null) {
            viewModelScope.launch {
                captureLedger.discard(mutateId)
                emitToast("已取消修改")
            }
        }
    }

    fun discardCapture(requestId: String) {
        viewModelScope.launch {
            if (pendingCaptureRequestId == requestId) clearPending()
            if (pendingMutateRequestId == requestId) {
                _recallHits.value = null
                _recallQuery.value = ""
                _pendingMutatePick.value = false
                pendingMutateText = null
                pendingMutateAction = null
                pendingMutateTurn = null
                pendingMutateRequestId = null
            }
            val refs = com.yuyue.app.data.CaptureMediaJson.decode(captureLedger.mediaJson(requestId))
            captureLedger.discard(requestId)
            mediaStore.deleteFiles(refs)
            emitToast("已丢弃这条")
        }
    }

    fun retryCapture(requestId: String) {
        CaptureQueueWork.enqueue(app, requestId)
        emitToast("已重新排队")
    }

    fun continueCapture(requestId: String) {
        val item = _captureQueue.value.find { it.requestId == requestId } ?: return
        when (item.status) {
            CaptureStatus.Retryable, CaptureStatus.Pending, CaptureStatus.Processing -> {
                retryCapture(requestId)
            }
            CaptureStatus.ForegroundRequired -> {
                val kind = CaptureSession.kindFromLastError(item.lastError)
                restoreInteractive(item)
                when (kind) {
                    InteractiveKind.RecordClarify, InteractiveKind.Recall -> Unit
                    InteractiveKind.MutatePick -> {
                        val nbId = _selectedNotebookId.value ?: return
                        val nbName = _notebooks.value.find { it.id == nbId }?.name ?: ""
                        viewModelScope.launch {
                            val raw = captureLedger.getRawTranscript(requestId) ?: item.rawTranscript
                            acceptViaKernel(
                                text = raw,
                                nbId = nbId,
                                nbName = nbName,
                                requestId = requestId,
                                source = CaptureIngress.Keyboard,
                            )
                        }
                    }
                    null -> viewModelScope.launch {
                        val raw = captureLedger.getRawTranscript(requestId).orEmpty()
                        captureLedger.discard(requestId)
                        if (raw.isNotBlank()) submitStructuredTurn(raw)
                    }
                }
            }
            CaptureStatus.Completed -> Unit
            CaptureStatus.Paused -> Unit
        }
    }

    fun pauseCapture(requestId: String) {
        CaptureQueueWork.cancel(app, requestId)
        viewModelScope.launch {
            captureLedger.markPaused(requestId)
            emitToast("已暂停")
        }
    }

    fun resumeCapture(requestId: String, rawOverride: String? = null) {
        viewModelScope.launch {
            if (!rawOverride.isNullOrBlank()) captureLedger.updateRawTranscript(requestId, rawOverride)
            val raw = captureLedger.getRawTranscript(requestId) ?: return@launch
            captureLedger.markRetryable(requestId, "继续整理")
            val nbId = _selectedNotebookId.value ?: return@launch
            val nbName = _notebooks.value.find { it.id == nbId }?.name ?: ""
            val already = repository.listByRequestId(requestId)
            if (already.isNotEmpty()) {
                val paths = com.yuyue.app.data.CaptureMediaJson
                    .decode(captureLedger.mediaJson(requestId))
                    .map { it.path }
                polishSaved(requestId, raw, nbId, nbName, paths, already)
                return@launch
            }
            acceptViaKernel(
                text = raw,
                nbId = nbId,
                nbName = nbName,
                requestId = requestId,
                source = CaptureIngress.Keyboard,
            )
        }
    }

    /** Clear pending clarify / mutate-pick UI only. Do not discard other requestIds. */
    private fun settleInteractiveCapture() {
        pendingCaptureRequestId = null
        pendingMutateRequestId = null
        _pendingMutatePick.value = false
        pendingMutateText = null
        pendingMutateAction = null
        pendingMutateTurn = null
        _recallHits.value = null
        _recallQuery.value = ""
        pendingEntries = emptyList()
        pendingTimeExplicit = false
        pendingRawTranscript = null
        clarifyRounds = 0
        recallClarifyRounds = 0
        _clarifyQuestion.value = null
        _pendingDraft.value = null
        turnHistory.clear()
    }

    private val _unlockDay = MutableStateFlow<LocalDate?>(null)
    val unlockDay: StateFlow<LocalDate?> = _unlockDay

    fun clearUnlockDay() {
        _unlockDay.value = null
    }

    fun offerSharedText(text: String) {
        when (val decision = SharedTextIntake.decide(_selectedNotebookId.value, text)) {
            is SharedTextIntake.Submit ->
                submitStructuredTurn(decision.text, source = CaptureIngress.Share)
            is SharedTextIntake.Hold -> heldSharedText = decision.text
            is SharedTextIntake.Ignore -> Unit
        }
    }

    private fun flushHeldSharedText() {
        val held = heldSharedText ?: return
        when (val decision = SharedTextIntake.decide(_selectedNotebookId.value, held)) {
            is SharedTextIntake.Submit -> {
                heldSharedText = null
                submitStructuredTurn(decision.text, source = CaptureIngress.Share)
            }
            is SharedTextIntake.Hold -> heldSharedText = decision.text
            is SharedTextIntake.Ignore -> heldSharedText = null
        }
    }

    fun submitStructuredTurn(
        text: String,
        imageUris: List<Uri> = emptyList(),
        source: CaptureIngress = CaptureIngress.Keyboard,
    ): Boolean {
        val nbId = _selectedNotebookId.value
        if (nbId == null) {
            emitToast("还没有记事本，请先添加一个")
            return false
        }
        val nbName = _notebooks.value.find { it.id == nbId }?.name ?: ""
        if (text.isBlank() && imageUris.isEmpty()) {
            emitToast("还没有可发送的内容")
            return false
        }
        viewModelScope.launch {
            val memoryId = pendingCaptureRequestId ?: pendingMutateRequestId
            val existingRaw = pendingRawTranscript
                ?: memoryId?.let { captureLedger.getRawTranscript(it) }
            val action = CaptureSession.resolve(
                pendingRequestId = pendingCaptureRequestId,
                pendingMutateRequestId = pendingMutateRequestId,
                queue = _captureQueue.value,
                existingRaw = existingRaw,
                newText = text,
            )
            val refs = runCatching {
                if (imageUris.isEmpty()) emptyList() else mediaStore.importUris(imageUris)
            }.getOrElse {
                emitToast(it.message ?: "照片保存失败")
                return@launch
            }
            val ingress = when {
                refs.isNotEmpty() && text.isBlank() -> CaptureIngress.Photo
                source == CaptureIngress.Share -> CaptureIngress.Share
                source == CaptureIngress.Shortcut -> CaptureIngress.Shortcut
                source == CaptureIngress.Voice -> CaptureIngress.Voice
                refs.isNotEmpty() -> CaptureIngress.Photo
                else -> source
            }
            when (action) {
                is SubmitAction.Continue -> {
                    captureLedger.updateRawTranscript(action.requestId, action.combinedRaw)
                    captureLedger.mergeMedia(action.requestId, refs)
                    pendingRawTranscript = action.combinedRaw
                    pendingCaptureRequestId = action.requestId
                    emitToast("已补上")
                    val selected = if (action.kind == InteractiveKind.MutatePick) {
                        _recallHits.value?.singleOrNull()?.id
                    } else {
                        null
                    }
                    acceptViaKernel(
                        text = action.combinedRaw,
                        nbId = nbId,
                        nbName = nbName,
                        imagePaths = refs.map { it.path },
                        requestId = action.requestId,
                        selectedEventId = selected,
                        source = ingress,
                    )
                }
                SubmitAction.NewCapture -> {
                    settleInteractiveCapture()
                    val ok = acceptViaKernel(
                        text = text,
                        nbId = nbId,
                        nbName = nbName,
                        imagePaths = refs.map { it.path },
                        source = ingress,
                    )
                    if (!ok) {
                        mediaStore.deleteFiles(refs)
                    }
                }
            }
        }
        return true
    }

    /**
     * Thin adapter: maps [CaptureKernel] outcomes to existing toast / interactive UI.
     * Does not decide record/recall/mutate — kernel already did.
     */
    private suspend fun applyKernelOutcome(outcome: com.yuyue.app.data.capture.CaptureKernelOutcome) {
        outcome.privacyEvents.forEach { ev ->
            android.util.Log.i("CapturePrivacy", ev.toLogLine())
        }
        _voiceNonce.value += 1
        when (outcome.status) {
            CaptureKernelStatus.Rejected -> {
                emitToast("没听清，请再按住说一次")
            }
            CaptureKernelStatus.LocalSaved, CaptureKernelStatus.OfflineDegraded -> {
                emitToast(outcome.offlineStatusMessage ?: "已按原话记下")
                refreshMonth()
                _selectedDay.value?.let { openDay(it) }
                if (outcome.offlineStatusMessage != null) {
                    CaptureQueueWork.enqueue(app, outcome.requestId)
                }
            }
            CaptureKernelStatus.Completed -> {
                emitToast("已处理")
                refreshMonth()
                _selectedDay.value?.let { openDay(it) }
            }
            CaptureKernelStatus.ZeroWrite -> {
                when (outcome.route) {
                    RouteKind.Recall -> {
                        pendingCaptureRequestId = outcome.requestId
                        _recallQuery.value = captureLedger.getRawTranscript(outcome.requestId).orEmpty()
                        emitToast("正在查找，不会新建日记")
                    }
                    RouteKind.Mutate -> emitToast("没找到要改的那则")
                    else -> emitToast("未写入日记")
                }
            }
            CaptureKernelStatus.NeedsInteraction -> {
                when (outcome.interactiveKind) {
                    InteractiveKind.MutatePick -> {
                        pendingMutateRequestId = outcome.requestId
                        pendingMutateText = captureLedger.getRawTranscript(outcome.requestId)
                        _pendingMutatePick.value = true
                        if (outcome.candidateEventIds.isNotEmpty()) {
                            val hits = outcome.candidateEventIds.mapNotNull { repository.getEntry(it) }
                            _recallHits.value = hits
                        }
                        emitToast("要改哪一则？")
                    }
                    InteractiveKind.Recall -> {
                        pendingCaptureRequestId = outcome.requestId
                        emitToast("回忆缺少线索")
                    }
                    InteractiveKind.RecordClarify -> emitToast("还需要补充一点")
                    null -> emitToast("需要确认")
                }
            }
        }
    }

    /**
     * Sole write / route entry for organize APIs: always [CaptureKernel.accept].
     * FastExtract is never a write precondition (kernel polishes after local persist).
     */
    private suspend fun acceptViaKernel(
        text: String,
        nbId: String,
        nbName: String,
        imagePaths: List<String> = emptyList(),
        requestId: String? = null,
        selectedEventId: String? = null,
        source: CaptureIngress = CaptureIngress.Keyboard,
    ): Boolean {
        val kernel = captureKernel
        if (kernel == null) {
            emitToast("采集内核未就绪")
            return false
        }
        val outcome = runCatching {
            kernel.accept(
                CaptureCommand(
                    text = text,
                    mediaPaths = imagePaths,
                    source = source,
                    capturedAtMillis = System.currentTimeMillis(),
                    timezone = zone,
                    notebookId = nbId,
                    notebookName = nbName,
                    requestId = requestId,
                    selectedEventId = selectedEventId,
                ),
            )
        }.getOrElse {
            emitToast("原话保存失败，请重试")
            return false
        }
        applyKernelOutcome(outcome)
        return true
    }

    fun commitPendingDraft() {
        if (_jobRunning.value) return
        val nbId = _selectedNotebookId.value ?: return
        val requestId = pendingCaptureRequestId
        val transcript = pendingRawTranscript
            ?: pendingEntries.firstOrNull()?.raw?.ifBlank { null }
            ?: pendingEntries.firstOrNull()?.title
            ?: return
        if (transcript.isBlank() && pendingEntries.isEmpty()) {
            emitToast("还没有可记下的草稿")
            return
        }
        val nbName = _notebooks.value.find { it.id == nbId }?.name ?: ""
        clearPending()
        viewModelScope.launch {
            acceptViaKernel(
                text = transcript,
                nbId = nbId,
                nbName = nbName,
                requestId = requestId,
                source = CaptureIngress.Keyboard,
            )
        }
    }

    fun dismissPendingDraft() {
        val requestId = pendingCaptureRequestId
        turnHistory.clear()
        clearPending()
        if (requestId != null) {
            viewModelScope.launch {
                captureLedger.discard(requestId)
                emitToast("已取消")
            }
        }
    }

    private fun maybeRestoreInteractive(queue: List<CaptureQueueItem>) {
        if (pendingCaptureRequestId != null || pendingMutateRequestId != null) return
        if (!_clarifyQuestion.value.isNullOrBlank() || _pendingMutatePick.value) return
        val waiting = CaptureSession.waitingInteractive(queue) ?: return
        restoreInteractive(waiting)
    }

    private fun restoreInteractive(item: CaptureQueueItem) {
        val kind = CaptureSession.kindFromLastError(item.lastError) ?: return
        val question = CaptureSession.questionFromLastError(item.lastError)
        pendingCaptureRequestId = item.requestId
        pendingRawTranscript = item.rawTranscript
        turnHistory.clear()
        turnHistory += CaptureSession.rebuildTurnHistory(item.rawTranscript, question)
        when (kind) {
            InteractiveKind.RecordClarify -> {
                _clarifyQuestion.value = question
                _pendingDraft.value = PendingDraft(
                    question = question ?: "还需要补充什么？",
                    entries = pendingEntries,
                    timeExplicit = pendingTimeExplicit,
                )
            }
            InteractiveKind.Recall -> {
                _clarifyQuestion.value = question
            }
            InteractiveKind.MutatePick -> {
                pendingMutateRequestId = item.requestId
            }
        }
    }

    private fun clearPending() {
        pendingEntries = emptyList()
        pendingTimeExplicit = false
        pendingCaptureRequestId = null
        pendingRawTranscript = null
        clarifyRounds = 0
        recallClarifyRounds = 0
        _clarifyQuestion.value = null
        _pendingDraft.value = null
        turnHistory.clear()
    }

    private suspend fun polishSaved(
        requestId: String,
        text: String,
        nbId: String,
        nbName: String,
        paths: List<String>,
        existing: List<EntryEntity>,
        timeExplicit: Boolean? = null,
    ) {
        captureLedger.markPhase(requestId, "正在润色")
        _analyzingLabel.value = "正在润色"
        val locked = existing.map { e ->
            LockedPolishEntry(
                entryId = e.id,
                startAtIso = Instant.ofEpochMilli(e.startAtMillis).atZone(zone).toOffsetDateTime().toString(),
                startAtMillis = e.startAtMillis,
            )
        }
        val explicit = timeExplicit ?: (
            captureLedger.extractSlots(
                nowIso = TimeUtils.nowIso(zone),
                userText = text,
                notebookName = nbName,
                hasImages = paths.isNotEmpty(),
            )?.timeExplicit == true
        )
        runCatching {
            captureLedger.polishInPlace(
                requestId = requestId,
                rawTranscript = text,
                notebookId = nbId,
                notebookName = nbName,
                locked = locked,
                imagePaths = paths,
                timeExplicit = explicit,
            )
        }.onFailure { err ->
            captureLedger.markRetryable(requestId, err.message ?: "润色失败")
            emitToast("要点已记下，润色稍后重试")
            CaptureQueueWork.enqueue(app, requestId)
            refreshMonth()
            _selectedDay.value?.let { openDay(it) }
            return
        }
        captureLedger.markCompleted(requestId)
        refreshMonth()
        _selectedDay.value?.let { openDay(it) }
        val preview = repository.getEntry(existing.first().id)?.title?.trim()?.ifBlank { null }
            ?: text.trim().ifBlank { "一条日记" }
        CaptureEvents.emitCompleted(
            CaptureCompletedNotice(
                requestId = requestId,
                preview = preview.take(24),
                entryId = existing.first().id,
            ),
        )
    }

    private suspend fun maybeOfferBackupNudge() {
        if (profileStore.isBackupNudgeDone()) return
        val n = runCatching { repository.countActiveEntries() }.getOrDefault(0)
        if (n >= 10) _backupNudge.value = true
    }

    fun dismissBackupNudge() {
        profileStore.markBackupNudgeDone()
        _backupNudge.value = false
    }

    fun markWelcomeSeen() {
        profileStore.markWelcomeSeen()
        _welcomeSeen.value = true
    }

    fun completeFirstLaunch(showMicHint: Boolean) {
        markWelcomeSeen()
        if (showMicHint) emitToast("到首页按住说一句")
    }

    fun createNotebook(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val nb = repository.createNotebook(name)
                _selectedNotebookId.value = nb.id
                refreshMonth()
            }.onFailure { emitToast(it.message ?: "无法添加记事本") }
        }
    }

    fun updateNotebook(id: String, name: String, color: String?) {
        viewModelScope.launch {
            runCatching {
                repository.updateNotebook(id, name, color)
                emitToast("工作区已更新")
            }.onFailure { emitToast(it.message ?: "无法更新工作区") }
        }
    }

    fun deleteNotebook(id: String) {
        viewModelScope.launch {
            runCatching {
                val wasSelected = _selectedNotebookId.value == id
                repository.deleteNotebook(id)
                if (wasSelected) {
                    _selectedNotebookId.value = _notebooks.value.firstOrNull { it.id != id }?.id
                }
                refreshMonth()
                emitToast("工作区已删除")
            }.onFailure { emitToast(it.message ?: "无法删除工作区") }
        }
    }

    fun loadWorkspaceEntries(notebookId: String) {
        workspaceEntriesJob?.cancel()
        workspaceEntriesJob = viewModelScope.launch {
            repository.observeActiveEntries(notebookId).collect { _workspaceEntries.value = it }
        }
    }

    fun clearWorkspaceEntries() {
        workspaceEntriesJob?.cancel()
        _workspaceEntries.value = emptyList()
    }

    fun deleteEntries(ids: List<String>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                repository.deleteEntries(ids)
                emitToast(if (ids.size == 1) "已移入废纸篓" else "已移入废纸篓 ${ids.size} 则")
                refreshMonth()
            }.onFailure { emitToast(it.message ?: "无法删除") }
        }
    }

    fun setEntryDone(id: String, done: Boolean) {
        viewModelScope.launch {
            runCatching {
                val previous = repository.getEntry(id)?.done ?: !done
                repository.setEntryDone(id, done)
                rememberUndo(UndoAction.ToggleDone(id, previous))
                refreshMonth()
                _selectedDay.value?.let { openDay(it) }
                emitToast(if (done) "已完成" else "已标为未完成", "撤销", ToastKind.Undo)
            }.onFailure { emitToast(it.message ?: "无法更新") }
        }
    }

    fun submitMutate(text: String) {
        val nbId = _selectedNotebookId.value
        if (nbId == null) {
            emitToast("还没有记事本，请先添加一个")
            return
        }
        val nbName = _notebooks.value.find { it.id == nbId }?.name ?: ""
        viewModelScope.launch {
            acceptViaKernel(
                text = text,
                nbId = nbId,
                nbName = nbName,
                source = CaptureIngress.Keyboard,
            )
        }
    }

    fun applyPendingMutate(entryId: String): Boolean {
        val text = pendingMutateText ?: return false
        val requestId = pendingMutateRequestId
        pendingMutateText = null
        pendingMutateAction = null
        pendingMutateTurn = null
        pendingMutateRequestId = null
        _pendingMutatePick.value = false
        _recallHits.value = null
        val nbId = _selectedNotebookId.value ?: return false
        val nbName = _notebooks.value.find { it.id == nbId }?.name ?: ""
        viewModelScope.launch {
            acceptViaKernel(
                text = text,
                nbId = nbId,
                nbName = nbName,
                requestId = requestId,
                selectedEventId = entryId,
                source = CaptureIngress.Keyboard,
            )
        }
        return true
    }

    fun loadTrash() {
        viewModelScope.launch {
            repository.observeTrash().collect { _trashEntries.value = it }
        }
    }

    fun restoreTrashEntry(id: String) {
        viewModelScope.launch {
            runCatching {
                repository.restoreEntry(id)
                emitToast("已恢复")
                refreshMonth()
            }.onFailure { emitToast(it.message ?: "无法恢复") }
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            runCatching {
                val title = repository.getEntry(id)?.title.orEmpty()
                repository.deleteEntry(id)
                rememberUndo(UndoAction.SoftDelete(id))
                refreshMonth()
                emitToast(
                    if (title.isBlank()) "已移入废纸篓" else "已移入废纸篓：$title",
                    "撤销",
                    ToastKind.Undo,
                )
            }.onFailure { emitToast(it.message ?: "无法删除") }
        }
    }

    fun undoLast() {
        val action = lastUndo ?: (app as? YuyueApplication)?.container?.undoStore?.consume() ?: return
        clearRememberedUndo()
        viewModelScope.launch {
            runCatching {
                when (action) {
                    is UndoAction.ToggleDone -> repository.setEntryDone(action.id, action.previous)
                    is UndoAction.SoftDelete -> repository.restoreEntry(action.id)
                }
                refreshMonth()
                _selectedDay.value?.let { openDay(it) }
            }.onFailure { emitToast(it.message ?: "无法撤销") }
        }
    }

    fun purgeTrashEntry(id: String) {
        viewModelScope.launch {
            runCatching {
                if (repository.purgeEntry(id)) {
                    emitToast("已永久删除")
                    refreshMonth()
                } else {
                    emitToast("只能永久删除废纸篓里的记录")
                }
            }.onFailure { emitToast(it.message ?: "无法永久删除") }
        }
    }

    fun purgeAllTrash() {
        viewModelScope.launch {
            runCatching {
                val n = repository.purgeTrash()
                emitToast(if (n == 0) "废纸篓是空的" else "已永久删除 $n 则")
                refreshMonth()
            }.onFailure { emitToast(it.message ?: "无法清空废纸篓") }
        }
    }

    fun offerSharedMedia(text: String?, mimeType: String?, streamUri: Uri?) {
        when (
            val decision = SharedMediaIntake.decide(
                notebookId = _selectedNotebookId.value,
                text = text,
                mimeType = mimeType,
                hasStream = streamUri != null,
            )
        ) {
            is SharedMediaIntake.SubmitText -> submitStructuredTurn(decision.text)
            is SharedMediaIntake.HoldText -> heldSharedText = decision.text
            is SharedMediaIntake.SubmitImage -> {
                val uris = listOfNotNull(streamUri)
                submitStructuredTurn(decision.caption.orEmpty(), uris)
            }
            is SharedMediaIntake.HoldImage -> {
                heldSharedImage = HeldSharedImage(decision.caption, streamUri)
            }
            is SharedMediaIntake.Ignore -> Unit
        }
    }

    private data class HeldSharedImage(val caption: String?, val uri: Uri?)
    private var heldSharedImage: HeldSharedImage? = null

    private fun flushHeldSharedMedia() {
        val held = heldSharedImage ?: return
        when (
            val decision = SharedMediaIntake.decide(
                notebookId = _selectedNotebookId.value,
                text = held.caption,
                mimeType = "image/*",
                hasStream = held.uri != null,
            )
        ) {
            is SharedMediaIntake.SubmitImage -> {
                heldSharedImage = null
                submitStructuredTurn(decision.caption.orEmpty(), listOfNotNull(held.uri))
            }
            is SharedMediaIntake.HoldImage -> heldSharedImage = held
            else -> heldSharedImage = null
        }
    }

    fun patchEntry(id: String, result: EntryEditResult, silent: Boolean = false) {
        viewModelScope.launch {
            if (silent) _saveStatus.value = "保存中"
            runCatching {
                repository.patchEntry(
                    id = id,
                    title = result.title,
                    body = result.body,
                    done = result.done,
                    startAtMillis = result.startAtMillis,
                    place = result.place,
                    people = result.people,
                    diaryType = result.diaryType,
                    notebookId = result.notebookId,
                    clearPlace = result.place == null,
                    clearPeople = result.people == null,
                )
                refreshMonth()
                _selectedDay.value?.let { openDay(it) }
                if (silent) _saveStatus.value = "已保存"
                loadRevisions(id)
            }.onFailure {
                emitToast(it.message ?: "无法保存")
                if (silent) _saveStatus.value = "保存失败"
            }
        }
    }

    fun loadRevisions(entryId: String) {
        viewModelScope.launch {
            _revisions.value = runCatching { repository.listRevisions(entryId) }.getOrDefault(emptyList())
        }
    }

    fun restoreRevision(revisionId: String, asCopy: Boolean, onRestored: (EntryEntity) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val restored = repository.restoreRevision(revisionId, asCopy) ?: return@runCatching
                refreshMonth()
                loadRevisions(if (asCopy) restored.id else restored.id)
                emitToast(if (asCopy) "已另存为副本" else "已恢复这一版")
                onRestored(restored)
            }.onFailure { emitToast(it.message ?: "无法恢复这一版") }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        (app as YuyueApplication).container.themeStore.mode = mode
        _themeMode.value = mode
    }

    fun setSearchScopeAll(all: Boolean) {
        _searchScopeAll.value = all
    }

    fun searchKeyword(query: String) {
        val q = query.trim()
        if (q.isEmpty()) {
            _searchHits.value = null
            return
        }
        viewModelScope.launch {
            runCatching {
                val nb = if (_searchScopeAll.value) null else _selectedNotebookId.value
                val hits = repository.searchLike(q, nb, 40)
                _searchHits.value = hits
            }.onFailure { emitToast(it.message ?: "查找失败") }
        }
    }

    fun setSearchOpen(open: Boolean) {
        _searchOpen.value = open
        if (!open) {
            _searchQuery.value = ""
            _searchHits.value = null
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) _searchHits.value = null
    }

    fun dismissSearchHits() {
        _searchHits.value = null
    }

    fun previewImport(raw: String) {
        runCatching {
            BackupIntegrity.ensureSize(raw)
            val payload = importJson.decodeFromString(ExportResponse.serializer(), raw)
            if (!BackupIntegrity.verify(payload)) {
                emitToast("这份备份校验失败")
                return
            }
            val plan = BackupImport.plan(payload, _notebooks.value)
            _importPreview.value = ImportPreview(
                raw = raw,
                notebooks = payload.notebooks.size,
                entries = payload.entries.size,
                skipped = plan.skippedCount,
                copies = plan.copyCount,
            )
        }.onFailure { emitToast(it.message ?: "无法读取这份备份") }
    }

    fun confirmImport(asCopy: Boolean = false) {
        val preview = _importPreview.value ?: return
        _importPreview.value = null
        viewModelScope.launch {
            runCatching {
                val plan = pendingImportBytes?.let { bytes ->
                    repository.importArchive(
                        bytes,
                        if (asCopy) ImportPolicy.RestoreAsCopy else ImportPolicy.NewerWins,
                    )
                } ?: repository.importJson(
                    preview.raw,
                    if (asCopy) ImportPolicy.RestoreAsCopy else ImportPolicy.NewerWins,
                )
                pendingImportBytes = null
                refreshMonth()
                emitToast("已导入 ${plan.entriesToUpsert.size} 则")
            }.onFailure { emitToast(it.message ?: "导入失败") }
        }
    }

    fun cancelImport() {
        _importPreview.value = null
        pendingImportBytes = null
    }

    fun showMessage(text: String) {
        emitToast(text)
    }

    fun clearMessage() {
        _message.value = null
        _toastAction.value = null
        toastKind = ToastKind.None
        pendingOpenEntryId = null
    }

    fun onToastAction() {
        when (toastKind) {
            ToastKind.Undo -> undoLast()
            ToastKind.OpenEntry -> pendingOpenEntryId?.let { requestOpenEntry(it) }
            ToastKind.None -> Unit
        }
    }

    fun requestOpenEntry(id: String) {
        viewModelScope.launch {
            val e = repository.getEntry(id) ?: return@launch
            _openEntryReady.emit(e)
        }
    }

    private fun emitToast(
        text: String,
        actionLabel: String? = null,
        kind: ToastKind = ToastKind.None,
        openEntryId: String? = null,
    ) {
        pendingOpenEntryId = openEntryId
        toastKind = kind
        _toastAction.value = actionLabel
        _message.value = text
    }

    suspend fun exportJson(): String = repository.exportJson()

    suspend fun exportArchive(): ByteArray = repository.exportArchive()

    suspend fun exportDiaryMarkdown(): String {
        val entries = repository.listActiveExportEntries()
        return com.yuyue.app.data.DiaryExport.toMarkdown(entries)
    }

    suspend fun exportDiaryTxt(): String {
        val entries = repository.listActiveExportEntries()
        return com.yuyue.app.data.DiaryExport.toTxt(entries)
    }

    suspend fun exportDiaryPdf(): ByteArray {
        val entries = repository.listActiveExportEntries()
        return com.yuyue.app.data.DiaryExport.toPdfBytes(entries)
    }

    suspend fun runEncryptedBackupToDownloads(): String? {
        val engine = (app as YuyueApplication).container.backupEngine
        val result = engine.backupToDownloads()
        return result.fileName ?: result.health.lastError
    }

    fun setBackupPassphrase(passphrase: String) {
        (app as YuyueApplication).container.backupStore.passphrase = passphrase
        if (passphrase.isNotBlank()) {
            com.yuyue.app.data.EncryptedBackupWork.enqueue(app)
        }
    }

    fun backupPassphraseSet(): Boolean =
        (app as YuyueApplication).container.backupStore.passphrase.isNotBlank()

    fun appLockEnabled(): Boolean =
        (app as YuyueApplication).container.appLockStore.enabled

    fun setAppLockPin(pin: String) {
        (app as YuyueApplication).container.appLockStore.setPin(pin)
    }

    fun clearAppLock() {
        (app as YuyueApplication).container.appLockStore.clear()
    }

    fun setAppLockBiometric(enabled: Boolean) {
        (app as YuyueApplication).container.appLockStore.biometricEnabled = enabled
    }

    fun verifyAppLockPin(pin: String): Boolean =
        (app as YuyueApplication).container.appLockStore.verifyPin(pin)

    fun setAppLockBiometricPreferred(enabled: Boolean) = setAppLockBiometric(enabled)


    fun previewImportBytes(bytes: ByteArray) {
        runCatching {
            val unpacked = com.yuyue.app.data.BackupArchive.unpackOrJson(bytes)
            pendingImportBytes = bytes
            previewImport(unpacked.json)
        }.onFailure { emitToast(it.message ?: "无法读取这份备份") }
    }

    private var pendingImportBytes: ByteArray? = null

    fun entryToDto(e: EntryEntity): EntryDto = EntryDto(
        id = e.id,
        notebookId = e.notebookId,
        entryKind = e.entryKind,
        status = e.status,
        subjectKey = e.subjectKey,
        polarity = e.polarity,
        title = e.title,
        body = e.body,
        rawTranscript = e.rawTranscript,
        startAt = Instant.ofEpochMilli(e.startAtMillis).atZone(zone).toOffsetDateTime().toString(),
        endAt = e.endAtMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toOffsetDateTime().toString() },
        statedAt = Instant.ofEpochMilli(e.statedAtMillis).atZone(zone).toOffsetDateTime().toString(),
        updatedAt = Instant.ofEpochMilli(e.updatedAtMillis).atZone(zone).toOffsetDateTime().toString(),
        done = e.done,
        utteranceId = e.utteranceId,
    )

    data class AiConfigSnapshot(
        val apiKey: String,
        val baseUrl: String,
        val model: String,
        val embeddingModel: String,
        val fastModel: String,
        val fastBaseUrl: String,
    )

    class Factory(
        private val repository: YuyueRepository,
        private val aiConfig: AiConfigStore,
        private val profileStore: ProfileStore,
        private val captureLedger: CaptureLedgerService,
        private val app: Application,
        private val captureKernel: CaptureKernel? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AppViewModel(
                repository,
                aiConfig,
                profileStore,
                captureLedger,
                app,
                captureKernel,
            ) as T
    }
}

data class PendingDraft(
    val question: String,
    val entries: List<StructuredEntry>,
    val timeExplicit: Boolean,
)

data class ImportPreview(
    val raw: String,
    val notebooks: Int,
    val entries: Int,
    val skipped: Int = 0,
    val copies: Int = 0,
)

sealed class UndoAction {
    data class ToggleDone(val id: String, val previous: Boolean) : UndoAction()
    data class SoftDelete(val id: String) : UndoAction()
}

enum class ToastKind { None, Undo, OpenEntry }
