package com.yuyue.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.speech.SpeechTranscriber
import java.time.LocalDate

/**
 * Pre-rebuild hand-rolled stack + overlay + viewingEntry navigation.
 * Kept for [com.yuyue.app.BuildConfig.FEATURE_REBUILD_SHELL] = false fallback.
 */
@Composable
fun LegacyNavHost(
    vm: AppViewModel,
    transcriber: SpeechTranscriber,
    onExport: () -> Unit,
    onExportPlain: () -> Unit = {},
    onExportMarkdown: () -> Unit = {},
    onExportTxt: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onImport: () -> Unit,
) {
    val notebooks by vm.notebooks.collectAsState()
    val saveStatus by vm.saveStatus.collectAsState()
    val revisions by vm.revisions.collectAsState()
    val workspaceEntries by vm.workspaceEntries.collectAsState()
    val unlockDay by vm.unlockDay.collectAsState()
    val welcomeSeen by vm.welcomeSeen.collectAsState()

    var overlay by remember { mutableStateOf<LegacyOverlay?>(null) }
    var stack by remember { mutableStateOf(listOf<LegacyDest>(LegacyDest.Welcome)) }
    var viewingEntry by remember { mutableStateOf<EntryEntity?>(null) }
    var editingEntry by remember { mutableStateOf<EntryEntity?>(null) }

    LaunchedEffect(Unit) {
        vm.openEntryReady.collect { viewingEntry = it }
    }

    fun push(dest: LegacyDest) {
        stack = stack + dest
    }

    fun pop() {
        if (stack.size > 1) stack = stack.dropLast(1)
    }

    fun openDay(day: LocalDate) {
        vm.openDay(day)
        val last = stack.last()
        stack = when {
            last is LegacyDest.Day && last.date == day -> stack
            last is LegacyDest.Day -> stack.dropLast(1) + LegacyDest.Day(day)
            else -> stack + LegacyDest.Day(day)
        }
    }

    LaunchedEffect(unlockDay) {
        val day = unlockDay ?: return@LaunchedEffect
        openDay(day)
        vm.clearUnlockDay()
    }

    BackHandler(
        enabled = editingEntry != null ||
            viewingEntry != null ||
            overlay != null ||
            stack.size > 1,
    ) {
        when {
            editingEntry != null -> editingEntry = null
            viewingEntry != null -> viewingEntry = null
            overlay is LegacyOverlay.WorkspaceEntries -> {
                val detailId = (overlay as LegacyOverlay.WorkspaceEntries).notebookId
                vm.clearWorkspaceEntries()
                overlay = LegacyOverlay.WorkspaceDetail(detailId)
            }
            overlay is LegacyOverlay.WorkspaceDetail -> overlay = LegacyOverlay.Settings
            overlay is LegacyOverlay.Trash -> overlay = LegacyOverlay.Settings
            overlay != null -> overlay = null
            stack.last() is LegacyDest.Day -> {
                vm.closeDay()
                pop()
            }
            else -> pop()
        }
    }

    when {
        !welcomeSeen -> FirstLaunchScreen(
            onStart = { vm.completeFirstLaunch(showMicHint = false) },
            onMicHint = { vm.completeFirstLaunch(showMicHint = true) },
        )
        editingEntry != null -> {
            val entry = editingEntry!!
            LaunchedEffect(entry.id) { vm.loadRevisions(entry.id) }
            EntryEditScreen(
                entry = entry,
                notebooks = notebooks,
                saveStatus = saveStatus,
                revisions = revisions,
                onBack = { editingEntry = null },
                onSave = { result ->
                    vm.patchEntry(entry.id, result, silent = true)
                    viewingEntry = entry.copy(
                        title = result.title,
                        body = result.body,
                        done = result.done,
                        startAtMillis = result.startAtMillis,
                        place = result.place,
                        people = result.people,
                        diaryType = result.diaryType,
                        notebookId = result.notebookId ?: entry.notebookId,
                    )
                },
                onDelete = {
                    vm.deleteEntry(entry.id)
                    editingEntry = null
                    viewingEntry = null
                },
                onRestoreRevision = { revisionId, asCopy ->
                    vm.restoreRevision(revisionId, asCopy) { restored ->
                        editingEntry = restored
                        viewingEntry = restored
                    }
                },
            )
        }
        viewingEntry != null -> {
            val entry = viewingEntry!!
            val mediaByEntry by vm.mediaByEntryId.collectAsState()
            EntryDetailScreen(
                entry = entry,
                media = mediaByEntry[entry.id].orEmpty(),
                onBack = { viewingEntry = null },
                onToggleDone = { done ->
                    vm.setEntryDone(entry.id, done)
                    viewingEntry = entry.copy(done = done)
                },
                onEdit = {
                    editingEntry = entry
                },
            )
        }
        overlay is LegacyOverlay.Trash -> TrashScreen(
            vm = vm,
            onBack = { overlay = LegacyOverlay.Settings },
        )
        overlay is LegacyOverlay.WorkspaceEntries -> {
            val notebookId = (overlay as LegacyOverlay.WorkspaceEntries).notebookId
            val nb = notebooks.find { it.id == notebookId }
            LaunchedEffect(notebookId) { vm.loadWorkspaceEntries(notebookId) }
            WorkspaceEntriesScreen(
                notebookName = nb?.name ?: "工作区",
                entries = workspaceEntries,
                onBack = {
                    vm.clearWorkspaceEntries()
                    overlay = LegacyOverlay.WorkspaceDetail(notebookId)
                },
                onOpenEntry = { viewingEntry = it },
                onDeleteSelected = { vm.deleteEntries(it) },
            )
        }
        overlay is LegacyOverlay.WorkspaceDetail -> {
            val notebookId = (overlay as LegacyOverlay.WorkspaceDetail).notebookId
            val nb = notebooks.find { it.id == notebookId }
            if (nb == null) {
                LaunchedEffect(notebookId) { overlay = LegacyOverlay.Settings }
            } else {
                WorkspaceDetailScreen(
                    notebook = nb,
                    onBack = { overlay = LegacyOverlay.Settings },
                    onSave = { name, color -> vm.updateNotebook(nb.id, name, color) },
                    onManageEntries = {
                        overlay = LegacyOverlay.WorkspaceEntries(nb.id)
                    },
                )
            }
        }
        overlay is LegacyOverlay.Settings -> SettingsScreen(
            vm = vm,
            onBack = {
                vm.dismissRecallHits()
                overlay = null
            },
            onExport = onExport,
            onExportPlain = onExportPlain,
            onExportMarkdown = onExportMarkdown,
            onExportTxt = onExportTxt,
            onExportPdf = onExportPdf,
            onImport = onImport,
            onOpenTrash = { overlay = LegacyOverlay.Trash },
            onEditWorkspace = { nb ->
                overlay = LegacyOverlay.WorkspaceDetail(nb.id)
            },
        )
        else -> when (val top = stack.last()) {
            LegacyDest.Welcome -> WelcomeScreen(
                vm = vm,
                transcriber = transcriber,
                onOpenSettings = { overlay = LegacyOverlay.Settings },
                onOpenCalendar = { push(LegacyDest.Calendar) },
                onOpenDay = { openDay(it) },
                onOpenEntry = { viewingEntry = it },
                onEditEntry = { editingEntry = it },
                onDeleteEntry = { vm.deleteEntry(it.id) },
            )
            LegacyDest.Calendar -> CalendarScreen(
                vm = vm,
                transcriber = transcriber,
                onBack = { pop() },
                onOpenDay = { openDay(it) },
                onOpenEntry = { viewingEntry = it },
            )
            is LegacyDest.Day -> DayTimelineScreen(
                vm = vm,
                transcriber = transcriber,
                day = top.date,
                onBack = {
                    vm.closeDay()
                    pop()
                },
                onEntryClick = { viewingEntry = it },
            )
        }
    }
}

private sealed class LegacyOverlay {
    data object Settings : LegacyOverlay()
    data object Trash : LegacyOverlay()
    data class WorkspaceDetail(val notebookId: String) : LegacyOverlay()
    data class WorkspaceEntries(val notebookId: String) : LegacyOverlay()
}

private sealed class LegacyDest {
    data object Welcome : LegacyDest()
    data object Calendar : LegacyDest()
    data class Day(val date: LocalDate) : LegacyDest()
}
