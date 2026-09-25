package com.cinetrack.ui.components.recommendations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.cinetrack.ui.components.shared.shimmerEffect

/**
 * Skeleton fedele al deck di raccomandazioni [FlickMovieCard] e [FlickControls]:
 * - Stack a 2 card per profondità del mazzo (card sottostante scalata)
 * - Card principale con raggio 48dp, backdrop shimmer, gradiente e metadati (titolo, match, anno, sinossi)
 * - Controlli inferiori fluttuanti (Pass 56dp, Info 48dp, Like 56dp)
 */
@Composable
fun FlickDeckSkeleton(
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(48.dp)

    Box(modifier = modifier) {
        // Area mazzo di carte
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            // Card sottostante per simulare profondità dello stack
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 0.94f
                        scaleY = 0.94f
                        translationY = 16.dp.toPx()
                        alpha = 0.50f
                        shape = cardShape
                        clip = true
                    }
                    .clip(cardShape)
                    .background(Color(0xFF141416))
                    .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)), cardShape)
            )

            // Card principale superiore
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        shape = cardShape
                        clip = true
                        shadowElevation = 16f
                    }
                    .clip(cardShape)
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), cardShape)
                    .background(Color(0xFF141416))
            ) {
                // Backdrop shimmer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shimmerEffect()
                )

                // Gradiente verticale oscurante (esattamente come FlickMovieCard)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.28f to Color.Transparent,
                                    0.48f to Color(0xFF141416).copy(alpha = 0.50f),
                                    0.68f to Color(0xFF141416).copy(alpha = 0.88f),
                                    0.86f to Color(0xFF141416).copy(alpha = 0.98f),
                                    1.0f to Color(0xFF141416)
                                )
                            )
                        )
                )

                // Colonna metadati inferiore
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    // Logo / Titolo bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Row: Match Pill + Rating
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Match score pill placeholder
                        Box(
                            modifier = Modifier
                                .width(96.dp)
                                .height(28.dp)
                                .clip(CircleShape)
                                .shimmerEffect()
                        )
                        // Rating placeholder
                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Anno • Generi placeholder line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.50f)
                            .height(13.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Sinossi / Overview placeholder a 3 righe
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.96f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }

        // Barra controlli inferiori skeleton (Pass, Info, Like)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pass button skeleton (56dp)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1C1E).copy(alpha = 0.70f))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), CircleShape)
                        .shimmerEffect()
                )

                // Info button skeleton (48dp)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1C1E).copy(alpha = 0.70f))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), CircleShape)
                        .shimmerEffect()
                )

                // Like button skeleton (56dp)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C1C1E).copy(alpha = 0.70f))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), CircleShape)
                        .shimmerEffect()
                )
            }
        }
    }
}
