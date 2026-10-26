package com.yuyue.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.ui.components.YuyueAlertDialog
import com.yuyue.app.ui.theme.yuyueTopBarColors
import com.yuyue.app.util.TimeUtils
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceEntriesScreen(
    notebookName: String,
    entries: List<EntryEntity>,
    onBack: () -> Unit,
    onOpenEntry: (EntryEntity) -> Unit,
    onDeleteSelected: (List<String>) -> Unit,
) {
    var selected by remember { mutableStateOf(setOf<String>()) }
    var confirmDelete by remember { mutableStateOf(false) }
    val zone = remember { ZoneId.of("Asia/Shanghai") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selected.isEmpty()) notebookName
                        else "已选 ${selected.size}",
                    )
                },
                colors = yuyueTopBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (selected.isNotEmpty()) {
                        TextButton(onClick = { selected = emptySet() }) { Text("取消选择") }
                    }
                },
            )
        },
        bottomBar = {
            if (selected.isNotEmpty()) {
                Button(
                    onClick = { confirmDelete = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text("移入废纸篓")
                }
            }
        },
    ) { padding ->
        if (entries.isEmpty()) {
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "这个工作区还没有日记",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
            ) {
                items(entries, key = { it.id }) { entry ->
                    val checked = entry.id in selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = if (checked) selected - entry.id else selected + entry.id
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { on ->
                                selected = if (on) selected + entry.id else selected - entry.id
                            },
                        )
                        Column(
                            Modifier
                                .weight(1f)
                                .clickable { onOpenEntry(entry) },
                        ) {
                            Text(
                                entry.title.ifBlank { "（无标题）" },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${TimeUtils.millisToLocalDate(entry.startAtMillis, zone)} ${TimeUtils.formatTime(entry.startAtMillis, zone)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        YuyueAlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = "移入废纸篓？",
            text = "将删除选中的 ${selected.size} 则日记，可在废纸篓找回。",
            confirmLabel = "移入废纸篓",
            onConfirm = {
                onDeleteSelected(selected.toList())
                selected = emptySet()
                confirmDelete = false
            },
            dismissLabel = "取消",
        )
    }
}
