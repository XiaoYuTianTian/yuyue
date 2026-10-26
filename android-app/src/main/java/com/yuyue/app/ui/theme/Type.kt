package com.yuyue.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 标题用衬线建立纸面阅读感；正文用系统无衬线保证中文可读与动态字体。
 * 后续可替换为打包的书宋 / 思源，但不引入 Inter/Roboto 品牌感。
 */
private val DisplayFace = FontFamily.Serif
private val BodyFace = FontFamily.SansSerif

val YuyueTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFace,
        fontSize = 34.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.4).sp,
        lineHeight = 42.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFace,
        fontSize = 28.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.3).sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFace,
        fontSize = 22.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.2).sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFace,
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = BodyFace,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFace,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFace,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFace,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFace,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFace,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.3.sp,
    ),
)
