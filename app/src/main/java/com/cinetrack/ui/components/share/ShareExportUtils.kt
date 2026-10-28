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
import androidx.core.content.FileProvider
import com.cinetrack.R
import java.io.File
import java.io.FileOutputStream

/**
 * Saves the story card bitmap into MediaStore (Pictures/FlickTrove)
 */
internal fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Boolean {
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
internal fun shareBitmapExternal(context: Context, bitmap: Bitmap, title: String) {
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
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_intent_text, title))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, context.getString(R.string.share_intent_chooser_title))
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, context.getString(R.string.share_toast_share_error), Toast.LENGTH_SHORT).show()
    }
}

/**
 * Saves bitmap into cacheDir/images and returns its FileProvider content Uri
 */
internal fun saveBitmapToCacheUri(context: Context, bitmap: Bitmap, title: String): Uri? {
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
