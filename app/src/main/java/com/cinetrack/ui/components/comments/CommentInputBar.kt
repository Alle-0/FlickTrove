package com.cinetrack.ui.components.comments

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.cinetrack.R
import com.cinetrack.data.model.AppComment
import com.cinetrack.ui.components.common.FlickTroveSwitch
import com.cinetrack.ui.utils.MarkdownVisualTransformation
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.premiumScrollbar
import com.cinetrack.util.VibrationHelper
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

internal tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

@Composable
fun CommentInputBar(
    modifier: Modifier = Modifier,
    inputText: TextFieldValue,
    onInputTextChanged: (TextFieldValue) -> Unit,
    replyingTo: AppComment?,
    onCancelReply: () -> Unit,
    isUserBanned: Boolean,
    banExpiration: String?,
    isUserAnonymous: Boolean,
    onGuestAuthTrigger: () -> Unit,
    isSpoiler: Boolean,
    onSpoilerChanged: (Boolean) -> Unit,
    attachedMedia: List<String>,
    onAttachedMediaChanged: (List<String>) -> Unit,
    isUploadingImage: Boolean,
    onPickImage: () -> Unit,
    onSendComment: () -> Unit,
    accentColor: Color,
    hazeState: HazeState,
    focusRequester: FocusRequester
) {
    val context = LocalContext.current
    val imageLoader = context.imageLoader
    var isInputExpanded by remember { mutableStateOf(false) }
    var isMarkdownMenuExpanded by remember { mutableStateOf(false) }

    val topCornerRadius by animateDpAsState(
        targetValue = if (replyingTo != null) 24.dp else 50.dp,
        label = "topCornerRadius"
    )
    val boxShape = RoundedCornerShape(
        topStart = topCornerRadius,
        topEnd = topCornerRadius,
        bottomStart = 50.dp,
        bottomEnd = 50.dp
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(boxShape)
            .hazeChild(
                state = hazeState,
                shape = boxShape,
                style = HazeStyle(tint = Color(0xFF1E1E1E).copy(alpha = 0.85f), blurRadius = 15.dp)
            )
            .animateContentSize(alignment = Alignment.BottomCenter)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            val lastReplyingTo = remember { mutableStateOf(replyingTo) }
            if (replyingTo != null) {
                lastReplyingTo.value = replyingTo
            }

            AnimatedVisibility(
                visible = replyingTo != null,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
            ) {
                lastReplyingTo.value?.let { replyTarget ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = buildAnnotatedString {
                                    append(stringResource(R.string.comment_replying_to) + " ")
                                    withStyle(style = SpanStyle(color = accentColor, fontWeight = FontWeight.Bold)) {
                                        append(replyTarget.userDisplayName)
                                    }
                                },
                                color = Color.White.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.labelSmall
                            )
                            val previewMediaRegex = Regex("!\\[(?:gif|foto)\\]\\((.*?)\\)")
                            val cleanText = replyTarget.text.replace(previewMediaRegex, "").trim()
                            val previewText = if (cleanText.isEmpty() && previewMediaRegex.containsMatchIn(replyTarget.text)) {
                                stringResource(R.string.comment_image_preview)
                            } else {
                                cleanText
                            }
                            Text(
                                text = "\"$previewText\"",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.comment_cancel_btn),
                            color = Color.White.copy(0.7f),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.bounceClick { onCancelReply() }
                        )
                    }
                }
            }

            if (isUserBanned) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .background(Color(0xFF330000), CircleShape)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = banExpiration?.let { stringResource(R.string.comment_banned_temporary, it) }
                            ?: stringResource(R.string.comment_banned_permanent),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val maxChar = 4000
                        val inputScrollState = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(max = 120.dp)
                                .border(1.dp, if (isInputExpanded) accentColor else Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                                .premiumScrollbar(inputScrollState, width = 3f, paddingEnd = 6f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { focusRequester.requestFocus() }
                                .padding(horizontal = 16.dp, vertical = 15.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            BasicTextField(
                                value = inputText,
                                enabled = !isUserAnonymous,
                                onValueChange = {
                                    if (it.text.length <= maxChar) {
                                        onInputTextChanged(it)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { focusState -> isInputExpanded = focusState.isFocused }
                                    .verticalScroll(inputScrollState),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                                cursorBrush = SolidColor(accentColor),
                                visualTransformation = remember { MarkdownVisualTransformation() }
                            )
                            if (inputText.text.isEmpty()) {
                                Text(
                                    stringResource(R.string.comment_input_hint),
                                    color = Color.White.copy(alpha = 0.4f),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .bounceClick {
                                    if (isUserAnonymous) {
                                        onGuestAuthTrigger()
                                    } else {
                                        onSendComment()
                                    }
                                }
                                .background(accentColor, RoundedCornerShape(50.dp))
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(R.string.comment_send_btn), color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (inputText.text.isNotEmpty()) {
                        val maxChar = 4000
                        Text(
                            text = "${inputText.text.length}/$maxChar",
                            color = if (inputText.text.length == maxChar) Color.Red.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.3f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, end = 100.dp),
                            textAlign = TextAlign.End
                        )
                    }

                    val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                    AnimatedVisibility(visible = isInputExpanded || attachedMedia.isNotEmpty() || inputText.text.isNotEmpty() || isKeyboardOpen) {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            if (attachedMedia.isNotEmpty()) {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(attachedMedia.size) { index ->
                                        val url = attachedMedia[index]
                                        Box(modifier = Modifier.size(64.dp)) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(context).data(url).build(),
                                                imageLoader = imageLoader,
                                                contentDescription = "Attachment",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp)
                                                    .size(20.dp)
                                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                    .bounceClick { onAttachedMediaChanged(attachedMedia.filter { it != url }) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_x),
                                                    contentDescription = "Remove",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.comment_spoiler_toggle),
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FlickTroveSwitch(
                                    checked = isSpoiler,
                                    onCheckedChange = {
                                        VibrationHelper.vibrateTick(context)
                                        onSpoilerChanged(it)
                                    },
                                    accentColor = accentColor,
                                    modifier = Modifier.scale(0.85f)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .bounceClick(enabled = attachedMedia.isEmpty()) {
                                            val fragmentManager = context.findFragmentActivity()?.supportFragmentManager
                                            if (fragmentManager != null) {
                                                val settings = com.giphy.sdk.ui.GPHSettings(
                                                    theme = com.giphy.sdk.ui.themes.GPHTheme.Dark,
                                                    mediaTypeConfig = arrayOf(com.giphy.sdk.ui.GPHContentType.gif)
                                                )
                                                GiphyDialogCustomizer.prepareStaticRadius(context)
                                                val dialog = com.giphy.sdk.ui.views.GiphyDialogFragment.newInstance(settings)
                                                GiphyDialogCustomizer.customizeDialog(dialog, fragmentManager)
                                                dialog.gifSelectionListener = object : com.giphy.sdk.ui.views.GiphyDialogFragment.GifSelectionListener {
                                                    override fun didSearchTerm(term: String) {}
                                                    override fun onDismissed(selectedContentType: com.giphy.sdk.ui.GPHContentType) {}
                                                    override fun onGifSelected(media: com.giphy.sdk.core.models.Media, searchTerm: String?, selectedContentType: com.giphy.sdk.ui.GPHContentType) {
                                                        val gifUrl = media.images.fixedHeight?.gifUrl ?: media.images.original?.gifUrl ?: ""
                                                        if (gifUrl.isNotEmpty()) {
                                                            onAttachedMediaChanged(listOf(gifUrl))
                                                        }
                                                        dialog.dismiss()
                                                    }
                                                }
                                                dialog.show(fragmentManager, "giphy_dialog")
                                            } else {
                                                android.widget.Toast.makeText(context, "Fragment manager not found", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val gifAlpha = if (attachedMedia.isEmpty()) 0.8f else 0.3f
                                    Icon(painterResource(id = R.drawable.ic_gif), contentDescription = "GIF", tint = Color.White.copy(alpha = gifAlpha), modifier = Modifier.size(20.dp))
                                }
                                // Foto
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .bounceClick(enabled = attachedMedia.isEmpty() && !isUploadingImage) {
                                            if (!isUploadingImage) onPickImage()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isUploadingImage) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = accentColor, strokeWidth = 2.dp)
                                    } else {
                                        val fotoAlpha = if (attachedMedia.isEmpty()) 0.8f else 0.3f
                                        Icon(painterResource(id = R.drawable.ic_image), contentDescription = "Foto", tint = Color.White.copy(alpha = fotoAlpha), modifier = Modifier.size(20.dp))
                                    }
                                }
                                // MD Toggle
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .bounceClick { isMarkdownMenuExpanded = !isMarkdownMenuExpanded },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(painterResource(id = R.drawable.ic_pencil), contentDescription = "Markdown", tint = if (isMarkdownMenuExpanded) accentColor else Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                                }
                            }
                            AnimatedVisibility(visible = isMarkdownMenuExpanded) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
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

                                    LazyRow {
                                        item {
                                            MdBtn(onClick = { mdAction("**", "**") }) { Text("B", fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.titleMedium) }
                                            MdBtn(onClick = { mdAction("*", "*") }) { Text("I", fontStyle = FontStyle.Italic, color = Color.White, style = MaterialTheme.typography.titleMedium) }
                                            MdBtn(onClick = { mdAction("~~", "~~") }) { Text("S", textDecoration = TextDecoration.LineThrough, color = Color.White, style = MaterialTheme.typography.titleMedium) }
                                            MdBtn(onClick = { mdAction("> ", "") }) { Text("\"\"", color = Color.White, style = MaterialTheme.typography.titleMedium) }
                                            MdBtn(onClick = { mdAction("- ", "") }) { Text("•", color = Color.White, style = MaterialTheme.typography.titleMedium) }
                                            MdBtn(onClick = { mdAction("1. ", "") }) { Text("1.", color = Color.White, style = MaterialTheme.typography.titleMedium) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MdBtn(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .bounceClick { onClick() }
            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
    Spacer(modifier = Modifier.width(12.dp))
}
