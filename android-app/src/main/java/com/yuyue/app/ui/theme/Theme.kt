package com.yuyue.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Moss,
    onPrimary = Color.White,
    primaryContainer = MossMist,
    onPrimaryContainer = MossDeep,
    secondary = Clay,
    onSecondary = Color.White,
    secondaryContainer = ClayMist,
    onSecondaryContainer = ClayDeep,
    tertiary = Hazard,
    onTertiary = Color.White,
    tertiaryContainer = HazardMist,
    onTertiaryContainer = Color(0xFF5C241C),
    background = Paper,
    onBackground = Ink,
    surface = PaperRaised,
    onSurface = Ink,
    surfaceVariant = PaperDim,
    onSurfaceVariant = InkMute,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = PaperRaised,
    surfaceContainer = Paper,
    surfaceContainerHigh = PaperDim,
    surfaceContainerHighest = Color(0xFFD5DAD2),
    outline = Color(0xFFC5CBC2),
    outlineVariant = Color(0xFFDCE1D9),
    error = Hazard,
    onError = Color.White,
    inverseSurface = Ink,
    inverseOnSurface = Paper,
    inversePrimary = MossMist,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FBF9F),
    onPrimary = MossDeep,
    primaryContainer = MossDeep,
    onPrimaryContainer = MossMist,
    secondary = Color(0xFFD4B09A),
    onSecondary = ClayDeep,
    secondaryContainer = Color(0xFF4A3428),
    onSecondaryContainer = ClayMist,
    tertiary = Color(0xFFE08B7E),
    onTertiary = Color(0xFF3B1210),
    tertiaryContainer = Color(0xFF5C241C),
    onTertiaryContainer = HazardMist,
    background = NightPaper,
    onBackground = NightInk,
    surface = NightRaised,
    onSurface = NightInk,
    surfaceVariant = NightHigh,
    onSurfaceVariant = NightMute,
    surfaceContainerLowest = Color(0xFF0C0F0C),
    surfaceContainerLow = NightPaper,
    surfaceContainer = NightRaised,
    surfaceContainerHigh = NightHigh,
    surfaceContainerHighest = Color(0xFF323A33),
    outline = Color(0xFF3E473F),
    outlineVariant = Color(0xFF2A322B),
    error = Color(0xFFE08B7E),
    onError = Color(0xFF3B1210),
    inverseSurface = NightInk,
    inverseOnSurface = NightPaper,
    inversePrimary = Moss,
)

@Composable
fun YuyueTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = darkTheme ?: systemDark
    val colors = if (useDark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDark
                isAppearanceLightNavigationBars = !useDark
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = YuyueTypography,
        shapes = YuyueShapes,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun yuyueTopBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.background,
    titleContentColor = MaterialTheme.colorScheme.onBackground,
    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
)
