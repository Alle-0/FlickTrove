package com.cinetrack.ui.components.share

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.cinetrack.R
import com.cinetrack.data.model.Movie
import com.cinetrack.ui.components.common.SegmentedControl
import com.cinetrack.ui.components.common.SegmentedOption
import com.cinetrack.ui.components.shared.FlickTroveBottomSheet
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.VibrationHelper
import com.google.firebase.auth.FirebaseAuth
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareStoryCardModal(
    visible: Boolean,
    movie: Movie,
    accentColor: Color,
    hazeState: HazeState? = null,
    onDismiss: () -> Unit,
    onShareToComments: (Uri) -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val userDisplayName = remember(currentUser) { currentUser?.displayName?.takeIf { it.isNotBlank() } }
    val userPhotoUrl = remember(currentUser) { currentUser?.photoUrl?.toString() }

    var config by remember(movie) {
        mutableStateOf(
            ShareStoryCardConfig(
                coverStyle = ShareCoverStyle.POSTER_FLOATING,
                showRating = (movie.personalRating ?: 0.0) > 0.0,
                showProfile = userDisplayName != null,
                showVibe = !movie.emotionalVibes.isNullOrBlank() && movie.emotionalVibes != "[]",
                showWatermark = true
            )
        )
    }

    var isProcessing by remember { mutableStateOf(false) }

    suspend fun captureCardBitmap(): Bitmap? {
        return try {
            delay(120) // Ensure layout is drawn
            val imageBitmap = graphicsLayer.toImageBitmap()
            val androidBmp = imageBitmap.asAndroidBitmap()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && androidBmp.config == Bitmap.Config.HARDWARE) {
                androidBmp.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                androidBmp
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    FlickTroveBottomSheet(
        onDismissRequest = onDismiss,
        hazeState = hazeState,
        dragOnlyFromHandle = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_share),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Story Card",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Scaled Live 9:16 Preview Card
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .aspectRatio(9f / 16f)
                    .drawWithContent {
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(graphicsLayer)
                    },
                contentAlignment = Alignment.Center
            ) {
                ShareStoryCard(
                    movie = movie,
                    config = config,
                    accentColor = accentColor,
                    userDisplayName = userDisplayName,
                    userPhotoUrl = userPhotoUrl,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cover Style Selector with fluid drag, physics, and haptics
            SegmentedControl(
                options = listOf(
                    SegmentedOption(value = ShareCoverStyle.POSTER_FLOATING, title = "Poster"),
                    SegmentedOption(value = ShareCoverStyle.BACKDROP_FULL, title = "Backdrop")
                ),
                selected = config.coverStyle,
                accentColor = accentColor,
                vibrationEnabled = true,
                context = context,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                height = 42.dp,
                onSelect = { newStyle ->
                    config = config.copy(coverStyle = newStyle)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Customization Toggle Chips (Il voto è fisso, opzioni per Profilo e Vibe)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Toggle Profilo
                if (userDisplayName != null) {
                    ShareOptionChip(
                        label = "Profilo",
                        isActive = config.showProfile,
                        icon = ImageVector.vectorResource(id = R.drawable.ic_persona),
                        accentColor = accentColor,
                        onClick = { config = config.copy(showProfile = !config.showProfile) }
                    )
                }

                // Toggle Vibe
                if (!movie.emotionalVibes.isNullOrBlank() && movie.emotionalVibes != "[]") {
                    ShareOptionChip(
                        label = "Vibe",
                        isActive = config.showVibe,
                        icon = ImageVector.vectorResource(id = R.drawable.ic_sparkle),
                        accentColor = accentColor,
                        onClick = { config = config.copy(showVibe = !config.showVibe) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3 Bottom Action Buttons: [ Salva ] [ Condividi ] [ Nei commenti ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Salva
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .bounceClick(scaleDown = 0.94f) {
                            if (isProcessing) return@bounceClick
                            isProcessing = true
                            coroutineScope.launch {
                                val bitmap = captureCardBitmap()
                                if (bitmap != null) {
                                    val success = withContext(Dispatchers.IO) {
                                        saveBitmapToGallery(context, bitmap, movie.displayName)
                                    }
                                    if (success) {
                                        Toast.makeText(context, "Card salvata nella galleria!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Errore durante il salvataggio", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                isProcessing = false
                            }
                        }
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(50))
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_scaricare),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Salva",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // 2. Condividi (Social / External)
                Row(
                    modifier = Modifier
                        .weight(1.15f)
                        .height(46.dp)
                        .bounceClick(scaleDown = 0.94f) {
                            if (isProcessing) return@bounceClick
                            isProcessing = true
                            coroutineScope.launch {
                                val bitmap = captureCardBitmap()
                                if (bitmap != null) {
                                    withContext(Dispatchers.IO) {
                                        shareBitmapExternal(context, bitmap, movie.displayName)
                                    }
                                }
                                isProcessing = false
                            }
                        }
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_share),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Condividi",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // 3. Condividi nei commenti
                Row(
                    modifier = Modifier
                        .weight(1.3f)
                        .height(46.dp)
                        .bounceClick(scaleDown = 0.94f) {
                            if (isProcessing) return@bounceClick
                            isProcessing = true
                            coroutineScope.launch {
                                val bitmap = captureCardBitmap()
                                if (bitmap != null) {
                                    val uri = withContext(Dispatchers.IO) {
                                        saveBitmapToCacheUri(context, bitmap, movie.displayName)
                                    }
                                    if (uri != null) {
                                        onDismiss()
                                        onShareToComments(uri)
                                    } else {
                                        Toast.makeText(context, "Errore nella preparazione dell'immagine", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                isProcessing = false
                            }
                        }
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accentColor,
                                    accentColor.copy(alpha = 0.88f)
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_send),
                        contentDescription = null,
                        tint = ColorUtils.contentColorForAccent(accentColor),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Nei commenti",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        color = ColorUtils.contentColorForAccent(accentColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareOptionChip(
    label: String,
    isActive: Boolean,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .bounceClick(scaleDown = 0.90f) { onClick() }
            .clip(CircleShape)
            .background(
                if (isActive) accentColor.copy(alpha = 0.2f)
                else Color.White.copy(alpha = 0.06f)
            )
            .border(
                0.5.dp,
                if (isActive) accentColor.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.12f),
                CircleShape
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) ColorUtils.lightenForText(accentColor, 1.35f) else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) Color.White else Color.White.copy(alpha = 0.7f)
        )
    }
}

/**
 * Saves the story card bitmap into MediaStore (Pictures/FlickTrove)
 */
private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Boolean {
    return try {
        val cleanTitle = title.replace("[^a-zA-Z0-9]".toRegex(), "_").take(30)
        val filename = "FlickTrove_${cleanTitle}_${System.currentTimeMillis()}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FlickTrove")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return false

        resolver.openOutputStream(uri)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

/**
 * Saves bitmap into cacheDir/images and opens standard Android ACTION_SEND intent
 */
private fun shareBitmapExternal(context: Context, bitmap: Bitmap, title: String) {
    try {
        val cachePath = File(context.cacheDir, "images")
        if (!cachePath.exists()) cachePath.mkdirs()

        val cleanTitle = title.replace("[^a-zA-Z0-9]".toRegex(), "_").take(30)
        val imageFile = File(cachePath, "story_${cleanTitle}_${System.currentTimeMillis()}.png")
        FileOutputStream(imageFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
        }

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "Guarda la mia scheda per $title su FlickTrove! 🎬")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Condividi Story Card")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Errore nella condivisione", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Saves bitmap into cacheDir/images and returns its FileProvider content Uri
 */
private fun saveBitmapToCacheUri(context: Context, bitmap: Bitmap, title: String): Uri? {
    return try {
        val cachePath = File(context.cacheDir, "images")
        if (!cachePath.exists()) cachePath.mkdirs()

        val cleanTitle = title.replace("[^a-zA-Z0-9]".toRegex(), "_").take(30)
        val imageFile = File(cachePath, "comment_card_${cleanTitle}_${System.currentTimeMillis()}.png")
        FileOutputStream(imageFile).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
        }

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
