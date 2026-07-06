package com.giles.einklauncher.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.sp

/**
 * Typography for the launcher. Weight and size do all the hierarchy work — there is no
 * colour dimming. Numerals for the clock are large and light; section headers are small,
 * uppercase and letter-tracked.
 */
object EinkType {

    /** Big clock numerals, e.g. "9:41". */
    val Clock = TextStyle(
        fontSize = 64.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (-1).sp,
        lineHeight = 64.sp,
    )

    /** The AM/PM suffix — visually smaller, baseline-aligned with the numerals. */
    val ClockPeriod = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
    )

    /** Small uppercase tracked labels, e.g. "NOTES", settings section headers. */
    val SectionHeader = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 1.5.sp,
        textGeometricTransform = TextGeometricTransform(scaleX = 1f),
    )

    /** The larger "NOTES" screen title. */
    val ScreenTitle = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 2.sp,
    )

    val WidgetPrimary = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal)
    val WidgetSecondary = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal)
    val DateLine = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal)

    val AppLabel = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.3.sp)
    val Monogram = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Light)

    val Body = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp)
    val Control = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal)
}
