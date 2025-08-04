package com.example.localllmapp

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.*

/**
 * Handler for Telegram integration using Bot API or MasterBot
 * This can be adapted based on your specific Telegram integration method
 */
class TelegramApiHandler(private val context: Context) {

    companion object {
        private const val TAG = "TelegramApiHandler"

        // Replace with your actual Telegram Bot token
        private const val BOT_TOKEN = "YOUR_BOT_TOKEN"
        private const val TELEGRAM_API_BASE_URL = "https://api.telegram.org/bot$BOT_TOKEN"

        // MasterBot configuration (if using MasterBot instead of direct API)
        private const val MASTERBOT_API_URL = "YOUR_MASTERBOT_API_URL"
        private const val MASTERBOT_API_KEY = "YOUR_MASTERBOT_API_KEY"
    }

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val client = OkHttpClient()

    /**
     * Send message via Telegram
     */
    suspend fun sendMessage(
        phoneNumber: String,
        message: String,
        useMasterBot: Boolean = false
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (useMasterBot) {
                    sendViaMasterBot(phoneNumber, message)
                } else {
                    sendViaBotApi(phoneNumber, message)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending Telegram message", e)
                false
            }
        }
    }

    /**
     * Send message using Telegram Bot API
     */
    private fun sendViaBotApi(phoneNumber: String, message: String): Boolean {
        // Note: Telegram Bot API doesn't support sending messages to phone numbers directly
        // You need to have the chat_id or username
        // This is a simplified example - you'll need to implement proper user mapping

        val chatId = getChatIdFromPhoneNumber(phoneNumber)
        if (chatId == null) {
            Log.e(TAG, "No chat ID found for phone number: $phoneNumber")
            return false
        }

        val url = "$TELEGRAM_API_BASE_URL/sendMessage"

        val json = JSONObject().apply {
            put("chat_id", chatId)
            put("text", message)
            put("parse_mode", "HTML")
        }

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                Log.d(TAG, "Message sent successfully via Bot API")
                logMessageToFirestore(phoneNumber, message, "bot_api", true)
                return true
            } else {
                Log.e(TAG, "Failed to send message: ${response.body?.string()}")
                logMessageToFirestore(phoneNumber, message, "bot_api", false)
                return false
            }
        }
    }

    /**
     * Send message using MasterBot API
     */
    private fun sendViaMasterBot(phoneNumber: String, message: String): Boolean {
        // This is a template for MasterBot integration
        // Adjust based on your MasterBot API specifications

        val json = JSONObject().apply {
            put("phone_number", phoneNumber)
            put("message", message)
            put("api_key", MASTERBOT_API_KEY)
        }

        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(MASTERBOT_API_URL)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $MASTERBOT_API_KEY")
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                Log.d(TAG, "Message sent successfully via MasterBot")
                logMessageToFirestore(phoneNumber, message, "masterbot", true)
                return true
            } else {
                Log.e(TAG, "Failed to send via MasterBot: ${response.body?.string()}")
                logMessageToFirestore(phoneNumber, message, "masterbot", false)
                return false
            }
        }
    }

    /**
     * Get chat ID from phone number
     * This requires maintaining a mapping of phone numbers to chat IDs
     */
    private fun getChatIdFromPhoneNumber(phoneNumber: String): String? {
        // In a real implementation, you would:
        // 1. Query Firestore for existing mapping
        // 2. Use Telegram's contact import feature
        // 3. Or maintain a local database

        // For now, returning null - implement based on your needs
        return null
    }

    /**
     * Process incoming Telegram updates (webhooks)
     */
    fun processWebhookUpdate(update: JSONObject) {
        try {
            val message = update.optJSONObject("message")
            if (message != null) {
                val chatId = message.getJSONObject("chat").getLong("id")
                val text = message.optString("text", "")
                val from = message.getJSONObject("from")
                val firstName = from.optString("first_name", "")
                val username = from.optString("username", "")

                Log.d(TAG, "Received message from $firstName (@$username): $text")

                // Process based on your workflow
                handleIncomingMessage(chatId, username, text)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing webhook update", e)
        }
    }

    /**
     * Handle incoming Telegram messages
     */
    private fun handleIncomingMessage(chatId: Long, username: String, text: String) {
        // Check if this matches any workflow
        // This is where you'd implement your logic to:
        // 1. Check if the message matches workflow criteria
        // 2. Extract relevant data
        // 3. Trigger appropriate actions (e.g., send email via Gmail)

        Log.d(TAG, "Processing message for workflow matching")
    }

    /**
     * Log message to Firestore for analytics
     */
    private fun logMessageToFirestore(
        phoneNumber: String,
        message: String,
        method: String,
        success: Boolean
    ) {
        try {
            val userId = auth.currentUser?.uid ?: return

            val messageLog = hashMapOf(
                "userId" to userId,
                "phoneNumber" to phoneNumber,
                "message" to message.take(100), // Truncate for privacy
                "method" to method,
                "success" to success,
                "timestamp" to Date()
            )

            firestore.collection("telegram_logs")
                .add(messageLog)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error logging to Firestore", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in logMessageToFirestore", e)
        }
    }

    /**
     * Initialize Telegram bot webhook
     */
    suspend fun setupWebhook(webhookUrl: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = "$TELEGRAM_API_BASE_URL/setWebhook"

                val json = JSONObject().apply {
                    put("url", webhookUrl)
                    put("allowed_updates", listOf("message", "callback_query"))
                }

                val requestBody = json.toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    response.isSuccessful
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up webhook", e)
                false
            }
        }
    }

    /**
     * Store Telegram user mapping in Firestore
     */
    suspend fun storeUserMapping(phoneNumber: String, chatId: Long, username: String?) {
        try {
            val userId = auth.currentUser?.uid ?: return

            val mapping = hashMapOf(
                "userId" to userId,
                "phoneNumber" to phoneNumber,
                "chatId" to chatId,
                "username" to (username ?: ""),
                "createdAt" to Date(),
                "lastUpdated" to Date()
            )

            firestore.collection("telegram_mappings")
                .document(phoneNumber)
                .set(mapping)
                .addOnSuccessListener {
                    Log.d(TAG, "User mapping stored successfully")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error storing user mapping", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in storeUserMapping", e)
        }
    }
}