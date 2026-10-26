package com.yuyue.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.yuyue.app.ui.theme.Pulse
import com.yuyue.app.ui.theme.Signal
import java.time.LocalTime
import java.time.ZoneId

enum class DayPart { Morning, Afternoon, Evening }

fun dayPartForHour(hour: Int): DayPart = when {
    hour < 12 -> DayPart.Morning
    hour < 18 -> DayPart.Afternoon
    else -> DayPart.Evening
}

fun dayPartNow(zone: ZoneId): DayPart =
    dayPartForHour(LocalTime.now(zone).hour)

/**
 * 欢迎页时段氛围：顶部淡光；可叠一层记事本主色，切换工作区更明显。
 */
@Composable
fun WelcomeAtmosphere(
    dayPart: DayPart,
    accent: Color? = null,
    modifier: Modifier = Modifier,
) {
    val base = MaterialTheme.colorScheme.background
    val palette = remember(dayPart) { atmospherePalette(dayPart) }
    val breath = rememberInfiniteTransition(label = "welcomeBreath")
    val glowPulse by breath.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(6400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(base)

        val primary = accent ?: palette.primary
        val secondary = accent ?: palette.secondary

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    primary.copy(alpha = if (accent != null) 0.18f * glowPulse else 0.10f * glowPulse),
                    primary.copy(alpha = if (accent != null) 0.06f else 0.03f),
                    Color.Transparent,
                ),
                center = Offset(w * palette.primaryX, h * -0.02f),
                radius = w * 0.85f,
            ),
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    secondary.copy(alpha = if (accent != null) 0.10f else 0.05f),
                    Color.Transparent,
                ),
                center = Offset(w * palette.secondaryX, h * 0.12f),
                radius = w * 0.55f,
            ),
        )
        if (accent != null) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = h * 0.45f,
                ),
            )
        }
    }
}

private data class AtmospherePalette(
    val primary: Color,
    val secondary: Color,
    val primaryX: Float,
    val secondaryX: Float,
)

private fun atmospherePalette(part: DayPart): AtmospherePalette = when (part) {
    DayPart.Morning -> AtmospherePalette(
        primary = Color(0xFFFFC28A),
        secondary = Color(0xFF7EB6FF),
        primaryX = 0.18f,
        secondaryX = 0.82f,
    )
    DayPart.Afternoon -> AtmospherePalette(
        primary = Pulse,
        secondary = Signal,
        primaryX = 0.5f,
        secondaryX = 0.2f,
    )
    DayPart.Evening -> AtmospherePalette(
        primary = Color(0xFF4A6FA5),
        secondary = Signal,
        primaryX = 0.78f,
        secondaryX = 0.25f,
    )
}
