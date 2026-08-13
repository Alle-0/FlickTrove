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
        
        // Notifications are automatically handled by FCM if the app is in the background
        // AND the payload contains a "notification" object.
        // But if the app is in the foreground, we should display it manually.
        val notification = remoteMessage.notification
        val data = remoteMessage.data
        
        if (notification != null) {
            var title = notification.title
            if (title.isNullOrEmpty() && !notification.titleLocalizationKey.isNullOrEmpty()) {
                val resId = resources.getIdentifier(notification.titleLocalizationKey, "string", packageName)
                if (resId != 0) {
                    val args = notification.titleLocalizationArgs
                    title = if (args != null) getString(resId, *args) else getString(resId)
                }
            }
            if (title == null) title = ""

            var body = notification.body
            if (body.isNullOrEmpty() && !notification.bodyLocalizationKey.isNullOrEmpty()) {
                val resId = resources.getIdentifier(notification.bodyLocalizationKey, "string", packageName)
                if (resId != 0) {
                    val args = notification.bodyLocalizationArgs
                    body = if (args != null) getString(resId, *args) else getString(resId)
                }
            }
            if (body == null) body = ""
            
            val mediaId = data["mediaId"]?.toLongOrNull() ?: 0L
            val mediaType = data["mediaType"] ?: ""
            
            if (mediaId != 0L && mediaType.isNotEmpty() && NotificationHelper.hasNotificationPermission(this)) {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    this.data = android.net.Uri.parse("flicktrove://media/\$mediaType/\$mediaId")
                }
                
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    mediaId.toInt(), // unique request code
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val builder = NotificationCompat.Builder(this, "flicktrove_episodes") // reuse the general channel or create a new one
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setColor(0xFF00BFA5.toInt())
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)

                NotificationManagerCompat.from(this).notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
            }
        }
    }
}
