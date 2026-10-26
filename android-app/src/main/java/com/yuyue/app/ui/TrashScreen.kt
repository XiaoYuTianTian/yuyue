package com.yuyue.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.ui.components.ObserveYuyueToasts
import com.yuyue.app.ui.components.YuyueAlertDialog
import com.yuyue.app.ui.components.YuyueTopToastHost
import com.yuyue.app.ui.theme.yuyueTopBarColors
import com.yuyue.app.util.TimeUtils
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
) {
    val trash by vm.trashEntries.collectAsState()
    val zone = ZoneId.of("Asia/Shanghai")
    val snackbar = remember { SnackbarHostState() }
    var confirmPurgeAll by remember { mutableStateOf(false) }
    var confirmPurgeId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { vm.loadTrash() }
    ObserveYuyueToasts(vm, snackbar)

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {},
            topBar = {
                TopAppBar(
                    title = { Text("废纸篓") },
                    colors = yuyueTopBarColors(),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        if (trash.isNotEmpty()) {
                            TextButton(onClick = { confirmPurgeAll = true }) {
                                Text("清空")
                            }
                        }
                    },
                )
            },
        ) { padding ->
            if (trash.isEmpty()) {
                Box(
                    Modifier.padding(padding).fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("暂无已删除记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    Modifier.padding(padding).fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(trash, key = { it.id }) { e ->
                        TrashEntryCard(
                            e,
                            zone,
                            onRestore = { vm.restoreTrashEntry(e.id) },
                            onPurge = { confirmPurgeId = e.id },
                        )
                    }
                }
            }
        }
        YuyueTopToastHost(snackbar)
    }

    if (confirmPurgeAll) {
        YuyueAlertDialog(
            onDismissRequest = { confirmPurgeAll = false },
            title = "清空废纸篓？",
            text = "将永久删除全部 ${trash.size} 则，无法撤销。",
            confirmLabel = "永久删除",
            onConfirm = {
                vm.purgeAllTrash()
                confirmPurgeAll = false
            },
            dismissLabel = "取消",
        )
    }
    confirmPurgeId?.let { id ->
        YuyueAlertDialog(
            onDismissRequest = { confirmPurgeId = null },
            title = "永久删除？",
            text = "这条记录将从本机彻底删除，无法撤销。",
            confirmLabel = "永久删除",
            onConfirm = {
                vm.purgeTrashEntry(id)
                confirmPurgeId = null
            },
            dismissLabel = "取消",
        )
    }
}

@Composable
private fun TrashEntryCard(
    entry: EntryEntity,
    zone: ZoneId,
    onRestore: () -> Unit,
    onPurge: () -> Unit,
) {
    Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(entry.title, style = MaterialTheme.typography.titleMedium)
            if (entry.body.isNotBlank()) Text(entry.body, maxLines = 2)
            Text(
                TimeUtils.formatTime(entry.startAtMillis, zone),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row {
                TextButton(onClick = onRestore) { Text("恢复") }
                TextButton(onClick = onPurge) { Text("永久删除") }
            }
        }
    }
}
