package com.acadex.app.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    // Auth Routes
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")

    // Bottom Navigation Routes
    object Home : Screen("home")
    object Notes : Screen("notes")
    object Assignments : Screen("assignments")
    object Planner : Screen("planner")
    object Notifications : Screen("notifications")
    object Profile : Screen("profile")

    // Detail / Action Routes
    object AddAnnouncement : Screen("add_announcement")
    object AddEditNote : Screen("add_edit_note?noteId={noteId}") {
        fun createRoute(noteId: String?) = if (noteId != null) "add_edit_note?noteId=$noteId" else "add_edit_note"
    }
    object AddEditAssignment : Screen("add_edit_assignment?assignmentId={assignmentId}") {
        fun createRoute(assignmentId: String?) = if (assignmentId != null) "add_edit_assignment?assignmentId=$assignmentId" else "add_edit_assignment"
    }
    object AddEditTask : Screen("add_edit_task?taskId={taskId}&date={date}") {
        fun createRoute(taskId: String?, date: Long?) = buildString {
            append("add_edit_task")
            var hasArgs = false
            if (taskId != null) {
                append("?taskId=$taskId")
                hasArgs = true
            }
            if (date != null) {
                append(if (hasArgs) "&" else "?")
                append("date=$date")
            }
        }
    }
}

data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Screen.Home.route, Icons.Default.Home),
    BottomNavItem("Notes", Screen.Notes.route, Icons.AutoMirrored.Filled.List),
    BottomNavItem("Assignments", Screen.Assignments.route, Icons.Default.Check),
    BottomNavItem("Planner", Screen.Planner.route, Icons.Default.DateRange),
    BottomNavItem("Alerts", Screen.Notifications.route, Icons.Default.Notifications),
    BottomNavItem("Profile", Screen.Profile.route, Icons.Default.Person)
)
