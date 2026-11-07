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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.offset
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.ui.LocalAppPadding
import com.cinetrack.ui.LocalHazeState
import com.cinetrack.ui.components.common.CinematicBackground
import com.cinetrack.ui.components.account.AvatarSelectionModal
import com.cinetrack.ui.components.account.TrophyShowcaseCard
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
import com.cinetrack.ui.components.folder.FolderCreateDialog
import com.cinetrack.ui.screens.TrophyRoomTab
import com.cinetrack.ui.screens.HistoryTab
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.EntryPointAccessors
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.SuccessResult
import coil3.toBitmap
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

        val trophyRoomViewModel = hiltViewModel<com.cinetrack.ui.viewmodel.TrophyRoomViewModel>()
        val trophyItems by trophyRoomViewModel.trophyItems.collectAsStateWithLifecycle()

        val historyViewModel = hiltViewModel<com.cinetrack.ui.history.HistoryViewModel>()
        val historyUiState by historyViewModel.uiState.collectAsStateWithLifecycle()

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
        val showTrophies by settingsViewModel.showTrophies.collectAsStateWithLifecycle()
        val showDiary by settingsViewModel.showDiary.collectAsStateWithLifecycle()
        val dashboardCardOrder by settingsViewModel.dashboardCardOrder.collectAsStateWithLifecycle()
        
        // Firebase User Info
        val currentUser = Firebase.auth.currentUser
        val avatarSelection = com.cinetrack.ui.components.account.LocalAvatarSelection.current
        val prefs = remember { context.getSharedPreferences("user_name_changes", android.content.Context.MODE_PRIVATE) }
        
        val guestLabel = stringResource(R.string.settings_guest)
        var currentDisplayName by remember(currentUser, guestLabel) {
            mutableStateOf(
                when {
                    currentUser == null || currentUser.isAnonymous -> guestLabel
                    else -> currentUser.displayName.takeIf { !it.isNullOrBlank() }
                        ?: currentUser.email?.substringBefore("@")
                        ?: guestLabel
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
        var actualHeaderLuminance by remember { mutableFloatStateOf(0f) }
        var actualBackdropLuminance by remember { mutableFloatStateOf(0f) }
        
        val baseDarkColor = remember { Color(0xFF0F0F16) }
        val targetBackgroundColor = if (extractedColor != null) {
            val vividAccent = com.cinetrack.ui.utils.ColorUtils.ensureVividAccent(extractedColor!!)
            androidx.compose.ui.graphics.lerp(vividAccent, baseDarkColor, 0.85f)
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
                    val bitmap = result.image.toBitmap()
                    val color = ColorUtils.extractAccentColor(bitmap)
                    if (color != Color.Unspecified) {
                        rawExtractedColor = color
                        extractedColor = color
                    }

                    // Sample 16x16 thumbnail for true pixel luminance
                    val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, 16, 16, true)
                    var headerLumSum = 0f
                    var totalLumSum = 0f
                    for (y in 0 until 16) {
                        for (x in 0 until 16) {
                            val pixel = scaled.getPixel(x, y)
                            val r = (pixel shr 16) and 0xFF
                            val g = (pixel shr 8) and 0xFF
                            val b = pixel and 0xFF
                            val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                            totalLumSum += lum
                            if (y < 8) {
                                headerLumSum += lum
                            }
                        }
                    }
                    actualHeaderLuminance = headerLumSum / (16f * 8f)
                    actualBackdropLuminance = totalLumSum / (16f * 16f)
                }
            } else {
                extractedColor = null
                rawExtractedColor = null
                actualHeaderLuminance = 0f
                actualBackdropLuminance = 0f
            }
        }

        val backgroundHazeState = remember { HazeState() }
        val scrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = activeHazeState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(state = backgroundHazeState)
                    .background(animatedBgColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp)
                ) {
                    if (avatarBackdrop != null) {
                        val imageUrl = buildTmdbImageUrl(avatarBackdrop!!, ImageType.BACKDROP, currentImageQuality)
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
                                        0.0f to Color.Black.copy(alpha = 0.35f),
                                        0.18f to Color.Transparent,
                                        0.55f to animatedBgColor.copy(alpha = 0.40f),
                                        0.75f to animatedBgColor.copy(alpha = 0.85f),
                                        0.92f to animatedBgColor.copy(alpha = 0.98f),
                                        1.0f to animatedBgColor
                                    )
                                )
                        )
                    } else {
                        CinematicBackground(modifier = Modifier.fillMaxSize())
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = paddingValues.calculateBottomPadding() + 80.dp)
                ) {
                    val statusBarsTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    val cutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
                    val safeDrawingTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
                    val safeTop = maxOf(statusBarsTop, maxOf(cutoutTop, safeDrawingTop))
                    val compactTop = safeTop + 46.dp + 8.dp
                    val restingTop = safeTop + 46.dp + 28.dp

                    // Top Spacer for clearance: lets the cards start right below the resting avatar
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(restingTop + 120.dp + 24.dp)
                    )
                    
                    // Capture delegated props into local vals (needed by dashboard cards)
                    val snapshotRawColor = rawExtractedColor
                    val snapshotBackdrop = avatarBackdrop
                    val rawLuminance = snapshotRawColor?.luminance() ?: 0f

                    // Dashboard Cards
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val isDiaryInDashboardOrder = "diary" in dashboardCardOrder || "history" in dashboardCardOrder
                        dashboardCardOrder.forEach { cardKey ->
                            when (cardKey) {
                                "stats" -> {
                                    if (showGeneralStats) {
                                        GeneralStatsCard(
                                            stats = statsUiState.stats,
                                            hazeState = backgroundHazeState,
                                            backgroundLuminance = actualBackdropLuminance,
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
                                            backgroundLuminance = actualBackdropLuminance,
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
                                            backgroundLuminance = actualBackdropLuminance,
                                            onFlowClick = { tabNavigator.current = FlowTab },
                                            onFlowStatsClick = { tabNavigator.current = FlowStatsTab }
                                        )
                                    }
                                }
                                "trophies" -> {
                                    if (showTrophies) {
                                        TrophyShowcaseCard(
                                            hazeState = backgroundHazeState,
                                            backgroundLuminance = actualBackdropLuminance,
                                            isEnabled = true,
                                            unlockedBadges = trophyItems.filter { it.isUnlocked },
                                            onClick = {
                                                tabNavigator.current = TrophyRoomTab
                                            }
                                        )
                                    }
                                }
                                "diary", "history" -> {
                                    if (showDiary) {
                                        com.cinetrack.ui.components.account.HistoryDiaryCard(
                                            hazeState = backgroundHazeState,
                                            backgroundLuminance = actualBackdropLuminance,
                                            recentEntries = historyUiState.allEntries.take(3),
                                            totalCount = historyUiState.allEntries.size,
                                            onClick = {
                                                tabNavigator.current = HistoryTab
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        if (!isDiaryInDashboardOrder && showDiary) {
                            com.cinetrack.ui.components.account.HistoryDiaryCard(
                                hazeState = backgroundHazeState,
                                backgroundLuminance = actualBackdropLuminance,
                                recentEntries = historyUiState.allEntries.take(3),
                                totalCount = historyUiState.allEntries.size,
                                onClick = {
                                    tabNavigator.current = HistoryTab
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))

                }

                // Morphing Profile Header (Avatar and Name smoothly shrink & dock under GlassyTopBar)
                val statusBarsTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val cutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
                val safeDrawingTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
                val safeTop = maxOf(statusBarsTop, maxOf(cutoutTop, safeDrawingTop))
                val compactTop = safeTop + 46.dp + 8.dp
                val restingTop = safeTop + 46.dp + 28.dp

                val density = LocalDensity.current
                val scrollThreshold = with(density) { 140.dp.toPx() }
                val progress by remember {
                    derivedStateOf {
                        (scrollState.value.toFloat() / scrollThreshold).coerceIn(0f, 1f)
                    }
                }

                val currentHeaderTop = lerp(restingTop, compactTop, progress)

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = currentHeaderTop)
                        .padding(horizontal = 24.dp)
                        .zIndex(20f)
                ) {
                    val totalWidth = maxWidth
                    var nameWidthPx by remember { mutableIntStateOf(0) }
                    val nameWidthDp = with(density) { nameWidthPx.toDp() }

                    val avatarSize = lerp(120.dp, 34.dp, progress)
                    val avatarInnerSize = lerp(100.dp, 30.dp, progress)
                    val avatarBorderWidth = lerp(2.dp, 1.5.dp, progress)
                    val avatarIconSize = lerp(48.dp, 16.dp, progress)

                    val dockedNameX = 34.dp + 10.dp
                    val restingNameX = if (nameWidthDp > 0.dp) (totalWidth - nameWidthDp).coerceAtLeast(dockedNameX) else totalWidth - 110.dp
                    val currentNameX = lerp(restingNameX, dockedNameX, progress)

                    val restingNameY = (120.dp - 44.dp) / 2
                    val dockedNameY = (34.dp - 36.dp) / 2
                    val currentNameY = lerp(restingNameY, dockedNameY, progress)

                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(avatarSize)
                            .bounceClick(scaleDown = 0.95f) {
                                settingsViewModel.triggerEditProfileMenu()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeGlass(
                                    state = backgroundHazeState,
                                    shape = CircleShape,
                                    containerColor = if (actualHeaderLuminance > 0.38f) Color.Black.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.10f),
                                    borderColor = (rawExtractedColor ?: MaterialTheme.colorScheme.primary).copy(alpha = 0.35f),
                                    borderWidth = 1.dp,
                                    blurRadius = com.cinetrack.ui.theme.HazeStyles.SmallGlassBlurRadius
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(avatarInnerSize)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .border(avatarBorderWidth, animatedBorderColor.copy(alpha = 0.8f), CircleShape),
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
                                        painter = painterResource(id = R.drawable.ic_persona),
                                        contentDescription = "Default Profile",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(avatarIconSize)
                                    )
                                }
                            }
                        }
                    }

                    // Name Pill
                    val nameContainerColor = if (actualHeaderLuminance > 0.38f) {
                        Color.Black.copy(alpha = 0.45f + (0.10f * progress))
                    } else {
                        androidx.compose.ui.graphics.lerp(
                            Color.White.copy(alpha = 0.12f),
                            Color(0xFF16161E).copy(alpha = 0.70f),
                            progress
                        )
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = currentNameX, y = currentNameY)
                            .onGloballyPositioned { coordinates ->
                                nameWidthPx = coordinates.size.width
                            }
                            .bounceClick(scaleDown = 0.95f) {
                                settingsViewModel.triggerEditProfileMenu()
                            }
                            .hazeGlass(
                                state = backgroundHazeState,
                                shape = RoundedCornerShape(50),
                                containerColor = nameContainerColor,
                                borderColor = if (actualHeaderLuminance > 0.38f)
                                    Color.White.copy(alpha = 0.20f)
                                else
                                    Color.White.copy(alpha = 0.25f),
                                borderWidth = 1.dp
                            )
                            .padding(
                                horizontal = lerp(24.dp, 12.dp, progress),
                                vertical = lerp(8.dp, 6.dp, progress)
                            )
                    ) {
                        val textShadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black.copy(alpha = if (actualHeaderLuminance > 0.38f) 0.5f else 0.7f),
                            offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                            blurRadius = 4f
                        )
                        Text(
                            text = currentDisplayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                shadow = textShadow,
                                fontSize = lerp(16.sp, 14.sp, progress)
                            ),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
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
