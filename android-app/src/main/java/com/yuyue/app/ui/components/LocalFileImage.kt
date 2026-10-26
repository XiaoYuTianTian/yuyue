package com.yuyue.app.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import java.io.File

@Composable
fun LocalFileImage(
    path: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val bitmap = remember(path) {
        if (path.isNullOrBlank()) {
            null
        } else {
            runCatching {
                when {
                    path.startsWith("/") || File(path).exists() ->
                        BitmapFactory.decodeFile(path)?.asImageBitmap()
                    else -> context.contentResolver.openInputStream(Uri.parse(path))?.use {
                        BitmapFactory.decodeStream(it)?.asImageBitmap()
                    }
                }
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}
