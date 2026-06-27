package com.acadex.app.features.notifications

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acadex.app.data.models.AppNotification
import com.acadex.app.data.models.NotificationPreferences
import com.acadex.app.data.models.NotificationType
import com.acadex.app.ui.theme.Primary
import com.acadex.app.ui.theme.Secondary
import com.acadex.app.ui.theme.Success
import com.acadex.app.ui.theme.Warning
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel
) {
    val grouped by viewModel.groupedNotifications.collectAsState()
    val prefs by viewModel.preferences.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    var showSettings by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Notifications",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (unreadCount > 0) {
                        Text(
                            "$unreadCount unread",
                            fontSize = 12.sp,
                            color = Primary
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (unreadCount > 0) {
                        TextButton(onClick = { viewModel.markAllAsRead() }) {
                            Text("Mark all read", color = Primary, fontSize = 13.sp)
                        }
                    }
                    IconButton(onClick = { showSettings = !showSettings }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Notification Settings",
                            tint = if (showSettings) Primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Settings Panel
            if (showSettings) {
                NotificationSettingsPanel(
                    preferences = prefs,
                    isLoading = isLoading,
                    onPreferencesChange = { viewModel.updatePreferences(it) }
                )
            }

            // Error Banner
            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // Notification List
            val hasAny = grouped.today.isNotEmpty() || grouped.yesterday.isNotEmpty() || grouped.earlier.isNotEmpty()

            if (!hasAny) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "You're all caught up!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            "No notifications yet",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (grouped.today.isNotEmpty()) {
                        item {
                            NotificationSectionHeader("Today")
                        }
                        items(grouped.today, key = { it.id }) { notif ->
                            NotificationCard(
                                notification = notif,
                                onRead = { viewModel.markAsRead(notif.id) },
                                onDelete = { viewModel.deleteNotification(notif.id) }
                            )
                        }
                    }
                    if (grouped.yesterday.isNotEmpty()) {
                        item { NotificationSectionHeader("Yesterday") }
                        items(grouped.yesterday, key = { it.id }) { notif ->
                            NotificationCard(
                                notification = notif,
                                onRead = { viewModel.markAsRead(notif.id) },
                                onDelete = { viewModel.deleteNotification(notif.id) }
                            )
                        }
                    }
                    if (grouped.earlier.isNotEmpty()) {
                        item { NotificationSectionHeader("Earlier") }
                        items(grouped.earlier, key = { it.id }) { notif ->
                            NotificationCard(
                                notification = notif,
                                onRead = { viewModel.markAsRead(notif.id) },
                                onDelete = { viewModel.deleteNotification(notif.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationSectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        letterSpacing = 1.sp
    )
}

@Composable
private fun NotificationCard(
    notification: AppNotification,
    onRead: () -> Unit,
    onDelete: () -> Unit
) {
    val bgColor by animateColorAsState(
        if (!notification.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.surface,
        label = "notif_bg"
    )

    val (icon, iconColor) = when (notification.type) {
        NotificationType.ASSIGNMENT -> Pair(Icons.Default.Check, Warning)
        NotificationType.NOTES -> Pair(Icons.Default.Article, Success)
        NotificationType.ANNOUNCEMENT -> Pair(Icons.Default.Campaign, Primary)
        NotificationType.REMINDER -> Pair(Icons.Default.Alarm, MaterialTheme.colorScheme.error)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable { if (!notification.isRead) onRead() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    notification.title,
                    fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (!notification.isRead) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Primary)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                notification.message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                formatTimestamp(notification.createdAt),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun NotificationSettingsPanel(
    preferences: NotificationPreferences,
    isLoading: Boolean,
    onPreferencesChange: (NotificationPreferences) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Notification Preferences",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Primary)
                }
            }
            Spacer(Modifier.height(8.dp))

            PreferenceToggleRow("Assignments", Icons.Default.Check, preferences.assignmentsEnabled) {
                onPreferencesChange(preferences.copy(assignmentsEnabled = it))
            }
            PreferenceToggleRow("Notes & Resources", Icons.Default.Article, preferences.notesEnabled) {
                onPreferencesChange(preferences.copy(notesEnabled = it))
            }
            PreferenceToggleRow("Announcements", Icons.Default.Campaign, preferences.announcementsEnabled) {
                onPreferencesChange(preferences.copy(announcementsEnabled = it))
            }
            PreferenceToggleRow("Reminders", Icons.Default.Alarm, preferences.remindersEnabled) {
                onPreferencesChange(preferences.copy(remindersEnabled = it))
            }
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = Primary)
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = Primary, checkedTrackColor = Primary.copy(alpha = 0.4f))
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> {
            val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
