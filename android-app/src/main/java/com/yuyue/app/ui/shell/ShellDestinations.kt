package com.yuyue.app.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yuyue.app.data.YuyueRepository
import com.yuyue.app.speech.SpeechTranscriber
import com.yuyue.app.ui.AppViewModel
import com.yuyue.app.ui.DayTimelineScreen
import com.yuyue.app.ui.EntryEditScreen
import com.yuyue.app.ui.TrashScreen
import com.yuyue.app.ui.WorkspaceDetailScreen
import com.yuyue.app.ui.WorkspaceEntriesScreen
import com.yuyue.app.ui.shell.screens.EntryPaperScreen
import com.yuyue.app.ui.shell.screens.FacetBrowseScreen
import com.yuyue.app.ui.shell.screens.FacetTimelineScreen
import com.yuyue.app.ui.shell.screens.HomePaperScreen
import com.yuyue.app.ui.shell.screens.LocalExtrasScreen
import com.yuyue.app.ui.shell.screens.OnboardingScreen
import com.yuyue.app.ui.shell.screens.PeriodReviewScreen
import com.yuyue.app.ui.shell.screens.PhotoWallScreen
import com.yuyue.app.ui.shell.screens.RecallPaperScreen
import com.yuyue.app.ui.shell.screens.TimePaperScreen
import com.yuyue.app.ui.shell.screens.VaultPaperScreen
import java.time.LocalDate

@Composable
fun OnboardingDestination(
    onFinished: () -> Unit,
    appVm: AppViewModel,
    transcriber: SpeechTranscriber,
) {
    OnboardingScreen(
        transcriber = transcriber,
        onCommitFirstEntry = { text ->
            val ok = appVm.submitStructuredTurn(text)
            if (ok) appVm.completeFirstLaunch(showMicHint = false)
            ok
        },
        onFinished = onFinished,
    )
}

@Composable
fun HomeDestination(
    appVm: AppViewModel,
    onOpenVault: () -> Unit,
    onOpenTime: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onOpenEntry: (String) -> Unit,
    onEditEntry: (String) -> Unit,
) {
    // onOpenDay / onEditEntry retained for route compatibility; Home capsules open entry.
    @Suppress("UNUSED_PARAMETER")
    val unusedDay = onOpenDay
    @Suppress("UNUSED_PARAMETER")
    val unusedEdit = onEditEntry
    HomePaperScreen(
        appVm = appVm,
        onOpenVault = onOpenVault,
        onOpenTime = onOpenTime,
        onOpenEntry = onOpenEntry,
    )
}

@Composable
fun TimeDestination(
    appVm: AppViewModel,
    onOpenDay: (LocalDate) -> Unit,
    onOpenEntry: (String) -> Unit,
    onOpenPeople: () -> Unit = {},
    onOpenPlaces: () -> Unit = {},
    onOpenTopics: () -> Unit = {},
    onOpenWeekReview: () -> Unit = {},
    onOpenMonthReview: () -> Unit = {},
    onOpenPhotoWall: () -> Unit = {},
    onOpenLocalExtras: () -> Unit = {},
) {
    @Suppress("UNUSED_PARAMETER")
    val unused = onOpenDay
    TimePaperScreen(
        appVm = appVm,
        onOpenEntry = onOpenEntry,
        onOpenPeople = onOpenPeople,
        onOpenPlaces = onOpenPlaces,
        onOpenTopics = onOpenTopics,
        onOpenWeekReview = onOpenWeekReview,
        onOpenMonthReview = onOpenMonthReview,
        onOpenPhotoWall = onOpenPhotoWall,
        onOpenLocalExtras = onOpenLocalExtras,
    )
}

@Composable
fun RecallDestination(
    appVm: AppViewModel,
    recallVm: RecallViewModel,
    onOpenEntry: (String) -> Unit,
    onExpandCapture: () -> Unit,
    onOpenPeople: () -> Unit = {},
    onOpenPlaces: () -> Unit = {},
    onOpenTopics: () -> Unit = {},
    onOpenWeekReview: () -> Unit = {},
    onOpenMonthReview: () -> Unit = {},
    onOpenPhotoWall: () -> Unit = {},
    onOpenLocalExtras: () -> Unit = {},
) {
    @Suppress("UNUSED_PARAMETER")
    val unused = onExpandCapture
    RecallPaperScreen(
        appVm = appVm,
        recallVm = recallVm,
        onOpenEntry = onOpenEntry,
        onOpenPeople = onOpenPeople,
        onOpenPlaces = onOpenPlaces,
        onOpenTopics = onOpenTopics,
        onOpenWeekReview = onOpenWeekReview,
        onOpenMonthReview = onOpenMonthReview,
        onOpenPhotoWall = onOpenPhotoWall,
        onOpenLocalExtras = onOpenLocalExtras,
    )
}

@Composable
fun VaultDestination(
    appVm: AppViewModel,
    onExport: () -> Unit,
    onExportPlain: () -> Unit = {},
    onExportMarkdown: () -> Unit = {},
    onExportTxt: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onImport: () -> Unit,
    onOpenTrash: () -> Unit,
    onEditWorkspace: (String) -> Unit,
    onOpenPeople: () -> Unit = {},
    onOpenPlaces: () -> Unit = {},
    onOpenTopics: () -> Unit = {},
    onOpenWeekReview: () -> Unit = {},
    onOpenMonthReview: () -> Unit = {},
    onOpenPhotoWall: () -> Unit = {},
    onOpenLocalExtras: () -> Unit = {},
) {
    VaultPaperScreen(
        appVm = appVm,
        onExport = onExport,
        onExportPlain = onExportPlain,
        onExportMarkdown = onExportMarkdown,
        onExportTxt = onExportTxt,
        onExportPdf = onExportPdf,
        onImport = onImport,
        onOpenTrash = onOpenTrash,
        onEditWorkspace = onEditWorkspace,
        onOpenPeople = onOpenPeople,
        onOpenPlaces = onOpenPlaces,
        onOpenTopics = onOpenTopics,
        onOpenWeekReview = onOpenWeekReview,
        onOpenMonthReview = onOpenMonthReview,
        onOpenPhotoWall = onOpenPhotoWall,
        onOpenLocalExtras = onOpenLocalExtras,
    )
}

@Composable
fun FacetBrowseDestination(
    repository: YuyueRepository,
    axis: String,
    onBack: () -> Unit,
    onOpenBucket: (String, String) -> Unit,
    onOpenEntry: (String) -> Unit,
) {
    val vm: DifferentiatorViewModel = viewModel(factory = DifferentiatorViewModel.Factory(repository))
    val facet = DifferentiatorAggregate.parseAxis(axis)
    FacetBrowseScreen(
        axis = facet,
        vm = vm,
        onBack = onBack,
        onOpenBucket = { bucket -> onOpenBucket(DifferentiatorAggregate.axisRoute(facet), bucket.key) },
        onOpenEntry = onOpenEntry,
    )
}

@Composable
fun FacetTimelineDestination(
    repository: YuyueRepository,
    axis: String,
    key: String,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
) {
    val vm: DifferentiatorViewModel = viewModel(factory = DifferentiatorViewModel.Factory(repository))
    FacetTimelineScreen(
        axis = DifferentiatorAggregate.parseAxis(axis),
        key = key,
        vm = vm,
        onBack = onBack,
        onOpenEntry = onOpenEntry,
    )
}

@Composable
fun PeriodReviewDestination(
    repository: YuyueRepository,
    kind: String,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onOpenPerson: (String) -> Unit,
    onOpenPlace: (String) -> Unit,
    onOpenTopic: (String) -> Unit,
) {
    val vm: DifferentiatorViewModel = viewModel(factory = DifferentiatorViewModel.Factory(repository))
    PeriodReviewScreen(
        kind = kind,
        vm = vm,
        onBack = onBack,
        onOpenEntry = onOpenEntry,
        onOpenPerson = onOpenPerson,
        onOpenPlace = onOpenPlace,
        onOpenTopic = onOpenTopic,
    )
}

@Composable
fun PhotoWallDestination(
    repository: YuyueRepository,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
) {
    val vm: DifferentiatorViewModel = viewModel(factory = DifferentiatorViewModel.Factory(repository))
    PhotoWallScreen(vm = vm, onBack = onBack, onOpenEntry = onOpenEntry)
}

@Composable
fun LocalExtrasDestination(
    repository: YuyueRepository,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
) {
    val vm: DifferentiatorViewModel = viewModel(factory = DifferentiatorViewModel.Factory(repository))
    LocalExtrasScreen(vm = vm, onBack = onBack, onOpenEntry = onOpenEntry)
}

@Composable
fun EntryDestination(
    repository: YuyueRepository,
    appVm: AppViewModel,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val entryVm: EntryViewModel = viewModel(factory = EntryViewModel.Factory(repository))
    val detail by entryVm.detail.collectAsState()
    val media by entryVm.media.collectAsState()
    val revisions by entryVm.revisions.collectAsState()
    when (val state = detail) {
        EntryDetailState.Loading -> {
            Scaffold { padding ->
                Text("加载中…", modifier = Modifier.padding(padding).padding(24.dp))
            }
        }
        EntryDetailState.Missing -> {
            Scaffold { padding ->
                Column(
                    Modifier.padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("条目不存在或已删除")
                    TextButton(onClick = onBack) { Text("返回") }
                }
            }
        }
        is EntryDetailState.Present -> {
            val current = state.entry
            EntryPaperScreen(
                entry = current,
                media = media,
                revisions = revisions,
                onBack = onBack,
                onEdit = { onEdit(current.id) },
            )
        }
    }
}

@Composable
fun EntryEditDestination(
    repository: YuyueRepository,
    appVm: AppViewModel,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val editVm: EntryEditViewModel = viewModel(factory = EntryEditViewModel.Factory(repository))
    val detail by editVm.detail.collectAsState()
    val notebooks by appVm.notebooks.collectAsState()
    val saveStatus by appVm.saveStatus.collectAsState()
    val revisions by appVm.revisions.collectAsState()
    when (val state = detail) {
        EntryDetailState.Loading -> {
            Scaffold { padding ->
                Text("加载中…", modifier = Modifier.padding(padding).padding(24.dp))
            }
        }
        EntryDetailState.Missing -> {
            Scaffold { padding ->
                Column(
                    Modifier.padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("条目不存在或已删除")
                    TextButton(onClick = onBack) { Text("返回") }
                }
            }
        }
        is EntryDetailState.Present -> {
            val current = state.entry
            LaunchedEffect(current.id) { appVm.loadRevisions(current.id) }
            EntryEditScreen(
                entry = current,
                notebooks = notebooks,
                saveStatus = saveStatus,
                revisions = revisions,
                onBack = onBack,
                onSave = { result -> appVm.patchEntry(current.id, result, silent = true) },
                onDelete = {
                    appVm.deleteEntry(current.id)
                    onDeleted()
                },
                onRestoreRevision = { revisionId, asCopy ->
                    appVm.restoreRevision(revisionId, asCopy) { /* Flow refreshes entry */ }
                },
            )
        }
    }
}

@Composable
fun DayDestination(
    appVm: AppViewModel,
    transcriber: SpeechTranscriber,
    epochDay: Long,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
) {
    val day = LocalDate.ofEpochDay(epochDay)
    LaunchedEffect(day) { appVm.openDay(day) }
    DayTimelineScreen(
        vm = appVm,
        transcriber = transcriber,
        day = day,
        onBack = {
            appVm.closeDay()
            onBack()
        },
        onEntryClick = { onOpenEntry(it.id) },
    )
}

@Composable
fun TrashDestination(
    appVm: AppViewModel,
    onBack: () -> Unit,
) {
    TrashScreen(vm = appVm, onBack = onBack)
}

@Composable
fun WorkspaceDetailDestination(
    appVm: AppViewModel,
    notebookId: String,
    onBack: () -> Unit,
    onManageEntries: () -> Unit,
) {
    val notebooks by appVm.notebooks.collectAsState()
    val nb = notebooks.find { it.id == notebookId }
    if (nb == null) {
        LaunchedEffect(notebookId) { onBack() }
        return
    }
    WorkspaceDetailScreen(
        notebook = nb,
        onBack = onBack,
        onSave = { name, color -> appVm.updateNotebook(nb.id, name, color) },
        onManageEntries = onManageEntries,
    )
}

@Composable
fun WorkspaceEntriesDestination(
    appVm: AppViewModel,
    notebookId: String,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
) {
    val notebooks by appVm.notebooks.collectAsState()
    val workspaceEntries by appVm.workspaceEntries.collectAsState()
    val nb = notebooks.find { it.id == notebookId }
    LaunchedEffect(notebookId) { appVm.loadWorkspaceEntries(notebookId) }
    WorkspaceEntriesScreen(
        notebookName = nb?.name ?: "工作区",
        entries = workspaceEntries,
        onBack = {
            appVm.clearWorkspaceEntries()
            onBack()
        },
        onOpenEntry = { onOpenEntry(it.id) },
        onDeleteSelected = { appVm.deleteEntries(it) },
    )
}
