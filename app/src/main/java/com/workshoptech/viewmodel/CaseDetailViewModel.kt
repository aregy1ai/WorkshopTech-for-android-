package com.workshoptech.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workshoptech.data.entity.CaseEntity
import com.workshoptech.data.entity.CasePhotoEntity
import com.workshoptech.data.entity.InspectionEntity
import com.workshoptech.data.repository.WorkshopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CaseDetailState(val case: CaseEntity? = null, val photos: List<CasePhotoEntity> = emptyList(), val inspections: List<InspectionEntity> = emptyList(), val isLoading: Boolean = true, val error: String? = null)

class CaseDetailViewModel(private val repository: WorkshopRepository) : ViewModel() {
    private val _state = MutableStateFlow(CaseDetailState())
    val state: StateFlow<CaseDetailState> = _state

    fun loadCase(caseId: String) {
        viewModelScope.launch { _state.value = _state.value.copy(isLoading = true); repository.observeCase(caseId).collect { _state.value = _state.value.copy(case = it, isLoading = false) } }
        viewModelScope.launch { repository.observePhotos(caseId).collect { _state.value = _state.value.copy(photos = it) } }
        viewModelScope.launch { repository.observeInspections(caseId).collect { _state.value = _state.value.copy(inspections = it) } }
    }
    fun updateStatus(caseId: String, status: String) { viewModelScope.launch { repository.updateCaseStatus(caseId, status) } }
}
