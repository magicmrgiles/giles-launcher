package com.giles.einklauncher.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.giles.einklauncher.data.notes.LineFormat
import com.giles.einklauncher.data.notes.LineType
import com.giles.einklauncher.data.notes.NoteLineEntity
import com.giles.einklauncher.ui.components.OutlineIconButton
import com.giles.einklauncher.ui.components.noRippleClickable
import com.giles.einklauncher.ui.theme.EinkType
import com.giles.einklauncher.ui.theme.einkColors

/**
 * Screen 2. A single continuous note with mixed line types and per-line character
 * formatting. Typing happens on an inline live line at the end; Done commits it and a fresh
 * live line takes its place.
 */
@Composable
fun NotesScreen(
    lines: List<NoteLineEntity>,
    onAppend: (LineType, String, LineFormat) -> Unit,
    onToggleCheckbox: (Long) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = einkColors

    var draft by remember { mutableStateOf("") }
    var activeType by remember { mutableStateOf(LineType.TEXT) }
    var format by remember { mutableStateOf(LineFormat()) }
    var showFormatMenu by remember { mutableStateOf(false) }
    var showListMenu by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    fun commit() {
        if (draft.isBlank()) return
        onAppend(activeType, draft, format)
        draft = ""
    }

    Column(
        modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = 56.dp, bottom = 44.dp),
    ) {
        Text(
            text = "NOTES",
            style = EinkType.ScreenTitle,
            color = colors.content,
            modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        Box(Modifier.weight(1f)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, colors.content, RoundedCornerShape(14.dp))
                    .noRippleClickable { focusRequester.requestFocus() }
                    .verticalScroll(scrollState)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                lines.forEachIndexed { index, line ->
                    CommittedLine(
                        line = line,
                        numberValue = numberValueAt(lines, index),
                        onToggleCheckbox = onToggleCheckbox,
                    )
                }

                LiveLine(
                    draft = draft,
                    onDraftChange = { value ->
                        if (value.contains('\n')) {
                            draft = value.replace("\n", "")
                            commit()
                        } else {
                            draft = value
                        }
                    },
                    onCommit = { commit() },
                    activeType = activeType,
                    nextNumber = trailingNumberCount(lines) + 1,
                    format = format,
                    isEmptyNote = lines.isEmpty(),
                    focusRequester = focusRequester,
                )
            }

            // Fade-out gradient only when content overflows AND we're not at the bottom yet.
            val overflow = scrollState.maxValue > 0
            val atBottom = scrollState.value >= scrollState.maxValue - 4
            if (overflow && !atBottom) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 1.dp)
                        .height(28.dp)
                        .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, colors.background))
                        )
                )
            }
        }

        Toolbar(
            showFormatMenu = showFormatMenu,
            showListMenu = showListMenu,
            format = format,
            activeType = activeType,
            onToggleFormatMenu = {
                showFormatMenu = !showFormatMenu
                showListMenu = false
            },
            onToggleListMenu = {
                showListMenu = !showListMenu
                showFormatMenu = false
            },
            onToggleBold = { format = format.copy(bold = !format.bold) },
            onToggleItalic = { format = format.copy(italic = !format.italic) },
            onToggleUnderline = { format = format.copy(underline = !format.underline) },
            onToggleStrike = { format = format.copy(strike = !format.strike) },
            onSetType = { type -> activeType = if (activeType == type) LineType.TEXT else type },
            onClear = {
                onClear()
                draft = ""
            },
        )
    }
}

@Composable
private fun CommittedLine(
    line: NoteLineEntity,
    numberValue: Int?,
    onToggleCheckbox: (Long) -> Unit,
) {
    val colors = einkColors
    val style = lineTextStyle(line.bold, line.italic, line.underline, line.strike, line.done)

    when (line.type) {
        LineType.CHECKBOX -> Row(
            Modifier.fillMaxWidth().noRippleClickable { onToggleCheckbox(line.id) },
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (line.done) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                contentDescription = null,
                tint = colors.content,
                modifier = Modifier.size(17.dp).padding(top = 1.dp),
            )
            Text(line.text, style = style, color = colors.content)
        }

        LineType.BULLET -> Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("•", style = EinkType.Body, color = colors.content)
            Text(line.text, style = style, color = colors.content)
        }

        LineType.NUMBER -> Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("${numberValue ?: 1}.", style = EinkType.Body, color = colors.content, modifier = Modifier.width(18.dp))
            Text(line.text, style = style, color = colors.content)
        }

        LineType.TEXT -> Text(
            line.text,
            style = style.copy(fontWeight = if (line.bold) FontWeight.Bold else FontWeight.Medium),
            color = colors.content,
        )
    }
}

@Composable
private fun LiveLine(
    draft: String,
    onDraftChange: (String) -> Unit,
    onCommit: () -> Unit,
    activeType: LineType,
    nextNumber: Int,
    format: LineFormat,
    isEmptyNote: Boolean,
    focusRequester: FocusRequester,
) {
    val colors = einkColors
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (activeType) {
            LineType.CHECKBOX -> Icon(
                imageVector = Icons.Outlined.CheckBoxOutlineBlank,
                contentDescription = null,
                tint = colors.content,
                modifier = Modifier.size(17.dp).padding(top = 1.dp),
            )
            LineType.BULLET -> Text("•", style = EinkType.Body, color = colors.content)
            LineType.NUMBER -> Text("$nextNumber.", style = EinkType.Body, color = colors.content, modifier = Modifier.width(18.dp))
            LineType.TEXT -> {}
        }
        BasicTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            textStyle = lineTextStyle(format.bold, format.italic, format.underline, format.strike, false)
                .copy(color = colors.content),
            cursorBrush = SolidColor(colors.content),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onCommit() }),
            decorationBox = { inner ->
                if (draft.isEmpty() && isEmptyNote) {
                    Text("Type to add to this note", style = EinkType.Body, color = colors.content)
                }
                inner()
            },
        )
    }
}

@Composable
private fun Toolbar(
    showFormatMenu: Boolean,
    showListMenu: Boolean,
    format: LineFormat,
    activeType: LineType,
    onToggleFormatMenu: () -> Unit,
    onToggleListMenu: () -> Unit,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderline: () -> Unit,
    onToggleStrike: () -> Unit,
    onSetType: (LineType) -> Unit,
    onClear: () -> Unit,
) {
    val colors = einkColors
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlineIconButton(onClick = onToggleFormatMenu, selected = showFormatMenu) {
                Icon(Icons.Outlined.TextFields, "Text format", tint = iconTint(showFormatMenu), modifier = Modifier.size(14.dp))
            }
            if (showFormatMenu) {
                OutlineIconButton(onClick = onToggleBold, selected = format.bold) {
                    Icon(Icons.Outlined.FormatBold, "Bold", tint = iconTint(format.bold), modifier = Modifier.size(14.dp))
                }
                OutlineIconButton(onClick = onToggleItalic, selected = format.italic) {
                    Icon(Icons.Outlined.FormatItalic, "Italic", tint = iconTint(format.italic), modifier = Modifier.size(14.dp))
                }
                OutlineIconButton(onClick = onToggleUnderline, selected = format.underline) {
                    Icon(Icons.Outlined.FormatUnderlined, "Underline", tint = iconTint(format.underline), modifier = Modifier.size(14.dp))
                }
                OutlineIconButton(onClick = onToggleStrike, selected = format.strike) {
                    Icon(Icons.Outlined.FormatStrikethrough, "Strikethrough", tint = iconTint(format.strike), modifier = Modifier.size(14.dp))
                }
            }
            OutlineIconButton(onClick = onToggleListMenu, selected = showListMenu || activeType != LineType.TEXT) {
                Icon(Icons.Outlined.FormatListBulleted, "List", tint = iconTint(showListMenu || activeType != LineType.TEXT), modifier = Modifier.size(14.dp))
            }
            if (showListMenu) {
                OutlineIconButton(onClick = { onSetType(LineType.CHECKBOX) }, selected = activeType == LineType.CHECKBOX) {
                    Icon(Icons.Outlined.CheckBox, "Checkbox", tint = iconTint(activeType == LineType.CHECKBOX), modifier = Modifier.size(14.dp))
                }
                OutlineIconButton(onClick = { onSetType(LineType.BULLET) }, selected = activeType == LineType.BULLET) {
                    Text("•", style = EinkType.Control, color = iconTint(activeType == LineType.BULLET))
                }
                OutlineIconButton(onClick = { onSetType(LineType.NUMBER) }, selected = activeType == LineType.NUMBER) {
                    Icon(Icons.Outlined.FormatListNumbered, "Numbered", tint = iconTint(activeType == LineType.NUMBER), modifier = Modifier.size(14.dp))
                }
            }
        }
        OutlineIconButton(onClick = onClear) {
            Icon(Icons.Outlined.Delete, "Clear note", tint = colors.content, modifier = Modifier.size(14.dp))
        }
    }
}

// ---- helpers ----

@Composable
private fun iconTint(selected: Boolean): Color =
    if (selected) einkColors.background else einkColors.content

private fun lineTextStyle(
    bold: Boolean,
    italic: Boolean,
    underline: Boolean,
    strike: Boolean,
    done: Boolean,
): TextStyle {
    val decorations = buildList {
        if (underline) add(TextDecoration.Underline)
        if (strike || done) add(TextDecoration.LineThrough)
    }
    return EinkType.Body.copy(
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (decorations.isEmpty()) TextDecoration.None else TextDecoration.combine(decorations),
    )
}

/** Number to render for the numbered line at [index] (1-based within its consecutive run). */
private fun numberValueAt(lines: List<NoteLineEntity>, index: Int): Int? {
    if (lines[index].type != LineType.NUMBER) return null
    var n = 1
    var j = index - 1
    while (j >= 0 && lines[j].type == LineType.NUMBER) {
        n++
        j--
    }
    return n
}

/** How many NUMBER lines trail the end of the note (for the live line's next number). */
private fun trailingNumberCount(lines: List<NoteLineEntity>): Int {
    var n = 0
    var j = lines.lastIndex
    while (j >= 0 && lines[j].type == LineType.NUMBER) {
        n++
        j--
    }
    return n
}
