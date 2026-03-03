package com.example.illustcart

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "flash_sales_channel"
        private const val NOTIFICATION_ID = 1
    }

    /**
     * ✅ UPDATED: Handles both notification and data messages
     *
     * When app is CLOSED: System displays notification automatically from notification payload
     * When app is OPEN: This method processes data payload for custom handling
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "📩 Message received from: ${message.from}")

        // Check if message contains a notification payload (when app is in foreground)
        message.notification?.let { notification ->
            Log.d(TAG, "Notification payload received:")
            Log.d(TAG, "  Title: ${notification.title}")
            Log.d(TAG, "  Body: ${notification.body}")

            // When app is in foreground, we need to manually show notification
            // because Android doesn't auto-display it
            val title = notification.title ?: "Flash Sale!"
            val body = notification.body ?: "Check out this amazing deal!"
            val imageUrl = message.data["imageUrl"] ?: notification.imageUrl?.toString()

            showNotification(title, body, imageUrl)
            return
        }

        // Check if message contains a data payload (legacy support or when app processes it)
        if (message.data.isNotEmpty()) {
            Log.d(TAG, "Data payload received:")
            val title = message.data["title"] ?: "Flash Sale!"
            val body = message.data["body"] ?: "Check out this amazing deal!"
            val imageUrl = message.data["imageUrl"]

            Log.d(TAG, "  Title: $title")
            Log.d(TAG, "  Body: $body")
            Log.d(TAG, "  Image URL: $imageUrl")

            // Show notification with custom handling
            showNotification(title, body, imageUrl)
        }
    }

    /**
     * ✅ UPDATED: Enhanced notification display with image support
     */
    private fun showNotification(title: String, body: String, imageUrl: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create notification channel (Android 8.0+)
                createNotificationChannel()

                // Download artwork image if available
                var artworkBitmap: Bitmap? = null
                if (!imageUrl.isNullOrEmpty()) {
                    try {
                        artworkBitmap = Glide.with(applicationContext)
                            .asBitmap()
                            .load(imageUrl)
                            .submit(512, 512)  // Larger size for better quality
                            .get()
                        Log.d(TAG, "✅ Artwork image downloaded successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to download artwork image: ${e.message}")
                    }
                }

                // Intent to open MainActivity when notification clicked
                val intent = Intent(this@MyFirebaseMessagingService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    // ✅ NEW: Add extra to identify this came from flash sale notification
                    putExtra("from_notification", true)
                    putExtra("notification_type", "flash_sale")
                }

                val pendingIntent = PendingIntent.getActivity(
                    this@MyFirebaseMessagingService,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Build notification
                val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                val notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)  // White icon for status bar
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setVibrate(longArrayOf(0, 500, 200, 500))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setColor(applicationContext.getColor(R.color.colorAccent))  // Flash sale color

                // ✅ NEW: Add large icon (artwork image)
                if (artworkBitmap != null) {
                    notificationBuilder.setLargeIcon(artworkBitmap)

                    // ✅ NEW: Add big picture style for expanded notification
                    notificationBuilder.setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(artworkBitmap)
                            .bigLargeIcon(null as Bitmap?)  // Hide large icon when expanded
                            .setBigContentTitle(title)
                            .setSummaryText(body)
                    )
                } else {
                    // Fallback: use app icon
                    try {
                        val appIcon = Glide.with(applicationContext)
                            .asBitmap()
                            .load(R.mipmap.ic_launcher)
                            .submit(128, 128)
                            .get()
                        notificationBuilder.setLargeIcon(appIcon)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to load app icon: ${e.message}")
                    }
                }

                // Show notification
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())

                Log.d(TAG, "✅ Notification displayed successfully")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error showing notification: ${e.message}", e)
            }
        }
    }

    /**
     * Create notification channel for Android 8.0+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Flash Sales",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for flash sale deals and exclusive offers"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                enableLights(true)
                lightColor = android.graphics.Color.RED  // Flash sale indicator
                setShowBadge(true)  // Show badge on app icon
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Called when a new FCM token is generated
     * You can use this to send the token to your backend server if needed
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "🔑 New FCM token: $token")

        // ✅ NEW: Automatically subscribe to flash_sales topic
        com.google.firebase.messaging.FirebaseMessaging.getInstance()
            .subscribeToTopic("flash_sales")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, " Auto-subscribed to flash_sales topic")
                } else {
                    Log.e(TAG, "Failed to subscribe to flash_sales topic")
                }
            }

        // ✅ OPTIONAL: Send token to your backend server
        // sendTokenToServer(token)
    }
}