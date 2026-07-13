package com.workshoptech.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workshoptech.data.entity.InspectionEntity
import com.workshoptech.data.repository.WorkshopRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class InspectionState(
    val inspections: List<InspectionEntity> = emptyList(),
    val currentInspection: InspectionEntity? = null,
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val savedSuccess: Boolean = false,
    val error: String? = null
)

/**
 * Inspection checkpoint types matching T1–T6.
 */
enum class InspectionType(val label: String) {
    T1("T1: استلام السيارة"),
    T2("T2: بعد السمكرة"),
    T3("T3: تسليم للدهان"),
    T4("T4: قبل الرش"),
    T5("T5: بعد الدهان"),
    T6("T6: التسليم النهائي")
}

class InspectionViewModel(
    private val repository: WorkshopRepository
) : ViewModel() {

    private val _state = MutableStateFlow(InspectionState())
    val state: StateFlow<InspectionState> = _state.asStateFlow()

    fun loadForCase(caseId: String) {
        viewModelScope.launch {
            repository.observeInspections(caseId).collect { list ->
                _state.value = _state.value.copy(inspections = list, isLoading = false)
            }
        }
    }

    fun openInspection(caseId: String, type: InspectionType) {
        viewModelScope.launch {
            val existing = repository.getLatestInspection(caseId, type.name)
            _state.value = _state.value.copy(
                currentInspection = existing ?: InspectionEntity(
                    inspectionId = UUID.randomUUID().toString(),
                    caseId       = caseId,
                    type         = type.name,
                    status       = "PENDING",
                    inspectorId  = null,
                    checklist    = null,
                    aiAnalysis   = null,
                    notes        = null,
                    images       = null,
                    timestamp    = System.currentTimeMillis()
                )
            )
        }
    }

    fun saveInspection(
        inspectionId: String,
        caseId: String,
        type: String,
        status: String,
        inspectorId: String?,
        checklist: String?,
        notes: String?,
        images: String?
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            val result = repository.upsertInspection(
                InspectionEntity(
                    inspectionId = inspectionId,
                    caseId       = caseId,
                    type         = type,
                    status       = status,
                    inspectorId  = inspectorId,
                    checklist    = checklist,
                    aiAnalysis   = null,
                    notes        = notes,
                    images       = images,
                    timestamp    = System.currentTimeMillis()
                )
            )
            _state.value = _state.value.copy(
                isSaving     = false,
                savedSuccess = result.isSuccess,
                error        = result.exceptionOrNull()?.message
            )
        }
    }

    fun clearSuccess() {
        _state.value = _state.value.copy(savedSuccess = false)
    }
}
