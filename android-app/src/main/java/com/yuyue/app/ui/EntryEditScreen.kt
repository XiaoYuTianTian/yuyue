package com.yuyue.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yuyue.app.data.EntryRevision
import com.yuyue.app.data.RevisionRecord
import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.data.local.EntryRevisionEntity
import com.yuyue.app.data.local.NotebookEntity
import com.yuyue.app.ui.components.YuyueAlertDialog
import com.yuyue.app.ui.components.YuyueWheelTimeDialog
import com.yuyue.app.ui.theme.done
import com.yuyue.app.ui.theme.pending
import com.yuyue.app.ui.theme.yuyueTopBarColors
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class EntryEditResult(
    val title: String,
    val body: String,
    val done: Boolean,
    val startAtMillis: Long,
    val place: String?,
    val people: String?,
    val diaryType: String?,
    val notebookId: String? = null,
)

private val diaryTypes = listOf(
    "life" to "生活",
    "work" to "工作",
    "food" to "饮食",
    "meeting" to "会议",
    "travel" to "出行",
    "health" to "健康",
    "other" to "其他",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditScreen(
    entry: EntryEntity,
    notebooks: List<NotebookEntity> = emptyList(),
    onBack: () -> Unit,
    onSave: (EntryEditResult) -> Unit,
    onDelete: () -> Unit,
    saveStatus: String = "已保存",
    revisions: List<EntryRevisionEntity> = emptyList(),
    onRestoreRevision: (revisionId: String, asCopy: Boolean) -> Unit = { _, _ -> },
) {
    val zone = ZoneId.of("Asia/Shanghai")
    var title by remember(entry.id) { mutableStateOf(entry.title) }
    var body by remember(entry.id) { mutableStateOf(entry.body) }
    var done by remember(entry.id) { mutableStateOf(entry.done) }
    var place by remember(entry.id) { mutableStateOf(entry.place.orEmpty()) }
    var people by remember(entry.id) { mutableStateOf(entry.people.orEmpty()) }
    var diaryType by remember(entry.id) { mutableStateOf(entry.diaryType ?: "life") }
    var notebookId by remember(entry.id) { mutableStateOf(entry.notebookId) }
    val initial = Instant.ofEpochMilli(entry.startAtMillis).atZone(zone)
    var date by remember(entry.id) { mutableStateOf(initial.toLocalDate()) }
    var hour by remember(entry.id) { mutableIntStateOf(initial.hour) }
    var minute by remember(entry.id) { mutableIntStateOf(initial.minute) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var previewRevision by remember { mutableStateOf<EntryRevisionEntity?>(null) }
    var typeOpen by remember { mutableStateOf(false) }
    var notebookOpen by remember { mutableStateOf(false) }
    var savedFlash by remember { mutableStateOf(false) }

    fun startMillis(): Long =
        date.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()

    fun currentResult() = EntryEditResult(
        title = title.trim(),
        body = body.trim(),
        done = done,
        startAtMillis = startMillis(),
        place = place.trim().ifBlank { null },
        people = people.trim().ifBlank { null },
        diaryType = diaryType,
        notebookId = notebookId,
    )

    fun saveNow() {
        onSave(currentResult())
        savedFlash = true
    }

    LaunchedEffect(title, body, done, date, hour, minute, place, people, diaryType, notebookId) {
        delay(800)
        onSave(currentResult())
    }

    LaunchedEffect(savedFlash) {
        if (savedFlash) {
            delay(1200)
            savedFlash = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(if (savedFlash) "已保存" else "编辑 · $saveStatus")
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
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { saveNow() },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    Text(if (savedFlash) "已保存" else "保存")
                }
                OutlinedButton(
                    onClick = { showDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("删除")
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) {
                    Text(date.format(DateTimeFormatter.ofPattern("M月d日")))
                }
                OutlinedButton(onClick = { showTime = true }, modifier = Modifier.weight(1f)) {
                    Text("%02d:%02d".format(hour, minute))
                }
            }
            OutlinedTextField(
                value = place,
                onValueChange = { place = it },
                label = { Text("地点") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = people,
                onValueChange = { people = it },
                label = { Text("人物") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            ExposedDropdownMenuBox(expanded = typeOpen, onExpandedChange = { typeOpen = it }) {
                OutlinedTextField(
                    value = diaryTypes.find { it.first == diaryType }?.second ?: diaryType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("类型") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeOpen) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = typeOpen, onDismissRequest = { typeOpen = false }) {
                    diaryTypes.forEach { (id, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                diaryType = id
                                typeOpen = false
                            },
                        )
                    }
                }
            }
            if (notebooks.isNotEmpty()) {
                ExposedDropdownMenuBox(expanded = notebookOpen, onExpandedChange = { notebookOpen = it }) {
                    OutlinedTextField(
                        value = notebooks.find { it.id == notebookId }?.name ?: "记事本",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("记事本") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(notebookOpen) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = notebookOpen, onDismissRequest = { notebookOpen = false }) {
                        notebooks.forEach { nb ->
                            DropdownMenuItem(
                                text = { Text(nb.name) },
                                onClick = {
                                    notebookId = nb.id
                                    notebookOpen = false
                                },
                            )
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (done) "已做" else "未做",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (done) {
                        MaterialTheme.colorScheme.done
                    } else {
                        MaterialTheme.colorScheme.pending
                    },
                )
                Switch(
                    checked = done,
                    onCheckedChange = { done = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.done,
                        uncheckedThumbColor = MaterialTheme.colorScheme.pending,
                        uncheckedTrackColor = MaterialTheme.colorScheme.pending.copy(alpha = 0.38f),
                        uncheckedBorderColor = MaterialTheme.colorScheme.pending,
                    ),
                )
            }
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("正文") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
            )
            if (revisions.isNotEmpty()) {
                Text("版本历史", style = MaterialTheme.typography.titleMedium)
                revisions.forEach { row ->
                    val whenLabel = Instant.ofEpochMilli(row.createdAtMillis)
                        .atZone(zone)
                        .format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
                    TextButton(
                        onClick = { previewRevision = row },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "$whenLabel · ${revisionReasonLabel(row.reason)}",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (showDate) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        picker.selectedDateMillis?.let { ms ->
                            date = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDate = false
                    },
                ) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("取消") } },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            colors = androidx.compose.material3.DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            DatePicker(state = picker)
        }
    }

    if (showTime) {
        YuyueWheelTimeDialog(
            initialHour = hour,
            initialMinute = minute,
            onDismiss = { showTime = false },
            onConfirm = { h, m ->
                hour = h
                minute = m
                showTime = false
            },
        )
    }

    if (showDelete) {
        YuyueAlertDialog(
            onDismissRequest = { showDelete = false },
            title = "删除这条记录？",
            text = "会移到废纸篓，可在设置里恢复。",
            confirmLabel = "删除",
            onConfirm = {
                showDelete = false
                onDelete()
            },
            dismissLabel = "取消",
        )
    }

    previewRevision?.let { row ->
        val snap = remember(row.id) {
            EntryRevision.restore(
                RevisionRecord(row.id, row.entryId, row.snapshotJson, row.createdAtMillis, row.reason),
            )
        }
        YuyueAlertDialog(
            onDismissRequest = { previewRevision = null },
            title = "这一版",
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(snap.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        Instant.ofEpochMilli(snap.startAtMillis)
                            .atZone(zone)
                            .format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        snap.body.ifBlank { "（无正文）" },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            },
            buttons = {
                TextButton(onClick = { previewRevision = null }) { Text("关闭") }
                TextButton(
                    onClick = {
                        onRestoreRevision(row.id, true)
                        previewRevision = null
                    },
                ) { Text("另存为副本") }
                TextButton(
                    onClick = {
                        onRestoreRevision(row.id, false)
                        previewRevision = null
                    },
                ) { Text("恢复这一版") }
            },
        )
    }
}

private fun revisionReasonLabel(reason: String): String = when (reason) {
    "edit" -> "编辑"
    "import" -> "导入"
    "delete" -> "删除"
    "restore" -> "恢复"
    "restore_copy" -> "另存"
    else -> reason
}
