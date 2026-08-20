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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import com.cinetrack.ui.components.card.PersonCard
import com.cinetrack.data.model.GlobalMovieStats
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import com.cinetrack.ui.components.shared.FlickTroveBottomSheet
import com.cinetrack.ui.utils.blockBottomSheetVerticalDrag
import com.cinetrack.ui.utils.rememberBottomSheetNestedScrollConnection
import com.cinetrack.ui.utils.verticalFadingEdges
import dev.chrisbanes.haze.HazeState

/**
 * DetailCast
 * Renders Directors and Main Cast in horizontal LazyRows.
 * Features profile avatars and premium typography.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DetailCast(
    crew: List<CrewMember>,
    cast: List<CastMember>,
    accentColor: Color,
    globalStats: GlobalMovieStats? = null,
    hazeState: HazeState? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onPersonClick: (Long, String?) -> Unit,
    onSheetStateChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (crew.isEmpty() && cast.isEmpty()) return

    var showAllCrew by remember { mutableStateOf(false) }
    var showAllCast by remember { mutableStateOf(false) }

    LaunchedEffect(showAllCrew, showAllCast) {
        onSheetStateChange(showAllCrew || showAllCast)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // REGIA SECTION
        val directors = remember(crew) { crew.filter { it.job == "Director" } }
        if (directors.isNotEmpty()) {
            val groupedDirectors = remember(directors) {
                directors.groupBy { it.id }.map { (_, members) ->
                    val first = members.first()
                    val combinedJobs = members.map { it.job }.distinct().joinToString(" / ")
                    first.copy(job = combinedJobs)
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showAllCrew = true }
                    .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.detail_director),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    ),
                    color = Color.White.copy(alpha = 0.5f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.detail_crew).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_right),
                        contentDescription = "See All",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                items(groupedDirectors.take(15), key = { "dir-${it.id}" }, contentType = { "person" }) { person ->
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showAllCast = true }
                    .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.detail_cast),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 3.sp
                    ),
                    color = Color.White.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_right),
                    contentDescription = "See All",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                items(groupedCast.take(15), key = { "cast-${it.id}" }, contentType = { "person" }) { person ->
                    PersonCard(
                        id = person.id,
                        name = person.name,
                        subLabel = person.character ?: "-",
                        imagePath = person.profilePath,
                        accentColor = accentColor,
                        mvpPercentage = null,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onClick = { onPersonClick(person.id, person.profilePath) }
                    )
                }
            }
        }
        }
    

    if (showAllCrew && crew.isNotEmpty()) {
        val groupedCrewByDepartment = crew.groupBy { it.department.ifBlank { "Other" } }
        val sections = groupedCrewByDepartment.map { (dept, members) ->
            val uniqueMembers = members.groupBy { it.id }.map { (_, dups) ->
                val first = dups.first()
                val combinedJobs = dups.map { it.job }.distinct().joinToString(" / ")
                BottomSheetPerson(first.id, first.name, combinedJobs, first.profilePath)
            }
            BottomSheetSection(dept, uniqueMembers)
        }.sortedBy { it.title } // Sort alphabetically by department

        PeopleBottomSheet(
            title = stringResource(R.string.detail_crew),
            sections = sections,
            accentColor = accentColor,
            hazeState = hazeState,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onDismiss = { showAllCrew = false },
            onPersonClick = onPersonClick
        )
    }

    if (showAllCast && cast.isNotEmpty()) {
        val groupedCast = cast.groupBy { it.id }.map { (_, members) ->
            val first = members.first()
            val combinedCharacters = members.mapNotNull { it.character }.distinct().filter { it.isNotBlank() }.joinToString(" / ")
            BottomSheetPerson(first.id, first.name, combinedCharacters.ifBlank { "-" }, first.profilePath, null)
        }
        PeopleBottomSheet(
            title = stringResource(R.string.detail_cast),
            people = groupedCast,
            accentColor = accentColor,
            hazeState = hazeState,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onDismiss = { showAllCast = false },
            onPersonClick = onPersonClick
        )
    }
}

data class BottomSheetPerson(val id: Long, val name: String, val subLabel: String, val profilePath: String?, val mvpPercentage: Int? = null)

data class BottomSheetSection(val title: String, val people: List<BottomSheetPerson>)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PeopleBottomSheet(
    title: String,
    people: List<BottomSheetPerson>? = null,
    sections: List<BottomSheetSection>? = null,
    accentColor: Color,
    hazeState: HazeState? = null,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    onDismiss: () -> Unit,
    onPersonClick: (Long, String?) -> Unit
) {
    FlickTroveBottomSheet(onDismissRequest = onDismiss, hazeState = hazeState) {
        val nestedScrollConnection = rememberBottomSheetNestedScrollConnection()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 48.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().blockBottomSheetVerticalDrag()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            val gridState = rememberLazyGridState()
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .nestedScroll(nestedScrollConnection)
                    .verticalFadingEdges(gridState, 32.dp, 32.dp)
            ) {
                if (sections != null) {
                    sections.forEach { section ->
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                            val localizedTitle = when (section.title.lowercase()) {
                                "acting", "actors" -> stringResource(R.string.dept_acting)
                                "art" -> stringResource(R.string.dept_art)
                                "camera" -> stringResource(R.string.dept_camera)
                                "costume & make-up" -> stringResource(R.string.dept_costume)
                                "crew" -> stringResource(R.string.dept_crew)
                                "directing" -> stringResource(R.string.dept_directing)
                                "editing" -> stringResource(R.string.dept_editing)
                                "lighting" -> stringResource(R.string.dept_lighting)
                                "production" -> stringResource(R.string.dept_production)
                                "sound" -> stringResource(R.string.dept_sound)
                                "visual effects" -> stringResource(R.string.dept_visual_effects)
                                "writing" -> stringResource(R.string.dept_writing)
                                "creator" -> stringResource(R.string.dept_creator)
                                else -> section.title
                            }

                            Text(
                                text = localizedTitle.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                                color = accentColor,
                                modifier = Modifier.padding(top = 16.dp, bottom = 0.dp)
                            )
                        }
                        items(section.people, key = { "sec-${section.title}-${it.id}" }) { person ->
                            PersonCard(
                                id = person.id,
                                name = person.name,
                                subLabel = person.subLabel,
                                imagePath = person.profilePath,
                                accentColor = accentColor,
                                mvpPercentage = person.mvpPercentage,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                showSubLabelContainer = false,
                                onClick = { 
                                    onDismiss()
                                    onPersonClick(person.id, person.profilePath) 
                                }
                            )
                        }
                    }
                } else if (people != null) {
                    items(people, key = { it.id }) { person ->
                        PersonCard(
                            id = person.id,
                            name = person.name,
                            subLabel = person.subLabel,
                            imagePath = person.profilePath,
                            accentColor = accentColor,
                            mvpPercentage = person.mvpPercentage,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            showSubLabelContainer = false,
                            onClick = { 
                                onDismiss()
                                onPersonClick(person.id, person.profilePath) 
                            }
                        )
                    }
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
    mvpPercentage: Int? = null,
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
        
        if (mvpPercentage != null && mvpPercentage > 0) {
            Text(
                text = "$mvpPercentage%",
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)
            )
        }
        
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
