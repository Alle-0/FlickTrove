package com.cinetrack.ui.screens

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.cinetrack.ui.utils.bounceClick
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.ui.LocalAppPadding
import com.cinetrack.ui.LocalHazeState
import com.cinetrack.ui.components.common.CinematicBackground
import com.cinetrack.ui.components.account.AvatarSelectionModal
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.SetOptions
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.style.TextAlign
import com.cinetrack.ui.components.glass.hazeGlass
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.vectorResource
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import com.cinetrack.ui.viewmodel.FlowViewModel
import com.cinetrack.ui.viewmodel.StatsViewModel
import com.cinetrack.ui.viewmodel.FoldersViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.cinetrack.ui.components.account.GeneralStatsCard
import com.cinetrack.ui.components.account.MyFoldersCard
import com.cinetrack.ui.components.account.YourFlowCard
import com.cinetrack.ui.screens.FlowStatsTab
import com.cinetrack.ui.screens.FoldersTab
import com.cinetrack.ui.screens.FolderDetailTab
import com.cinetrack.ui.screens.FolderCreateDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.EntryPointAccessors
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithContent
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
object AccountTab : Tab {
    override val options: TabOptions
        @Composable
        get() {
            val title = stringResource(id = R.string.bottom_bar_account)
            return remember(title) {
                TabOptions(
                    index = 3u,
                    title = title,
                    icon = null
                )
            }
        }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        val tabNavigator = LocalTabNavigator.current
        val paddingValues = LocalAppPadding.current
        val hazeState = LocalHazeState.current
        val parentNavigator = LocalNavigator.currentOrThrow.parent ?: LocalNavigator.currentOrThrow
        
        val activeHazeState = hazeState ?: remember { HazeState() }
        
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        
        val flowViewModel = hiltViewModel<FlowViewModel>()
        val flowUiState by flowViewModel.uiState.collectAsStateWithLifecycle()
        
        val statsViewModel = hiltViewModel<StatsViewModel>()
        val statsUiState by statsViewModel.uiState.collectAsStateWithLifecycle()
        
        val foldersViewModel = hiltViewModel<FoldersViewModel>()
        val folders by foldersViewModel.folders.collectAsStateWithLifecycle()
        val allMovies by foldersViewModel.allMovies.collectAsStateWithLifecycle()

        val context = androidx.compose.ui.platform.LocalContext.current
        var currentContext = context
        while (currentContext is android.content.ContextWrapper && currentContext !is androidx.activity.ComponentActivity) {
            currentContext = currentContext.baseContext
        }
        val activity = currentContext as? androidx.activity.ComponentActivity
        val settingsViewModel = if (activity != null) {
            hiltViewModel<com.cinetrack.ui.viewmodel.SettingsViewModel>(activity)
        } else {
            hiltViewModel<com.cinetrack.ui.viewmodel.SettingsViewModel>()
        }
        val showMyFolders by settingsViewModel.showMyFolders.collectAsStateWithLifecycle()
        val showYourFlow by settingsViewModel.showYourFlow.collectAsStateWithLifecycle()
        val showGeneralStats by settingsViewModel.showGeneralStats.collectAsStateWithLifecycle()
        val dashboardCardOrder by settingsViewModel.dashboardCardOrder.collectAsStateWithLifecycle()
        
        // Firebase User Info
        val currentUser = Firebase.auth.currentUser
        val avatarSelection = com.cinetrack.ui.components.account.LocalAvatarSelection.current
        val prefs = remember { context.getSharedPreferences("user_name_changes", android.content.Context.MODE_PRIVATE) }
        
        var currentDisplayName by remember(currentUser) {
            mutableStateOf(
                when {
                    currentUser == null || currentUser.isAnonymous -> "Guest"
                    else -> currentUser.displayName.takeIf { !it.isNullOrBlank() }
                        ?: currentUser.email?.substringBefore("@")
                        ?: "User"
                }
            )
        }


        var currentPhotoUrl by remember { mutableStateOf(currentUser?.photoUrl) }
        var avatarBanned by remember { mutableStateOf(false) }
        var avatarBackdrop by remember(currentUser?.uid) { 
            mutableStateOf(prefs.getString("avatar_backdrop_${currentUser?.uid}", null)) 
        }

        // Admin-enforced name change & cross-device sync
        LaunchedEffect(currentUser?.uid) {
            if (currentUser != null && !currentUser.isAnonymous) {
                Firebase.firestore.collection("users").document(currentUser.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error == null && snapshot != null && snapshot.exists()) {
                            val firestoreName = snapshot.getString("displayName")
                            if (!firestoreName.isNullOrBlank()) {
                                if (currentDisplayName != firestoreName) {
                                    currentDisplayName = firestoreName
                                }
                                if (firestoreName != currentUser.displayName) {
                                    currentUser.updateProfile(userProfileChangeRequest {
                                        displayName = firestoreName
                                    })
                                }
                            }
                            
                            val firestorePhoto = snapshot.getString("photoUrl")
                            val newUri = firestorePhoto?.let { android.net.Uri.parse(it) }
                            if (currentPhotoUrl != newUri) {
                                currentPhotoUrl = newUri
                            }
                            
                            val authPhoto = currentUser.photoUrl?.toString()
                            if (firestorePhoto != authPhoto) {
                                currentUser.updateProfile(userProfileChangeRequest {
                                    photoUri = newUri
                                })
                            }

                            avatarBanned = snapshot.getBoolean("avatarBanned") ?: false
                            
                            val dbBackdrop = snapshot.getString("avatarBackdrop")
                            if (dbBackdrop != avatarBackdrop) {
                                avatarBackdrop = dbBackdrop
                                if (dbBackdrop != null) {
                                    prefs.edit().putString("avatar_backdrop_${currentUser.uid}", dbBackdrop).apply()
                                } else {
                                    prefs.edit().remove("avatar_backdrop_${currentUser.uid}").apply()
                                }
                            }
                        }
                    }
            }
        }
        var extractedColor by remember { mutableStateOf<Color?>(null) }
        var rawExtractedColor by remember { mutableStateOf<Color?>(null) }
        
        val baseDarkColor = remember { Color(0xFF161620) }
        val targetBackgroundColor = if (extractedColor != null) {
            val vividAccent = com.cinetrack.ui.utils.ColorUtils.ensureVividAccent(extractedColor!!)
            androidx.compose.ui.graphics.lerp(vividAccent, baseDarkColor, 0.68f)
        } else {
            baseDarkColor
        }
        
        val animatedBgColor by animateColorAsState(
            targetValue = targetBackgroundColor, 
            animationSpec = tween(durationMillis = 800),
            label = "backgroundColor"
        )
        val animatedBorderColor by animateColorAsState(
            targetValue = rawExtractedColor ?: MaterialTheme.colorScheme.primary, 
            animationSpec = tween(durationMillis = 800),
            label = "borderColor"
        )
        val currentImageQuality = LocalImageQuality.current
        
        LaunchedEffect(avatarBackdrop, currentImageQuality) {
            if (avatarBackdrop != null) {
                val imageUrl = buildTmdbImageUrl(avatarBackdrop!!, ImageType.BACKDROP, currentImageQuality)
                val loader = context.imageLoader
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as BitmapDrawable).bitmap
                    val raw = ColorUtils.extractAverageColor(bitmap)
                    rawExtractedColor = raw
                    val ambientColor = ColorUtils.darkenForAmbient(raw)
                    val finalColor = ColorUtils.ensureMinimumLuminance(ambientColor, 0.25f)
                    extractedColor = finalColor
                }
            } else {
                extractedColor = null
                rawExtractedColor = null
            }
        }

        val backgroundHazeState = remember { HazeState() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(
                    state = activeHazeState,
                    style = HazeStyle(blurRadius = 24.dp, tint = Color.Black.copy(alpha = 0.5f))
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(animatedBgColor)
                    .haze(state = backgroundHazeState)
            ) {
                if (avatarBackdrop != null) {
                val imageUrl = buildTmdbImageUrl(avatarBackdrop!!, ImageType.BACKDROP, currentImageQuality)
                Box(modifier = Modifier.fillMaxWidth().height(480.dp)) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.3f to Color.Transparent,
                                    0.5f to animatedBgColor.copy(alpha = 0.3f),
                                    0.7f to animatedBgColor.copy(alpha = 0.7f),
                                    0.85f to animatedBgColor.copy(alpha = 0.9f),
                                    1.0f to animatedBgColor
                                )
                            )
                    )
                }
            } else {
                CinematicBackground(modifier = Modifier.fillMaxSize())
                }
            }

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.foundation.LocalOverscrollFactory provides null
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = paddingValues.calculateBottomPadding() + 80.dp)
                    ) {
                    // Top Spacer for Status Bar & padding + GlassyTopBar clearance
                    Spacer(Modifier.height(paddingValues.calculateTopPadding() + WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 120.dp))
                    
                    // Capture delegated props into local vals (needed by name pill + dashboard cards)
                    val snapshotRawColor = rawExtractedColor
                    val snapshotBackdrop = avatarBackdrop
                    val rawLuminance = snapshotRawColor?.luminance() ?: 0f

                    // User Profile Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar Container
                        Box(
                            modifier = Modifier
                                .size(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(112.dp)
                                    .hazeGlass(
                                        state = backgroundHazeState,
                                        shape = CircleShape,
                                        containerColor = if (rawLuminance > 0.35f) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                                        borderColor = if (rawLuminance > 0.35f) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.12f),
                                        borderWidth = 1.dp,
                                        blurRadius = com.cinetrack.ui.theme.HazeStyles.SmallGlassBlurRadius
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.1f))
                                        .border(2.dp, animatedBorderColor.copy(alpha = 0.8f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (currentPhotoUrl != null) {
                                        AsyncImage(
                                            model = currentPhotoUrl,
                                            contentDescription = "Profile Picture",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_persona), // Assuming this icon exists
                                            contentDescription = "Default Profile",
                                            tint = Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                            }
                        }
                    }
                        
                    Spacer(modifier = Modifier.width(16.dp))
                        
                        // Name Container
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .hazeGlass(
                                        state = backgroundHazeState,
                                        shape = RoundedCornerShape(50),
                                        // Stronger overlay when backdrop is light so pill has contrast
                                        containerColor = if (rawLuminance > 0.35f)
                                            Color.Black.copy(alpha = 0.4f)
                                        else
                                            Color.White.copy(alpha = 0.05f),
                                        borderColor = Color.White.copy(alpha = 0.1f),
                                        borderWidth = 1.dp
                                    )
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                            ) {
                            val namePencilColor = Color(0xFFF0F0F0)
                            val textShadow = androidx.compose.ui.graphics.Shadow(
                                color = if (namePencilColor == Color(0xFF1A1A1A)) Color.White.copy(alpha = 0.3f)
                                        else Color.Black.copy(alpha = 0.4f),
                                offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                                blurRadius = 4f
                            )
                            Text(
                                text = currentDisplayName,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = namePencilColor,
                                    shadow = textShadow
                                ),
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Dashboard Cards
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        dashboardCardOrder.forEach { cardKey ->
                            when (cardKey) {
                                "stats" -> {
                                    if (showGeneralStats) {
                                        GeneralStatsCard(
                                            stats = statsUiState.stats,
                                            hazeState = backgroundHazeState,
                                            backgroundLuminance = rawLuminance,
                                            onClick = { tabNavigator.current = StatsTab }
                                        )
                                    }
                                }
                                "folders" -> {
                                    if (showMyFolders) {
                                        MyFoldersCard(
                                            folders = folders,
                                            allMovies = allMovies,
                                            hazeState = backgroundHazeState,
                                            backgroundLuminance = rawLuminance,
                                            onViewAllClick = { tabNavigator.current = FoldersTab },
                                            onFolderClick = { folder ->
                                                tabNavigator.current = FolderDetailTab(folder.id, folder.name, folder.color)
                                            }
                                        )
                                    }
                                }
                                "flow" -> {
                                    if (showYourFlow) {
                                        YourFlowCard(
                                            hazeState = backgroundHazeState,
                                            backgroundLuminance = rawLuminance,
                                            onFlowClick = { tabNavigator.current = FlowTab },
                                            onFlowStatsClick = { tabNavigator.current = FlowStatsTab }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))


                }
                }
            }
            // Removed local AvatarSelectionModal usage
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp) // Avoid bottom bar
            )
        }

    }
}

@Composable
fun AccountMenuButton(
    icon: Int,
    title: String,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit = {},
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .hazeGlass(
                state = hazeState,
                shape = RoundedCornerShape(50),
                containerColor = Color.White.copy(alpha = 0.05f),
                borderColor = Color.White.copy(alpha = 0.1f),
                borderWidth = 1.dp
            )
            .bounceClick { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            trailingContent()
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                painter = painterResource(id = R.drawable.ic_right),
                contentDescription = "Go",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
