package com.cinetrack.ui.components.comments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.horizontalFadingEdges

@Composable
internal fun MdBtn(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .bounceClick { onClick() }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** Pill-shaped button for text-only formatting actions (Quote, List, ecc.) */
@Composable
internal fun MdBtnPill(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .bounceClick { onClick() }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun CommentMarkdownToolbar(
    visible: Boolean,
    inputText: TextFieldValue,
    onInputTextChanged: (TextFieldValue) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = visible, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            val mdAction = { prefix: String, suffix: String ->
                val selection = inputText.selection
                val text = inputText.text
                if (selection.collapsed) {
                    val newText = text.substring(0, selection.start) + prefix + suffix + text.substring(selection.end)
                    onInputTextChanged(TextFieldValue(newText, selection = TextRange(selection.start + prefix.length)))
                } else {
                    val newText = text.substring(0, selection.start) + prefix + text.substring(selection.start, selection.end) + suffix + text.substring(selection.end)
                    onInputTextChanged(TextFieldValue(newText, selection = TextRange(selection.end + prefix.length + suffix.length)))
                }
            }

            val mdListState = rememberLazyListState()
            LazyRow(
                state = mdListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalFadingEdges(
                        lazyListState = mdListState,
                        leftEdgeWidth = 14.dp,
                        rightEdgeWidth = 14.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                item { MdBtn(onClick = { mdAction("**", "**") }) { Text("B", fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.sp) } }
                item { MdBtn(onClick = { mdAction("*", "*") }) { Text("I", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp) } }
                item { MdBtn(onClick = { mdAction("~~", "~~") }) { Text("S", textDecoration = TextDecoration.LineThrough, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp) } }
                item { MdBtnPill(onClick = { mdAction("> ", "") }) { Text("Quote", color = Color.White, fontSize = 11.sp) } }
                item { MdBtnPill(onClick = { mdAction("- ", "") }) { Text("• List", color = Color.White, fontSize = 11.sp) } }
                item { MdBtnPill(onClick = { mdAction("1. ", "") }) { Text("1. List", color = Color.White, fontSize = 11.sp) } }
                item {
                    MdBtnPill(
                        onClick = {
                            val pos = inputText.selection.start.coerceIn(0, inputText.text.length)
                            val needsSpace = pos > 0 && !inputText.text[pos - 1].isWhitespace()
                            mdAction(if (needsSpace) " #" else "#", "")
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_film),
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "#",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
