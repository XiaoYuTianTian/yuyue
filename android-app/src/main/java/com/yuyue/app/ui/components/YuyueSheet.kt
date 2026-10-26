package com.yuyue.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SheetDragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.height(4.dp).fillMaxWidth(0.12f),
            shape = RoundedCornerShape(2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
        ) {}
    }
}
