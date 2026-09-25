package com.cinetrack.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.cinetrack.ui.components.shared.MovieCardSkeleton
import com.cinetrack.ui.components.shared.shimmerEffect

fun LazyListScope.homeFeedSkeletons() {
    // 1. Hero Spotlight Carousel Skeleton (fedele 1:1 con card centrale, peeking laterale, testi centrati e indicatore Symbiont)
    item(key = "skeleton_hero") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp),
                contentAlignment = Alignment.Center
            ) {
                // Card laterale sinistra che sbuca (Peeking card)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(44.dp)
                        .align(Alignment.CenterStart)
                        .offset(x = (-24).dp)
                        .graphicsLayer { alpha = 0.40f }
                        .clip(RoundedCornerShape(36.dp))
                        .background(Color(0xFF101118))
                )

                // Card laterale destra che sbuca (Peeking card)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(44.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 24.dp)
                        .graphicsLayer { alpha = 0.40f }
                        .clip(RoundedCornerShape(36.dp))
                        .background(Color(0xFF101118))
                )

                // Card centrale principale
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(36.dp))
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(36.dp)
                        )
                        .background(Color(0xFF14151E))
                ) {
                    // Sfondo con shimmer
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shimmerEffect()
                    )

                    // Gradiente scuro in basso (1:1 con HeroSpotlightCarousel)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.30f to Color.Transparent,
                                    0.50f to Color.Black.copy(alpha = 0.35f),
                                    0.70f to Color.Black.copy(alpha = 0.75f),
                                    0.85f to Color.Black.copy(alpha = 0.92f),
                                    1.0f to Color.Black
                                )
                            )
                    )

                    // Contenuti centrati in basso (Titolo/Logo + Anno & Generi)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Titolo / Logo Skeleton centrato
                        Box(
                            modifier = Modifier
                                .width(220.dp)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.22f))
                                .shimmerEffect()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Anno e Pillole generi centrate
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Anno
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.20f))
                                    .shimmerEffect()
                            )

                            // 2-3 Pillole generi
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .width(66.dp)
                                        .height(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .shimmerEffect()
                                )
                            }
                        }
                    }
                }
            }

            // Indicatori Pager (fedeli 1:1 con SymbiontPagerIndicator)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Worm attivo allungato nel colore primario NeonTeal
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                        .shimmerEffect()
                )
                repeat(5) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f))
                    )
                }
            }
        }
    }

    // 2. Standard Row Skeleton (es. Dalla tua Watchlist / Trending)
    item(key = "skeleton_row_1") {
        Column {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    .width(150.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF161822))
                    .shimmerEffect()
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(4) {
                    MovieCardSkeleton(width = 110.dp)
                }
            }
        }
    }

    // 3. The Trove's Pick Skeleton (fedele al layout 1:1 con poster a sinistra e metadati)
    item(key = "skeleton_trove_pick") {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Intestazione sezione "The Trove's Pick"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.20f))
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.20f))
                        .shimmerEffect()
                )
            }

            // Card Trove's Pick
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(255.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.08f),
                        RoundedCornerShape(32.dp)
                    )
                    .background(Color(0xFF101217))
            ) {
                // Sfondo shimmer con gradiente verticale dal basso
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shimmerEffect()
                )

                // Overlay gradiente orizzontale: sfumatura più scura a sinistra (0.80f che sfuma verso destra)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.80f),
                                    0.40f to Color.Black.copy(alpha = 0.42f),
                                    0.75f to Color.Transparent,
                                    1.0f to Color.Transparent
                                )
                            )
                        )
                )

                // Overlay gradiente verticale: velatura in alto (0.58f) e nero profondo e solido in basso (fino a 1.0f)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.58f),
                                    0.22f to Color.Black.copy(alpha = 0.22f),
                                    0.42f to Color.Transparent,
                                    0.55f to Color.Black.copy(alpha = 0.60f),
                                    0.75f to Color.Black.copy(alpha = 0.92f),
                                    1.0f to Color.Black
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Locandina / Poster skeleton
                    Box(
                        modifier = Modifier
                            .width(136.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                0.5.dp,
                                Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(16.dp)
                            )
                            .background(Color(0xFF181A25))
                            .shimmerEffect()
                    )

                    // Dettagli a destra skeleton
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Badge "CURATED FOR YOU" skeleton
                        Box(
                            modifier = Modifier
                                .width(96.dp)
                                .height(12.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                                .shimmerEffect()
                        )

                        // Metadati in basso skeleton
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            // Titolo skeleton
                            Box(
                                modifier = Modifier
                                    .width(115.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.22f))
                                    .shimmerEffect()
                            )

                            // Anno • Genere
                            Box(
                                modifier = Modifier
                                    .width(85.dp)
                                    .height(13.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .shimmerEffect()
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Pillole Match & Rating (ariose)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(72.dp)
                                        .height(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .shimmerEffect()
                                )
                                Box(
                                    modifier = Modifier
                                        .width(46.dp)
                                        .height(24.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.15f))
                                        .shimmerEffect()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 4. Ulteriori righe skeleton (Popolari / Più votati)
    items(count = 2, key = { "skeleton_row_extra_$it" }) {
        Column {
            Box(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    .width(130.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF161822))
                    .shimmerEffect()
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                userScrollEnabled = false
            ) {
                items(4) {
                    MovieCardSkeleton(width = 110.dp)
                }
            }
        }
    }
}
