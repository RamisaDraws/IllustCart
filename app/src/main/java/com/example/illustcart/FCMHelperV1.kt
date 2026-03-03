package com.example.illustcart

import android.content.Context
import android.util.Log
import com.google.auth.oauth2.GoogleCredentials
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayInputStream

object FCMHelperV1 {

    private const val TAG = "FCMHelperV1"

    // REPLACE WITH PROJECT ID (from Firebase Console)
    private const val PROJECT_ID = "illustcart"

    private const val FCM_API_URL = "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send"
    private const val SCOPES = "https://www.googleapis.com/auth/firebase.messaging"

    private val client = OkHttpClient()
    private val gson = Gson()

    /**
     * Send flash sale notification to all users subscribed to "flash_sales" topic
     * ✅ UPDATED: Now works even when app is closed!
     */
    fun sendFlashSaleNotification(
        context: Context,
        productName: String,
        discount: Int,
        durationHours: Int,
        durationMinutes: Int,
        originalPrice: String,
        salePrice: String,
        printsLeft: Int,
        imageUrl: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get access token
                val accessToken = getAccessToken(context)

                if (accessToken == null) {
                    Log.e(TAG, "Failed to get access token")
                    return@launch
                }

                // Format duration
                val duration = when {
                    durationHours > 0 -> "${durationHours}h ${durationMinutes}m"
                    durationMinutes > 0 -> "${durationMinutes}m"
                    else -> "Limited time"
                }

                // Build notification message
                val title = "Flash Sale Alert!"
                val body = "$productName - $discount% OFF for $duration\n$originalPrice → $salePrice | $printsLeft prints left"

                // ✅ NEW: Create FCM V1 message with BOTH notification AND data payloads
                // This ensures notifications work even when app is closed
                val message = mapOf(
                    "message" to mapOf(
                        "topic" to "flash_sales",

                        // ✅ NOTIFICATION PAYLOAD - Displayed by system when app is closed
                        "notification" to mapOf(
                            "title" to title,
                            "body" to body,
                            "image" to imageUrl  // Shows image in notification (Android 5.0+)
                        ),

                        // ✅ DATA PAYLOAD - Used by app when it's open for custom handling
                        "data" to mapOf(
                            "title" to title,
                            "body" to body,
                            "imageUrl" to imageUrl,
                            "productName" to productName,
                            "discount" to discount.toString(),
                            "originalPrice" to originalPrice,
                            "salePrice" to salePrice,
                            "printsLeft" to printsLeft.toString()
                        ),

                        // Android-specific settings
                        "android" to mapOf(
                            "priority" to "high",
                            "notification" to mapOf(
                                "channel_id" to "flash_sales_channel",  // Match your notification channel
                                "sound" to "default",
                                "notification_priority" to "PRIORITY_HIGH",
                                "visibility" to "PUBLIC",
                                "icon" to "ic_notification",  // Your notification icon
                                "color" to "#FF5722",  // Orange/red for flash sales
                                "tag" to "flash_sale_$productName",  // Group similar notifications
                                "click_action" to "OPEN_FLASH_SALE"  // Custom action on tap
                            )
                        )
                    )
                )

                val jsonPayload = gson.toJson(message)

                // Send HTTP POST request
                val requestBody = jsonPayload.toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(FCM_API_URL)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    Log.d(TAG, "Notification sent successfully!")
                    Log.d(TAG, "Response: ${response.body?.string()}")
                } else {
                    Log.e(TAG, "Failed to send notification. Code: ${response.code}")
                    Log.e(TAG, "Error: ${response.body?.string()}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error sending notification: ${e.message}", e)
            }
        }
    }

    /**
     * Get OAuth2 access token from service account JSON
     */
    private suspend fun getAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            // Read service account JSON from assets
            val inputStream = context.assets.open("service-account.json")

            val credentials = GoogleCredentials
                .fromStream(inputStream)
                .createScoped(listOf(SCOPES))

            // Refresh to get access token
            credentials.refresh()

            val accessToken = credentials.accessToken.tokenValue
            Log.d(TAG, "Access token obtained successfully")

            return@withContext accessToken

        } catch (e: Exception) {
            Log.e(TAG, "Error getting access token: ${e.message}", e)
            return@withContext null
        }
    }
}