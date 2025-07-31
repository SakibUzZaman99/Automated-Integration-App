package com.example.localllmapp

import android.app.Notification
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MyNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListener"
        private const val GMAIL_PACKAGE = "com.google.android.gm"
        private const val TELEGRAM_PACKAGE = "org.telegram.messenger"

        // Broadcast action for sending notification data
        const val NOTIFICATION_RECEIVED_ACTION = "com.example.localllmapp.NOTIFICATION_RECEIVED"

    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        sbn?.let { notification ->
            val packageName = notification.packageName

            // Check if it's from Gmail or Telegram
            if (packageName == GMAIL_PACKAGE || packageName == TELEGRAM_PACKAGE) {
                val notificationData = extractNotificationData(notification)
                notificationData?.let { data ->
                    // Send the data via broadcast or callback
                    sendNotificationData(data)
                    Log.d(TAG, "Notification from ${data.appName}: ${data.title}")
                }
            }
        }
    }

    private fun extractNotificationData(sbn: StatusBarNotification): NotificationData? {
        try {
            val notification = sbn.notification
            val extras = notification.extras

            val appName = when (sbn.packageName) {
                GMAIL_PACKAGE -> "Gmail"
                TELEGRAM_PACKAGE -> "Telegram"
                else -> "Unknown"
            }

            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: text
            val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
            val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString() ?: ""

            // Extract sender information
            val sender = extractSenderInfo(sbn, extras)

            // Get timestamp
            val timestamp = sbn.postTime

            // Get notification ID and tag
            val notificationId = sbn.id
            val notificationTag = sbn.tag ?: ""

            // Extract additional metadata
            val category = notification.category ?: ""
            val group = notification.group ?: ""
            val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

            return NotificationData(
                appName = appName,
                packageName = sbn.packageName,
                title = title,
                text = text,
                bigText = bigText,
                subText = subText,
                summaryText = summaryText,
                sender = sender,
                timestamp = timestamp,
                notificationId = notificationId,
                notificationTag = notificationTag,
                category = category,
                group = group,
                isGroupSummary = isGroupSummary,
                extras = extractAllExtras(extras)
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting notification data", e)
            return null
        }
    }

    private fun extractSenderInfo(sbn: StatusBarNotification, extras: Bundle): String {
        return when (sbn.packageName) {
            GMAIL_PACKAGE -> {
                // For Gmail, sender is usually in title or sub text
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

                // Gmail often puts sender in title, subject in big text
                if (title.contains("@") || title.contains("<")) {
                    title
                } else {
                    subText.ifEmpty { title }
                }
            }
            TELEGRAM_PACKAGE -> {
                // For Telegram, sender is usually in the title
                extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            }
            else -> ""
        }
    }

    private fun extractAllExtras(extras: Bundle): Map<String, String> {
        val extrasMap = mutableMapOf<String, String>()

        for (key in extras.keySet()) {
            try {
                val value = extras.get(key)
                extrasMap[key] = value?.toString() ?: "null"
            } catch (e: Exception) {
                extrasMap[key] = "Error: ${e.message}"
            }
        }

        return extrasMap
    }

    private fun sendNotificationData(data: NotificationData) {
        // Method 1: Explicit Broadcast Intent
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            action = NOTIFICATION_RECEIVED_ACTION
            putExtra("notification_data", data)
        }
        sendBroadcast(intent)

        // Method 2: You can also store in database, send to server, etc.
        // Example: Store in Room database
        // notificationRepository.insertNotification(data)

        // Method 3: Send to your main activity if it's running
        // NotificationDataManager.getInstance().addNotification(data)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Handle notification removal if needed
        sbn?.let {
            Log.d(TAG, "Notification removed from ${it.packageName}")
        }
    }
}