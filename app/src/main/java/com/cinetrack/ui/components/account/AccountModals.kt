package com.cinetrack.ui.components.account

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import com.cinetrack.ui.components.shared.ModalBackButton
import com.cinetrack.ui.components.shared.ModalCloseButton
import com.cinetrack.ui.components.shared.MorphGlassModal
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.R
import com.cinetrack.ui.components.common.FlickTroveSwitch
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.viewmodel.SettingsViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.animateFloatAsState
import kotlin.math.roundToInt
import com.cinetrack.ui.utils.bounceClick
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Close
import java.util.Collections

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

private enum class EditProfileSubScreen {
    MENU,
    CHANGE_NAME,
    CHANGE_AVATAR,
    CHANGE_COVER
}

private data class DashboardSettingItem(
    val iconRes: Int,
    val titleRes: Int,
    val descRes: Int,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)

@Composable
fun AccountModals(
    settingsViewModel: SettingsViewModel,
    globalHazeState: HazeState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val navigator = cafe.adriel.voyager.navigator.LocalNavigator.current
    
    val currentUser = remember { Firebase.auth.currentUser }
    val avatarSelection = LocalAvatarSelection.current

    val prefs = remember { context.getSharedPreferences("user_name_changes", android.content.Context.MODE_PRIVATE) }
    var nameChangesCount by remember(currentUser?.uid) { 
        mutableIntStateOf(prefs.getInt("changes_${currentUser?.uid}", 0))
    }
    
    var showProfileMenu by remember { mutableStateOf(false) }
    var editProfileTriggerBounds by remember { mutableStateOf<Rect?>(null) }
    var showDashboardSettings by remember { mutableStateOf(false) }
    var editProfileSubScreen by remember { mutableStateOf(EditProfileSubScreen.MENU) }
    
    var nameInput by remember { mutableStateOf("") }
    var isCheckingNameLive by remember { mutableStateOf(false) }
    var nameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var avatarBanned by remember { mutableStateOf(false) }
    
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
    
    
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        settingsViewModel.showEditProfileMenu.collect { bounds ->
            if (currentUser != null && !currentUser.isAnonymous) {
                editProfileTriggerBounds = bounds
                editProfileSubScreen = EditProfileSubScreen.MENU
                showProfileMenu = true
            } else {
                settingsViewModel.triggerGuestAuthDialog()
            }
        }
    }
    
    
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
                        val dbNameChanges = snapshot.getLong("nameChangesCount")?.toInt() ?: 0
                        if (dbNameChanges != nameChangesCount) {
                            nameChangesCount = dbNameChanges
                            prefs.edit().putInt("changes_${currentUser.uid}", dbNameChanges).apply()
                        }
                        val firestorePhoto = snapshot.getString("photoUrl")
                        val newUri = firestorePhoto?.let { Uri.parse(it) }
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
                    }
                }
        }
    }
    
    LaunchedEffect(nameInput) {
        if (nameInput.isBlank() || nameInput == currentDisplayName) {
            nameAvailable = null
            return@LaunchedEffect
        }
        if (nameInput.length < 3) {
            nameAvailable = null
            return@LaunchedEffect
        }
        
        isCheckingNameLive = true
        kotlinx.coroutines.delay(500)
        try {
            val doc = Firebase.firestore.collection("usernames").document(nameInput.lowercase()).get().await()
            nameAvailable = !doc.exists() || doc.getString("uid") == currentUser?.uid
            if (!nameAvailable!!) {
                nameError = context.getString(R.string.account_error_name_taken)
            }
        } catch(e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch(e: Exception) {
            nameAvailable = false
            nameError = e.localizedMessage
        }
        isCheckingNameLive = false
    }
    
    val validator = remember { com.cinetrack.domain.EmailValidatorUseCase() }

    val currentMaxWidth = when (editProfileSubScreen) {
        EditProfileSubScreen.MENU -> 340.dp
        EditProfileSubScreen.CHANGE_NAME -> 400.dp
        EditProfileSubScreen.CHANGE_AVATAR, EditProfileSubScreen.CHANGE_COVER -> 520.dp
    }
    val currentWidthFraction = when (editProfileSubScreen) {
        EditProfileSubScreen.MENU -> 0.85f
        EditProfileSubScreen.CHANGE_NAME -> 0.90f
        EditProfileSubScreen.CHANGE_AVATAR, EditProfileSubScreen.CHANGE_COVER -> 0.92f
    }
    val currentFixedHeightFraction: Float? = when (editProfileSubScreen) {
        EditProfileSubScreen.MENU, EditProfileSubScreen.CHANGE_NAME -> null
        EditProfileSubScreen.CHANGE_AVATAR, EditProfileSubScreen.CHANGE_COVER -> 0.68f
    }

    // Profile Menu Modal
    MorphGlassModal(
        isVisible = showProfileMenu,
        onDismissRequest = {
            if (editProfileSubScreen != EditProfileSubScreen.MENU) {
                editProfileSubScreen = EditProfileSubScreen.MENU
            } else {
                showProfileMenu = false
            }
        },
        triggerBounds = editProfileTriggerBounds,
        hazeState = globalHazeState,
        targetMaxWidth = currentMaxWidth,
        targetWidthFraction = currentWidthFraction,
        fixedModalHeightFraction = currentFixedHeightFraction,
        minModalHeight = 100.dp,
        targetCornerRadius = 32.dp,
        zIndex = 80000f
    ) { contentAlpha ->
        var isCheckingName by remember { mutableStateOf(false) }

        AnimatedContent(
            targetState = editProfileSubScreen,
            transitionSpec = {
                if (targetState == EditProfileSubScreen.MENU) {
                    (slideInHorizontally { -it / 3 } + fadeIn(tween(200)))
                        .togetherWith(slideOutHorizontally { it / 3 } + fadeOut(tween(150)))
                } else {
                    (slideInHorizontally { it / 3 } + fadeIn(tween(200)))
                        .togetherWith(slideOutHorizontally { -it / 3 } + fadeOut(tween(150)))
                }
            },
            label = "EditProfileSubScreenTransition"
        ) { targetScreen ->
            when (targetScreen) {
                EditProfileSubScreen.MENU -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.account_edit_profile),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            ModalCloseButton(
                                onClick = { showProfileMenu = false }
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .bounceClick {
                                    nameInput = currentDisplayName
                                    editProfileSubScreen = EditProfileSubScreen.CHANGE_NAME
                                }
                                .padding(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_pencil),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.account_change_name_menu),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_right),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .bounceClick {
                                    if (avatarBanned) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.avatar_banned_message))
                                        }
                                    } else {
                                        editProfileSubScreen = EditProfileSubScreen.CHANGE_AVATAR
                                    }
                                }
                                .padding(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_persona),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.account_change_avatar_menu),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_right),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .bounceClick {
                                    editProfileSubScreen = EditProfileSubScreen.CHANGE_COVER
                                }
                                .padding(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_image),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.account_change_cover_menu),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.ic_right),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                EditProfileSubScreen.CHANGE_NAME -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ModalBackButton(
                                onClick = { editProfileSubScreen = EditProfileSubScreen.MENU }
                            )
                            Text(
                                stringResource(R.string.account_change_name_title),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            ModalCloseButton(
                                onClick = { showProfileMenu = false }
                            )
                        }
                        
                        val changesLeft = (2 - nameChangesCount).coerceAtLeast(0)
                        val limitReached = nameChangesCount >= 2
                        Text(
                            stringResource(R.string.account_name_changes_left, changesLeft),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (limitReached) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val maxNameLength = 20
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = {
                                    if (limitReached) return@OutlinedTextField
                                    val filtered = it.filterNot { char -> char.isWhitespace() }
                                    if (filtered.length <= maxNameLength) {
                                        nameInput = filtered
                                        if (filtered.isNotEmpty() && filtered.length < 3) {
                                            nameError = context.getString(R.string.account_error_name_short)
                                        } else if (validator.containsOffensiveWords(filtered)) {
                                            nameError = context.getString(R.string.account_error_name_profanity)
                                        } else {
                                            nameError = null
                                        }
                                    } else {
                                        nameError = context.getString(R.string.account_error_name_long)
                                    }
                                },
                                label = { Text(stringResource(R.string.account_new_name_label)) },
                                singleLine = true,
                                enabled = !limitReached,
                                isError = nameError != null || nameAvailable == false,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    errorBorderColor = MaterialTheme.colorScheme.error
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge,
                                trailingIcon = {
                                    if (isCheckingNameLive) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                                    } else if (nameAvailable == true) {
                                        Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.Green, modifier = Modifier.size(18.dp))
                                    } else if (nameAvailable == false) {
                                        Icon(imageVector = Icons.Rounded.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            )
                            if (nameError != null || nameAvailable == false) {
                                Text(
                                    text = nameError ?: stringResource(R.string.account_error_name_taken),
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val isSaveEnabled = !limitReached && nameInput.isNotBlank() && nameInput != currentDisplayName &&
                                          nameInput.length >= 3 && nameAvailable == true &&
                                          nameError == null && !isCheckingNameLive && !isCheckingName

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.1f))
                                    .bounceClick { 
                                        editProfileSubScreen = EditProfileSubScreen.MENU
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(R.string.auth_guest_dialog_cancel),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSaveEnabled) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f))
                                    .bounceClick(enabled = isSaveEnabled) {
                                        if (nameChangesCount >= 2) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(context.getString(R.string.account_error_max_name_changes))
                                            }
                                            return@bounceClick
                                        }
                                        isCheckingName = true
                                        scope.launch {
                                            try {
                                                val oldNameDoc = Firebase.firestore.collection("usernames").document(currentDisplayName.lowercase())
                                                val newNameDoc = Firebase.firestore.collection("usernames").document(nameInput.lowercase())
                                                
                                                val existingDoc = newNameDoc.get().await()
                                                if (existingDoc.exists() && existingDoc.getString("uid") != currentUser!!.uid) {
                                                    isCheckingName = false
                                                    nameAvailable = false
                                                    nameError = context.getString(R.string.account_error_name_taken)
                                                    return@launch
                                                }
                                                
                                                Firebase.firestore.runTransaction { transaction ->
                                                    if (oldNameDoc.path != newNameDoc.path) {
                                                        transaction.delete(oldNameDoc)
                                                    }
                                                    transaction.set(newNameDoc, hashMapOf("uid" to currentUser!!.uid, "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                                                    transaction.update(
                                                        Firebase.firestore.collection("users").document(currentUser!!.uid),
                                                        "displayName", nameInput,
                                                        "nameChangesCount", com.google.firebase.firestore.FieldValue.increment(1)
                                                    )
                                                }.await()
                                                
                                                currentUser!!.updateProfile(userProfileChangeRequest {
                                                    displayName = nameInput
                                                }).await()
                                                
                                                currentDisplayName = nameInput
                                                nameChangesCount++
                                                prefs.edit().putInt("changes_${currentUser.uid}", nameChangesCount).apply()
                                                showProfileMenu = false
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar(context.getString(R.string.account_error_name_update_failed))
                                            } finally {
                                                isCheckingName = false
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCheckingName) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                } else {
                                    Text(
                                        stringResource(R.string.account_save), 
                                        fontWeight = FontWeight.Bold, 
                                        color = if (isSaveEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                }
                            }
                        }
                    }
                }

                EditProfileSubScreen.CHANGE_AVATAR -> {
                    AvatarSelectionContent(
                        mode = AvatarSelectionMode.AVATAR,
                        onBack = { editProfileSubScreen = EditProfileSubScreen.MENU },
                        onDismissRequest = { showProfileMenu = false },
                        onCharacterSelected = { newUrl, _ ->
                            val oldUrl = currentPhotoUrl
                            currentPhotoUrl = newUrl?.let { Uri.parse(it) }
                            currentUser?.updateProfile(userProfileChangeRequest { 
                                photoUri = newUrl?.let { Uri.parse(it) } 
                            })?.addOnSuccessListener {
                                val updates = mutableMapOf<String, Any?>("photoUrl" to newUrl)
                                Firebase.firestore.collection("users").document(currentUser!!.uid)
                                    .set(updates, SetOptions.merge())
                            }?.addOnFailureListener {
                                currentPhotoUrl = oldUrl
                                scope.launch { snackbarHostState.showSnackbar("Failed to update avatar. Please try again.") }
                            }
                            showProfileMenu = false
                        }
                    )
                }

                EditProfileSubScreen.CHANGE_COVER -> {
                    AvatarSelectionContent(
                        mode = AvatarSelectionMode.BACKDROP,
                        onBack = { editProfileSubScreen = EditProfileSubScreen.MENU },
                        onDismissRequest = { showProfileMenu = false },
                        onCharacterSelected = { _, backdropUrl ->
                            if (backdropUrl != null) {
                                prefs.edit().putString("avatar_backdrop_${currentUser!!.uid}", backdropUrl).apply()
                            } else {
                                prefs.edit().remove("avatar_backdrop_${currentUser!!.uid}").apply()
                            }
                            Firebase.firestore.collection("users").document(currentUser!!.uid)
                                .set(mapOf("avatarBackdrop" to backdropUrl), SetOptions.merge())
                            showProfileMenu = false
                        }
                    )
                }
            }
        }
    }

        Box(modifier = Modifier.fillMaxSize().zIndex(90000f)) {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
            )
        }
    }
