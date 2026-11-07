package com.markfoundry.spindle.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.markfoundry.spindle.R

/** Type scale (spec s2). Floor is 12sp everywhere in the app. */
@Immutable
data class SpindleType(
    val display: TextStyle,
    val title: TextStyle,
    val artist: TextStyle,
    val meta: TextStyle,
    val time: TextStyle,
    val wordmark: TextStyle,
    val button: TextStyle,
)

@OptIn(ExperimentalTextApi::class)
private fun variableFamily(resId: Int): FontFamily =
    if (!SpindleFlags.BUNDLED_FONTS) {
        FontFamily.SansSerif
    } else {
        FontFamily(
            listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map {
                Font(resId, it, variationSettings = FontVariation.Settings(FontVariation.weight(it.weight)))
            },
        )
    }

val JostFamily = variableFamily(R.font.jost)
val SoraFamily = variableFamily(R.font.sora)

private fun scale(family: FontFamily, displayWeight: FontWeight, titleTracking: Boolean) = SpindleType(
    display = TextStyle(fontFamily = family, fontWeight = displayWeight, fontSize = 28.sp, lineHeight = 34.sp),
    title = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = if (titleTracking) (-0.01).em else 0.em,
    ),
    artist = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
    meta = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    time = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum",
    ),
    wordmark = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
    ),
    button = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
)

val AnalogType = scale(JostFamily, FontWeight.Bold, titleTracking = true)
val FluxType = scale(SoraFamily, FontWeight.Medium, titleTracking = false)
