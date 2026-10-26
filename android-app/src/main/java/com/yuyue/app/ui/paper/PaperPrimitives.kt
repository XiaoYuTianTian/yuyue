package com.yuyue.app.ui.paper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** 纸面脚手架：单栏、无卡片堆叠，留给核心屏幕使用�?*/
@Composable
fun PaperScaffold(
    title: String? = null,
    subtitle: String? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    bottomBar: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(Modifier.padding(contentPadding).weight(1f)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() },
                )
            }
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (title != null || subtitle != null) Spacer(Modifier.height(16.dp))
            content()
        }
        bottomBar?.invoke()
    }
}

@Composable
fun EventCapsule(
    timeLabel: String,
    title: String,
    support: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = timeLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (!support.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    support,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun PaperEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.CenterStart) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
