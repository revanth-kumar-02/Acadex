package com.acadex.app.core.navigation

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.acadex.app.features.authentication.*
import com.acadex.app.features.home.*
import com.acadex.app.features.notes.*
import com.acadex.app.features.assignments.*
import com.acadex.app.features.planner.*
import com.acadex.app.features.profile.*
import com.acadex.app.ui.theme.Primary

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Notes.route,
        Screen.Assignments.route,
        Screen.Planner.route,
        Screen.Profile.route
    )

    val startDestination = Screen.Splash.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Primary,
                                selectedTextColor = Primary,
                                indicatorColor = Primary.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    viewModel = authViewModel,
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onNavigateToForgot = { navController.navigate(Screen.ForgotPassword.route) },
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    viewModel = authViewModel,
                    onNavigateToLogin = {
                        navController.popBackStack()
                    },
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(
                    viewModel = authViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Bottom Nav Screen Graphs
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToAddAssignment = { navController.navigate(Screen.AddEditAssignment.createRoute(null)) },
                    onNavigateToUploadNotes = { navController.navigate(Screen.AddEditNote.createRoute(null)) },
                    onNavigateToPostAnnouncement = { navController.navigate(Screen.AddAnnouncement.route) },
                    onNavigateToAssignmentDetail = { id -> navController.navigate(Screen.AddEditAssignment.createRoute(id)) }
                )
            }

            composable(Screen.Notes.route) {
                val notesViewModel: NotesViewModel = hiltViewModel()
                NotesScreen(
                    viewModel = notesViewModel,
                    onNavigateToAddNote = { navController.navigate(Screen.AddEditNote.createRoute(null)) },
                    onNavigateToNoteDetail = {}
                )
            }

            composable(Screen.Planner.route) {
                val plannerViewModel: PlannerViewModel = hiltViewModel()
                PlannerScreen(
                    viewModel = plannerViewModel,
                    onNavigateToAddTask = { date -> navController.navigate(Screen.AddEditTask.createRoute(null, date)) },
                    onNavigateToEditTask = { id -> navController.navigate(Screen.AddEditTask.createRoute(id, null)) }
                )
            }

            composable(Screen.Assignments.route) {
                val assignmentsViewModel: AssignmentsViewModel = hiltViewModel()
                AssignmentsScreen(
                    viewModel = assignmentsViewModel,
                    onNavigateToAddAssignment = { navController.navigate(Screen.AddEditAssignment.createRoute(null)) },
                    onNavigateToAssignmentDetail = { id -> navController.navigate(Screen.AddEditAssignment.createRoute(id)) }
                )
            }

            composable(Screen.Profile.route) {
                val profileViewModel: ProfileViewModel = hiltViewModel()
                ProfileScreen(viewModel = profileViewModel)
            }

            // Detail / Action Screens
            composable(Screen.AddAnnouncement.route) {
                val homeViewModel: HomeViewModel = hiltViewModel()
                AddAnnouncementScreen(
                    viewModel = homeViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.AddEditNote.route,
                arguments = listOf(navArgument("noteId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId")
                val notesViewModel: NotesViewModel = hiltViewModel()
                AddEditNoteScreen(
                    noteId = noteId,
                    viewModel = notesViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.AddEditAssignment.route,
                arguments = listOf(navArgument("assignmentId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val assignmentId = backStackEntry.arguments?.getString("assignmentId")
                val assignmentsViewModel: AssignmentsViewModel = hiltViewModel()
                AddEditAssignmentScreen(
                    assignmentId = assignmentId,
                    viewModel = assignmentsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.AddEditTask.route,
                arguments = listOf(
                    navArgument("taskId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("date") {
                        type = NavType.LongType
                        defaultValue = 0L
                    }
                )
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId")
                val date = backStackEntry.arguments?.getLong("date") ?: System.currentTimeMillis()
                val plannerViewModel: PlannerViewModel = hiltViewModel()
                AddEditTaskScreen(
                    taskId = taskId,
                    date = date,
                    viewModel = plannerViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
