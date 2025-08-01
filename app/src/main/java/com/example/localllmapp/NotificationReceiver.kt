package com.example.localllmapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == MyNotificationListenerService.NOTIFICATION_RECEIVED_ACTION) {
            val notificationData = intent.getParcelableExtra<NotificationData>("notification_data")

            notificationData?.let { data ->
                Log.d(TAG, "Received notification data: ${data.appName} - ${data.title}")

                // Process your notification data here
                processNotificationData(data)

                // The data will automatically be received by NotificationDisplayActivity
                // if it's currently running, since we're using the same broadcast action
            }
        }
    }

    private fun processNotificationData(data: NotificationData) {
        // Handle the notification data based on your requirements
        when (data.appName) {
            "Gmail" -> {
                Log.d(TAG, "Gmail notification - Sender: ${data.sender}, Subject: ${data.title}")
                // Process Gmail notification
                handleGmailNotification(data)
            }
            "Telegram" -> {
                Log.d(TAG, "Telegram notification - From: ${data.sender}, Message: ${data.text}")
                // Process Telegram notification
                handleTelegramNotification(data)
            }
        }
    }

    private fun handleGmailNotification(data: NotificationData) {
        // Your Gmail-specific processing logic
        // You can extract email subject, sender, preview text, etc.

        // Example: Log all available data
        Log.d(TAG, "Gmail Data:")
        Log.d(TAG, "Sender: ${data.sender}")
        Log.d(TAG, "Subject: ${data.title}")
        Log.d(TAG, "Preview: ${data.text}")
        Log.d(TAG, "Full text: ${data.bigText}")
        Log.d(TAG, "Timestamp: ${data.timestamp}")

        // TODO: Add your custom Gmail processing logic here
        // e.g., save to database, send to server, etc.
    }

    private fun handleTelegramNotification(data: NotificationData) {
        // Your Telegram-specific processing logic
        // You can extract chat name, sender, message content, etc.

        // Example: Log all available data
        Log.d(TAG, "Telegram Data:")
        Log.d(TAG, "From: ${data.sender}")
        Log.d(TAG, "Message: ${data.text}")
        Log.d(TAG, "Full message: ${data.bigText}")
        Log.d(TAG, "Timestamp: ${data.timestamp}")

        // TODO: Add your custom Telegram processing logic here
        // e.g., save to database, send to server, etc.
    }
}