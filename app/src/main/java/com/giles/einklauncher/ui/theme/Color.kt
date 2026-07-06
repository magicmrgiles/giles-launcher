package com.giles.einklauncher.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Black = Color(0xFF000000)
val White = Color(0xFFFFFFFF)

/**
 * The entire palette. There are no greys and no accent colours by design — hierarchy
 * comes from size and weight only. [content] is what you draw (text, icons, hairlines);
 * [background] is the surface behind it. "Selected/active" states swap the two.
 */
@Immutable
data class EinkColors(
    val background: Color,
    val content: Color,
) {
    /** Colour pair for an inverted (selected/active) surface. */
    val invertedBackground: Color get() = content
    val invertedContent: Color get() = background
}

val LightColors = EinkColors(background = White, content = Black)
val DarkColors = EinkColors(background = Black, content = White)

val LocalEinkColors = staticCompositionLocalOf { LightColors }
