package com.acadex.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acadex.app.presentation.theme.Primary
import com.acadex.app.presentation.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAnnouncementScreen(
    viewModel: HomeViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.dashboardState.collectAsState()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var broadcastTarget by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    var broadcastDropdownExpanded by remember { mutableStateOf(false) }

    val userDept = state.user?.department?.ifEmpty { "CSE" } ?: "CSE"
    val broadcastOptions = remember(userDept) {
        listOf(
            "$userDept Year 1",
            "$userDept Year 2",
            "$userDept Year 3",
            "$userDept Year 4",
            "Entire $userDept Department"
        )
    }

    LaunchedEffect(state.user) {
        if (broadcastTarget.isEmpty() && state.user != null) {
            broadcastTarget = "$userDept Year 2"
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
                .padding(20.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Post Announcement",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )

                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    IconButton(
                        onClick = {
                            if (title.isBlank() || content.isBlank() || broadcastTarget.isBlank()) return@IconButton
                            isSaving = true
                            viewModel.createAnnouncement(
                                title = title,
                                content = content,
                                broadcastTarget = broadcastTarget,
                                onSuccess = {
                                    isSaving = false
                                    onNavigateBack()
                                }
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            tint = Success
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Announcement Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Success),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Announcement Details / Message") },
                    minLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Success),
                    modifier = Modifier.fillMaxWidth()
                )

                // Broadcast Target Selector
                ExposedDropdownMenuBox(
                    expanded = broadcastDropdownExpanded,
                    onExpandedChange = { broadcastDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = broadcastTarget,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Broadcast Target") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = broadcastDropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Success),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = broadcastDropdownExpanded,
                        onDismissRequest = { broadcastDropdownExpanded = false }
                    ) {
                        broadcastOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    broadcastTarget = opt
                                    broadcastDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}
