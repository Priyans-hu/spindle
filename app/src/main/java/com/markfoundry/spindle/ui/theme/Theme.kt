package com.markfoundry.spindle.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme()
private val DarkColors = darkColorScheme()
private val SpindleTypography = Typography()

/** Analog follows the system: Day = light, Night = dark (spec s1). */
@Composable
fun analogColors(): SkinColors = if (isSystemInDarkTheme()) AnalogNight else AnalogDay

@Composable
fun SpindleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SpindleTypography,
        content = content
    )
}
