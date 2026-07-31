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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
        
        // Firebase User Info
        val currentUser = Firebase.auth.currentUser
        val context = androidx.compose.ui.platform.LocalContext.current
        val prefs = remember { context.getSharedPreferences("user_name_changes", android.content.Context.MODE_PRIVATE) }
        
        var nameChangesCount by remember(currentUser?.uid) { 
            mutableIntStateOf(prefs.getInt("changes_${currentUser?.uid}", 0))
        }
        var showNameDialog by remember { mutableStateOf(false) }
        var showNewFolderDialog by remember { mutableStateOf(false) }
        var showGuestAuthDialog by remember { mutableStateOf(false) }
        var nameInput by remember { mutableStateOf("") }

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
        


        // Admin-enforced name change sync
        LaunchedEffect(currentUser?.uid) {
            if (currentUser != null && !currentUser.isAnonymous) {
                Firebase.firestore.collection("users").document(currentUser.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error == null && snapshot != null && snapshot.exists()) {
                            val firestoreName = snapshot.getString("displayName")
                            if (!firestoreName.isNullOrBlank() && firestoreName != currentUser.displayName) {
                                // Firestore name is different (likely changed by admin), force update Auth and UI
                                currentDisplayName = firestoreName
                                currentUser.updateProfile(userProfileChangeRequest {
                                    displayName = firestoreName
                                })
                            }
                        }
                    }
            }
        }
        var currentPhotoUrl by remember { mutableStateOf(currentUser?.photoUrl) }
        
        var avatarBackdrop by remember(currentUser?.uid) { 
            mutableStateOf(prefs.getString("avatar_backdrop_${currentUser?.uid}", null)) 
        }
        var extractedColor by remember { mutableStateOf<Color?>(null) }
        var rawExtractedColor by remember { mutableStateOf<Color?>(null) }
        val animatedBgColor by animateColorAsState(
            targetValue = extractedColor ?: Color(0xFF0F1115), 
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Top Spacer for Status Bar & padding + GlassyTopBar clearance
                    Spacer(Modifier.height(paddingValues.calculateTopPadding() + WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 120.dp))
                    
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
                                .size(96.dp)
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
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
                            
                            // Edit Pencil Icon
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-2).dp, y = (-2).dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(animatedBorderColor)
                                    .border(2.dp, Color(0xFF1E1E1E), CircleShape), // Assuming dark background color for stroke
                                contentAlignment = Alignment.Center
                            ) {
                                val avatarSelection = com.cinetrack.ui.components.account.LocalAvatarSelection.current
                                val primaryColor = MaterialTheme.colorScheme.primary
                                val iconTint = if (primaryColor.luminance() > 0.5f) Color.Black else Color.White
                                        
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_pencil),
                                    contentDescription = "Edit photo",
                                    tint = iconTint,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .bounceClick {
                                            if (currentUser == null || currentUser.isAnonymous) {
                                                showGuestAuthDialog = true
                                            } else {
                                                avatarSelection.show { newUrl, backdropUrl ->
                                                    val oldUrl = currentPhotoUrl
                                                // Optimistic Update
                                                currentPhotoUrl = newUrl?.let { Uri.parse(it) }
                                                
                                                avatarBackdrop = backdropUrl
                                                if (backdropUrl != null) {
                                                    prefs.edit().putString("avatar_backdrop_${currentUser?.uid}", backdropUrl).apply()
                                                } else {
                                                    prefs.edit().remove("avatar_backdrop_${currentUser?.uid}").apply()
                                                }
                                                
                                                currentUser?.updateProfile(userProfileChangeRequest { 
                                                    photoUri = newUrl?.let { Uri.parse(it) } 
                                                })?.addOnSuccessListener {
                                                    val updates = if (newUrl != null) mapOf("photoUrl" to newUrl) else mapOf("photoUrl" to null)
                                                    Firebase.firestore.collection("users").document(currentUser.uid)
                                                        .set(updates, SetOptions.merge())
                                                }?.addOnFailureListener {
                                                    // Rollback
                                                    currentPhotoUrl = oldUrl
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("Failed to update avatar. Please try again.")
                                                    }
                                                }
                                                }
                                            }
                                        }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Name
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .hazeGlass(
                                    state = backgroundHazeState,
                                    shape = RoundedCornerShape(50),
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    borderColor = Color.White.copy(alpha = 0.1f),
                                    borderWidth = 1.dp
                                )
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (currentUser == null || currentUser.isAnonymous) {
                                        showGuestAuthDialog = true
                                    } else {
                                        nameInput = currentDisplayName
                                        showNameDialog = true
                                    }
                                }
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = currentDisplayName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.ic_pencil),
                                contentDescription = "Edit Name",
                                tint = animatedBorderColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Dashboard Cards
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GeneralStatsCard(
                            stats = statsUiState.stats,
                            hazeState = backgroundHazeState,
                            onClick = { tabNavigator.current = StatsTab }
                        )
                        
                        MyFoldersCard(
                            folders = folders,
                            allMovies = allMovies,
                            hazeState = backgroundHazeState,
                            onViewAllClick = { tabNavigator.current = FoldersTab },
                            onFolderClick = { folder -> 
                                tabNavigator.current = FolderDetailTab(folder.id, folder.name, folder.description ?: "") 
                            }
                        )

                        YourFlowCard(
                            hazeState = backgroundHazeState,
                            onFlowClick = { tabNavigator.current = FlowTab },
                            onFlowStatsClick = { tabNavigator.current = FlowStatsTab }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // Removed local AvatarSelectionModal usage
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp) // Avoid bottom bar
            )
        }
        val validator = remember { com.cinetrack.domain.EmailValidatorUseCase() }
        if (showNameDialog) {
                val focusManager = LocalFocusManager.current
                var isCheckingName by remember { mutableStateOf(false) }
                var nameError by remember { mutableStateOf<String?>(null) }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { 
                            focusManager.clearFocus()
                            showNameDialog = false 
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 400.dp)
                            .fillMaxWidth(0.85f)
                            .hazeGlass(state = activeHazeState, alpha = 1f, shape = RoundedCornerShape(32.dp))
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                focusManager.clearFocus()
                            }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                stringResource(R.string.account_change_name_title),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.account_name_changes_left, 2 - nameChangesCount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { 
                                    if (it.length <= 20) {
                                        nameInput = it
                                        if (it.isNotEmpty() && it.length < 3) {
                                            nameError = context.getString(R.string.account_error_name_short)
                                        } else if (validator.containsOffensiveWords(it)) {
                                            nameError = context.getString(R.string.account_error_name_profanity)
                                        } else {
                                            nameError = null
                                        }
                                    }
                                },
                                label = { Text(stringResource(R.string.account_new_name_label)) },
                                singleLine = true,
                                isError = nameError != null,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            
                            if (nameError != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = nameError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .bounceClick { showNameDialog = false },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(stringResource(R.string.settings_cancel), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                                }
                                
                                val isSaveEnabled = nameChangesCount < 2 && nameInput.isNotBlank() && nameInput != currentDisplayName && !isCheckingName && nameError == null
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSaveEnabled) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                        )
                                        .bounceClick {
                                            if (!isSaveEnabled) return@bounceClick
                                            focusManager.clearFocus()
                                            
                                            if (nameInput.length < 3) {
                                                nameError = context.getString(R.string.account_error_name_short)
                                                return@bounceClick
                                            }
                                            
                                            if (validator.containsOffensiveWords(nameInput)) {
                                                nameError = context.getString(R.string.account_error_name_profanity)
                                                return@bounceClick
                                            }
                                            
                                            isCheckingName = true
                                            Firebase.firestore.collection("users")
                                                .whereEqualTo("displayName", nameInput)
                                                .get()
                                                .addOnSuccessListener { snapshot ->
                                                    isCheckingName = false
                                                    if (!snapshot.isEmpty) {
                                                        nameError = context.getString(R.string.account_error_name_taken)
                                                    } else {
                                                        val newCount = nameChangesCount + 1
                                                        prefs.edit().putInt("changes_${currentUser?.uid}", newCount).apply()
                                                        nameChangesCount = newCount
                                                        currentDisplayName = nameInput
                                                        currentUser?.updateProfile(userProfileChangeRequest { 
                                                            displayName = nameInput 
                                                        })?.addOnSuccessListener {
                                                            Firebase.firestore.collection("users").document(currentUser.uid)
                                                                .set(mapOf("displayName" to nameInput), SetOptions.merge())
                                                        }
                                                        showNameDialog = false
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    isCheckingName = false
                                                    nameError = context.getString(R.string.account_error_checking_name)
                                                }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCheckingName) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Text(
                                            stringResource(R.string.account_save), 
                                            fontWeight = FontWeight.Bold, 
                                            color = if (isSaveEnabled) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                }
            }
        if (showGuestAuthDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { 
                            showGuestAuthDialog = false 
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 400.dp)
                            .fillMaxWidth(0.85f)
                            .hazeGlass(state = activeHazeState, alpha = 1f, shape = RoundedCornerShape(32.dp))
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) { }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                stringResource(R.string.auth_guest_prompt_title),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.auth_guest_prompt_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showGuestAuthDialog = false }) {
                                    Text(stringResource(R.string.auth_guest_dialog_cancel), color = Color.White.copy(alpha = 0.7f))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = { 
                                    showGuestAuthDialog = false 
                                    parentNavigator.push(com.cinetrack.ui.screens.LoginScreen())
                                }) {
                                    Text(stringResource(R.string.auth_create_account), color = Color(0xFF80DEEA), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
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
