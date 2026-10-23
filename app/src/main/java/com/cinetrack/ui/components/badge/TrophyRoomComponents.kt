package com.cinetrack.ui.components.badge

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.cinetrack.R
import com.cinetrack.data.repository.BadgeRepository
import com.cinetrack.ui.components.common.CinematicBackground
import com.cinetrack.ui.components.glass.GlassmorphicModal
import com.cinetrack.ui.components.navigation.GlassyBottomBar
import com.cinetrack.ui.components.navigation.GlassyTopBar
import com.cinetrack.ui.components.shared.ModalCloseButton
import com.cinetrack.ui.components.stats.statsCard
import com.cinetrack.ui.theme.FlickTroveTheme
import com.cinetrack.ui.theme.NeonTeal
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch

/**
 * Schermata Vetrina dei Trofei (Trophy Showcase) in Puro Stile FlickTrove:
 * - Sfondo: CinematicBackground()
 * - TopBar: GlassyTopBar nativa FlickTrove (pulsante filtri unico in alto a destra con bounds)
 * - Dashboard: Card sobria ed elegante in stile statsCard FlickTrove
 * - Griglia Trofei: 2 colonne con le card rifinite (senza doppioni di tab/filtri in mezzo)
 * - BottomBar: GlassyBottomBar nativa FlickTrove (i 4 tab storici: Home, Da vedere, Visti, Account)
 * - Modali: MorphGlassModal nativo per i filtri e GlassmorphicModal concentrico a 32dp per il dettaglio.
 */
@Composable
fun TrophyRoomScreenContent(
    trophyItems: List<TrophyRoomItemUi> = OFFICIAL_TROPHY_ROOM_CATALOG,
    paddingValues: PaddingValues = PaddingValues(),
    hazeState: HazeState? = null,
    targetBadgeId: String? = null,
    onTargetBadgeConsumed: () -> Unit = {},
    isFilterModalOpenExternal: Boolean = false,
    onFilterModalOpenExternalChange: (Boolean) -> Unit = {},
    onVerifySupporter: (suspend (String) -> BadgeRepository.SupporterVerificationResult)? = null,
    onMovieClick: ((Long, String) -> Unit)? = null,
    onResolveUnlockingMovies: (suspend (String) -> List<com.cinetrack.data.model.Movie>)? = null,
    onResolveUnlockingMovie: (suspend (String) -> com.cinetrack.data.model.Movie?)? = null
) {
    val localHaze = hazeState ?: remember { HazeState() }
    val gridState = rememberLazyGridState()

    var filterConfig by remember { mutableStateOf(TrophyFilterConfig()) }
    var filterTriggerBounds by remember { mutableStateOf<Rect?>(null) }
    // Se controllato dall'esterno (via MainScreen TopBar), usa lo stato esterno
    var isFilterModalOpenInternal by remember { mutableStateOf(false) }
    val isFilterModalOpen = isFilterModalOpenExternal || isFilterModalOpenInternal
    var selectedBadgeForModal by remember { mutableStateOf<TrophyRoomItemUi?>(null) }
    val activeTrophyDetailState = com.cinetrack.ui.LocalActiveTrophyDetailConfig.current
    val onSelectBadge: (TrophyRoomItemUi) -> Unit = { item ->
        selectedBadgeForModal = item
        activeTrophyDetailState?.value = com.cinetrack.ui.TrophyDetailModalConfig(
            item = item,
            onClose = {
                selectedBadgeForModal = null
                activeTrophyDetailState.value = null
            },
            onVerifySupporter = onVerifySupporter,
            onMovieClick = onMovieClick,
            onResolveUnlockingMovies = onResolveUnlockingMovies,
            onResolveUnlockingMovie = onResolveUnlockingMovie
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            activeTrophyDetailState?.value = null
        }
    }

    // Registra la configurazione filtri nel contesto globale di MainScreen per la GlassyTopBar
    val activeTrophyFilterState = com.cinetrack.ui.LocalActiveTrophyFilterConfig.current
    DisposableEffect(filterConfig) {
        activeTrophyFilterState.value = com.cinetrack.ui.TrophyFilterModalConfig(
            config = filterConfig,
            onApply = { newConfig -> filterConfig = newConfig }
        )
        onDispose {
            activeTrophyFilterState.value = null
        }
    }

    val unlockedCount = trophyItems.count { it.isUnlocked }
    val totalCount = trophyItems.size
    val completionFraction = (unlockedCount.toFloat() / totalCount.coerceAtLeast(1)).coerceIn(0f, 1f)
    val unlockedTierCounts = remember(trophyItems) {
        trophyItems.filter { it.isUnlocked }.groupBy { it.currentTier }.mapValues { it.value.size }
    }

    // 1. Filtraggio coerente guidato dal modale dei filtri
    val filteredItems = remember(filterConfig, trophyItems) {
        trophyItems.filter { item ->
            // Filtro Categoria (Multi-selezione: vuoto significa tutti)
            val matchesCategory = if (filterConfig.categories.isEmpty() || TrophyFilterCategory.ALL in filterConfig.categories) {
                true
            } else {
                filterConfig.categories.any { cat ->
                    when (cat) {
                        TrophyFilterCategory.PROGRESSIVE -> item.badge.category == BadgeTypeCategory.PROGRESSIVE_TIERS && !item.badge.id.startsWith("genre_") && item.badge.id != "world_tour"
                        TrophyFilterCategory.GENRES_ERAS -> item.badge.id.startsWith("genre_") || item.badge.id == "world_tour"
                        TrophyFilterCategory.HONORS -> item.badge.category == BadgeTypeCategory.DAY_ONE_HONOR || item.badge.category == BadgeTypeCategory.SPECIAL_ACHIEVEMENT
                        TrophyFilterCategory.LOST_REEL -> item.badge.category == BadgeTypeCategory.LOST_REEL_SECRET
                        TrophyFilterCategory.ALL -> true
                    }
                }
            }
            // Filtro Stato
            val matchesStatus = when (filterConfig.status) {
                TrophyStatusFilter.UNLOCKED -> item.isUnlocked
                TrophyStatusFilter.LOCKED -> !item.isUnlocked
                TrophyStatusFilter.ALL -> true
            }
            // Filtro Formato Tier (Multi-selezione: vuoto significa tutti)
            val matchesTier = filterConfig.tiers.isEmpty() || item.currentTier in filterConfig.tiers

            matchesCategory && matchesStatus && matchesTier
        }
    }

    // 2. Ordinamento Dinamico FlickTrove (guidato da filterConfig.sortBy e filterConfig.sortDirection):
    val sortedItems = remember(filteredItems, filterConfig.sortBy, filterConfig.sortDirection) {
        val baseComparator = compareByDescending<TrophyRoomItemUi> { it.isUnlocked }
        val specificComparator = when (filterConfig.sortBy) {
            TrophySortOption.TIER -> {
                compareByDescending<TrophyRoomItemUi> { getTierRank(it.currentTier) }
                    .thenByDescending { it.completionFraction() }
                    .thenBy { it.badge.title.lowercase() }
            }
            TrophySortOption.PROGRESS -> {
                compareByDescending<TrophyRoomItemUi> { it.completionFraction() }
                    .thenByDescending { getTierRank(it.currentTier) }
                    .thenBy { it.badge.title.lowercase() }
            }
            TrophySortOption.RARITY -> {
                compareBy<TrophyRoomItemUi> {
                    it.badge.rarityPercent.replace("%", "").trim().toFloatOrNull() ?: 100f
                }
                    .thenByDescending { getTierRank(it.currentTier) }
                    .thenBy { it.badge.title.lowercase() }
            }
            TrophySortOption.RECENT -> {
                compareByDescending<TrophyRoomItemUi> { it.unlockedDate ?: "" }
                    .thenByDescending { getTierRank(it.currentTier) }
                    .thenBy { it.badge.title.lowercase() }
            }
            TrophySortOption.ALPHABETICAL -> {
                compareBy<TrophyRoomItemUi> { it.badge.title.lowercase() }
                    .thenByDescending { getTierRank(it.currentTier) }
            }
        }
        val finalComparator = if (filterConfig.sortDirection == "asc") {
            baseComparator.then(specificComparator.reversed())
        } else {
            baseComparator.then(specificComparator)
        }
        filteredItems.sortedWith(finalComparator)
    }

    LaunchedEffect(targetBadgeId, sortedItems) {
        if (targetBadgeId != null && sortedItems.isNotEmpty()) {
            val itemIndex = sortedItems.indexOfFirst { it.badge.id == targetBadgeId }
            if (itemIndex != -1) {
                val targetItem = sortedItems[itemIndex]
                val gridIndex = itemIndex + 2 // Salta TrophyStatsSummaryCard e Spacer
                try {
                    gridState.animateScrollToItem(gridIndex)
                } catch (_: Exception) {}
                onSelectBadge(targetItem)
                onTargetBadgeConsumed()
            } else if (filterConfig.hasActiveFilters) {
                // Se i filtri attivi nascondono il trofeo, resettiamo per mostrarlo
                filterConfig = TrophyFilterConfig()
            }
        }
    }

    // Il top padding viene fornito da LocalAppPadding di MainScreen (via GlassyTopBar height + status bar)
    // Il bottom padding viene fornito da LocalAppPadding (80.dp per BottomBar)
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val cutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val effectiveTopInset = maxOf(statusBarTop, cutoutTop)
    val topPadding = if (paddingValues.calculateTopPadding() > 0.dp) paddingValues.calculateTopPadding() else effectiveTopInset + 10.dp + 46.dp + 22.dp
    val bottomPadding = if (paddingValues.calculateBottomPadding() > 0.dp) paddingValues.calculateBottomPadding() + 160.dp else 240.dp

    Box(modifier = Modifier.fillMaxSize()) {
        val hazeState = localHaze
        // 1. Sfondo Cinematografico FlickTrove
        CinematicBackground()

        // 2. Griglia dei Trofei con Haze
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = topPadding,
                bottom = bottomPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState)
        ) {
            // A. STATS CARD MINIMALISTA & CINETECA SUMMARY (Stile statsCard autentico FlickTrove)
            item(span = { GridItemSpan(maxLineSpan) }) {
                TrophyStatsSummaryCard(
                    unlockedCount = unlockedCount,
                    totalCount = totalCount,
                    completionFraction = completionFraction,
                    tierCounts = unlockedTierCounts
                )
            }

            // B. SEGNAPOSTO SPAZIALE TRA STATS E GRIGLIA (Niente filtri duplicati qui: sono nell'icona TopBar)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(2.dp))
            }

            // C. LE CARD DEI TROFEI NELLA GRIGLIA (Ordinamento: Sbloccati in cima, bloccati in fondo, tutto per rarità)
            items(
                items = sortedItems,
                key = { it.badge.id }
            ) { item ->
                TrophyBadgeCard(
                    badge = item.badge,
                    tier = item.currentTier,
                    isUnlocked = item.isUnlocked,
                    onClick = {
                        onSelectBadge(item)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        // La GlassyTopBar e la GlassyBottomBar sono fornite da MainScreen (fisse e condivise).
        // Il pulsante filtro nella TopBar triggera isFilterModalOpen tramite onFilterRequest.

        // 5. MODALE DI DETTAGLIO OVERLAY (Fallback locale se invocato fuori da MainScreen o in Preview)
        if (activeTrophyDetailState == null && selectedBadgeForModal != null) {
            val currentModalItem = selectedBadgeForModal!!
            GlassmorphicModal(
                visible = true,
                activeHazeState = localHaze,
                dimBackground = true,
                dismissOnClickOutside = true,
                animate = false,
                onDismissRequest = { selectedBadgeForModal = null }
            ) {
                TrophyDetailContent(
                    item = currentModalItem,
                    onClose = { selectedBadgeForModal = null },
                    onVerifySupporter = onVerifySupporter,
                    onMovieClick = onMovieClick,
                    onResolveUnlockingMovies = onResolveUnlockingMovies,
                    onResolveUnlockingMovie = onResolveUnlockingMovie
                )
            }
        }

        // 6. MODALE DEI FILTRI (Fallback se invocato fuori da MainScreen)
        if (activeTrophyFilterState.value == null) {
            TrophyFilterModal(
                isVisible = isFilterModalOpen,
                config = filterConfig,
                triggerBounds = filterTriggerBounds,
                hazeState = hazeState,
                onApply = { newConfig ->
                    filterConfig = newConfig
                    isFilterModalOpenInternal = false
                    onFilterModalOpenExternalChange(false)
                },
                onDismissRequest = {
                    isFilterModalOpenInternal = false
                    onFilterModalOpenExternalChange(false)
                }
            )
        }
    }
}

/**
 * Card di Riepilogo Trofei sobria ed elegante, costruita con Modifier.statsCard()
 * in perfetto stile FlickTrove (dark neutro, bordi satinati e micro-metriche).
 */
@Composable
private fun TrophyStatsSummaryCard(
    unlockedCount: Int,
    totalCount: Int,
    completionFraction: Float,
    tierCounts: Map<PreviewBadgeTier, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statsCard()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Header: solo titolo sezione pulito
            Text(
                text = stringResource(R.string.trophy_palmares_title),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                ),
                color = NeonTeal
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Body: Stat Ruota a sinistra + Rarità a destra
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. STAT RUOTA A SINISTRA (Ingrandita a 116.dp per bilanciare le 3 righe dei tier)
                TrophyMultiTierWheel(
                    unlockedCount = unlockedCount,
                    totalCount = totalCount,
                    completionFraction = completionFraction,
                    tierCounts = tierCounts,
                    modifier = Modifier.size(116.dp)
                )

                // 2. RARITÀ A DESTRA (2 colonne x 3 righe come prima, perfettamente allineate)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactTierBadge(PreviewBadgeTier.SUPER_8, (tierCounts[PreviewBadgeTier.SUPER_8] ?: 0).toString(), Modifier.weight(1f))
                        CompactTierBadge(PreviewBadgeTier.MM_16, (tierCounts[PreviewBadgeTier.MM_16] ?: 0).toString(), Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactTierBadge(PreviewBadgeTier.MM_35, (tierCounts[PreviewBadgeTier.MM_35] ?: 0).toString(), Modifier.weight(1f))
                        CompactTierBadge(PreviewBadgeTier.MM_70, (tierCounts[PreviewBadgeTier.MM_70] ?: 0).toString(), Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactTierBadge(PreviewBadgeTier.THE_FINAL_CUT, (tierCounts[PreviewBadgeTier.THE_FINAL_CUT] ?: 0).toString(), Modifier.weight(1f))
                        CompactTierBadge(PreviewBadgeTier.LOST_REEL, (tierCounts[PreviewBadgeTier.LOST_REEL] ?: 0).toString(), Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TrophyMultiTierWheel(
    unlockedCount: Int,
    totalCount: Int,
    completionFraction: Float,
    tierCounts: Map<PreviewBadgeTier, Int>,
    modifier: Modifier = Modifier
) {
    val tierOrder = remember {
        listOf(
            PreviewBadgeTier.SUPER_8,
            PreviewBadgeTier.MM_16,
            PreviewBadgeTier.MM_35,
            PreviewBadgeTier.MM_70,
            PreviewBadgeTier.THE_FINAL_CUT,
            PreviewBadgeTier.LOST_REEL
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = 8.5.dp.toPx()
            val diameter = size.minDimension - strokeWidthPx
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)
            val topLeft = androidx.compose.ui.geometry.Offset(strokeWidthPx / 2f, strokeWidthPx / 2f)

            // Anello di sfondo track satinato
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = diameter / 2f,
                style = Stroke(width = strokeWidthPx)
            )

            val safeTotal = totalCount.coerceAtLeast(1)
            val radiusPx = diameter / 2f
            val circumference = (2f * Math.PI.toFloat() * radiusPx).coerceAtLeast(1f)
            val capAngle = ((strokeWidthPx / 2f) / circumference) * 360f

            val activeTiers = tierOrder.filter { (tierCounts[it] ?: 0) > 0 }
            val hasMultipleTiers = activeTiers.size > 1
            val gapAngle = if (hasMultipleTiers) 3.5f else 0f

            var nextVisualStart = -90f

            for (tier in activeTiers) {
                val count = tierCounts[tier] ?: 0
                val rawSweep = (count.toFloat() / safeTotal.toFloat()) * 360f
                if (rawSweep > 0f) {
                    // Ampiezza visiva minima per consentire le due estremità arrotondate (pillola o punto circolare)
                    val minPillSpan = 2f * capAngle + 0.5f
                    val visualSpan = rawSweep.coerceAtLeast(minPillSpan)

                    val visualStart = nextVisualStart
                    val visualEnd = visualStart + visualSpan

                    // Il centro dell'arco tiene conto del cap arrotondato che sporge di capAngle a ciascun estremo
                    val arcStart = visualStart + capAngle
                    val arcSweep = (visualSpan - 2f * capAngle).coerceAtLeast(0.1f)

                    drawArc(
                        color = tier.primaryColor,
                        startAngle = arcStart,
                        sweepAngle = arcSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = strokeWidthPx,
                            cap = StrokeCap.Round
                        )
                    )

                    // Il segmento successivo inizia rigidamente DOPO il termine visivo del precedente + gap di respiro
                    nextVisualStart = visualEnd + gapAngle
                }
            }
        }

        // Centro della ruota: Percentuale in grande + conteggio
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${(completionFraction * 100).toInt()}%",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$unlockedCount/$totalCount",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun CompactTierBadge(
    tier: PreviewBadgeTier,
    count: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(26.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(
                width = 1.dp,
                color = tier.primaryColor.copy(alpha = 0.85f),
                shape = CircleShape
            )
            .padding(start = 7.dp, top = 2.dp, bottom = 2.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Pallino colorato del Tier
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(tier.primaryColor)
        )

        Spacer(modifier = Modifier.width(4.dp))

        // 2. Nome del Formato / Tier in bianco
        Text(
            text = tier.localizedFormatName(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            ),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.weight(1f))

        // 3. Badge numerico circolare concentrico a destra
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.18f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                ),
                color = Color.White.copy(alpha = 0.95f),
                textAlign = TextAlign.Center
            )
        }
    }
}




// -------------------------------------------------------------
// ANTEPRIME COMPOSE ANDROID STUDIO (SPLIT VIEW)
// -------------------------------------------------------------

@Preview(
    name = "Trophy Room Ufficiale FlickTrove (TopBar + BottomBar)",
    showBackground = true,
    backgroundColor = 0xFF090A10,
    device = "spec:width=411dp,height=891dp"
)
@Composable
private fun TrophyRoomScreenPreviewFull() {
    FlickTroveTheme {
        TrophyRoomScreenContent()
    }
}
