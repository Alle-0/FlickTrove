package com.cinetrack.ui.components.dialog

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.glass.GlassmorphicModal
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.premiumScrollbar
import com.cinetrack.ui.utils.verticalFadingEdges
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.VibrationHelper
import com.cinetrack.util.buildTmdbImageUrl
import dev.chrisbanes.haze.HazeState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

@Composable
fun FolderReorderModal(
    visible: Boolean,
    movies: ImmutableList<Movie>,
    folderName: String,
    folderColor: String? = null,
    hazeState: HazeState? = null,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    BackHandler(onBack = onDismiss)

    val fallbackHazeState = remember { HazeState() }
    val activeHaze = hazeState ?: fallbackHazeState

    GlassmorphicModal(
        visible = visible,
        activeHazeState = activeHaze,
        dimBackground = true,
        dismissOnClickOutside = true,
        onDismissRequest = onDismiss
    ) {
        val context = LocalContext.current
        val density = LocalDensity.current

        var localOrder by remember(movies) { mutableStateOf(movies.map { it.compositeId }) }
        val moviesMap = remember(movies) { movies.associateBy { it.compositeId } }

        var draggedItemKey by remember { mutableStateOf<String?>(null) }
        var dragOffset by remember { mutableFloatStateOf(0f) }
        var dropTrigger by remember { mutableIntStateOf(0) }
        var itemHeightPx by remember { mutableFloatStateOf(0f) }
        var autoScrollSpeed by remember { mutableFloatStateOf(0f) }
        var pointerWindowY by remember { mutableFloatStateOf(0f) }
        var containerTopInWindow by remember { mutableFloatStateOf(0f) }
        var containerBottomInWindow by remember { mutableFloatStateOf(0f) }

        val configuration = LocalConfiguration.current
        val maxDialogHeight = (configuration.screenHeightDp.dp * 0.76f).coerceIn(460.dp, 600.dp)
        val listScrollState = rememberScrollState()

        // Auto-scroll durante il drag in prossimità dei bordi superiore o inferiore
        LaunchedEffect(draggedItemKey, autoScrollSpeed) {
            if (draggedItemKey != null && autoScrollSpeed != 0f) {
                while (isActive && draggedItemKey != null && autoScrollSpeed != 0f) {
                    val consumed = listScrollState.scrollBy(autoScrollSpeed)
                    if (consumed != 0f) {
                        dragOffset += consumed
                    }
                    delay(16)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxDialogHeight)
                .padding(top = 22.dp, bottom = 18.dp)
        ) {
            // Header in stile SettingsDialogs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.folder_reorder_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.folder_reorder_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tasto Conferma / Salva
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .bounceClick {
                                VibrationHelper.vibrateClick(context)
                                onSave()
                            }
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_tick),
                            contentDescription = stringResource(R.string.folder_reorder_save),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Tasto Chiudi
                    com.cinetrack.ui.components.shared.ModalCloseButton(
                        onClose = onDismiss
                    )
                }
            }

            val draggedIndex = localOrder.indexOf(draggedItemKey)
            val visualTargetIndex = remember(draggedIndex, dragOffset, itemHeightPx) {
                if (draggedIndex == -1 || itemHeightPx == 0f) -1
                else {
                    val offsetSlots = (dragOffset / itemHeightPx).roundToInt()
                    (draggedIndex + offsetSlots).coerceIn(0, localOrder.size - 1)
                }
            }

            var lastHapticIndex by remember { mutableIntStateOf(-1) }
            LaunchedEffect(visualTargetIndex, draggedItemKey) {
                if (draggedItemKey != null && visualTargetIndex != -1) {
                    if (lastHapticIndex != -1 && lastHapticIndex != visualTargetIndex) {
                        VibrationHelper.vibrateTick(context)
                    }
                    lastHapticIndex = visualTargetIndex
                } else {
                    lastHapticIndex = -1
                }
            }

            val currentDraggedIndex by rememberUpdatedState(draggedIndex)
            val currentVisualTargetIndex by rememberUpdatedState(visualTargetIndex)
            val currentLocalOrder by rememberUpdatedState(localOrder)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .onGloballyPositioned { coordinates ->
                        val pos = coordinates.positionInWindow()
                        containerTopInWindow = pos.y
                        containerBottomInWindow = pos.y + coordinates.size.height.toFloat()
                    }
                    .verticalFadingEdges(
                        scrollState = listScrollState,
                        topEdgeHeight = 28.dp,
                        bottomEdgeHeight = 28.dp
                    )
                    .premiumScrollbar(listScrollState, width = 3f, paddingEnd = 6f)
                    .verticalScroll(listScrollState)
                    .padding(horizontal = 24.dp)
                    .padding(top = 4.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                localOrder.forEachIndexed { index, itemKey ->
                    val movie = moviesMap[itemKey]
                    if (movie != null) {
                        key(itemKey) {
                            val isDragging = draggedItemKey == itemKey
                            val translationTarget = when {
                                isDragging -> dragOffset
                                draggedIndex != -1 && visualTargetIndex != -1 -> {
                                    if (draggedIndex < index && index <= visualTargetIndex) {
                                        -itemHeightPx
                                    } else if (draggedIndex > index && index >= visualTargetIndex) {
                                        itemHeightPx
                                    } else {
                                        0f
                                    }
                                }
                                else -> 0f
                            }
                            val translation = remember(dropTrigger) { Animatable(0f) }

                            LaunchedEffect(translationTarget) {
                                if (!isDragging) {
                                    translation.animateTo(
                                        targetValue = translationTarget,
                                        animationSpec = spring(
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }

                            val finalTranslation = if (isDragging) dragOffset else translation.value

                            var handleCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { coordinates ->
                                        if (itemHeightPx == 0f) {
                                            itemHeightPx = coordinates.size.height.toFloat() + with(density) { 10.dp.toPx() }
                                        }
                                    }
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .graphicsLayer { translationY = finalTranslation }
                                    .background(
                                        if (isDragging) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isDragging) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.06f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                // Drag Handle a sinistra in pieno stile FlickTrove Settings
                                Box(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(width = 34.dp, height = 44.dp)
                                        .onGloballyPositioned { handleCoords = it }
                                        .pointerInput(itemKey) {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    VibrationHelper.vibrateTick(context)
                                                    draggedItemKey = itemKey
                                                    val startY = (handleCoords?.positionInWindow()?.y ?: 0f) + offset.y
                                                    pointerWindowY = startY
                                                    autoScrollSpeed = 0f
                                                },
                                                onDragEnd = {
                                                    autoScrollSpeed = 0f
                                                    if (currentDraggedIndex != -1 && currentVisualTargetIndex != -1 && currentDraggedIndex != currentVisualTargetIndex) {
                                                        VibrationHelper.vibrateTick(context)
                                                        val newList = currentLocalOrder.toMutableList()
                                                        val item = newList.removeAt(currentDraggedIndex)
                                                        newList.add(currentVisualTargetIndex, item)
                                                        localOrder = newList
                                                        onMove(currentDraggedIndex, currentVisualTargetIndex)
                                                    }
                                                    draggedItemKey = null
                                                    dragOffset = 0f
                                                    dropTrigger++
                                                },
                                                onDragCancel = {
                                                    autoScrollSpeed = 0f
                                                    draggedItemKey = null
                                                    dragOffset = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffset += dragAmount.y
                                                    pointerWindowY += dragAmount.y

                                                    val thresholdPx = with(density) { 64.dp.toPx() }
                                                    if (containerBottomInWindow > 0f && pointerWindowY > containerBottomInWindow - thresholdPx) {
                                                        val overflow = (pointerWindowY - (containerBottomInWindow - thresholdPx)).coerceAtLeast(0f)
                                                        val ratio = (overflow / thresholdPx).coerceIn(0.25f, 2.5f)
                                                        autoScrollSpeed = with(density) { 12.dp.toPx() } * ratio
                                                    } else if (containerTopInWindow > 0f && pointerWindowY < containerTopInWindow + thresholdPx) {
                                                        val overflow = ((containerTopInWindow + thresholdPx) - pointerWindowY).coerceAtLeast(0f)
                                                        val ratio = (overflow / thresholdPx).coerceIn(0.25f, 2.5f)
                                                        autoScrollSpeed = -with(density) { 12.dp.toPx() } * ratio
                                                    } else {
                                                        autoScrollSpeed = 0f
                                                    }
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DragHandle,
                                        contentDescription = "Drag to reorder",
                                        tint = if (isDragging) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Poster Thumbnail
                                val posterUrl = remember(movie.posterPath) {
                                    movie.posterPath?.let { buildTmdbImageUrl(it, ImageType.POSTER, ImageQuality.LOW) }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(width = 38.dp, height = 54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.06f))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (posterUrl != null) {
                                        AsyncImage(
                                            model = posterUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Titolo e Sottotitolo (Anno • Tipo)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = movie.title ?: movie.name ?: "",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val isTv = movie.mediaType == "tv"
                                    val typeLabel = if (isTv) stringResource(R.string.notif_media_tv) else stringResource(R.string.notif_media_movie)
                                    val releaseYear = (movie.releaseDate ?: movie.firstAirDate)?.take(4)
                                    val subtitle = if (!releaseYear.isNullOrBlank()) "$releaseYear • $typeLabel" else typeLabel
                                    Text(
                                        text = subtitle,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                val currentDisplayIndex = when {
                                    isDragging && visualTargetIndex != -1 -> visualTargetIndex + 1
                                    draggedIndex != -1 && visualTargetIndex != -1 -> {
                                        if (draggedIndex < index && index <= visualTargetIndex) index
                                        else if (draggedIndex > index && index >= visualTargetIndex) index + 2
                                        else index + 1
                                    }
                                    else -> index + 1
                                }

                                Text(
                                    text = "#$currentDisplayIndex",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
