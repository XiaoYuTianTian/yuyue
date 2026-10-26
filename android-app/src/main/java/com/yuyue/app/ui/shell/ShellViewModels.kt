package com.yuyue.app.ui.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yuyue.app.data.YuyueRepository
import com.yuyue.app.data.capture.CaptureCommand
import com.yuyue.app.data.capture.CaptureIngress
import com.yuyue.app.data.capture.CaptureKernel
import com.yuyue.app.data.capture.CaptureKernelOutcome
import com.yuyue.app.data.capture.CaptureLedgerService
import com.yuyue.app.data.capture.CaptureQueueItem
import com.yuyue.app.data.local.EntryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Global capture session shared across Home / Time / Recall (not a tab).
 *
 * UI: single [com.yuyue.app.ui.shell.CaptureOverlaySlot] on ShellScaffold.
 * Write path for shell submit still goes through AppViewModel.submitStructuredTurn
 * (toasts / clarify / pending draft). Direct [accept] / [offerSharedText] use kernel only.
 */
class CaptureViewModel(
    private val captureLedger: CaptureLedgerService?,
    private val captureKernel: CaptureKernel?,
) : ViewModel() {
    private val zone = ZoneId.of("Asia/Shanghai")

    private val _draftText = MutableStateFlow("")
    val draftText: StateFlow<String> = _draftText.asStateFlow()

    private val _captureExpanded = MutableStateFlow(false)
    val captureExpanded: StateFlow<Boolean> = _captureExpanded.asStateFlow()

    private val _composerFocusNonce = MutableStateFlow(0)
    val composerFocusNonce: StateFlow<Int> = _composerFocusNonce.asStateFlow()

    private val _notebookId = MutableStateFlow<String?>(null)
    private val _notebookName = MutableStateFlow("")

    val activeQueue: StateFlow<List<CaptureQueueItem>> =
        (captureLedger?.observeActiveQueue()
            ?: kotlinx.coroutines.flow.flowOf(emptyList()))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun bindNotebook(notebookId: String?, notebookName: String) {
        _notebookId.value = notebookId
        _notebookName.value = notebookName
    }

    fun setDraftText(text: String) {
        _draftText.value = text
    }

    fun setCaptureExpanded(expanded: Boolean) {
        _captureExpanded.value = expanded
    }

    fun requestComposerFocus() {
        _captureExpanded.value = true
        _composerFocusNonce.value = _composerFocusNonce.value + 1
    }

    /**
     * Share / paste intake: expands composer and, when a notebook is bound,
     * writes solely through [CaptureKernel].
     */
    fun offerSharedText(text: String) {
        _draftText.value = text
        _captureExpanded.value = true
        val nbId = _notebookId.value ?: return
        val kernel = captureKernel ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val outcome = kernel.accept(
                CaptureCommand(
                    text = trimmed,
                    source = CaptureIngress.Share,
                    capturedAtMillis = System.currentTimeMillis(),
                    timezone = zone,
                    notebookId = nbId,
                    notebookName = _notebookName.value,
                ),
            )
            logPrivacy(outcome)
        }
    }

    /** Direct kernel accept for shell / tests. */
    suspend fun accept(command: CaptureCommand): CaptureKernelOutcome? {
        val kernel = captureKernel ?: return null
        val outcome = kernel.accept(command)
        logPrivacy(outcome)
        return outcome
    }

    private fun logPrivacy(outcome: CaptureKernelOutcome) {
        outcome.privacyEvents.forEach { ev ->
            android.util.Log.i("CapturePrivacy", ev.toLogLine())
        }
    }

    class Factory(
        private val captureLedger: CaptureLedgerService?,
        private val captureKernel: CaptureKernel?,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CaptureViewModel(captureLedger, captureKernel) as T
    }
}

/** Home tab helpers; feed still observed via AppViewModel until read-model cutover. */
class HomeViewModel(
    private val repository: YuyueRepository,
) : ViewModel() {
    private val zone = ZoneId.of("Asia/Shanghai")
    val today: LocalDate = LocalDate.now(zone)

    fun sectionsFor(feed: List<EntryEntity>): HomeFeedSections =
        HomeFeedSplit.split(feed, today, zone)

    class Factory(private val repository: YuyueRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repository) as T
    }
}

/** Time tab: month / day navigation stubs. */
class TimeViewModel(
    private val repository: YuyueRepository,
) : ViewModel() {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val _month = MutableStateFlow(YearMonth.from(LocalDate.now(zone)))
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    fun setMonth(month: YearMonth) {
        _month.value = month
    }

    class Factory(private val repository: YuyueRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TimeViewModel(repository) as T
    }
}

/** Recall tab: query state; search still via legacy AppViewModel until kernel/recall cutover. */
class RecallViewModel(
    private val repository: YuyueRepository,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun setQuery(value: String) {
        _query.value = value
    }

    suspend fun searchLike(query: String, notebookId: String?): List<EntryEntity> =
        repository.searchLike(query, notebookId)

    class Factory(private val repository: YuyueRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RecallViewModel(repository) as T
    }
}

/** Vault tab: notebooks stream; lock/backup/theme still via AppViewModel prefs. */
class VaultViewModel(
    private val repository: YuyueRepository,
) : ViewModel() {
    val notebooks = repository.notebooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    class Factory(private val repository: YuyueRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            VaultViewModel(repository) as T
    }
}

/**
 * Task 8 differentiators: local facet / review / photo wall from legacy entries + media.
 * Domain Person/Place/Topic join tables are optional overlays when present.
 */
class DifferentiatorViewModel(
    private val repository: YuyueRepository,
) : ViewModel() {
    private val _entries = MutableStateFlow<List<EntryEntity>>(emptyList())
    val entries: StateFlow<List<EntryEntity>> = _entries.asStateFlow()

    private val _media = MutableStateFlow<List<com.yuyue.app.data.local.EntryMediaEntity>>(emptyList())
    val media: StateFlow<List<com.yuyue.app.data.local.EntryMediaEntity>> = _media.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _entries.value = repository.listActiveEntries()
            _media.value = repository.listAllMedia()
            _loading.value = false
        }
    }

    class Factory(private val repository: YuyueRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DifferentiatorViewModel(repository) as T
    }
}
