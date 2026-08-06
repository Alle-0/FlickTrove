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
import androidx.compose.foundation.lazy.rememberLazyListState
import com.cinetrack.ui.utils.verticalFadingEdges
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import com.cinetrack.ui.viewmodel.AvatarSelectionViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.CircularProgressIndicator
import androidx.lifecycle.compose.collectAsStateWithLifecycle

enum class AvatarSelectionMode { AVATAR, BACKDROP }

class AvatarSelectionState {
    var isVisible by mutableStateOf(false)
    var onSelected: ((String?, String?) -> Unit)? = null
    var mode by mutableStateOf(AvatarSelectionMode.AVATAR)

    fun show(mode: AvatarSelectionMode = AvatarSelectionMode.AVATAR, onSelected: (String?, String?) -> Unit) {
        this.mode = mode
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
    mode: AvatarSelectionMode = AvatarSelectionMode.AVATAR,
    onDismissRequest: () -> Unit,
    onCharacterSelected: (String?, String?) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<TMDBSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    
    var characterImagesMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var selectedMedia by remember { mutableStateOf<TMDBSearchResult?>(null) }
    var characters by remember { mutableStateOf<List<CastMember>?>(null) }
    var backdrops by remember { mutableStateOf<List<com.cinetrack.data.api.ImageItem>?>(null) }
    var isLoadingCharacters by remember { mutableStateOf(false) }
    
    val isUploading by viewModel.isUploading.collectAsStateWithLifecycle()
    val uploadError by viewModel.uploadError.collectAsStateWithLifecycle()
    
    val focusManager = LocalFocusManager.current

    val context = LocalContext.current
    LaunchedEffect(uploadError) {
        if (uploadError != null) {
            android.widget.Toast.makeText(context, uploadError, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadCustomAvatar(uri) { publicUrl ->
                onCharacterSelected(publicUrl, "Custom")
            }
        }
    }

    BackHandler(enabled = !isUploading) {
        onDismissRequest()
    }

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
            
            if (mode == AvatarSelectionMode.BACKDROP) {
                backdrops = response?.images?.backdrops
                return@LaunchedEffect
            }
            
            val tvdbMap = when (media) {
                is TMDBSearchResult.MovieResult -> {
                    // Use originalTitle (always English) so TVDB can always find the movie
                    val title = response?.originalTitle ?: response?.originalName
                        ?: response?.title ?: media.title
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
        val configuration = androidx.compose.ui.platform.LocalConfiguration.current
        val dynamicCoverRatio = remember(configuration.screenWidthDp) {
            (configuration.screenWidthDp.toFloat() / 480f).coerceIn(0.6f, 1.5f)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.65f)
                .hazeGlass(state = hazeState, shape = RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus()
                }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
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
                                    backdrops = null
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_left),
                                    contentDescription = stringResource(R.string.avatar_selection_back),
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (selectedMedia == null) stringResource(R.string.avatar_selection_title_select_media) else if (mode == AvatarSelectionMode.BACKDROP) stringResource(R.string.avatar_selection_title_choose_cover) else stringResource(R.string.avatar_selection_title_choose_avatar),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .bounceClick(onClick = onDismissRequest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_x),
                            contentDescription = stringResource(R.string.settings_close),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                if (selectedMedia == null) {
                    if (isUploading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        // Action buttons (Remove and Import)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                                    .bounceClick { onCharacterSelected(null, null) }
                                    .padding(vertical = 12.dp, horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_trash),
                                    contentDescription = if (mode == AvatarSelectionMode.BACKDROP) stringResource(R.string.avatar_selection_remove_cover) else stringResource(R.string.avatar_selection_remove_avatar),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (mode == AvatarSelectionMode.BACKDROP) stringResource(R.string.avatar_selection_remove_cover) else stringResource(R.string.avatar_selection_remove_avatar),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
    
                            if (mode == AvatarSelectionMode.AVATAR) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .bounceClick { galleryLauncher.launch("image/*") }
                                        .padding(vertical = 12.dp, horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_image),
                                        contentDescription = "Import",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.avatar_selection_import_avatar),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Step 1: Search
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                stringResource(R.string.avatar_selection_search_placeholder),
                                color = Color.White.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
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

                    if (isSearching) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        val listState = rememberLazyListState()
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.verticalFadingEdges(listState, 16.dp, 16.dp)
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
                                            text = if (result is TMDBSearchResult.MovieResult) stringResource(R.string.avatar_selection_movie) else stringResource(R.string.avatar_selection_tv_show),
                                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Step 2: Character/Backdrop Selection
                    if (isLoadingCharacters) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (mode == AvatarSelectionMode.BACKDROP && backdrops.isNullOrEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.avatar_selection_no_covers_found), color = Color.White.copy(alpha = 0.5f))
                        }
                    } else if (mode == AvatarSelectionMode.AVATAR && characters.isNullOrEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.avatar_selection_no_characters_found), color = Color.White.copy(alpha = 0.5f))
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
                                columns = GridCells.Fixed(if (mode == AvatarSelectionMode.BACKDROP) 2 else 3),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                if (mode == AvatarSelectionMode.BACKDROP) {
                                    items(backdrops ?: emptyList()) { backdrop ->
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.bounceClick {
                                                onCharacterSelected(null, backdrop.filePath)
                                            }
                                        ) {
                                            AsyncImage(
                                                model = "https://image.tmdb.org/t/p/w500${backdrop.filePath}",
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(dynamicCoverRatio)
                                                    .clip(RoundedCornerShape(12.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
                                } else {
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
                Spacer(modifier = Modifier.height(8.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.TopCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f))
                            )
                        )
                )
            }
        }
    }
}
}
