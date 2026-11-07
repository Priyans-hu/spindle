package com.markfoundry.spindle.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markfoundry.spindle.R
import com.markfoundry.spindle.skin.AllSkins
import com.markfoundry.spindle.skin.FluxSkin
import com.markfoundry.spindle.skin.PlayerSkin
import com.markfoundry.spindle.ui.theme.AnalogDayPalette
import com.markfoundry.spindle.ui.theme.DeckPalette
import com.markfoundry.spindle.ui.theme.FluxPalette
import com.markfoundry.spindle.ui.theme.SpindleDimens

/** 48dp text button: the active skin's swatch and "Looks" in ink. TalkBack hears "Change look". */
@Composable
fun LooksButton(skin: PlayerSkin, tint: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.looks_button)
    Row(
        modifier = modifier
            .heightIn(min = SpindleDimens.Looks)
            .clip(RoundedCornerShape(SpindleDimens.Looks / 2))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiniDisc(skin.id == FluxSkin.id, 20.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.looks_label),
            color = tint,
            style = skin.type.button.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

/** Looks sheet on the active skin's tray tokens; 64dp radio rows. Sheet text uses the system font. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinPicker(active: PlayerSkin, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = active.colors()
    val haptics = rememberHaptics()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Opaque: Flux glass would let the player show through the sheet.
        containerColor = colors.tray.compositeOver(colors.ground),
        contentColor = colors.text,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.text2) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
                .selectableGroup(),
        ) {
            Text(
                stringResource(R.string.looks_title),
                color = colors.text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            )
            AllSkins.forEach { skin ->
                val selected = skin.id == active.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .clip(RoundedCornerShape(SpindleDimens.ChipRadius))
                        .background(if (selected) colors.text.copy(alpha = 0.08f) else Color.Transparent)
                        .selectable(selected = selected, role = Role.RadioButton) {
                            if (!selected) haptics.skinCommit()
                            onSelect(skin.id)
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MiniDisc(skin.id == FluxSkin.id, 40.dp)
                    Spacer(Modifier.width(16.dp))
                    Text(
                        skin.displayName,
                        color = colors.text,
                        fontWeight = FontWeight.Medium,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected) {
                        Canvas(Modifier.size(24.dp)) {
                            val check = Path().apply {
                                moveTo(size.width * 0.2f, size.height * 0.52f)
                                lineTo(size.width * 0.42f, size.height * 0.72f)
                                lineTo(size.width * 0.8f, size.height * 0.3f)
                            }
                            drawPath(check, colors.accent, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                    }
                }
            }
        }
    }
}

/** Swatch: the skin's hero in miniature. */
@Composable
private fun MiniDisc(flux: Boolean, diameter: Dp) {
    Canvas(Modifier.size(diameter)) {
        val r = size.minDimension / 2f
        if (flux) {
            drawCircle(FluxPalette.Ground, r)
            drawCircle(
                Brush.sweepGradient(listOf(FluxPalette.Accent, FluxPalette.Violet, FluxPalette.Magenta, FluxPalette.Accent), center),
                r * 0.78f,
                style = Stroke(r * 0.22f),
            )
        } else {
            drawCircle(AnalogDayPalette.Ground, r)
            drawCircle(DeckPalette.Vinyl, r * 0.86f)
            drawCircle(AnalogDayPalette.Text, r * 0.34f)
        }
    }
}
