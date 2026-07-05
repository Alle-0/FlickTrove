package com.cinetrack.ui.components.detail

import androidx.compose.ui.res.stringResource
import com.cinetrack.R
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.LocalImageQuality
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.ui.utils.bounceClick
import coil.compose.AsyncImage
import com.cinetrack.data.api.CastMember
import com.cinetrack.data.api.CrewMember
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope

/**
 * DetailCast
 * Renders Directors and Main Cast in horizontal LazyRows.
 * Features profile avatars and premium typography.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DetailCast(
    directors: List<CrewMember>,
    cast: List<CastMember>,
    accentColor: Color,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onPersonClick: (Long, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (directors.isEmpty() && cast.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        // REGIA SECTION
        if (directors.isNotEmpty()) {
            val groupedDirectors = remember(directors) {
                directors.groupBy { it.id }.map { (_, members) ->
                    val first = members.first()
                    val combinedJobs = members.map { it.job }.distinct().joinToString(" / ")
                    first.copy(job = combinedJobs)
                }
            }

            Text(
                text = stringResource(R.string.detail_director),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                ),
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 12.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                items(groupedDirectors, key = { "dir-${it.id}" }, contentType = { "person" }) { person ->
                    PersonCard(
                        id = person.id,
                        name = person.name,
                        subLabel = "", // Nascondiamo il label "Director" poiché la sezione è già titolata REGIA
                        imagePath = person.profilePath,
                        accentColor = accentColor,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        showSubLabelContainer = false,
                        onClick = { onPersonClick(person.id, person.profilePath) }
                    )
                }
            }
        }

        // CAST SECTION
        if (cast.isNotEmpty()) {
            val groupedCast = remember(cast) {
                cast.groupBy { it.id }.map { (_, members) ->
                    val first = members.first()
                    val combinedCharacters = members.mapNotNull { it.character }.distinct().filter { it.isNotBlank() }.joinToString(" / ")
                    first.copy(character = combinedCharacters.ifBlank { null })
                }
            }

            Text(
                text = stringResource(R.string.detail_cast),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                ),
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 12.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                items(groupedCast, key = { "cast-${it.id}" }, contentType = { "person" }) { person ->
                    PersonCard(
                        id = person.id,
                        name = person.name,
                        subLabel = person.character ?: "-",
                        imagePath = person.profilePath,
                        accentColor = accentColor,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = { onPersonClick(person.id, person.profilePath) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PersonCard(
    id: Long,
    name: String,
    subLabel: String,
    imagePath: String?,
    accentColor: Color,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    showSubLabelContainer: Boolean = true,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .bounceClick { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val initials = remember(name) {
                name.split(" ")
                    .filter { it.isNotBlank() }
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2)
                    .joinToString("")
                    .uppercase()
            }
            if (initials.isNotEmpty()) {
                Text(
                    text = initials,
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_persona),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }

            if (!imagePath.isNullOrBlank()) {
                AsyncImage(
                    model = buildTmdbImageUrl(imagePath, ImageType.PROFILE, LocalImageQuality.current),
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // ... (Spacer and name Text remain the same) ...
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp,
            modifier = Modifier.fillMaxWidth().heightIn(min = 28.dp)
        )
        
        if (showSubLabelContainer || subLabel.isNotBlank()) {
            // Use a fixed height container for subLabel to maintain row consistency
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                if (subLabel.isNotBlank()) {
                    Text(
                        text = subLabel,
                        color = Color.White.copy(alpha = 0.4f), // Grigio chiaro invece di ciano
                        fontSize = 9.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        lineHeight = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
