package com.acadex.app.presentation.notes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.acadex.app.presentation.components.LoadingState
import com.acadex.app.presentation.theme.Primary
import com.acadex.app.presentation.theme.Secondary
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNoteScreen(
    noteId: String?,
    viewModel: NotesViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()

    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Notes") }
    var broadcastTarget by remember { mutableStateOf("") }

    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf<String?>(null) }
    var existingFileUrl by remember { mutableStateOf<String?>(null) }

    var isEditMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var broadcastDropdownExpanded by remember { mutableStateOf(false) }

    val categories = listOf("Notes", "PDF", "Document", "Study Resource")
    
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

    LaunchedEffect(noteId, currentUser) {
        if (broadcastTarget.isEmpty() && currentUser != null) {
            broadcastTarget = "$userDept Year 2"
        }

        if (noteId != null) {
            isEditMode = true
            val note = viewModel.getNoteById(noteId)
            note?.let {
                title = it.title
                subject = it.subject
                category = it.category
                broadcastTarget = it.broadcastTarget
                existingFileUrl = it.fileUrl
                fileName = it.fileName
            }
        }
        isLoading = false
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        fileUri = uri
        uri?.let {
            fileName = it.lastPathSegment ?: "document.pdf"
        }
    }

    if (isLoading) {
        LoadingState(message = "Loading notes editor...")
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
                    text = if (isEditMode) "Edit Shared Note" else "Upload Shared Note",
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

                            val stream: InputStream? = fileUri?.let { uri ->
                                context.contentResolver.openInputStream(uri)
                            }

                            viewModel.createNote(
                                title = title,
                                category = category,
                                subject = subject,
                                broadcastTarget = broadcastTarget,
                                fileStream = stream,
                                fileName = fileName,
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
                            tint = Secondary
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
                    label = { Text("Subject (e.g. OOPS)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Secondary),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notes Title (e.g. Unit 3 Notes)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Secondary),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Secondary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

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
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Secondary),
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

                // File Attachment picker button
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
                        text = if (fileName != null) "📄 $fileName" else "📄 Choose PDF or Notes File to Upload",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
