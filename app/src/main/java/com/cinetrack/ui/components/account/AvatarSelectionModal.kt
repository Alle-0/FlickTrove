package com.cinetrack.ui.components.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import com.cinetrack.ui.utils.bounceClick
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import coil.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.data.api.CastMember
import com.cinetrack.data.api.TMDBSearchResult
import com.cinetrack.data.repository.MovieRepository
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.LocalHazeState
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import com.cinetrack.ui.viewmodel.AvatarSelectionViewModel

class AvatarSelectionState {
    var isVisible by mutableStateOf(false)
    var onSelected: ((String?, String?) -> Unit)? = null

    fun show(onSelected: (String?, String?) -> Unit) {
        this.onSelected = onSelected
        this.isVisible = true
    }

    fun dismiss() {
        this.isVisible = false
        this.onSelected = null
    }
}

val LocalAvatarSelection = staticCompositionLocalOf<AvatarSelectionState> { 
    error("No AvatarSelectionState provided") 
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarSelectionModal(
    viewModel: AvatarSelectionViewModel = hiltViewModel(),
    hazeState: HazeState,
    onDismissRequest: () -> Unit,
    onCharacterSelected: (String?, String?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<TMDBSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    
    var characterImagesMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedMedia by remember { mutableStateOf<TMDBSearchResult?>(null) }
    var characters by remember { mutableStateOf<List<CastMember>?>(null) }
    var isLoadingCharacters by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current

    // Debounce search
    LaunchedEffect(searchQuery) {
        if (searchQuery.length < 3) {
            searchResults = emptyList()
            return@LaunchedEffect
        }
        delay(500)
        isSearching = true
        try {
            searchResults = viewModel.searchMulti(searchQuery)
        } catch (e: Exception) {
            // Handle error
            searchResults = emptyList()
        } finally {
            isSearching = false
        }
    }

    // Load characters when media is selected
    LaunchedEffect(selectedMedia) {
        val media = selectedMedia ?: return@LaunchedEffect
        isLoadingCharacters = true
        try {
            val response = when (media) {
                is TMDBSearchResult.MovieResult -> viewModel.getMovieDetails(media.id)
                is TMDBSearchResult.TvResult -> viewModel.getTVDetails(media.id)
                else -> null
            }
            
            val tvdbMap = when (media) {
                is TMDBSearchResult.MovieResult -> {
                    val title = response?.title ?: response?.originalTitle ?: media.title
                    val year = media.releaseDate?.take(4) ?: response?.releaseDate?.take(4)
                    if (title != null && year != null) viewModel.getMovieCharacterImages(title, year) else emptyMap()
                }
                is TMDBSearchResult.TvResult -> {
                    val tvdbId = response?.externalIds?.tvdbId?.toString()
                    if (tvdbId != null) viewModel.getSeriesCharacterImages(tvdbId) else emptyMap()
                }
                else -> emptyMap()
            }
            characterImagesMap = tvdbMap

            // Only keep characters that have a TVDB image
            characters = response?.credits?.cast?.filter { 
                val charName = it.character?.lowercase()?.trim()
                val actorName = it.name.lowercase().trim()
                val tvdbImage = charName?.let { name -> tvdbMap[name] } ?: tvdbMap[actorName]
                tvdbImage != null
            } ?: emptyList()
        } catch (e: Exception) {
            characters = emptyList()
        } finally {
            isLoadingCharacters = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismissRequest() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.65f)
                .hazeGlass(state = hazeState, shape = RoundedCornerShape(24.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus()
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectedMedia != null) {
                            IconButton(
                                onClick = { 
                                    selectedMedia = null 
                                    characters = null
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_left),
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (selectedMedia == null) "Select a Movie or Show" else "Choose your Avatar",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_x),
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                if (selectedMedia == null) {
                    // Step 1: Search
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search for a movie or TV show...", color = Color.White.copy(alpha = 0.5f)) },
                        leadingIcon = { 
                            Icon(
                                painter = painterResource(id = R.drawable.ic_lente), 
                                contentDescription = null, 
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            ) 
                        },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.White.copy(alpha = 0.1f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                            .bounceClick { onCharacterSelected(null, null) }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.account_remove_avatar),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isSearching) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(searchResults.filter { it is TMDBSearchResult.MovieResult || it is TMDBSearchResult.TvResult }) { result ->
                                val title = when (result) {
                                    is TMDBSearchResult.MovieResult -> result.title
                                    is TMDBSearchResult.TvResult -> result.name
                                    else -> ""
                                } ?: "Unknown"
                                
                                val posterPath = when (result) {
                                    is TMDBSearchResult.MovieResult -> result.posterPath
                                    is TMDBSearchResult.TvResult -> result.posterPath
                                    else -> null
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .bounceClick { selectedMedia = result }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (posterPath != null) {
                                        AsyncImage(
                                            model = "https://image.tmdb.org/t/p/w200$posterPath",
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(width = 40.dp, height = 60.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                    }
                                    Column {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.bodyLarge.copy(color = Color.White, fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (result is TMDBSearchResult.MovieResult) "Movie" else "TV Show",
                                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Step 2: Character Selection
                    if (isLoadingCharacters) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (characters.isNullOrEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No characters found.", color = Color.White.copy(alpha = 0.5f))
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0f to Color.Transparent,
                                            0.05f to Color.Black,
                                            0.95f to Color.Black,
                                            1f to Color.Transparent
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                items(characters ?: emptyList()) { character ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.bounceClick {
                                            val charName = character.character?.lowercase()?.trim()
                                            val actorName = character.name.lowercase().trim()
                                            val tvdbImage = charName?.let { name -> characterImagesMap[name] } ?: characterImagesMap[actorName]
                                            val url = tvdbImage ?: ""
                                            if (url.isNotEmpty()) {
                                                val backdrop = selectedMedia?.let {
                                                    when (it) {
                                                        is TMDBSearchResult.MovieResult -> it.backdropPath ?: it.posterPath
                                                        is TMDBSearchResult.TvResult -> it.backdropPath ?: it.posterPath
                                                        else -> null
                                                    }
                                                }
                                                onCharacterSelected(url, backdrop)
                                            }
                                        }
                                    ) {
                                        val charNameForDisplay = character.character?.lowercase()?.trim()
                                        val actorNameForDisplay = character.name.lowercase().trim()
                                        val tvdbImageForDisplay = charNameForDisplay?.let { name -> characterImagesMap[name] } ?: characterImagesMap[actorNameForDisplay]
                                        AsyncImage(
                                            model = tvdbImageForDisplay,
                                            contentDescription = character.name,
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = character.character ?: character.name,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
