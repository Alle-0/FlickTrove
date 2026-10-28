package com.cinetrack.ui.components.share

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.cinetrack.R
import com.cinetrack.data.api.ImageItem
import com.cinetrack.data.api.ImagesResponse
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.common.SegmentedControl
import com.cinetrack.ui.components.common.SegmentedOption
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.shimmerEffect
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.VibrationHelper
import com.cinetrack.util.buildTmdbImageUrl
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import java.util.Locale

internal fun getFlagEmoji(languageCode: String?): String {
    if (languageCode.isNullOrBlank()) return "🌐"
    return when (languageCode.lowercase().trim()) {
        "it" -> "🇮🇹"
        "en" -> "🇬🇧"
        "us" -> "🇺🇸"
        "fr" -> "🇫🇷"
        "es" -> "🇪🇸"
        "de" -> "🇩🇪"
        "ja" -> "🇯🇵"
        "ko" -> "🇰🇷"
        "zh", "cn" -> "🇨🇳"
        "pt" -> "🇵🇹"
        "ru" -> "🇷🇺"
        "ar" -> "🇸🇦"
        "hi" -> "🇮🇳"
        "el" -> "🇬🇷"
        "tr" -> "🇹🇷"
        "nl" -> "🇳🇱"
        "pl" -> "🇵🇱"
        "sv" -> "🇸🇪"
        "da" -> "🇩🇰"
        "no" -> "🇳🇴"
        "fi" -> "🇫🇮"
        "cs" -> "🇨🇿"
        "hu" -> "🇭🇺"
        "ro" -> "🇷🇴"
        "th" -> "🇹🇭"
        "id" -> "🇮🇩"
        "vi" -> "🇻🇳"
        "uk" -> "🇺🇦"
        "he", "iw" -> "🇮🇱"
        "fa" -> "🇮🇷"
        "bn" -> "🇧🇩"
        "ur" -> "🇵🇰"
        "ta" -> "🇮🇳"
        "te" -> "🇮🇳"
        "ml" -> "🇮🇳"
        "mr" -> "🇮🇳"
        "gu" -> "🇮🇳"
        "kn" -> "🇮🇳"
        "tl" -> "🇵🇭"
        "ms" -> "🇲🇾"
        "sk" -> "🇸🇰"
        "bg" -> "🇧🇬"
        "hr" -> "🇭🇷"
        "sr" -> "🇷🇸"
        "sl" -> "🇸🇮"
        "et" -> "🇪🇪"
        "lv" -> "🇱🇻"
        "lt" -> "🇱🇹"
        "is" -> "🇮🇸"
        "ga" -> "🇮🇪"
        "eu" -> "🇪🇸"
        "ca" -> "🇪🇸"
        "gl" -> "🇪🇸"
        else -> "🌐"
    }
}

internal fun getLanguageDisplayName(code: String): String {
    val flag = getFlagEmoji(code)
    val loc = Locale.forLanguageTag(code)
    val localizedName = loc.getDisplayLanguage(Locale.getDefault()).replaceFirstChar { it.uppercase() }
    val display = if (localizedName.isNotBlank() && localizedName != code) localizedName else code.uppercase()
    return "$flag $display"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CoverSelectionView(
    movie: Movie,
    accentColor: Color,
    initialImages: ImagesResponse?,
    allMediaImages: ImagesResponse?,
    config: ShareStoryCardConfig,
    coverTab: Int,
    onCoverTabChange: (Int) -> Unit,
    onConfigChange: (ShareStoryCardConfig) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val userLang = remember { Locale.getDefault().language.lowercase() }

    val activeImages = allMediaImages ?: initialImages
    val rawPosters = remember(activeImages?.posters, movie.posterPath) {
        val list = activeImages?.posters ?: emptyList()
        val defaultPath = movie.posterPath
        if (defaultPath != null && list.none { it.filePath == defaultPath }) {
            listOf(ImageItem(filePath = defaultPath, iso6391 = null)) + list
        } else list
    }
    val rawBackdrops = remember(activeImages?.backdrops, movie.backdropPath, movie.customBackdropPath) {
        val list = activeImages?.backdrops ?: emptyList()
        val defaultPath = movie.customBackdropPath ?: movie.backdropPath
        if (defaultPath != null && list.none { it.filePath == defaultPath }) {
            listOf(ImageItem(filePath = defaultPath, iso6391 = null)) + list
        } else list
    }

    // "togli il senza testo": filtra solo le locandine con lingua definita
    val validPosters = remember(rawPosters) {
        rawPosters.filter { !it.iso6391.isNullOrBlank() }
    }

    // Lingue disponibili per le locandine (ordinate: lingua utente prima, poi inglese, poi alfabeticamente)
    val availableLanguages = remember(validPosters, userLang) {
        val codes = validPosters.mapNotNull { it.iso6391?.lowercase() }.filter { it.isNotBlank() }.distinct()
        codes.sortedWith { a, b ->
            when {
                a == b -> 0
                a == userLang -> -1
                b == userLang -> 1
                a == "en" -> -1
                b == "en" -> 1
                else -> a.compareTo(b)
            }
        }
    }

    // Per default seleziona la lingua dell'utente ("fai apparire quelle della tua lingua")
    var selectedLanguageCode by remember(validPosters, userLang) {
        mutableStateOf(
            when {
                validPosters.any { it.iso6391.equals(userLang, ignoreCase = true) } -> userLang
                validPosters.any { it.iso6391.equals("en", ignoreCase = true) } -> "en"
                availableLanguages.isNotEmpty() -> availableLanguages.first()
                else -> "all"
            }
        )
    }

    val filteredPosters = remember(validPosters, selectedLanguageCode) {
        if (selectedLanguageCode == "all") {
            validPosters
        } else {
            validPosters.filter { it.iso6391.equals(selectedLanguageCode, ignoreCase = true) }
        }
    }

    // Per i backdrop: filtra i backdrop con scritte straniere casuali (turco, polacco, ecc.)
    // Dà priorità assoluta a quelli textless (puliti senza scritte), poi alla lingua dell'utente e all'inglese
    val validBackdrops = remember(rawBackdrops, userLang) {
        val relevant = rawBackdrops.filter {
            it.iso6391.isNullOrBlank() ||
            it.iso6391.equals(userLang, ignoreCase = true) ||
            it.iso6391.equals("en", ignoreCase = true)
        }
        (if (relevant.isNotEmpty()) relevant else rawBackdrops).sortedWith { a, b ->
            val aLang = a.iso6391?.lowercase()
            val bLang = b.iso6391?.lowercase()
            when {
                aLang.isNullOrBlank() && !bLang.isNullOrBlank() -> -1
                !aLang.isNullOrBlank() && bLang.isNullOrBlank() -> 1
                aLang == userLang && bLang != userLang -> -1
                bLang == userLang && aLang != userLang -> 1
                aLang == "en" && bLang != "en" -> -1
                bLang == "en" && aLang != "en" -> 1
                else -> 0
            }
        }
    }

    val currentDisplayList = if (coverTab == 0) filteredPosters else validBackdrops

    val defaultPoster = movie.posterPath
    val defaultBackdrop = movie.customBackdropPath ?: movie.backdropPath
    val hasCustomForCurrentTab = if (coverTab == 0) {
        config.customPosterPath != null && config.customPosterPath != defaultPoster
    } else {
        config.customBackdropPath != null && config.customBackdropPath != defaultBackdrop
    }

    val coverHazeState = remember { HazeState() }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var languageButtonBottomPx by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(coverTab) { languageMenuExpanded = false }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .bounceClick(scaleDown = 0.90f) { onBack() }
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_left),
                            contentDescription = stringResource(R.string.detail_content_desc_back),
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.share_cover_picker_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = if (coverTab == 0) stringResource(R.string.share_cover_picker_poster_hint) else stringResource(R.string.share_cover_picker_backdrop_hint),
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.60f)
                        )
                    }
                }

                if (hasCustomForCurrentTab) {
                    Text(
                        text = stringResource(R.string.share_cover_picker_reset),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ColorUtils.lightenForText(accentColor, 1.35f),
                        modifier = Modifier
                            .bounceClick(scaleDown = 0.92f) {
                                if (coverTab == 0) {
                                    onConfigChange(config.copy(customPosterPath = null))
                                } else {
                                    onConfigChange(config.copy(customBackdropPath = null))
                                }
                                onBack()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tab Selector (Posters vs Backdrops)
            SegmentedControl(
                options = listOf(
                    SegmentedOption(value = 0, title = stringResource(R.string.share_cover_picker_posters_tab, validPosters.size)),
                    SegmentedOption(value = 1, title = stringResource(R.string.share_cover_picker_backdrops_tab, validBackdrops.size))
                ),
                selected = coverTab,
                accentColor = accentColor,
                vibrationEnabled = true,
                context = context,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                height = 36.dp,
                onSelect = { newTab ->
                    onCoverTabChange(newTab)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Sezione Filtro Lingua: SOLO per le Locandine (coverTab == 0) con pulsante a tendina e icona standard ic_right
            if (coverTab == 0 && availableLanguages.isNotEmpty()) {
                val chevronRotation by animateFloatAsState(
                    targetValue = if (languageMenuExpanded) 270f else 90f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "langChevron"
                )

                val currentCount = if (selectedLanguageCode == "all") {
                    validPosters.size
                } else {
                    validPosters.count { it.iso6391.equals(selectedLanguageCode, ignoreCase = true) }
                }
                val currentLabel = if (selectedLanguageCode == "all") {
                    stringResource(R.string.share_cover_picker_all_languages)
                } else {
                    getLanguageDisplayName(selectedLanguageCode)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .padding(horizontal = 24.dp)
                        .onGloballyPositioned { coordinates ->
                            languageButtonBottomPx = coordinates.positionInParent().y + coordinates.size.height
                        }
                        .bounceClick(scaleDown = 0.97f) { languageMenuExpanded = !languageMenuExpanded }
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = currentLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        CountPill(
                            count = currentCount,
                            isSelected = false,
                            accentColor = accentColor
                        )
                    }

                    Icon(
                        painter = painterResource(id = R.drawable.ic_right),
                        contentDescription = stringResource(R.string.share_cover_picker_select_language),
                        tint = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier
                            .size(15.dp)
                            .rotate(chevronRotation)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Image Grid con gradiente superiore e inferiore di dissolvenza allo scroll
            // Per i backdrop: combacia la ratio con quella selezionata
            val isPosterTab = coverTab == 0
            val itemAspectRatio = if (isPosterTab) (2f / 3f) else config.aspectRatio.ratio
            val columns = if (isPosterTab || itemAspectRatio < 1f) 3 else 2

            if (currentDisplayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .haze(state = coverHazeState, style = HazeStyles.PremiumDark),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_image),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.share_cover_picker_empty),
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                // Box con dissolvenza sfumata in cima e in fondo allo scorrimento
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 300.dp, max = 500.dp)
                        .haze(state = coverHazeState, style = HazeStyles.PremiumDark)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.04f to Color.Black,
                                    0.96f to Color.Black,
                                    1f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(currentDisplayList, key = { it.filePath }) { item ->
                            val isSelected = if (isPosterTab) {
                                (config.customPosterPath ?: defaultPoster) == item.filePath
                            } else {
                                (config.customBackdropPath ?: defaultBackdrop) == item.filePath
                            }

                            val imageUrl = remember(item.filePath) {
                                val imageType = if (isPosterTab) ImageType.POSTER else ImageType.BACKDROP
                                buildTmdbImageUrl(item.filePath, imageType, ImageQuality.HIGH)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(itemAspectRatio)
                                    .bounceClick(scaleDown = 0.95f) {
                                        VibrationHelper.vibrateTick(context)
                                        if (isPosterTab) {
                                            val newPath = if (item.filePath == defaultPoster) null else item.filePath
                                            onConfigChange(config.copy(customPosterPath = newPath))
                                        } else {
                                            val newPath = if (item.filePath == defaultBackdrop) null else item.filePath
                                            onConfigChange(config.copy(customBackdropPath = newPath))
                                        }
                                        onBack() // Si applica subito alla card e torna indietro
                                    }
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1E1E24))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) accentColor else Color.White.copy(alpha = 0.10f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                SubcomposeAsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    loading = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0xFF1E1E24))
                                                .shimmerEffect()
                                        )
                                    }
                                )

                                // Top-right selection badge
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .padding(6.dp)
                                            .align(Alignment.TopEnd)
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(accentColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_tick),
                                            contentDescription = stringResource(R.string.share_cover_picker_selected),
                                            tint = ColorUtils.contentColorForAccent(accentColor),
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Layer 2: Tap-outside dismiss overlay across the entire modal when dropdown is open
        if (languageMenuExpanded) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(50f)
                    .pointerInput(Unit) {
                        detectTapGestures { languageMenuExpanded = false }
                    }
            )
        }

        // Layer 3: Floating In-tree Glass Dropdown with Haze & Top/Bottom scroll fade gradient
        if (coverTab == 0 && availableLanguages.isNotEmpty()) {
            val dropdownTopDp = if (languageButtonBottomPx > 0f) {
                with(LocalDensity.current) { languageButtonBottomPx.toDp() + 6.dp }
            } else {
                156.dp
            }

            AnimatedVisibility(
                visible = languageMenuExpanded,
                enter = fadeIn(tween(180)) + scaleIn(
                    initialScale = 0.95f,
                    transformOrigin = TransformOrigin(0.5f, 0f),
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ),
                exit = fadeOut(tween(130)) + scaleOut(
                    targetScale = 0.95f,
                    transformOrigin = TransformOrigin(0.5f, 0f),
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .offset(y = dropdownTopDp)
                    .zIndex(60f)
            ) {
                val scrollState = rememberScrollState()
                val canScrollUp = scrollState.canScrollBackward
                val canScrollDown = scrollState.canScrollForward

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredHeightIn(max = 280.dp)
                        .shadow(20.dp, RoundedCornerShape(16.dp))
                        .hazeGlass(
                            state = coverHazeState,
                            shape = RoundedCornerShape(16.dp),
                            blurRadius = 24.dp,
                            containerColor = Color(0xFF141418).copy(alpha = 0.60f),
                            borderWidth = 1.dp,
                            borderColor = Color.White.copy(alpha = 0.18f)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                if (canScrollUp || canScrollDown) {
                                    val topStop = if (canScrollUp) 0.08f else 0f
                                    val bottomStop = if (canScrollDown) 0.92f else 1f
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0f to (if (canScrollUp) Color.Transparent else Color.Black),
                                            topStop to Color.Black,
                                            bottomStop to Color.Black,
                                            1f to (if (canScrollDown) Color.Transparent else Color.Black)
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                            }
                            .verticalScroll(scrollState)
                            .padding(vertical = 6.dp)
                    ) {
                        // Opzione "Tutte le lingue"
                        val totalPosters = validPosters.size
                        val isAllSelected = selectedLanguageCode == "all"
                        LanguageMenuItem(
                            text = stringResource(R.string.share_cover_picker_all_languages),
                            count = totalPosters,
                            isSelected = isAllSelected,
                            accentColor = accentColor,
                            onClick = {
                                selectedLanguageCode = "all"
                                languageMenuExpanded = false
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            thickness = 0.5.dp
                        )

                        availableLanguages.forEach { code ->
                            val count = validPosters.count { it.iso6391.equals(code, ignoreCase = true) }
                            val isCurrent = selectedLanguageCode.equals(code, ignoreCase = true)
                            LanguageMenuItem(
                                text = getLanguageDisplayName(code),
                                count = count,
                                isSelected = isCurrent,
                                accentColor = accentColor,
                                onClick = {
                                    selectedLanguageCode = code
                                    languageMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun LanguageMenuItem(
    text: String,
    count: Int,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .bounceClick(scaleDown = 0.98f, onClick = onClick)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 13.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) ColorUtils.lightenForText(accentColor, 1.35f) else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(12.dp))
        CountPill(
            count = count,
            isSelected = isSelected,
            accentColor = accentColor
        )
    }
}

@Composable
internal fun CountPill(
    count: Int,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.24f)
                else Color.White.copy(alpha = 0.09f)
            )
            .border(
                0.6.dp,
                if (isSelected) accentColor.copy(alpha = 0.60f)
                else Color.White.copy(alpha = 0.14f),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isSelected) ColorUtils.lightenForText(accentColor, 1.35f) else Color.White.copy(alpha = 0.75f),
            lineHeight = 11.sp
        )
    }
}

