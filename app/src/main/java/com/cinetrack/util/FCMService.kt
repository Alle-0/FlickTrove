package com.cinetrack.util

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cinetrack.MainActivity
import com.cinetrack.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "Refreshed token: \$token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        
        val tokenData = mapOf("fcmToken" to token)
        db.collection("user_fcm_tokens").document(user.uid)
            .set(tokenData)
            .addOnSuccessListener { Log.d("FCMService", "FCM token updated for user \${user.uid}") }
            .addOnFailureListener { e -> Log.w("FCMService", "Error updating FCM token", e) }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        // We now use data-only messages to have full control over the notification appearance,
        // even when the app is in the background.
        val data = remoteMessage.data
        
        var title = remoteMessage.notification?.title ?: data["title"]
        val titleLocKey = remoteMessage.notification?.titleLocalizationKey ?: data["titleLocKey"]
        val titleLocArgsStr = data["titleLocArgs"]
        if (title.isNullOrEmpty() && !titleLocKey.isNullOrEmpty()) {
            val resId = resources.getIdentifier(titleLocKey, "string", packageName)
            if (resId != 0) {
                if (!titleLocArgsStr.isNullOrEmpty()) {
                    try {
                        val jsonArray = org.json.JSONArray(titleLocArgsStr)
                        val args = Array(jsonArray.length()) { i -> jsonArray.getString(i) }
                        title = getString(resId, *args)
                    } catch (e: Exception) {
                        title = getString(resId)
                    }
                } else {
                    title = getString(resId)
                }
            }
        }
        if (title.isNullOrEmpty()) title = ""

        var body = remoteMessage.notification?.body ?: data["body"]
        val bodyLocKey = remoteMessage.notification?.bodyLocalizationKey ?: data["bodyLocKey"]
        val bodyLocArgsStr = data["bodyLocArgs"]
        if (body.isNullOrEmpty() && !bodyLocKey.isNullOrEmpty()) {
            val resId = resources.getIdentifier(bodyLocKey, "string", packageName)
            if (resId != 0) {
                if (!bodyLocArgsStr.isNullOrEmpty()) {
                    try {
                        val jsonArray = org.json.JSONArray(bodyLocArgsStr)
                        val args = Array(jsonArray.length()) { i -> jsonArray.getString(i) }
                        body = getString(resId, *args)
                    } catch (e: Exception) {
                        body = getString(resId)
                    }
                } else {
                    body = getString(resId)
                }
            }
        }
        if (body.isNullOrEmpty()) body = ""
        
        val mediaId = data["mediaId"]?.toLongOrNull() ?: 0L
        val mediaType = data["mediaType"] ?: ""
        val mediaImage = data["mediaImage"] ?: ""
        
        if (title!!.isNotEmpty() && NotificationHelper.hasNotificationPermission(this)) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                if (mediaId != 0L && mediaType.isNotEmpty()) {
                    var uriStr = "flicktrove://media/$mediaType/$mediaId"
                    val commentId = data["commentId"]
                    if (!commentId.isNullOrEmpty()) {
                        uriStr += "?openComments=true&commentId=$commentId"
                    }
                    this.data = android.net.Uri.parse(uriStr)
                }
            }
            
            val pendingIntent = PendingIntent.getActivity(
                this,
                (System.currentTimeMillis() % 10000).toInt(), // unique request code
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Download image synchronously for the LargeIcon (copertina di fianco)
            var largeIconBitmap: android.graphics.Bitmap? = null
            if (mediaImage.isNotEmpty()) {
                try {
                    val url = java.net.URL(mediaImage)
                    largeIconBitmap = android.graphics.BitmapFactory.decodeStream(url.openStream())
                } catch (e: Exception) {
                    Log.w("FCMService", "Failed to download media image for notification", e)
                }
            }

            val builder = NotificationCompat.Builder(this, "flicktrove_episodes")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setColor(0xFF00BFA5.toInt())
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                
            if (largeIconBitmap != null) {
                builder.setLargeIcon(largeIconBitmap)
            }

            NotificationManagerCompat.from(this).notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
        }
    }
}
