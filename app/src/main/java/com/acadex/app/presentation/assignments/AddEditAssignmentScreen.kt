package com.acadex.app.presentation.assignments

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acadex.app.domain.model.Assignment
import com.acadex.app.domain.model.AssignmentPriority
import com.acadex.app.domain.model.AssignmentStatus
import com.acadex.app.presentation.components.LoadingState
import com.acadex.app.presentation.theme.Primary
import com.acadex.app.presentation.theme.Secondary
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAssignmentScreen(
    assignmentId: String?,
    viewModel: AssignmentsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(AssignmentPriority.MEDIUM) }
    var status by remember { mutableStateOf(AssignmentStatus.PENDING) }
    var dueDate by remember { mutableStateOf(System.currentTimeMillis() + 86400000 * 3) }
    var broadcastTarget by remember { mutableStateOf("") }

    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var attachmentName by remember { mutableStateOf<String?>(null) }
    var existingAttachmentUrl by remember { mutableStateOf<String?>(null) }

    var isEditMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    
    var priorityDropdownExpanded by remember { mutableStateOf(false) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }
    var broadcastDropdownExpanded by remember { mutableStateOf(false) }

    val userDept = currentUser?.department?.ifEmpty { "CSE" } ?: "CSE"
    val broadcastOptions = remember(userDept) {
        listOf(
            "$userDept Year 1",
            "$userDept Year 2",
            "$userDept Year 3",
            "$userDept Year 4",
            "Entire $userDept Department"
        )
    }

    LaunchedEffect(assignmentId, currentUser) {
        if (broadcastTarget.isEmpty() && currentUser != null) {
            broadcastTarget = "$userDept Year 2" // Default Year 2 target
        }
        
        if (assignmentId != null) {
            isEditMode = true
            val assignment = viewModel.getAssignmentById(assignmentId)
            assignment?.let {
                title = it.title
                description = it.description
                subject = it.subject
                priority = it.priority
                status = it.status
                dueDate = it.dueDate
                broadcastTarget = it.broadcastTarget
                existingAttachmentUrl = it.attachmentUrl
                if (it.attachmentUrl?.isNotEmpty() == true) {
                    attachmentName = it.attachmentUrl.substringAfterLast("/")
                }
            }
        }
        isLoading = false
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        attachmentUri = uri
        uri?.let {
            attachmentName = it.lastPathSegment ?: "document.pdf"
        }
    }

    if (isLoading) {
        LoadingState(message = "Loading assignment editor...")
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
                    text = if (isEditMode) "Edit Assignment" else "Create Assignment",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    IconButton(
                        onClick = {
                            if (title.isBlank() || subject.isBlank() || broadcastTarget.isBlank()) return@IconButton
                            isSaving = true

                            val stream: InputStream? = attachmentUri?.let { uri ->
                                context.contentResolver.openInputStream(uri)
                            }

                            val assignment = Assignment(
                                id = assignmentId ?: "",
                                title = title,
                                description = description,
                                subject = subject,
                                dueDate = dueDate,
                                priority = priority,
                                status = status,
                                broadcastTarget = broadcastTarget,
                                postedBy = currentUser?.name ?: "Student",
                                attachmentUrl = existingAttachmentUrl,
                                assignedDate = System.currentTimeMillis()
                            )

                            if (isEditMode) {
                                viewModel.updateAssignment(
                                    assignment = assignment,
                                    attachmentStream = stream,
                                    attachmentName = attachmentName,
                                    onSuccess = {
                                        isSaving = false
                                        onNavigateBack()
                                    },
                                    onFailure = { error ->
                                        isSaving = false
                                        android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
                                    }
                                )
                            } else {
                                viewModel.createAssignment(
                                    assignment = assignment,
                                    attachmentStream = stream,
                                    attachmentName = attachmentName,
                                    onSuccess = {
                                        isSaving = false
                                        onNavigateBack()
                                    },
                                    onFailure = { error ->
                                        isSaving = false
                                        android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            tint = Primary
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
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject (e.g. Data Structures)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Assignment Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Instructions / Submission Details") },
                    minLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary),
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
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary),
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

                Row(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = priorityDropdownExpanded,
                        onExpandedChange = { priorityDropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = priority.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Priority") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = priorityDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = priorityDropdownExpanded,
                            onDismissRequest = { priorityDropdownExpanded = false }
                        ) {
                            AssignmentPriority.values().forEach { priorityVal ->
                                DropdownMenuItem(
                                    text = { Text(priorityVal.name) },
                                    onClick = {
                                        priority = priorityVal
                                        priorityDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = statusDropdownExpanded,
                        onExpandedChange = { statusDropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = status.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Status") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = statusDropdownExpanded,
                            onDismissRequest = { statusDropdownExpanded = false }
                        ) {
                            AssignmentStatus.values().forEach { statusVal ->
                                DropdownMenuItem(
                                    text = { Text(statusVal.name) },
                                    onClick = {
                                        status = statusVal
                                        statusDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // File Attachment picker section
                Button(
                    onClick = { fileLauncher.launch("*/*") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Secondary.copy(alpha = 0.12f),
                        contentColor = Secondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (attachmentName != null) "📎 $attachmentName" else "📎 Add Optional Attachment File",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
