package com.acadex.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.acadex.app.domain.model.Assignment
import com.acadex.app.domain.model.AssignmentPriority
import com.acadex.app.domain.model.User
import com.acadex.app.presentation.components.GlassyCard
import com.acadex.app.presentation.components.PremiumCard
import com.acadex.app.presentation.theme.Accent
import com.acadex.app.presentation.theme.Primary
import com.acadex.app.presentation.theme.Secondary
import com.acadex.app.presentation.theme.Success
import com.acadex.app.utils.DateTimeUtils
import java.util.Date
import java.util.Locale

@Composable
fun WelcomeHeader(user: User?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Welcome back,",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Text(
                text = user?.name ?: "Student",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            if (user != null && user.department.isNotEmpty()) {
                Text(
                    text = "${user.department} | ${user.semester}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        // Profile Picture with Coil
        if (user?.profilePicUrl?.isNotEmpty() == true) {
            AsyncImage(
                model = user.profilePicUrl,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
            )
        } else {
            // Default Initial Circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(Primary, Secondary)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (user?.name ?: "S").take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
fun StatsWidget(stats: AcademicStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatItemCard(
            title = "Completed",
            value = stats.completedAssignmentsCount.toString(),
            subtitle = "Assignments",
            modifier = Modifier.weight(1f),
            color = Success
        )
        StatItemCard(
            title = "Notes",
            value = stats.totalNotesCount.toString(),
            subtitle = "Saved",
            modifier = Modifier.weight(1f),
            color = Primary
        )
    }
}

@Composable
private fun StatItemCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    color: Color
) {
    PremiumCard(
        modifier = modifier
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
    }
}

@Composable
fun QuickActionsWidget(
    onAddNote: () -> Unit,
    onAddAssignment: () -> Unit,
    onAddTask: () -> Unit
) {
    Column {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionButton(
                label = "New Note",
                icon = Icons.Default.Edit,
                color = Primary,
                onClick = onAddNote,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                label = "Add Assgn",
                icon = Icons.Default.Add,
                color = Success,
                onClick = onAddAssignment,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                label = "Plan Task",
                icon = Icons.Default.DateRange,
                color = Secondary,
                onClick = onAddTask,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PremiumCard(
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun QuoteWidget(quoteText: String, author: String) {
    GlassyCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                text = "\"$quoteText\"",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "- $author",
                style = MaterialTheme.typography.labelSmall,
                color = Accent,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun PendingAssignmentsWidget(
    assignments: List<Assignment>,
    onAssignmentClick: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pending Assignments",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${assignments.size} total",
                style = MaterialTheme.typography.bodySmall,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (assignments.isEmpty()) {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No pending assignments. Great job!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                assignments.take(3).forEach { assignment ->
                    AssignmentRow(assignment = assignment, onClick = { onAssignmentClick(assignment.id) })
                }
            }
        }
    }
}

@Composable
private fun AssignmentRow(assignment: Assignment, onClick: () -> Unit) {
    val dueDateStr = DateTimeUtils.formatDate(assignment.dueDate)

    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = assignment.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = assignment.subject,
                    style = MaterialTheme.typography.bodySmall,
                    color = Primary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Due: $dueDateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            
            // Priority Tag
            val priorityColor = when (assignment.priority) {
                AssignmentPriority.HIGH -> Color(0xFFEF4444)
                AssignmentPriority.MEDIUM -> Color(0xFFF59E0B)
                AssignmentPriority.LOW -> Color(0xFF10B981)
            }
            Box(
                modifier = Modifier
                    .background(priorityColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = assignment.priority.name,
                    color = priorityColor,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

enum class DeadlineType {
    ASSIGNMENT, TASK
}

data class DeadlineItem(
    val id: String,
    val title: String,
    val type: DeadlineType,
    val dueDate: Long,
    val daysRemaining: Int
)

@Composable
fun UpcomingDeadlinesWidget(
    deadlines: List<DeadlineItem>,
    onItemClick: (DeadlineItem) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Upcoming Deadlines",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${deadlines.size} total",
                style = MaterialTheme.typography.bodySmall,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (deadlines.isEmpty()) {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "No upcoming deadlines. Clean slate!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                deadlines.take(5).forEach { item ->
                    DeadlineRow(item = item, onClick = { onItemClick(item) })
                }
            }
        }
    }
}

@Composable
private fun DeadlineRow(item: DeadlineItem, onClick: () -> Unit) {
    val dateStr = DateTimeUtils.formatDate(item.dueDate)
    val typeColor = when (item.type) {
        DeadlineType.ASSIGNMENT -> Primary
        DeadlineType.TASK -> Secondary
    }
    
    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(typeColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.type.name,
                            color = typeColor,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Due: $dateStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
            
            val countText = when {
                item.daysRemaining > 1 -> "${item.daysRemaining} days left"
                item.daysRemaining == 1 -> "Tomorrow"
                item.daysRemaining == 0 -> "Today"
                item.daysRemaining < 0 -> "${-item.daysRemaining} days overdue"
                else -> "Today"
            }
            val countBg = if (item.daysRemaining <= 2) Color(0xFFEF4444).copy(alpha = 0.1f) else typeColor.copy(alpha = 0.1f)
            val countColor = if (item.daysRemaining <= 2) Color(0xFFEF4444) else typeColor

            Box(
                modifier = Modifier
                    .background(countBg, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = countText,
                    color = countColor,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
