package com.acadex.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acadex.app.presentation.components.LoadingState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAddNote: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToAddAssignment: () -> Unit,
    onNavigateToAddExam: () -> Unit,
    onNavigateToAssignmentDetail: (String) -> Unit,
    onNavigateToExamDetail: (String) -> Unit
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val quote by viewModel.quote.collectAsState()
    val announcements by viewModel.announcements.collectAsState()
    val scrollState = rememberScrollState()

    if (dashboardState.user == null) {
        LoadingState(message = "Loading academic dashboard...")
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            // Welcome Student
            WelcomeHeader(user = dashboardState.user)
            Spacer(modifier = Modifier.height(20.dp))

            // Stat Cards
            StatsWidget(stats = dashboardState.stats)
            Spacer(modifier = Modifier.height(24.dp))

            // Quote Widget
            if (quote != null) {
                QuoteWidget(quoteText = quote!!.quote, author = quote!!.author)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Quick Action Widgets
            QuickActionsWidget(
                onAddNote = onNavigateToAddNote,
                onAddTask = { onNavigateToPlanner() },
                onAddExam = onNavigateToAddExam
            )
            Spacer(modifier = Modifier.height(28.dp))

            // Pending Assignments Widget
            PendingAssignmentsWidget(
                assignments = dashboardState.pendingAssignments,
                onAssignmentClick = onNavigateToAssignmentDetail
            )
            Spacer(modifier = Modifier.height(28.dp))

            // Upcoming Exams Widget
            UpcomingExamsWidget(
                exams = dashboardState.upcomingExams,
                onExamClick = onNavigateToExamDetail
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
