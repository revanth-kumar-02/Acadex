package com.acadex.app.features.assignments

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acadex.app.data.models.Assignment
import com.acadex.app.data.models.AssignmentPriority
import com.acadex.app.data.models.AssignmentStatus
import com.acadex.app.ui.components.EmptyState
import com.acadex.app.ui.components.PremiumCard
import com.acadex.app.ui.theme.Primary
import com.acadex.app.ui.theme.Secondary
import com.acadex.app.ui.theme.Success
import com.acadex.app.core.utils.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentsScreen(
    viewModel: AssignmentsViewModel,
    onNavigateToAddAssignment: () -> Unit,
    onNavigateToAssignmentDetail: (String) -> Unit
) {
    val assignments by viewModel.assignmentsState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<AssignmentStatus?>(null) }

    // Filter assignments dynamically by search query, status, and department target
    val filteredAssignments = remember(assignments, searchQuery, selectedStatusFilter, currentUser) {
        if (currentUser == null) return@remember emptyList<Assignment>()
        val userDept = currentUser!!.department.ifEmpty { "CSE" }
        val userSem = currentUser!!.semester.ifEmpty { "Semester 3" }
        val userId = currentUser!!.uid

        assignments.filter { assignment ->
            // Broadcast target filter
            val isMatched = assignment.userId == userId || 
                            isTargetMatched(userDept, userSem, assignment.broadcastTarget)

            val matchesQuery = assignment.title.contains(searchQuery, ignoreCase = true) ||
                               assignment.subject.contains(searchQuery, ignoreCase = true)
            val matchesStatus = selectedStatusFilter == null || assignment.status == selectedStatusFilter
            
            isMatched && matchesQuery && matchesStatus
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Assignments",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Box(
                    modifier = Modifier
                        .background(Primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${filteredAssignments.size} items",
                        color = Primary,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search assignments...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Status filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStatusFilter == null,
                    onClick = { selectedStatusFilter = null },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary.copy(alpha = 0.1f),
                        selectedLabelColor = Primary
                    )
                )
                FilterChip(
                    selected = selectedStatusFilter == AssignmentStatus.PENDING,
                    onClick = { selectedStatusFilter = AssignmentStatus.PENDING },
                    label = { Text("Pending") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary.copy(alpha = 0.1f),
                        selectedLabelColor = Primary
                    )
                )
                FilterChip(
                    selected = selectedStatusFilter == AssignmentStatus.IN_PROGRESS,
                    onClick = { selectedStatusFilter = AssignmentStatus.IN_PROGRESS },
                    label = { Text("In Progress") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary.copy(alpha = 0.1f),
                        selectedLabelColor = Primary
                    )
                )
                FilterChip(
                    selected = selectedStatusFilter == AssignmentStatus.COMPLETED,
                    onClick = { selectedStatusFilter = AssignmentStatus.COMPLETED },
                    label = { Text("Completed") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary.copy(alpha = 0.1f),
                        selectedLabelColor = Primary
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (filteredAssignments.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Info,
                    title = "No assignments found",
                    description = if (searchQuery.isNotEmpty() || selectedStatusFilter != null) {
                        "Try adjusting your filters or search query"
                    } else {
                        "Broadcast a new assignment to keep your department classmates updated!"
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredAssignments, key = { it.id }) { assignment ->
                        AssignmentListItemCard(
                            assignment = assignment,
                            currentUserId = currentUser?.uid ?: "",
                            onClick = { onNavigateToAssignmentDetail(assignment.id) },
                            onDeleteClick = { viewModel.deleteAssignment(assignment.id) },
                            onToggleStatusClick = {
                                val nextStatus = when (assignment.status) {
                                    AssignmentStatus.PENDING -> AssignmentStatus.IN_PROGRESS
                                    AssignmentStatus.IN_PROGRESS -> AssignmentStatus.COMPLETED
                                    AssignmentStatus.COMPLETED -> AssignmentStatus.PENDING
                                }
                                viewModel.updateAssignment(assignment.copy(status = nextStatus), onSuccess = {})
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onNavigateToAddAssignment,
            containerColor = Primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Assignment")
        }
    }
}

@Composable
private fun AssignmentListItemCard(
    assignment: Assignment,
    currentUserId: String,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleStatusClick: () -> Unit
) {
    val context = LocalContext.current
    val priorityColor = when (assignment.priority) {
        AssignmentPriority.HIGH -> Color(0xFFEF4444)
        AssignmentPriority.MEDIUM -> Color(0xFFF59E0B)
        AssignmentPriority.LOW -> Color(0xFF10B981)
    }

    val statusColor = when (assignment.status) {
        AssignmentStatus.COMPLETED -> Success
        AssignmentStatus.IN_PROGRESS -> Secondary
        AssignmentStatus.PENDING -> Color.Gray
    }

    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = assignment.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = assignment.subject,
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Delete Button for owner
                if (assignment.userId == currentUserId) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (assignment.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = assignment.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (assignment.attachmentUrl?.isNotEmpty() == true) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .clickable {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(assignment.attachmentUrl))
                                context.startActivity(intent)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(text = "📎", fontSize = 12.sp, modifier = Modifier.padding(end = 6.dp))
                    Text(
                        text = assignment.attachmentUrl.substringAfterLast("/"),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = DateTimeUtils.formatDeadline(assignment.dueDate),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Primary
                    )
                }

                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .clickable { onToggleStatusClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (assignment.status) {
                            AssignmentStatus.COMPLETED -> "Completed"
                            AssignmentStatus.IN_PROGRESS -> "In Progress"
                            AssignmentStatus.PENDING -> "Pending"
                        },
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

private fun isTargetMatched(userDept: String, userSem: String, target: String): Boolean {
    if (target.isBlank()) return true
    val cleanedTarget = target.trim()
    
    val targetDept = when {
        cleanedTarget.startsWith("Entire ", ignoreCase = true) && cleanedTarget.endsWith(" Department", ignoreCase = true) -> {
            cleanedTarget.substring(7, cleanedTarget.length - 11).trim()
        }
        cleanedTarget.contains(" Year ", ignoreCase = true) -> {
            val yearIdx = cleanedTarget.indexOf(" Year ", ignoreCase = true)
            if (yearIdx != -1) cleanedTarget.substring(0, yearIdx).trim() else cleanedTarget
        }
        else -> cleanedTarget
    }

    if (!userDept.equals(targetDept, ignoreCase = true) && !cleanedTarget.contains(userDept, ignoreCase = true)) {
        return false
    }

    val yearIndex = cleanedTarget.indexOf(" Year ", ignoreCase = true)
    if (yearIndex != -1) {
        val yearStr = cleanedTarget.substring(yearIndex + 6).trim()
        val targetYear = yearStr.toIntOrNull() ?: return true
        
        val userYear = when {
            userSem.contains("Year 1", ignoreCase = true) || userSem.contains("1st Year", ignoreCase = true) || userSem.contains("Semester 1", ignoreCase = true) || userSem.contains("Semester 2", ignoreCase = true) -> 1
            userSem.contains("Year 2", ignoreCase = true) || userSem.contains("2nd Year", ignoreCase = true) || userSem.contains("Semester 3", ignoreCase = true) || userSem.contains("Semester 4", ignoreCase = true) -> 2
            userSem.contains("Year 3", ignoreCase = true) || userSem.contains("3rd Year", ignoreCase = true) || userSem.contains("Semester 5", ignoreCase = true) || userSem.contains("Semester 6", ignoreCase = true) -> 3
            userSem.contains("Year 4", ignoreCase = true) || userSem.contains("4th Year", ignoreCase = true) || userSem.contains("Semester 7", ignoreCase = true) || userSem.contains("Semester 8", ignoreCase = true) -> 4
            else -> {
                val digit = userSem.firstOrNull { it.isDigit() }?.digitToIntOrNull()
                if (digit != null) {
                    if (userSem.contains("sem", ignoreCase = true)) {
                        (digit + 1) / 2
                    } else {
                        digit
                    }
                } else 1
            }
        }
        return userYear == targetYear
    }
    return true
}
