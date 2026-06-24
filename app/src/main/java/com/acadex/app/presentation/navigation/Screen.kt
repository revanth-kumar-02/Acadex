package com.acadex.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    // Auth Routes
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")

    // Bottom Navigation Routes
    object Home : Screen("home")
    object Notes : Screen("notes")
    object Planner : Screen("planner")
    object Resources : Screen("resources")
    object Profile : Screen("profile")

    // Detail / Action Routes
    object NoteDetail : Screen("note_detail/{noteId}") {
        fun createRoute(noteId: String) = "note_detail/$noteId"
    }
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
    object AddEditExam : Screen("add_edit_exam?examId={examId}") {
        fun createRoute(examId: String?) = if (examId != null) "add_edit_exam?examId=$examId" else "add_edit_exam"
    }
}

data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("Home", Screen.Home.route, Icons.Default.Home),
    BottomNavItem("Notes", Screen.Notes.route, Icons.Default.List),
    BottomNavItem("Planner", Screen.Planner.route, Icons.Default.DateRange),
    BottomNavItem("Resources", Screen.Resources.route, Icons.Default.Info),
    BottomNavItem("Profile", Screen.Profile.route, Icons.Default.Person)
)
