package com.yuyue.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yuyue.app.data.NotebookAccent
import com.yuyue.app.data.local.NotebookEntity
import com.yuyue.app.ui.theme.parseNotebookColor
import com.yuyue.app.ui.theme.yuyueTopBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceDetailScreen(
    notebook: NotebookEntity,
    onBack: () -> Unit,
    onSave: (name: String, color: String?) -> Unit,
    onManageEntries: () -> Unit,
) {
    var name by remember(notebook.id) { mutableStateOf(notebook.name) }
    var color by remember(notebook.id) { mutableStateOf(notebook.color) }
    val palette = NotebookAccent.All

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("编辑工作区") },
                colors = yuyueTopBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "颜色",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                palette.chunked(8).forEach { rowColors ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowColors.forEach { hex ->
                            val selected = (color ?: notebook.color).equals(hex, ignoreCase = true)
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(parseNotebookColor(hex))
                                    .clickable { color = hex },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected) {
                                    Box(
                                        Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.92f)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Button(
                onClick = { onSave(name.trim(), color) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("保存")
            }
            OutlinedButton(
                onClick = onManageEntries,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("管理日记")
            }
        }
    }
}
