package com.cinetrack.ui.components.search

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.model.KeywordDictionary
import com.cinetrack.data.model.SortConfig
import com.cinetrack.data.model.UserPreferences
import com.cinetrack.ui.components.shared.layoutToggleIcon
import com.cinetrack.ui.components.shared.nextGridColumns
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.FilterPill
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Barra di ricerca a comportamento viscoelastico simbionte.
 *
 * Quando la categoria non prevede filtri ("person", "collection"), la barra di ricerca
 * si allunga verso destra inglobando fluidamente i pulsanti d'azione (filtri e layout toggle)
 * mediante fusione e ritrazione liquida.
 *
 * Al ritorno alle categorie con filtri ("movie", "tv"), la barra si restringe verso sinistra
 * e "rigurgita" dall'apice destro una goccia liquida organica (metaball), che si allunga
 * formando un collo di filamento teso con pointedness conica, per poi staccarsi con uno snap elastico
 * (rinculo e spring bounce) trasformandosi nel pulsante circolare perfetto dei filtri.
 */
@Composable
fun SymbiontSearchBarWithFilters(
    query: String,
    category: String,
    sortConfig: SortConfig,
    preferences: UserPreferences,
    suggestedFilters: List<FilterPill>,
    initialGenreName: String?,
    initialKeywordName: String?,
    textFieldValue: TextFieldValue,
    onTextFieldValueChange: (TextFieldValue) -> Unit,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onLayoutToggleClick: (Int) -> Unit,
    onFilterClick: () -> Unit,
    focusRequester: FocusRequester,
    keyboardController: SoftwareKeyboardController?,
    onFilterBoundsMeasured: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val filtersVisible = category != "person" && category != "collection"
    val density = LocalDensity.current

    // Progresso animato: 0f = filtri completamente inglobati (barra a tutta larghezza)
    //                     1f = filtri completamente rigurgitati e stabilizzati a destra
    val animProgress = remember { Animatable(if (filtersVisible) 1f else 0f) }
    var hasSnappedBounce by remember { mutableStateOf(false) }

    val bounceOffsetX = remember { Animatable(0f) }
    val bounceScale = remember { Animatable(1f) }
    val searchBounceX = remember { Animatable(0f) }

    LaunchedEffect(filtersVisible) {
        if (filtersVisible) {
            hasSnappedBounce = false
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 360,
                    easing = FastOutSlowInEasing
                )
            )
        } else {
            hasSnappedBounce = false
            animProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    val p = animProgress.value

    // Trigger dello Snap e del rimbalzo elastico alla soglia p = 0.68
    LaunchedEffect(p >= 0.68f) {
        if (p >= 0.68f && !hasSnappedBounce) {
            hasSnappedBounce = true
            launch {
                bounceOffsetX.snapTo(4f)
                bounceOffsetX.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                bounceScale.snapTo(1.16f)
                bounceScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                searchBounceX.snapTo(-3f)
                searchBounceX.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        } else if (p < 0.20f) {
            hasSnappedBounce = false
        }
    }

    // Rimbalzo morbido di arrivo quando la barra ingloba tutto a fine corsa (p -> 0)
    LaunchedEffect(p <= 0.02f && !filtersVisible) {
        if (p <= 0.02f && !filtersVisible) {
            searchBounceX.snapTo(3f)
            searchBounceX.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val isDetached = p >= 0.68f

    // Ritrazione rapida del filamento residuo verso la calotta della barra dopo lo snap
    val filamentRecoil by animateFloatAsState(
        targetValue = if (isDetached) 0f else 1f,
        animationSpec = tween(durationMillis = 130, easing = FastOutLinearInEasing),
        label = "SymbiontFilamentRecoil"
    )

    val hasActiveFilters = sortConfig.selectedGenres.isNotEmpty() ||
            sortConfig.selectedKeywords.isNotEmpty() ||
            sortConfig.selectedProviders.isNotEmpty() ||
            sortConfig.selectedDecades.isNotEmpty() ||
            sortConfig.sortType != "popularity"

    val symbioteColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)

    BoxWithConstraints(
        modifier = modifier.height(44.dp)
    ) {
        val totalWidth = maxWidth
        val totalWidthPx = with(density) { totalWidth.toPx() }
        val heightPx = with(density) { 44.dp.toPx() }
        val cy = heightPx / 2f
        val rCapPx = heightPx / 2f

        val showLayoutToggle = preferences.showLayoutToggle
        val filterGroupWidth = if (showLayoutToggle) (44.dp + 12.dp + 44.dp) else 44.dp
        val filterGroupWidthPx = with(density) { filterGroupWidth.toPx() }
        val gapPx = with(density) { 12.dp.toPx() }
        val totalFiltersSpacePx = filterGroupWidthPx + gapPx

        // Larghezza animata della barra di ricerca
        val searchBarWidthPx = (totalWidthPx - (totalFiltersSpacePx * p) + with(density) { searchBounceX.value.dp.toPx() })
            .coerceIn(rCapPx * 2f, totalWidthPx)
        val searchBarWidth = with(density) { searchBarWidthPx.toDp() }

        // Posizione centro del cerchio filtri a riposo
        val restBtnCenterPx = totalWidthPx - rCapPx
        // Posizione della calotta destra della barra
        val xLeftCapPx = searchBarWidthPx - rCapPx

        val onSurfaceColor = MaterialTheme.colorScheme.onSurface

        // --- LAYER UNIFICATO SFONDO SIMBIONTE (CompositingStrategy.Offscreen per Zero Sovrapposizione Alpha) ---
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    compositingStrategy = CompositingStrategy.Offscreen,
                    alpha = 0.05f
                )
        ) {
            // 1. Sfondo base della barra di ricerca a capsula (onSurface solido ad alpha 1.0)
            drawRoundRect(
                color = onSurfaceColor,
                topLeft = Offset.Zero,
                size = Size(searchBarWidthPx, heightPx),
                cornerRadius = CornerRadius(cy, cy)
            )

            // 2. Rinculo del filamento subito dopo lo snap elastico (da 6dp a 0dp in 130ms)
            if (isDetached && filamentRecoil > 0.01f) {
                val recoilLen = with(density) { 6.dp.toPx() } * filamentRecoil
                val nubPath = Path().apply {
                    moveTo(xLeftCapPx, cy - rCapPx * 0.4f * filamentRecoil)
                    cubicTo(
                        x1 = xLeftCapPx + rCapPx * 0.8f, y1 = cy - rCapPx * 0.2f * filamentRecoil,
                        x2 = xLeftCapPx + rCapPx + recoilLen, y2 = cy,
                        x3 = xLeftCapPx + rCapPx + recoilLen, y3 = cy
                    )
                    cubicTo(
                        x1 = xLeftCapPx + rCapPx + recoilLen, y1 = cy,
                        x2 = xLeftCapPx + rCapPx * 0.8f, y2 = cy + rCapPx * 0.2f * filamentRecoil,
                        x3 = xLeftCapPx, y3 = cy + rCapPx * 0.4f * filamentRecoil
                    )
                    close()
                }
                drawPath(nubPath, color = onSurfaceColor)
            }

            // 3. Ponte liquido viscoelastico e goccia emergente durante la connessione simbiotica (0.02 < p < 0.68)
            if (!isDetached && p > 0.02f) {
                val t = (p / 0.68f).coerceIn(0f, 1f)
                val startDropX = xLeftCapPx + rCapPx
                val xDrop = startDropX + (restBtnCenterPx - startDropX) * t
                val rDrop = (with(density) { 4.dp.toPx() } + (rCapPx - with(density) { 4.dp.toPx() }) * t).coerceAtLeast(1f)

                val yBase = rCapPx * (1f - 0.25f * t)
                val waistHalfHeight = (yBase.coerceAtMost(rDrop) * (1f - 0.72f * t))
                    .coerceAtLeast(with(density) { 1.5.dp.toPx() })

                val distX = (xDrop - xLeftCapPx).coerceAtLeast(1f)
                val c1X = xLeftCapPx + distX * 0.35f
                val c1YTop = cy - waistHalfHeight * 1.15f
                val c1YBot = cy + waistHalfHeight * 1.15f

                val pointedness = 0.3f + 0.9f * t
                val c2X = (xDrop - rDrop * pointedness).coerceAtLeast(c1X + distX * 0.1f)
                val c2YTop = cy - rDrop
                val c2YBot = cy + rDrop

                val bridgePath = Path().apply {
                    moveTo(xLeftCapPx, cy - yBase)
                    cubicTo(
                        x1 = c1X, y1 = c1YTop,
                        x2 = c2X, y2 = c2YTop,
                        x3 = xDrop, y3 = cy - rDrop
                    )
                    arcTo(
                        rect = Rect(
                            left = xDrop - rDrop,
                            top = cy - rDrop,
                            right = xDrop + rDrop,
                            bottom = cy + rDrop
                        ),
                        startAngleDegrees = -90f,
                        sweepAngleDegrees = 180f,
                        forceMoveTo = false
                    )
                    cubicTo(
                        x1 = c2X, y1 = c2YBot,
                        x2 = c1X, y2 = c1YBot,
                        x3 = xLeftCapPx, y3 = cy + yBase
                    )
                    lineTo(xLeftCapPx, cy - yBase)
                    close()
                }

                // Disegno con onSurface solido: dove il ponte si innesta nella calotta della barra,
                // la fusione è al 100% solida nel buffer e viene scalata globalmente a 0.05f alpha
                // eliminando alla radice qualsiasi regione più chiara o bordo interno.
                drawPath(bridgePath, color = onSurfaceColor)
            }
        }

        // --- BARRA DI RICERCA (Capsula elastica con contenuti) ---
        Box(
            modifier = Modifier
                .width(searchBarWidth)
                .height(44.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        try {
                            focusRequester.requestFocus()
                        } catch (_: Exception) {}
                        keyboardController?.show()
                    }
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 14.dp, end = 2.dp)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_lente),
                    contentDescription = stringResource(R.string.search_content_desc),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                val primaryColor = MaterialTheme.colorScheme.primary
                CompositionLocalProvider(
                    LocalTextSelectionColors provides TextSelectionColors(
                        handleColor = primaryColor,
                        backgroundColor = primaryColor.copy(alpha = 0.4f)
                    )
                ) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = {
                            onTextFieldValueChange(it)
                            onQueryChanged(it.text)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onQueryChanged(query) }),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    val placeholderText = when {
                                        sortConfig.selectedGenres.isNotEmpty() -> {
                                            val gid = sortConfig.selectedGenres.first()
                                            val name = suggestedFilters.find { it.id == gid && !it.isKeyword }?.name
                                                ?: initialGenreName
                                                ?: stringResource(R.string.search_fallback_genre)
                                            stringResource(R.string.search_active_genre_format, name)
                                        }
                                        sortConfig.selectedKeywords.isNotEmpty() -> {
                                            val kid = sortConfig.selectedKeywords.first()
                                            val dictName = KeywordDictionary.getLocalizedKeywordName(kid, preferences.contentLanguage)
                                                ?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                                            val name = suggestedFilters.find { it.id == kid }?.name
                                                ?: dictName
                                                ?: initialKeywordName
                                                ?: stringResource(R.string.search_fallback_keyword)
                                            stringResource(R.string.search_active_keyword_format, name)
                                        }
                                        else -> when (category) {
                                            "tv" -> stringResource(R.string.search_placeholder_tv)
                                            "person" -> stringResource(R.string.search_placeholder_person)
                                            "collection" -> stringResource(R.string.search_placeholder_collection)
                                            else -> stringResource(R.string.search_placeholder_movie)
                                        }
                                    }
                                    val hasActiveFilter = sortConfig.selectedGenres.isNotEmpty() || sortConfig.selectedKeywords.isNotEmpty()
                                    Text(
                                        text = placeholderText,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (hasActiveFilter) 0.6f else 0.3f),
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .bounceClick(scaleDown = 0.82f) {
                            onTextFieldValueChange(TextFieldValue(""))
                            onClearQuery()
                            focusRequester.requestFocus()
                            keyboardController?.show()
                        }
                        .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
                            contentDescription = stringResource(R.string.search_content_desc_clear),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(12.dp))
                }
            }
        }

        // --- CONTROLLI E ICONE DEI FILTRI (Visibili a distacco avvenuto p >= 0.68) ---
        if (isDetached || (p > 0.65f && filtersVisible)) {
            val buttonsAlpha = if (isDetached) {
                if (!filtersVisible) ((p - 0.68f) / 0.25f).coerceIn(0f, 1f) else 1f
            } else {
                ((p - 0.65f) / 0.03f).coerceIn(0f, 1f)
            }

            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = bounceOffsetX.value.dp)
                    .graphicsLayer {
                        scaleX = bounceScale.value
                        scaleY = bounceScale.value
                        alpha = buttonsAlpha
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showLayoutToggle) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .bounceClick(scaleDown = 0.92f) {
                                val nextColumns = nextGridColumns(preferences.gridColumns)
                                onLayoutToggleClick(nextColumns)
                            }
                            .background(symbioteColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = layoutToggleIcon(preferences.gridColumns),
                            contentDescription = stringResource(R.string.search_content_desc_layout),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .onGloballyPositioned { layoutCoordinates ->
                            if (isDetached) {
                                val position = layoutCoordinates.positionInWindow()
                                onFilterBoundsMeasured(
                                    Rect(
                                        position.x,
                                        position.y,
                                        position.x + layoutCoordinates.size.width,
                                        position.y + layoutCoordinates.size.height
                                    )
                                )
                            }
                        }
                        .bounceClick {
                            keyboardController?.hide()
                            onFilterClick()
                        }
                        .background(symbioteColor, CircleShape)
                        .then(
                            if (hasActiveFilters) {
                                val borderAlpha = if (isDetached) 1f else ((p - 0.68f) / 0.32f).coerceIn(0f, 1f)
                                Modifier.border(
                                    BorderStroke(
                                        1.5.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha)
                                    ),
                                    CircleShape
                                )
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_filtri),
                        contentDescription = stringResource(R.string.folder_detail_filters),
                        tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
