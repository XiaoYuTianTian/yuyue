package com.yuyue.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun TextComposerBar(
    text: String,
    onTextChange: (String) -> Unit,
    enabled: Boolean,
    analyzing: Boolean,
    clarifyQuestion: String?,
    pendingDraft: com.yuyue.app.ui.PendingDraft? = null,
    analyzingHint: String? = null,
    analyzingLabel: String = "稍等片刻",
    onSwitchToVoice: () -> Unit,
    onAttachPhoto: (() -> Unit)? = null,
    hasImages: Boolean = false,
    onSubmit: () -> Unit,
    onSaveNow: (() -> Unit)? = null,
    onCancelPending: (() -> Unit)? = null,
    requestFocus: Boolean = true,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(requestFocus, enabled) {
        if (requestFocus && enabled) {
            focus.requestFocus()
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        if (analyzing) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                AnalyzingBlock(hint = analyzingHint, title = analyzingLabel)
            }
            return@Column
        }

        if (!clarifyQuestion.isNullOrBlank()) {
            Text(
                clarifyQuestion,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp, end = 4.dp),
            )
        }
        SlotChips(pendingDraft)
        if (onSaveNow != null && pendingDraft != null || onCancelPending != null && !clarifyQuestion.isNullOrBlank()) {
            Row(
                Modifier.padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onCancelPending != null && !clarifyQuestion.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = onCancelPending,
                        enabled = enabled,
                    ) { Text("取消") }
                }
                if (onSaveNow != null && pendingDraft != null) {
                    OutlinedButton(
                        onClick = onSaveNow,
                        enabled = enabled && pendingDraft.entries.isNotEmpty(),
                    ) { Text("先记下") }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(
                onClick = onSwitchToVoice,
                enabled = enabled,
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "改回语音",
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (onAttachPhoto != null) {
                IconButton(onClick = onAttachPhoto, enabled = enabled) {
                    Icon(
                        Icons.Default.AddAPhoto,
                        contentDescription = "添加照片",
                        tint = if (enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
                enabled = enabled,
                singleLine = false,
                minLines = 2,
                maxLines = 6,
                placeholder = { Text("要记的事…") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
            )
            Button(
                onClick = onSubmit,
                enabled = enabled && (text.isNotBlank() || hasImages),
            ) {
                Text("发送")
            }
        }
    }
}
