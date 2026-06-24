package com.acadex.app.presentation.resources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acadex.app.domain.model.Resource
import com.acadex.app.domain.model.User
import com.acadex.app.domain.repository.AuthRepository
import com.acadex.app.domain.usecase.ResourceUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResourcesViewModel @Inject constructor(
    authRepository: AuthRepository,
    private val resourceUseCases: ResourceUseCases
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory = _selectedCategory.asStateFlow()

    private val currentUser = authRepository.currentUser

    // Dynamically query resources matching current student profile
    val resourcesState: StateFlow<List<Resource>> = combine(
        currentUser,
        _selectedCategory,
        _searchQuery
    ) { user, category, query ->
        Triple(user, category, query)
    }.flatMapLatest { (user, category, query) ->
        val dept = user?.department?.ifEmpty { "CSE" } ?: "CSE"
        val sem = user?.semester?.ifEmpty { "Semester 5" } ?: "Semester 5"
        
        resourceUseCases.getResources(dept, sem, category)
    }.combine(_searchQuery) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            val lowerQuery = query.lowercase()
            list.filter {
                it.title.lowercase().contains(lowerQuery) ||
                it.subject.lowercase().contains(lowerQuery) ||
                it.fileName.lowercase().contains(lowerQuery)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun downloadResource(resourceId: String, onDownloadComplete: () -> Unit) {
        viewModelScope.launch {
            resourceUseCases.incrementDownloadCount(resourceId)
            // Simulating download trigger
            onDownloadComplete()
        }
    }

    // Support uploading study material
    fun uploadResource(
        title: String,
        category: String,
        subject: String,
        fileName: String,
        fileUrl: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val user = currentUser.value
            val dept = user?.department?.ifEmpty { "CSE" } ?: "CSE"
            val sem = user?.semester?.ifEmpty { "Semester 5" } ?: "Semester 5"
            
            val newResource = Resource(
                title = title,
                category = category,
                department = dept,
                semester = sem,
                subject = subject,
                fileUrl = fileUrl,
                fileName = fileName
            )
            
            resourceUseCases.createResource(newResource)
                .onSuccess { onSuccess() }
        }
    }
}
