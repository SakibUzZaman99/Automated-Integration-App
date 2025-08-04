package com.example.localllmapp

import android.app.Notification
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.localllmapp.workflow.WorkflowExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListener"
        private const val GMAIL_PACKAGE = "com.google.android.gm"
        private const val TELEGRAM_PACKAGE = "org.telegram.messenger"
        const val NOTIFICATION_RECEIVED_ACTION = "com.example.localllmapp.NOTIFICATION_RECEIVED"
    }

    private lateinit var workflowExecutor: WorkflowExecutor
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotificationListenerService created")
        workflowExecutor = WorkflowExecutor(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        sbn?.let { notification ->
            val packageName = notification.packageName

            // Check if it's from Gmail or Telegram
            if (packageName == GMAIL_PACKAGE || packageName == TELEGRAM_PACKAGE) {
                Log.d(TAG, "Relevant notification detected from: $packageName")

                // Extract basic info for workflow matching
                val extras = notification.notification.extras
                val appName = when (packageName) {
                    GMAIL_PACKAGE -> "Gmail"
                    TELEGRAM_PACKAGE -> "Telegram"
                    else -> return
                }

                val sender = extractSenderInfo(notification, extras)
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()

                // Process in background
                serviceScope.launch {
                    try {
                        Log.d(TAG, "Processing workflow for $appName notification")
                        workflowExecutor.processNotification(
                            appName = appName,
                            notificationSender = sender,
                            notificationTitle = title
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing workflow", e)
                    }
                }

                // Still send broadcast for UI updates if needed
                val notificationData = extractNotificationData(notification)
                notificationData?.let { data ->
                    sendNotificationData(data)
                }
            }
        }
    }

    private fun extractSenderInfo(sbn: StatusBarNotification, extras: Bundle): String {
        return when (sbn.packageName) {
            GMAIL_PACKAGE -> {
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

                if (title.contains("@") || title.contains("<")) {
                    title
                } else {
                    subText.ifEmpty { title }
                }
            }
            TELEGRAM_PACKAGE -> {
                extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            }
            else -> ""
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
            val sender = extractSenderInfo(sbn, extras)
            val timestamp = sbn.postTime
            val notificationId = sbn.id
            val notificationTag = sbn.tag ?: ""
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
                extras = emptyMap() // Simplified for performance
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting notification data", e)
            return null
        }
    }

    private fun sendNotificationData(data: NotificationData) {
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            action = NOTIFICATION_RECEIVED_ACTION
            putExtra("notification_data", data)
        }
        sendBroadcast(intent)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        sbn?.let {
            Log.d(TAG, "Notification removed from ${it.packageName}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        workflowExecutor.cleanup()
    }
}