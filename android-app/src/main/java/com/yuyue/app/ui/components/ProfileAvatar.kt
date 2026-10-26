package com.yuyue.app.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Home / settings avatar. Uses a user-picked image when present; otherwise a
 * default monogram circle colored from the display name.
 */
@Composable
fun ProfileAvatar(
    displayName: String,
    avatarUri: String?,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val label = remember(displayName) {
        displayName.trim().take(1).ifBlank { "语" }
    }
    val tint = remember(displayName) { avatarColor(displayName) }
    val bitmap = remember(avatarUri) {
        if (avatarUri.isNullOrBlank()) {
            null
        } else {
            runCatching {
                when {
                    avatarUri.startsWith("/") || avatarUri.startsWith("file:") -> {
                        val path = if (avatarUri.startsWith("file:")) {
                            Uri.parse(avatarUri).path
                        } else {
                            avatarUri
                        }
                        path?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
                    }
                    else -> {
                        context.contentResolver.openInputStream(Uri.parse(avatarUri))?.use { stream ->
                            BitmapFactory.decodeStream(stream)?.asImageBitmap()
                        }
                    }
                }
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "头像",
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier
                .size(size)
                .clip(CircleShape)
                .background(tint),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
        }
    }
}

private fun avatarColor(name: String): Color {
    val palette = listOf(
        Color(0xFF0D9488),
        Color(0xFF5B9DFF),
        Color(0xFFE07A5F),
        Color(0xFF7C6A9A),
        Color(0xFF3D6B4F),
        Color(0xFFC45C4A),
        Color(0xFF4A6FA5),
    )
    val idx = (name.hashCode().and(0x7fffffff)) % palette.size
    return palette[idx]
}
