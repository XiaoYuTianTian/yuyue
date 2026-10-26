package com.yuyue.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yuyue.app.R
import com.yuyue.app.ui.components.WelcomeAtmosphere
import com.yuyue.app.ui.components.dayPartNow
import java.time.LocalTime
import java.time.ZoneId

/** 首次启动教学页（示意语音，不录音）。 */
@Composable
fun FirstLaunchScreen(
    onStart: () -> Unit,
    onMicHint: () -> Unit,
) {
    val zone = remember { ZoneId.of("Asia/Shanghai") }
    val dayPart = remember(LocalTime.now(zone).hour) { dayPartNow(zone) }

    Box(Modifier.fillMaxSize()) {
        WelcomeAtmosphere(dayPart = dayPart, modifier = Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(0.28f))
            Box(
                Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_yuyue_logo),
                    contentDescription = "语曰",
                    modifier = Modifier.size(64.dp),
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "语曰",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "用声音留住回忆",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(0.38f))
            FilledIconButton(
                onClick = onMicHint,
                modifier = Modifier.size(88.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "语音示意",
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "按住说一句就能记",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(0.42f))
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text("开始使用")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
