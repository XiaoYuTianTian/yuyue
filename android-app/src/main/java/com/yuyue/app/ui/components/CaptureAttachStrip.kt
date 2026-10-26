package com.yuyue.app.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun CaptureAttachStrip(
    uris: List<Uri>,
    onRemove: (Uri) -> Unit,
    onSendPhotosOnly: () -> Unit,
) {
    if (uris.isEmpty()) return
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        uris.forEach { uri ->
            Box(Modifier.size(56.dp)) {
                LocalFileImage(
                    path = uri.toString(),
                    contentDescription = "待发送照片",
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                )
                IconButton(
                    onClick = { onRemove(uri) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    ),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "去掉这张", modifier = Modifier.size(12.dp))
                }
            }
        }
        FilledTonalButton(
            onClick = onSendPhotosOnly,
            shape = CircleShape,
        ) {
            Text("发送照片")
        }
    }
}
