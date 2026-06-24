package com.acadex.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.acadex.app.presentation.components.LoadingState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAddNote: () -> Unit,
    onNavigateToAddAssignment: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToAddExam: () -> Unit,
    onNavigateToAssignmentDetail: (String) -> Unit,
    onNavigateToExamDetail: (String) -> Unit
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val quote by viewModel.quote.collectAsState()
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
                onAddAssignment = onNavigateToAddAssignment,
                onAddTask = { onNavigateToPlanner() },
                onAddExam = onNavigateToAddExam
            )
            Spacer(modifier = Modifier.height(28.dp))

            // Unified Upcoming Deadlines Widget
            UpcomingDeadlinesWidget(
                deadlines = dashboardState.upcomingDeadlines,
                onItemClick = { item ->
                    when (item.type) {
                        DeadlineType.ASSIGNMENT -> onNavigateToAssignmentDetail(item.id)
                        DeadlineType.TASK -> onNavigateToPlanner()
                        DeadlineType.EXAM -> onNavigateToExamDetail(item.id)
                    }
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
