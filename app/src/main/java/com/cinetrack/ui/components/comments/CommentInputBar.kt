package com.cinetrack.ui.components.comments

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.cinetrack.R
import com.cinetrack.data.model.AppComment
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
    focusRequester: FocusRequester,
    postToCommsUni: Boolean = true,
    onPostToCommsUniChanged: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val imageLoader = context.imageLoader
    var isInputExpanded by remember { mutableStateOf(false) }
    var isMarkdownMenuExpanded by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0
    val isExpanded = isInputExpanded || isKeyboardOpen || inputText.text.isNotEmpty() || attachedMedia.isNotEmpty() || replyingTo != null
    val isCompact = !isExpanded

    val outerCorner by animateDpAsState(
        targetValue = if (isCompact) 30.dp else 24.dp,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "outerBoxCorner"
    )
    val boxShape = RoundedCornerShape(outerCorner)

    val outerInnerPaddingH by animateDpAsState(
        targetValue = if (isCompact) 8.dp else 12.dp,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "outerInnerPaddingH"
    )
    val outerInnerPaddingV by animateDpAsState(
        targetValue = if (isCompact) 8.dp else 10.dp,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "outerInnerPaddingV"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clip(boxShape)
            .hazeChild(
                state = hazeState,
                shape = boxShape,
                style = HazeStyle(tint = Color(0xFF161616).copy(alpha = 0.88f), blurRadius = 20.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = boxShape
            )
            .animateContentSize(alignment = Alignment.BottomCenter)
            .padding(horizontal = outerInnerPaddingH, vertical = outerInnerPaddingV)
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
                val maxChar = 4000

                // Attached Media Previews
                if (attachedMedia.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(attachedMedia.size) { index ->
                            val url = attachedMedia[index]
                            Box(modifier = Modifier.size(58.dp)) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(url).build(),
                                    imageLoader = imageLoader,
                                    contentDescription = "Attachment",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .size(18.dp)
                                        .background(Color.Black.copy(alpha = 0.65f), CircleShape)
                                        .bounceClick { onAttachedMediaChanged(attachedMedia.filter { it != url }) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_x),
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(9.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Send button state (hoisted — used in the main Row, always visible)
                val canSend = (inputText.text.isNotBlank() || attachedMedia.isNotEmpty()) && !isUserAnonymous
                val sendBgColor by animateColorAsState(
                    targetValue = if (canSend) accentColor else Color.White.copy(alpha = 0.08f),
                    label = "sendBgColor"
                )
                val sendIconTint by animateColorAsState(
                    targetValue = if (canSend) Color.Black else Color.White.copy(alpha = 0.38f),
                    label = "sendIconTint"
                )

                // Main input Row: [TextBox weight(1f)] [Send — always here]
                val inputScrollState = rememberScrollState()
                val textBoxCorner by animateDpAsState(
                    targetValue = if (isCompact) 22.dp else 16.dp,
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                    label = "textBoxCorner"
                )
                val textBoxShape = RoundedCornerShape(textBoxCorner)
                val textPadH by animateDpAsState(
                    targetValue = if (isCompact) 16.dp else 13.dp,
                    animationSpec = tween(250, easing = FastOutSlowInEasing),
                    label = "textPadH"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 44.dp, max = 130.dp)
                            .clip(textBoxShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.dp, Color.White.copy(alpha = 0.10f), textBoxShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                isInputExpanded = true
                                focusRequester.requestFocus()
                            }
                            .padding(horizontal = textPadH, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart
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
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                fontSize = 15.sp,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = SolidColor(accentColor),
                            visualTransformation = remember { MarkdownVisualTransformation() }
                        )
                        if (inputText.text.isEmpty()) {
                            Text(
                                stringResource(R.string.comment_input_hint),
                                color = Color.White.copy(alpha = 0.38f),
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send button — always next to the text box
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .bounceClick(enabled = canSend || isUserAnonymous) {
                                if (isUserAnonymous) {
                                    onGuestAuthTrigger()
                                } else if (canSend) {
                                    onSendComment()
                                }
                            }
                            .clip(CircleShape)
                            .background(sendBgColor)
                            .border(
                                width = 1.dp,
                                color = if (canSend) Color.Transparent else Color.White.copy(alpha = 0.10f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_send),
                            contentDescription = stringResource(R.string.comment_send_btn),
                            tint = sendIconTint,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }

                // Expandable section: char count + markdown menu + toolbar
                // Slides in when focused or has text/media; hidden in compact/idle state
                AnimatedVisibility(
                    visible = !isCompact,
                    enter = expandVertically(
                        expandFrom = Alignment.Top,
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200)),
                    exit = shrinkVertically(
                        shrinkTowards = Alignment.Top,
                        animationSpec = tween(240, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(160))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Character count when typing
                        if (inputText.text.isNotEmpty()) {
                            Text(
                                text = "${inputText.text.length}/$maxChar",
                                color = if (inputText.text.length == maxChar) Color.Red.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.3f),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 6.dp, top = 2.dp),
                                textAlign = TextAlign.Start
                            )
                        }

                        // Markdown formatting menu
                        AnimatedVisibility(visible = isMarkdownMenuExpanded) {
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

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    item { MdBtn(onClick = { mdAction("**", "**") }) { Text("B", fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.sp) } }
                                    item { MdBtn(onClick = { mdAction("*", "*") }) { Text("I", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp) } }
                                    item { MdBtn(onClick = { mdAction("~~", "~~") }) { Text("S", textDecoration = TextDecoration.LineThrough, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp) } }
                                    item { MdBtnPill(onClick = { mdAction("> ", "") }) { Text("Quote", color = Color.White, fontSize = 11.sp) } }
                                    item { MdBtnPill(onClick = { mdAction("- ", "") }) { Text("• List", color = Color.White, fontSize = 11.sp) } }
                                    item { MdBtnPill(onClick = { mdAction("1. ", "") }) { Text("1. List", color = Color.White, fontSize = 11.sp) } }
                                }
                            }
                        }

                        // Action Toolbar — no send button here (it lives next to the text box)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Foto
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .bounceClick(enabled = attachedMedia.isEmpty() && !isUploadingImage) {
                                        if (!isUploadingImage) onPickImage()
                                    }
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploadingImage) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = accentColor, strokeWidth = 2.dp)
                                } else {
                                    val fotoAlpha = if (attachedMedia.isEmpty()) 0.75f else 0.25f
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_image),
                                        contentDescription = "Foto",
                                        tint = Color.White.copy(alpha = fotoAlpha),
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // GIF
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
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
                                    }
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                val gifAlpha = if (attachedMedia.isEmpty()) 0.75f else 0.25f
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_gif),
                                    contentDescription = "GIF",
                                    tint = Color.White.copy(alpha = gifAlpha),
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Markdown pencil toggle
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .bounceClick { isMarkdownMenuExpanded = !isMarkdownMenuExpanded }
                                    .clip(CircleShape)
                                    .background(if (isMarkdownMenuExpanded) accentColor.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f))
                                    .border(1.dp, if (isMarkdownMenuExpanded) accentColor.copy(alpha = 0.45f) else Color.Transparent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_pencil),
                                    contentDescription = "Markdown",
                                    tint = if (isMarkdownMenuExpanded) accentColor else Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Spoiler pill button
                            Box(
                                modifier = Modifier
                                    .height(32.dp)
                                    .bounceClick {
                                        VibrationHelper.vibrateTick(context)
                                        onSpoilerChanged(!isSpoiler)
                                    }
                                    .clip(CircleShape)
                                    .background(
                                        if (isSpoiler) accentColor.copy(alpha = 0.16f)
                                        else Color.White.copy(alpha = 0.06f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSpoiler) accentColor.copy(alpha = 0.40f)
                                        else Color.White.copy(alpha = 0.15f),
                                        CircleShape
                                    )
                                    .padding(horizontal = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = if (isSpoiler) R.drawable.ic_eye_off else R.drawable.ic_eye),
                                        contentDescription = stringResource(R.string.comment_spoiler_toggle),
                                        tint = if (isSpoiler) accentColor else Color.White.copy(alpha = 0.65f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.comment_spoiler_toggle),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = if (isSpoiler) accentColor else Color.White.copy(alpha = 0.65f),
                                        maxLines = 1
                                    )
                                }
                            }

                            // Spacer pushes destination to the far right
                            Spacer(modifier = Modifier.weight(1f))

                            // Delivery destination pill
                            if (onPostToCommsUniChanged != null) {
                                Box(
                                    modifier = Modifier
                                        .height(32.dp)
                                        .bounceClick {
                                            VibrationHelper.vibrateTick(context)
                                            onPostToCommsUniChanged(!postToCommsUni)
                                        }
                                        .clip(CircleShape)
                                        .background(
                                            if (postToCommsUni) accentColor.copy(alpha = 0.14f)
                                            else Color.White.copy(alpha = 0.06f)
                                        )
                                        .border(
                                            1.dp,
                                            if (postToCommsUni) accentColor.copy(alpha = 0.35f)
                                            else Color.White.copy(alpha = 0.15f),
                                            CircleShape
                                        )
                                        .padding(horizontal = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = if (postToCommsUni) R.drawable.ic_world else R.drawable.ic_lock),
                                            contentDescription = null,
                                            tint = if (postToCommsUni) accentColor else Color.White.copy(alpha = 0.65f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (postToCommsUni) stringResource(R.string.comment_destination_commsuni)
                                                   else stringResource(R.string.comment_destination_flicktrove),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            ),
                                            color = if (postToCommsUni) accentColor else Color.White.copy(alpha = 0.65f),
                                            maxLines = 1
                                        )
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
private fun MdBtnPill(onClick: () -> Unit, content: @Composable () -> Unit) {
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
