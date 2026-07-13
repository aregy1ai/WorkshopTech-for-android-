package com.workshoptech.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workshoptech.data.entity.*
import com.workshoptech.data.repository.WorkshopRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CaseDetailState(
    val case: CaseEntity?               = null,
    val customer: CustomerEntity?       = null,
    val photos: List<CasePhotoEntity>   = emptyList(),
    val inspections: List<InspectionEntity> = emptyList(),
    val tasks: List<WorkflowTaskEntity> = emptyList(),
    val videos: List<VideoEntity>       = emptyList(),
    val isLoading: Boolean              = true,
    val isUpdating: Boolean             = false,
    val error: String?                  = null
)

class CaseDetailViewModel(
    private val repository: WorkshopRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CaseDetailState())
    val state: StateFlow<CaseDetailState> = _state.asStateFlow()

    fun loadCase(caseId: String) {
        viewModelScope.launch {
            combine(
                repository.observeCase(caseId),
                repository.observePhotos(caseId),
                repository.observeInspections(caseId),
                repository.observeTasks(caseId),
                repository.observeVideos(caseId)
            ) { case, photos, inspections, tasks, videos ->
                CaseDetailState(
                    case        = case,
                    photos      = photos,
                    inspections = inspections,
                    tasks       = tasks,
                    videos      = videos,
                    isLoading   = false
                )
            }.catch { e ->
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }.collect { s ->
                val customer = s.case?.customerId?.let { repository.findCustomerById(it) }
                _state.value = s.copy(customer = customer)
            }
        }
    }

    fun updateStatus(caseId: String, newStatus: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUpdating = true)
            val result = repository.updateCaseStatus(caseId, newStatus)
            _state.value = _state.value.copy(
                isUpdating = false,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
