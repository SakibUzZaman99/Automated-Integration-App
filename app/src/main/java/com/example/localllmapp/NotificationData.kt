package com.example.localllmapp

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NotificationData(
    val appName: String = "",
    val packageName: String = "",
    val title: String = "",
    val text: String = "",
    val bigText: String = "",
    val subText: String = "",
    val summaryText: String = "",
    val sender: String = "",
    val timestamp: Long = 0L,
    val notificationId: Int = 0,
    val notificationTag: String = "",
    val category: String = "",
    val group: String = "",
    val isGroupSummary: Boolean = false,
    val extras: Map<String, String> = emptyMap()
) : Parcelable