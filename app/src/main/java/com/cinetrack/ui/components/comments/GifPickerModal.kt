package com.cinetrack.ui.components.comments

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.cinetrack.R
import com.cinetrack.data.model.KlipyMediaType
import com.cinetrack.ui.components.shared.FlickTroveModal
import com.cinetrack.ui.components.shared.ModalCloseButton
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.premiumScrollbar
import com.cinetrack.ui.utils.verticalFadingEdges
import com.cinetrack.ui.viewmodel.GifPickerViewModel
import dev.chrisbanes.haze.HazeState

@Composable
fun GifPickerModal(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    hazeState: HazeState,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onGifSelected: (String) -> Unit,
    viewModel: GifPickerViewModel = hiltViewModel()
) {
    if (!isVisible) return

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val focusManager = LocalFocusManager.current

    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0
    val animatedGridHeight by animateDpAsState(
        targetValue = if (isKeyboardOpen) 240.dp else (configuration.screenHeightDp.dp * 0.65f).coerceIn(380.dp, 580.dp),
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "gifPickerGridHeight"
    )

    FlickTroveModal(
        isVisible = isVisible,
        onDismissRequest = onDismiss,
        hazeState = hazeState,
        maxWidth = 500.dp,
        maxWidthFraction = 0.96f,
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar with GIF / Stickers segmented toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KlipyMediaType.entries.forEach { mediaType ->
                        val isSelected = uiState.selectedType == mediaType
                        val labelRes = if (mediaType == KlipyMediaType.GIF) {
                            R.string.comment_gif
                        } else {
                            R.string.comment_stickers
                        }
                        Box(
                            modifier = Modifier
                                .bounceClick(scaleDown = 0.94f) {
                                    viewModel.selectMediaType(mediaType)
                                }
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) accentColor.copy(alpha = 0.24f) else Color.Transparent
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) accentColor.copy(alpha = 0.60f) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(labelRes),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.55f),
                                maxLines = 1
                            )
                        }
                    }
                }

                ModalCloseButton(onClick = onDismiss)
            }

            Spacer(modifier = Modifier.height(14.dp))

            val focusRequester = remember { FocusRequester() }
            var isSearchFocused by remember { mutableStateOf(false) }

            val searchBorderColor by animateColorAsState(
                targetValue = if (isSearchFocused) accentColor else Color.White.copy(alpha = 0.12f),
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "searchBorderColor"
            )
            val searchBorderWidth by animateDpAsState(
                targetValue = if (isSearchFocused) 1.5.dp else 1.dp,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "searchBorderWidth"
            )

            // Search Bar (CircleShape)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (isSearchFocused) 0.10f else 0.08f))
                    .border(searchBorderWidth, searchBorderColor, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        focusRequester.requestFocus()
                    }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_lente),
                        contentDescription = null,
                        tint = if (isSearchFocused) accentColor else Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(18.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (uiState.query.isEmpty()) {
                            // KLIPY brand guidelines require the "Search KLIPY" placeholder
                            Text(
                                text = "Search KLIPY",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.40f)
                            )
                        }

                        BasicTextField(
                            value = uiState.query,
                            onValueChange = { viewModel.onQueryChanged(it) },
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(accentColor),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .onFocusChanged { isSearchFocused = it.isFocused }
                        )
                    }

                    if (uiState.query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .bounceClick {
                                    viewModel.onQueryChanged("")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_x),
                                contentDescription = stringResource(R.string.gif_picker_cd_clear),
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content Area: Grid / Shimmer / Empty / Error
            val isStickerTab = uiState.selectedType == KlipyMediaType.STICKER
            val columnCount = if (isStickerTab) 3 else 2

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(animatedGridHeight),
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isLoading && uiState.items.isEmpty() -> {
                        GifPickerSkeletonGrid(columns = columnCount)
                    }

                    uiState.errorMessage != null && uiState.items.isEmpty() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    if (isStickerTab) R.string.sticker_picker_load_error else R.string.gif_picker_load_error
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Box(
                                modifier = Modifier
                                    .bounceClick { viewModel.retry() }
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.2f))
                                    .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.gif_picker_retry),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    !uiState.isLoading && uiState.items.isEmpty() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = if (uiState.isSearching) {
                                    stringResource(
                                        if (isStickerTab) R.string.sticker_picker_search_empty else R.string.gif_picker_search_empty,
                                        uiState.query
                                    )
                                } else {
                                    stringResource(
                                        if (isStickerTab) R.string.sticker_picker_empty else R.string.gif_picker_empty
                                    )
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    else -> {
                        val context = LocalContext.current
                        key(uiState.selectedType) {
                            val gridState = rememberLazyStaggeredGridState()

                            // Trigger on-demand pagination only when scrolling near the end
                            val shouldLoadMore by remember {
                                derivedStateOf {
                                    val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                    val total = gridState.layoutInfo.totalItemsCount
                                    total > 0 && lastVisible >= total - 4
                                }
                            }

                            LaunchedEffect(shouldLoadMore) {
                                if (shouldLoadMore && !uiState.isLoading && !uiState.isLoadingMore && uiState.hasMore) {
                                    viewModel.loadNextPage()
                                }
                            }

                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Fixed(columnCount),
                                state = gridState,
                                contentPadding = PaddingValues(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalItemSpacing = 8.dp,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalFadingEdges(gridState, topEdgeHeight = 10.dp, bottomEdgeHeight = 12.dp)
                                    .premiumScrollbar(gridState, width = 2.5f, paddingEnd = 2f, paddingVertical = 10f)
                            ) {
                                items(
                                    items = uiState.items,
                                    key = { it.id }
                                ) { item ->
                                    val isSticker = item.type == KlipyMediaType.STICKER
                                    val itemRatio = remember(item.width, item.height, isSticker) {
                                        val ratio = item.width.toFloat() / item.height.toFloat()
                                        if (isSticker) ratio.coerceIn(0.8f, 1.25f) else ratio.coerceIn(0.65f, 1.6f)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(itemRatio)
                                            .bounceClick(scaleDown = 0.94f) {
                                                viewModel.onGifSelected(item)
                                                onGifSelected(item.fullUrl)
                                                onDismiss()
                                            }
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White.copy(alpha = 0.06f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val request = remember(item.previewUrl) {
                                            ImageRequest.Builder(context)
                                                .data(item.previewUrl)
                                                .crossfade(true)
                                                .crossfade(250)
                                                .build()
                                        }
                                        AsyncImage(
                                            model = request,
                                            contentDescription = item.title,
                                            contentScale = if (isSticker) ContentScale.Fit else ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .then(if (isSticker) Modifier.padding(8.dp) else Modifier)
                                        )
                                    }
                                }

                                if (uiState.isLoadingMore) {
                                    item(span = StaggeredGridItemSpan.FullLine) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = accentColor,
                                                strokeWidth = 2.dp
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
}

@Composable
private fun GifPickerSkeletonGrid(columns: Int = 2) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (columns == 3) {
            repeat(2) { rowIdx ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { colIdx ->
                        val height = if ((rowIdx + colIdx) % 2 == 0) 105.dp else 92.dp
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(height)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(95.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(135.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                )
            }
        }
    }
}
