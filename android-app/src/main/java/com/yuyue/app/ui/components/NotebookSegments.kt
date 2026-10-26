package com.yuyue.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yuyue.app.data.local.NotebookEntity
import com.yuyue.app.ui.theme.parseNotebookColor

@Composable
fun NotebookSegmentedControl(
    notebooks: List<NotebookEntity>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (notebooks.isEmpty()) return
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        tonalElevation = 1.dp,
    ) {
        Row(
            Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            notebooks.forEach { nb ->
                val selected = nb.id == selectedId
                val nbColor = parseNotebookColor(nb.color)
                val bg = when {
                    selected -> nbColor.copy(alpha = 0.22f)
                    else -> Color.Transparent
                }
                Row(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .clickable { onSelect(nb.id) }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(if (selected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(nbColor),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        nb.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAdd)
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
