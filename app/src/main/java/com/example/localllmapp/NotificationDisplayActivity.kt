package com.example.localllmapp

import android.annotation.SuppressLint
import androidx.compose.ui.unit.sp
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

class NotificationDisplayActivity : ComponentActivity() {
    private var notificationReceiver: BroadcastReceiver? = null

    // Explicit type to avoid extension conflicts
    private val notifications: SnapshotStateList<NotificationData> = mutableStateListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var showPermissionDialog by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                Log.d("NotificationDisplay", "LaunchedEffect triggered")
                val hasPermission = isNotificationServiceEnabled()
                Log.d("NotificationDisplay", "Has permission: $hasPermission")

                if (!hasPermission) {
                    showPermissionDialog = true
                }
                setupNotificationReceiver()
            }

            MaterialTheme {
                NotificationDisplayScreen(
                    notifications = notifications,
                    hasPermission = isNotificationServiceEnabled(),
                    onBackClick = { finish() },
                    onOpenSettings = {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                )

                if (showPermissionDialog) {
                    PermissionDialog(
                        onDismiss = { showPermissionDialog = false },
                        onOpenSettings = {
                            showPermissionDialog = false
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupNotificationReceiver()
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val packageName = packageName
        val flat = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            for (name in names) {
                val componentName = ComponentName.unflattenFromString(name)
                if (componentName != null && TextUtils.equals(packageName, componentName.packageName)) {
                    return true
                }
            }
        }
        return false
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun setupNotificationReceiver() {
        if (notificationReceiver == null) {
            notificationReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == MyNotificationListenerService.NOTIFICATION_RECEIVED_ACTION) {
                        val notificationData = intent.getParcelableExtra<NotificationData>("notification_data")
                        notificationData?.let { data ->
                            notifications.add(0, data) // safe now
                            Log.d("NotificationDisplay", "Notification added: ${data.appName}")
                        }
                    }
                }
            }
            val filter = IntentFilter(MyNotificationListenerService.NOTIFICATION_RECEIVED_ACTION)
            //registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            registerReceiver(notificationReceiver, filter, Context.RECEIVER_NOT_EXPORTED)


        }
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                // ignore if not registered
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDisplayScreen(
    notifications: List<NotificationData>,
    hasPermission: Boolean,
    onBackClick: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF191919), Color(0xFF333639))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notification Monitor",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // Test button to manually add a notification
                    TextButton(onClick = {
                        val testNotification = NotificationData(
                            appName = "Gmail",
                            packageName = "com.google.android.gm",
                            title = "Test Notification",
                            text = "This is a test",
                            bigText = "This is a test notification",
                            sender = "test@example.com",
                            timestamp = System.currentTimeMillis()
                        )
                        if (notifications is SnapshotStateList<NotificationData>) {
                            (notifications as SnapshotStateList<NotificationData>).add(0, testNotification)
                        }
                        Log.d("NotificationDisplay", "Test notification added. List size: ${notifications.size}")
                    }) {
                        Text("TEST", color = Color.White, fontSize = 12.sp)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF23272A))
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(paddingValues)
        ) {
            if (!hasPermission) {
                NoPermissionView(onOpenSettings)
            } else if (notifications.isEmpty()) {
                EmptyNotificationsView()
            } else {
                NotificationsList(notifications)
            }
        }
    }
}

@Composable
fun NoPermissionView(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFF8AB4F8))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Notification Access Required", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Please enable notification access in settings to monitor Gmail and Telegram notifications.",
            fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onOpenSettings, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8AB4F8))) {
            Text("Open Settings", color = Color.Black)
        }
    }
}

@Composable
fun EmptyNotificationsView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFF8AB4F8))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Waiting for Notifications", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Send yourself a Gmail or Telegram message to see it appear here in real-time!",
            fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
fun NotificationsList(notifications: List<NotificationData>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(notifications) { notification -> NotificationCard(notification) }
    }
}

@Composable
fun NotificationCard(notification: NotificationData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2F33))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notification.appName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (notification.appName) {
                        "Gmail" -> Color(0xFFEA4335)
                        "Telegram" -> Color(0xFF0088CC)
                        else -> Color.White
                    }
                )
                Text(formatTimestamp(notification.timestamp), fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (notification.sender.isNotEmpty()) {
                Text("From: ${notification.sender}", fontSize = 14.sp, color = Color(0xFF8AB4F8), fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (notification.title.isNotEmpty()) {
                Text(notification.title, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
            }
            if (notification.bigText.isNotEmpty()) {
                Text(notification.bigText, fontSize = 12.sp, color = Color.LightGray, maxLines = 3)
            }
            if (notification.category.isNotEmpty() || notification.group.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (notification.category.isNotEmpty()) {
                        Chip("Category: ${notification.category}", Color(0xFF3F51B5))
                    }
                    if (notification.isGroupSummary) {
                        Chip("Group Summary", Color(0xFF9C27B0))
                    }
                }
            }
        }
    }
}

@Composable
fun Chip(text: String, backgroundColor: Color) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(backgroundColor.copy(alpha = 0.3f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, fontSize = 10.sp, color = backgroundColor)
    }
}

@Composable
fun PermissionDialog(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notification Access Required") },
        text = { Text("This app needs notification access to read Gmail and Telegram notifications. Please enable it in the settings.") },
        confirmButton = { TextButton(onClick = onOpenSettings) { Text("Open Settings") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
