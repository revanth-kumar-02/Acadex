package com.acadex.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acadex.app.presentation.components.EmptyState
import com.acadex.app.presentation.components.LoadingState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAddAssignment: () -> Unit,
    onNavigateToUploadNotes: () -> Unit,
    onNavigateToPostAnnouncement: () -> Unit,
    onNavigateToAssignmentDetail: (String) -> Unit
) {
    val state by viewModel.dashboardState.collectAsState()

    if (state.isLoading) {
        LoadingState(message = "Loading department feed...")
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                WelcomeHeader(user = state.user)
            }
            
            item {
                QuickActionsWidget(
                    onAddAssignment = onNavigateToAddAssignment,
                    onUploadNotes = onNavigateToUploadNotes,
                    onPostAnnouncement = onNavigateToPostAnnouncement
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Academic Feed",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (state.feedItems.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Info,
                        title = "Feed is empty",
                        description = "No notes, assignments, or announcements have been broadcasted yet. Be the first to share one!"
                    )
                }
            } else {
                items(state.feedItems, key = { item ->
                    when (item) {
                        is FeedItem.AssignmentItem -> "assignment_${item.id}"
                        is FeedItem.NotesItem -> "note_${item.id}"
                        is FeedItem.AnnouncementItem -> "announcement_${item.id}"
                    }
                }) { feedItem ->
                    FeedItemCard(
                        item = feedItem,
                        onAssignmentClick = onNavigateToAssignmentDetail,
                        onAssignmentStatusToggle = { id, status ->
                            viewModel.updateAssignmentStatus(id, status)
                        }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
