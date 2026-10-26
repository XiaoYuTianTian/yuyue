package com.yuyue.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yuyue.app.ui.theme.Signal
import kotlinx.coroutines.delay

private val ToastShape = RoundedCornerShape(999.dp)

@Composable
fun TitleSavedToast(
    title: String,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(title) {
        delay(2800)
        onDismiss()
    }
    Surface(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .widthIn(max = 340.dp)
            .fillMaxWidth(),
        shape = ToastShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
    ) {
        Column(
            Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "已记下",
                style = MaterialTheme.typography.labelLarge,
                color = Signal.copy(alpha = 0.9f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
