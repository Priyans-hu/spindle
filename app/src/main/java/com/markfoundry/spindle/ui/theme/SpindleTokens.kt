package com.markfoundry.spindle.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Raw palettes: one `val Name = Color(0x...)` per line so design/contrast.py can parse them.
// Ratios in comments are WCAG vs the role's background (spec s2).

object AnalogDayPalette {
    val Ground = Color(0xFFE8645A)
    val Tray = Color(0xFF2B356A)
    val TrayEdge = Color.Transparent
    val Text = Color(0xFFF2E7D0)       // 9.40 on Tray
    val Text2 = Color(0xFFE3D3B2)      // 7.82 on Tray
    val Ink = Color(0xFF151A3A)        // 5.16 on Ground
    val Accent = Color(0xFFF07468)     // 4.07 on Tray (UI part)
    val PlayFill = Color(0xFFB8352B)
    val PlayIcon = Color(0xFFF2E7D0)   // 4.78 on PlayFill
    val PlayRim = Color(0xFFF2E7D0)    // 9.40 on Tray, carries the 1.97 fill
    val SeekTrack = Color(0xFF8490D8)  // 3.83 on Tray
    val SeekThumb = Color(0xFFF2E7D0)  // 9.40 on Tray
    val CtaFill = Color(0xFF2B356A)
    val CtaLabel = Color(0xFFF2E7D0)   // 9.40 on CtaFill
    val Shadow = Color(0x4D151A3A)     // print shadow, 30%
    val DiscRim = Color.Transparent
}

object AnalogNightPalette {
    val Ground = Color(0xFF10132A)
    val Tray = Color(0xFF1C2142)
    val TrayEdge = Color(0x1AF2E7D0)   // cream 10%
    val Text = Color(0xFFF2E7D0)       // 12.72 on Tray
    val Text2 = Color(0xFFC9BFA6)      // 8.54 on Tray
    val Ink = Color(0xFFF2E7D0)        // 14.90 on Ground
    val Accent = Color(0xFFFF7A6E)     // 6.14 on Tray
    val PlayFill = Color(0xFFFF7A6E)   // 6.14 on Tray
    val PlayIcon = Color(0xFF141836)   // 6.81 on PlayFill
    val PlayRim = Color.Transparent
    val SeekTrack = Color(0xFF8E8A9E)  // 4.67 on Tray
    val SeekThumb = Color(0xFFF2E7D0)  // 12.72 on Tray
    val CtaFill = Color(0xFFFF7A6E)
    val CtaLabel = Color(0xFF141836)   // 6.81 on CtaFill
    val Shadow = Color.Transparent
    val DiscRim = Color(0x1FF2E7D0)    // cream 12%
}

object FluxPalette {
    val Ground = Color(0xFF07080F)
    val GroundTop = Color(0xFF10132A)
    val Tray = Color(0xE01A2040)       // glass 88%, floor 85%
    val TrayEdge = Color(0x24FFFFFF)   // white 14%
    val Text = Color(0xFFEAF2FF)       // 9.74 worst (glass over white art)
    val Text2 = Color(0xFFA3AECB)      // 4.95 worst, 7.43 over Ground
    val Ink = Color(0xFFEAF2FF)
    val Accent = Color(0xFF3FE6F2)     // 12.44
    val Violet = Color(0xFF8B6BFF)
    val Magenta = Color(0xFFFF4D8D)
    val PlayIcon = Color(0xFF07080F)   // >= 5.37 on the foil stops
    val SeekTrack = Color(0xFF8A96C6)  // 3.79 worst, 5.69 over Ground
    val SeekThumb = Color(0xFFEAF2FF)
    val CtaFill = Color(0xFF3FE6F2)
    val CtaLabel = Color(0xFF07080F)   // 13.15 on CtaFill
}

/** Fixed record parts shared by every Analog variant (not text, no ratio claims). */
object DeckPalette {
    val Vinyl = Color(0xFF17130F)
    val Groove = Color(0xFF241C17)
    val ArmMetal = Color(0xFFD9D5CC)
    val ArmAccent = Color(0xFF33429C)
    val LabelA = Color(0xFF2F5F96)
    val LabelB = Color(0xFFD7823F)
}

/** Color roles a skin reads; never hard-code a hex outside ui/theme. */
@Immutable
data class SkinColors(
    val ground: Color,
    val tray: Color,
    val trayEdge: Color,
    val text: Color,
    val text2: Color,
    val ink: Color,
    val accent: Color,
    val playFill: Color,
    val playIcon: Color,
    val playRim: Color,
    val seekTrack: Color,
    val seekThumb: Color,
    val ctaFill: Color,
    val ctaLabel: Color,
    val shadow: Color,
    val discRim: Color,
)

val AnalogDay = with(AnalogDayPalette) {
    SkinColors(Ground, Tray, TrayEdge, Text, Text2, Ink, Accent, PlayFill, PlayIcon, PlayRim, SeekTrack, SeekThumb, CtaFill, CtaLabel, Shadow, DiscRim)
}

val AnalogNight = with(AnalogNightPalette) {
    SkinColors(Ground, Tray, TrayEdge, Text, Text2, Ink, Accent, PlayFill, PlayIcon, PlayRim, SeekTrack, SeekThumb, CtaFill, CtaLabel, Shadow, DiscRim)
}

val Flux = with(FluxPalette) {
    SkinColors(Ground, Tray, TrayEdge, Text, Text2, Ink, Accent, Accent, PlayIcon, Color.Transparent, SeekTrack, SeekThumb, CtaFill, CtaLabel, Color.Transparent, Color.Transparent)
}

object SpindleDimens {
    val Gutter = 24.dp
    val TrayPadding = 24.dp
    val TransportGap = 24.dp
    val Play = 72.dp
    val PrevNext = 56.dp
    val Looks = 48.dp
    val SeekRow = 48.dp
    val SeekTrack = 4.dp
    val Thumb = 16.dp
    val ThumbPressed = 22.dp
    val StaticMarker = 8.dp
    val ChipRadius = 12.dp
    val AnalogTrayRadius = 28.dp
    val FluxTrayRadius = 32.dp
    val Cta = 56.dp
    val PrintShadow = 4.dp
    val PlayRim = 2.dp
}
