package com.yuyue.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.yuyue.app.data.capture.isEntryPolishing
import com.yuyue.app.data.WelcomeFeed
import com.yuyue.app.data.WelcomeFilter
import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.speech.SpeechTranscriber
import com.yuyue.app.ui.components.CaptureQueueBar
import com.yuyue.app.ui.components.NotebookSegmentedControl
import com.yuyue.app.ui.components.ObserveYuyueToasts
import com.yuyue.app.ui.components.ProfileAvatar
import com.yuyue.app.ui.components.RecallResultCard
import com.yuyue.app.ui.components.TitleSavedToast
import com.yuyue.app.ui.components.VoiceCaptureDock
import com.yuyue.app.ui.components.WelcomeAtmosphere
import com.yuyue.app.ui.components.YuyueTopToastHost
import com.yuyue.app.ui.components.dayPartNow
import com.yuyue.app.ui.theme.done
import com.yuyue.app.ui.theme.parseNotebookColor
import com.yuyue.app.ui.theme.pending
import com.yuyue.app.util.TimeUtils
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun WelcomeScreen(
    vm: AppViewModel,
    transcriber: SpeechTranscriber,
    onOpenSettings: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onOpenEntry: (EntryEntity) -> Unit,
    onEditEntry: (EntryEntity) -> Unit,
    onDeleteEntry: (EntryEntity) -> Unit,
) {
    val notebooks by vm.notebooks.collectAsState()
    val selectedId by vm.selectedNotebookId.collectAsState()
    val feed by vm.welcomeFeed.collectAsState()
    val jobRunning by vm.jobRunning.collectAsState()
    val savedTitle by vm.savedTitle.collectAsState()
    val clarifyQuestion by vm.clarifyQuestion.collectAsState()
    val recallHits by vm.recallHits.collectAsState()
    val recallQuery by vm.recallQuery.collectAsState()
    val searchHits by vm.searchHits.collectAsState()
    val searchAll by vm.searchScopeAll.collectAsState()
    val searching by vm.searchOpen.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val pendingDraft by vm.pendingDraft.collectAsState()
    val profile by vm.profile.collectAsState()
    val backupNudge by vm.backupNudge.collectAsState()
    val onThisDay by vm.onThisDay.collectAsState()
    val pendingMutate by vm.pendingMutatePick.collectAsState()
    val voiceNonce by vm.voiceNonce.collectAsState()
    val composerFocusNonce by vm.composerFocusNonce.collectAsState()
    val captureQueue by vm.captureQueue.collectAsState()
    val analyzingLabel by vm.analyzingLabel.collectAsState()
    val mediaByEntryId by vm.mediaByEntryId.collectAsState()
    val zone = remember { ZoneId.of("Asia/Shanghai") }
    val today = remember { LocalDate.now(zone) }
    val dayPart = remember(LocalTime.now(zone).hour) { dayPartNow(zone) }

    var showAddNotebook by remember { mutableStateOf(false) }
    var newNotebookName by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(WelcomeFilter.All) }
    var longPressEntry by remember { mutableStateOf<EntryEntity?>(null) }
    var confirmDelete by remember { mutableStateOf<EntryEntity?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val visibleFeed = remember(feed, filter, today) {
        WelcomeFeed.applyFilter(feed, filter, today, zone)
    }
    val selectedNotebook = remember(notebooks, selectedId) {
        notebooks.find { it.id == selectedId }
    }
    val notebookAccent = remember(selectedNotebook?.color) {
        parseNotebookColor(selectedNotebook?.color)
    }

    BackHandler(enabled = searching) { vm.setSearchOpen(false) }

    LaunchedEffect(selectedId, searchAll, searching) {
        if (searching && searchQuery.isNotBlank()) vm.searchKeyword(searchQuery)
    }

    ObserveYuyueToasts(vm, snackbar)

    Box(Modifier.fillMaxSize()) {
        WelcomeAtmosphere(
            dayPart = dayPart,
            accent = notebookAccent,
            modifier = Modifier.fillMaxSize(),
        )

        Scaffold(
            snackbarHost = {},
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                if (searching) {
                    Column(Modifier.fillMaxWidth()) {
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
                    }
                } else {
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
                }
            },
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                    WelcomeHeader(
                        greeting = WelcomeFeed.greetingNow(zone),
                        displayName = profile.displayName,
                        avatarUri = profile.avatarUri,
                        searching = searching,
                        onCalendar = onOpenCalendar,
                        onSearch = { vm.setSearchOpen(!searching) },
                        onSettings = onOpenSettings,
                    )
                    NotebookSegmentedControl(
                        notebooks = notebooks,
                        selectedId = selectedId,
                        onSelect = { vm.selectNotebook(it) },
                        onAdd = { showAddNotebook = true },
                    )
                    if (!searching && onThisDay.isNotEmpty()) {
                        OnThisDayBanner(
                            entries = onThisDay,
                            onOpen = onOpenEntry,
                        )
                    }
                    if (searching) {
                        WelcomeSearchPanel(
                            query = searchQuery,
                            onQueryChange = { vm.setSearchQuery(it) },
                            searchAll = searchAll,
                            onToggleScope = { vm.setSearchScopeAll(!searchAll) },
                            hits = searchHits,
                            zone = zone,
                            today = today,
                            onSearch = { vm.searchKeyword(searchQuery) },
                            onOpenEntry = onOpenEntry,
                            onLongClickEntry = { longPressEntry = it },
                            onToggleDone = { vm.setEntryDone(it.id, !it.done) },
                            thumbs = mediaByEntryId.mapValues { it.value.firstOrNull()?.thumbPath },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    } else {
                    if (backupNudge) {
                        BackupNudgeBanner(
                            onOpenSettings = {
                                vm.dismissBackupNudge()
                                onOpenSettings()
                            },
                            onDismiss = { vm.dismissBackupNudge() },
                        )
                    }
                    val summary = remember(feed, today) {
                        WelcomeFeed.summary(feed, today, System.currentTimeMillis(), zone)
                    }
                    if (summary != null) {
                        WelcomeSummaryLine(
                            summary = summary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 2.dp),
                        )
                    }
                    WelcomeFilterChips(
                        selected = filter,
                        onSelect = { filter = it },
                    )
                    if (feed.isEmpty()) {
                        Box(
                            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "记下你的第一篇日记",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                TextButton(onClick = onOpenCalendar) {
                                    Text("翻翻月历")
                                }
                            }
                        }
                    } else if (visibleFeed.isEmpty()) {
                        Box(
                            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "这一档还空着",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    } else {
                        LazyColumn(
                            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                        ) {
                            items(visibleFeed, key = { it.id }) { entry ->
                                WelcomeFeedRow(
                                    entry = entry,
                                    zone = zone,
                                    today = today,
                                    thumbPath = mediaByEntryId[entry.id]?.firstOrNull()?.thumbPath,
                                    polishing = captureQueue.isEntryPolishing(entry.id),
                                    onClick = { onOpenEntry(entry) },
                                    onLongClick = { longPressEntry = entry },
                                    onToggleDone = { vm.setEntryDone(entry.id, !entry.done) },
                                )
                            }
                        }
                    }
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

    if (showAddNotebook) {
        com.yuyue.app.ui.components.YuyueAlertDialog(
            onDismissRequest = { showAddNotebook = false },
            title = "新记事本",
            confirmLabel = "添加",
            onConfirm = {
                vm.createNotebook(newNotebookName)
                newNotebookName = ""
                showAddNotebook = false
            },
            dismissLabel = "取消",
            confirmEnabled = newNotebookName.isNotBlank(),
        ) {
            OutlinedTextField(
                value = newNotebookName,
                onValueChange = { newNotebookName = it },
                label = { Text("名称") },
                singleLine = true,
            )
        }
    }

    longPressEntry?.let { entry ->
        com.yuyue.app.ui.components.YuyueAlertDialog(
            onDismissRequest = { longPressEntry = null },
            title = entry.title.ifBlank { "这条记录" },
            text = {
                Text(
                    "请选择操作",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            buttons = {
                TextButton(onClick = { longPressEntry = null }) { Text("取消") }
                TextButton(
                    onClick = {
                        longPressEntry = null
                        confirmDelete = entry
                    },
                ) { Text("删除") }
                TextButton(
                    onClick = {
                        longPressEntry = null
                        onEditEntry(entry)
                    },
                ) { Text("编辑") }
            },
        )
    }

    confirmDelete?.let { entry ->
        com.yuyue.app.ui.components.YuyueAlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = "删除这条记录？",
            text = "会移到废纸篓，可在设置里恢复。",
            confirmLabel = "删除",
            onConfirm = {
                onDeleteEntry(entry)
                confirmDelete = null
            },
            dismissLabel = "取消",
        )
    }
}

@Composable
private fun WelcomeFilterChips(
    selected: WelcomeFilter,
    onSelect: (WelcomeFilter) -> Unit,
) {
    val items = listOf(
        WelcomeFilter.Next to "接下来",
        WelcomeFilter.Recent to "最近",
        WelcomeFilter.All to "全部",
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            items.forEach { (value, label) ->
                val on = value == selected
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (on) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            else Color.Transparent,
                        )
                        .clickable { onSelect(value) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (on) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.92f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OnThisDayBanner(
    entries: List<EntryEntity>,
    onOpen: (EntryEntity) -> Unit,
) {
    val zone = ZoneId.of("Asia/Shanghai")
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(
                "那年今日",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            entries.take(4).forEach { entry ->
                val year = TimeUtils.millisToLocalDate(entry.startAtMillis, zone).year
                Text(
                    "$year · ${entry.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(entry) }
                        .padding(vertical = 6.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BackupNudgeBanner(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "日记只在本机，可去设置导出备份",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenSettings) { Text("去设置") }
            TextButton(onClick = onDismiss) { Text("知道了") }
        }
    }
}

@Composable
private fun WelcomeSummaryLine(
    summary: WelcomeFeed.Summary,
    modifier: Modifier = Modifier,
) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
    val ink = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f)
    val accent = MaterialTheme.colorScheme.primary
    Column(modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        if (summary.todayPending > 0) {
            Text(
                buildAnnotatedString {
                    append("今天还有 ")
                    withStyle(
                        SpanStyle(
                            color = accent,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    ) {
                        append("${summary.todayPending}")
                    }
                    append(" 件未完成")
                },
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
        }
        val nextWhen = summary.nextWhen
        val nextTitle = summary.nextTitle
        if (nextWhen != null && nextTitle != null) {
            Text(
                buildAnnotatedString {
                    append("下一项  ")
                    withStyle(SpanStyle(color = ink)) {
                        append(nextWhen)
                    }
                    append("  ")
                    withStyle(SpanStyle(color = ink)) {
                        append(nextTitle)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else if (summary.todayPending <= 0 && summary.recentCount != null) {
            Text(
                "最近 ${summary.recentCount} 则记录",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
        }
    }
}

@Composable
private fun WelcomeHeader(
    greeting: String,
    displayName: String,
    avatarUri: String?,
    searching: Boolean,
    onCalendar: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val searchTint = if (searching) MaterialTheme.colorScheme.primary else muted
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProfileAvatar(
                displayName = displayName,
                avatarUri = avatarUri,
                size = 44.dp,
            )
            Column {
                Text(greeting, style = MaterialTheme.typography.displayLarge, color = ink)
                Text(
                    displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted,
                )
            }
        }
        Row {
            IconButton(onClick = onCalendar) {
                Icon(Icons.Default.DateRange, contentDescription = "月历", tint = muted)
            }
            IconButton(onClick = onSearch) {
                Icon(
                    if (searching) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (searching) "关闭查找" else "查找",
                    tint = searchTint,
                )
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = "设置", tint = muted)
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun WelcomeSearchPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    searchAll: Boolean,
    onToggleScope: () -> Unit,
    hits: List<EntryEntity>?,
    zone: ZoneId,
    today: LocalDate,
    onSearch: () -> Unit,
    onOpenEntry: (EntryEntity) -> Unit,
    onLongClickEntry: (EntryEntity) -> Unit,
    onToggleDone: (EntryEntity) -> Unit,
    thumbs: Map<String, String?> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (hits == null) focusRequester.requestFocus()
    }
    LaunchedEffect(query, searchAll) {
        if (query.isBlank()) return@LaunchedEffect
        delay(320)
        onSearch()
    }
    Column(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                label = { Text("关键词、人物、地点") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = !searchAll,
                    onClick = { if (searchAll) onToggleScope() },
                    label = { Text("当前") },
                )
                FilterChip(
                    selected = searchAll,
                    onClick = { if (!searchAll) onToggleScope() },
                    label = { Text("全部") },
                )
            }
        }
        when {
            hits == null -> Box(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "输入后自动查找",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            hits.isEmpty() -> Box(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "没有找到相关日记",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            else -> LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            ) {
                items(hits, key = { it.id }) { entry ->
                    WelcomeFeedRow(
                        entry = entry,
                        zone = zone,
                        today = today,
                        thumbPath = thumbs[entry.id],
                        polishing = false,
                        onClick = { onOpenEntry(entry) },
                        onLongClick = { onLongClickEntry(entry) },
                        onToggleDone = { onToggleDone(entry) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WelcomeFeedRow(
    entry: EntryEntity,
    zone: ZoneId,
    today: LocalDate,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleDone: () -> Unit,
    thumbPath: String? = null,
    polishing: Boolean = false,
) {
    val day = TimeUtils.millisToLocalDate(entry.startAtMillis, zone)
    val isToday = day == today
    val ink = if (entry.done) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onBackground
    }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.copy(
        alpha = if (entry.done) 0.72f else 1f,
    )
    val dayColor = when {
        entry.done -> muted
        isToday -> MaterialTheme.colorScheme.primary
        else -> muted
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.width(52.dp)) {
            Text(
                WelcomeFeed.shortDayLabel(day, today),
                style = MaterialTheme.typography.labelLarge,
                color = dayColor,
                fontWeight = if (isToday && !entry.done) FontWeight.SemiBold else FontWeight.Medium,
            )
            Text(
                TimeUtils.formatTime(entry.startAtMillis, zone),
                style = MaterialTheme.typography.labelLarge,
                color = muted,
            )
        }
        if (!thumbPath.isNullOrBlank()) {
            com.yuyue.app.ui.components.LocalFileImage(
                path = thumbPath,
                contentDescription = "照片",
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp)),
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
                    .background(
                        if (entry.done) MaterialTheme.colorScheme.done else MaterialTheme.colorScheme.pending,
                    ),
            )
        }
    }
}
