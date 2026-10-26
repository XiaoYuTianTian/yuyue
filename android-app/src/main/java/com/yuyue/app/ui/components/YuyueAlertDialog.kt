package com.yuyue.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Soft dialog surface — same language as the home toast capsule. */
private val DialogShape = RoundedCornerShape(20.dp)

@Composable
fun YuyueAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String? = null,
    confirmLabel: String,
    onConfirm: () -> Unit,
    dismissLabel: String = "取消",
    confirmEnabled: Boolean = true,
    content: (@Composable () -> Unit)? = null,
) {
    YuyueAlertDialog(
        onDismissRequest = onDismissRequest,
        title = title,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (text != null) {
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                }
                content?.invoke()
            }
        },
        buttons = {
            TextButton(onClick = onDismissRequest) { Text(dismissLabel) }
            TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmLabel) }
        },
    )
}

@Composable
fun YuyueAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: @Composable (() -> Unit)? = null,
    buttons: @Composable RowScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = DialogShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        title = {
            Text(title, style = MaterialTheme.typography.titleMedium)
        },
        text = text,
        confirmButton = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                content = buttons,
            )
        },
    )
}
