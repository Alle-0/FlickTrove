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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
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
    var showDashboardSettings by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var showGuestAuthDialog by remember { mutableStateOf(false) }
    
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
    
    val showMyFolders by settingsViewModel.showMyFolders.collectAsStateWithLifecycle()
    val showYourFlow by settingsViewModel.showYourFlow.collectAsStateWithLifecycle()
    
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        settingsViewModel.showEditProfileMenu.collect {
            if (currentUser != null && !currentUser.isAnonymous) {
                showProfileMenu = true
            } else {
                showGuestAuthDialog = true
            }
        }
    }
    
    LaunchedEffect(Unit) {
        settingsViewModel.showGuestAuthDialog.collect {
            showGuestAuthDialog = true
        }
    }
    
    LaunchedEffect(Unit) {
        settingsViewModel.showDashboardSettingsMenu.collect {
            showDashboardSettings = true
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
            nameAvailable = !doc.exists()
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

    if (showProfileMenu || showDashboardSettings || showNameDialog || showGuestAuthDialog) {
        Box(modifier = Modifier.zIndex(80000f)) {
            // Profile Menu Modal
            if (showProfileMenu) {
                BackHandler(enabled = showProfileMenu) {
                    showProfileMenu = false
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showProfileMenu = false },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 300.dp)
                            .fillMaxWidth(0.85f)
                            .hazeGlass(state = globalHazeState, alpha = 1f, shape = RoundedCornerShape(32.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { focusManager.clearFocus() }
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                stringResource(R.string.account_edit_profile),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showProfileMenu = false
                                        nameInput = currentDisplayName
                                        showNameDialog = true
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
                                Text(stringResource(R.string.account_change_name_menu), color = MaterialTheme.colorScheme.onSurface)
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showProfileMenu = false
                                        if (avatarBanned) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(context.getString(R.string.avatar_banned_message))
                                            }
                                        } else {
                                            avatarSelection.show(
                                                mode = com.cinetrack.ui.components.account.AvatarSelectionMode.AVATAR,
                                                onDismissed = { showProfileMenu = true }
                                            ) { newUrl, _ ->
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
                                            }
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
                                Text(stringResource(R.string.account_change_avatar_menu), color = MaterialTheme.colorScheme.onSurface)
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        showProfileMenu = false
                                        avatarSelection.show(
                                            mode = com.cinetrack.ui.components.account.AvatarSelectionMode.BACKDROP,
                                            onDismissed = { showProfileMenu = true }
                                        ) { _, backdropUrl ->
                                            if (backdropUrl != null) {
                                                prefs.edit().putString("avatar_backdrop_${currentUser!!.uid}", backdropUrl).apply()
                                            } else {
                                                prefs.edit().remove("avatar_backdrop_${currentUser!!.uid}").apply()
                                            }
                                            Firebase.firestore.collection("users").document(currentUser!!.uid)
                                                .set(mapOf("avatarBackdrop" to backdropUrl), SetOptions.merge())
                                        }
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
                                Text(stringResource(R.string.account_change_cover_menu), color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
            
            // Dashboard Settings Modal
            if (showDashboardSettings) {
                BackHandler(enabled = showDashboardSettings) {
                    showDashboardSettings = false
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showDashboardSettings = false },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 350.dp)
                            .fillMaxWidth(0.9f)
                            .hazeGlass(state = globalHazeState, alpha = 1f, shape = RoundedCornerShape(32.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { focusManager.clearFocus() }
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                stringResource(R.string.settings_ui_layout),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_cartella),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.settings_show_my_folders), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                    Text(stringResource(R.string.settings_show_my_folders_desc), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
                                }
                                FlickTroveSwitch(
                                    checked = showMyFolders,
                                    onCheckedChange = { settingsViewModel.toggleShowMyFolders(it) },
                                    accentColor = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_sparkle),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.settings_show_your_flow), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                                    Text(stringResource(R.string.settings_show_your_flow_desc), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), style = MaterialTheme.typography.bodyMedium)
                                }
                                FlickTroveSwitch(
                                    checked = showYourFlow,
                                    onCheckedChange = { settingsViewModel.toggleShowYourFlow(it) },
                                    accentColor = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Name Dialog
            if (showNameDialog) {
                var isCheckingName by remember { mutableStateOf(false) }
                BackHandler(enabled = showNameDialog) {
                    showNameDialog = false
                    showProfileMenu = true
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { 
                            focusManager.clearFocus()
                            showNameDialog = false
                            showProfileMenu = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 400.dp)
                            .fillMaxWidth(0.85f)
                            .hazeGlass(state = globalHazeState, alpha = 1f, shape = RoundedCornerShape(32.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { focusManager.clearFocus() }
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
                            
                            val maxNameLength = 20
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = {
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
                            
                            val isSaveEnabled = nameInput.isNotBlank() && nameInput != currentDisplayName && 
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
                                        .clickable { 
                                            showNameDialog = false
                                            showProfileMenu = true
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
                                        .clickable(enabled = isSaveEnabled) {
                                            if (nameChangesCount >= 2) {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(context.getString(R.string.account_error_max_name_changes))
                                                }
                                                return@clickable
                                            }
                                            isCheckingName = true
                                            scope.launch {
                                                try {
                                                    val oldNameDoc = Firebase.firestore.collection("usernames").document(currentDisplayName.lowercase())
                                                    val newNameDoc = Firebase.firestore.collection("usernames").document(nameInput.lowercase())
                                                    
                                                    val existingDoc = newNameDoc.get().await()
                                                    if (existingDoc.exists()) {
                                                        isCheckingName = false
                                                        nameAvailable = false
                                                        nameError = context.getString(R.string.account_error_name_taken)
                                                        return@launch
                                                    }
                                                    
                                                    Firebase.firestore.runTransaction { transaction ->
                                                        transaction.delete(oldNameDoc)
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
                                                    showNameDialog = false
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
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showGuestAuthDialog = false },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 400.dp)
                            .fillMaxWidth(0.85f)
                            .hazeGlass(state = globalHazeState, alpha = 1f, shape = RoundedCornerShape(32.dp))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                stringResource(R.string.account_guest_sync_title),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.account_guest_sync_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = { showGuestAuthDialog = false },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                                ) {
                                    Text(stringResource(R.string.auth_guest_dialog_cancel), color = MaterialTheme.colorScheme.onSurface)
                                }
                                Button(
                                    onClick = {
                                        showGuestAuthDialog = false
                                        val rootNav = navigator?.parent ?: navigator
                                        rootNav?.push(com.cinetrack.ui.screens.LoginScreen())
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(stringResource(R.string.auth_btn_login), color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
            
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
            )
        }
    }
}
