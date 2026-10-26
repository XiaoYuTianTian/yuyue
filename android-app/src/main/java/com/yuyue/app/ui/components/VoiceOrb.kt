package com.yuyue.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun VoiceOrbBar(
    listening: Boolean,
    busy: Boolean,
    enabled: Boolean = true,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit,
    onSwitchToText: () -> Unit,
    onAttachPhoto: (() -> Unit)? = null,
    statusMessage: String? = null,
) {
    val haptic = LocalHapticFeedback.current
    val pulse by rememberInfiniteTransition(label = "micPulse").animateFloat(
        initialValue = 1f,
        targetValue = if (listening) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scale",
    )
    val orbColor = if (listening) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    val onOrb = if (listening) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.88f))
            .padding(top = 8.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(92.dp)
                    .scale(if (listening) pulse else 1f)
                    .drawBehind {
                        drawCircle(
                            color = orbColor.copy(alpha = if (listening) 0.22f else 0.10f),
                            radius = size.minDimension / 2f,
                        )
                    }
                    .border(
                        width = 1.dp,
                        color = orbColor.copy(alpha = if (listening) 0.55f else 0.28f),
                        shape = CircleShape,
                    ),
            )
            Surface(
                modifier = Modifier
                    .size(72.dp)
                    .semantics {
                        contentDescription = if (listening) "松开结束" else "按住说话"
                        onClick {
                            if (listening) onHoldEnd() else onHoldStart()
                            true
                        }
                    }
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        detectTapGestures(
                            onPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onHoldStart()
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    onHoldEnd()
                                }
                            },
                        )
                    },
                shape = CircleShape,
                color = orbColor,
                shadowElevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = onOrb)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
            } else {
                Spacer(Modifier.size(48.dp))
            }
            Text(
                when {
                    !enabled -> "正在分析…"
                    busy && !listening -> "正在准备识别…"
                    listening -> "正在听… 松开结束"
                    !statusMessage.isNullOrBlank() -> statusMessage
                    else -> "按住说一句"
                },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            IconButton(
                onClick = onSwitchToText,
                enabled = enabled,
            ) {
                Icon(
                    Icons.Default.Keyboard,
                    contentDescription = "改用打字",
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
