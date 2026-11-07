package com.markfoundry.spindle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markfoundry.spindle.ui.theme.SkinColors
import com.markfoundry.spindle.ui.theme.SpindleDimens

/** Bottom tray: top-rounded, filled with the tray token, 1dp top edge (Night cream / Flux glass). */
fun Modifier.tray(colors: SkinColors, radius: Dp): Modifier =
    clip(RoundedCornerShape(topStart = radius, topEnd = radius))
        .background(colors.tray)
        .drawBehind {
            // The edge's sides fall outside the clip, leaving only the top arc.
            val r = radius.toPx()
            drawRoundRect(
                color = colors.trayEdge,
                topLeft = Offset(0f, 0.5.dp.toPx()),
                size = Size(size.width, size.height + r * 2),
                cornerRadius = CornerRadius(r),
                style = Stroke(1.dp.toPx()),
            )
        }

/** 64dp wordmark row under the status bar; its center lines up with the 48dp Looks button. */
@Composable
fun WordmarkRow(color: Color, style: TextStyle, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = SpindleDimens.Gutter),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text("Spindle", color = color, style = style)
    }
}

/** Circular transport button with a filled face and/or a ring; labelled for TalkBack. */
@Composable
fun TransportButton(
    label: String,
    onClick: () -> Unit,
    diameter: Dp,
    fill: Brush = SolidColor(Color.Transparent),
    ring: Color = Color.Transparent,
    ringWidth: Dp = 1.5.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(diameter)
            .clip(CircleShape)
            .background(fill)
            .border(ringWidth, ring, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
