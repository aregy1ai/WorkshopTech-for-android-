package com.workshoptech.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workshoptech.data.entity.InspectionEntity
import com.workshoptech.data.entity.InspectionStatus
import com.workshoptech.data.repository.WorkshopRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class InspectionState(
    val inspections: List<InspectionEntity>   = emptyList(),
    val currentInspection: InspectionEntity?  = null,
    val isSaving: Boolean                     = false,
    val isLoading: Boolean                    = true,
    val savedSuccess: Boolean                 = false,
    val error: String?                        = null
)

/**
 * Six-step quality checkpoint types (T1–T6).
 * Named InspectionCheckpoint to avoid clash with the entity's object InspectionType.
 */
enum class InspectionCheckpoint(val label: String) {
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
            repository.observeInspections(caseId)
                .catch { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.localizedMessage)
                }
                .collect { list ->
                    _state.value = _state.value.copy(inspections = list, isLoading = false)
                }
        }
    }

    fun openInspection(caseId: String, checkpoint: InspectionCheckpoint) {
        viewModelScope.launch {
            val existing = repository.getLatestInspection(caseId, checkpoint.name)
            _state.value = _state.value.copy(
                currentInspection = existing ?: InspectionEntity(
                    inspectionId = UUID.randomUUID().toString(),
                    caseId       = caseId,
                    type         = checkpoint.name,
                    status       = InspectionStatus.PENDING,
                    checklistJson = null,
                    defectsJson  = null,
                    inspectedBy  = null,
                    notes        = null,
                    signaturePath = null,
                    photoIds     = null,
                    deltaE       = null,
                    createdAt    = System.currentTimeMillis()
                )
            )
        }
    }

    fun saveInspection(
        inspectionId:  String,
        caseId:        String,
        type:          String,
        status:        String,
        inspectedBy:   String?,
        checklistJson: String?,
        notes:         String?,
        photoIds:      String?
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            val existing = _state.value.currentInspection
            val result = repository.upsertInspection(
                InspectionEntity(
                    inspectionId  = inspectionId,
                    caseId        = caseId,
                    type          = type,
                    status        = status,
                    checklistJson = checklistJson,
                    defectsJson   = existing?.defectsJson,
                    inspectedBy   = inspectedBy,
                    notes         = notes,
                    signaturePath = existing?.signaturePath,
                    photoIds      = photoIds,
                    deltaE        = existing?.deltaE,
                    createdAt     = existing?.createdAt ?: System.currentTimeMillis(),
                    completedAt   = if (status == InspectionStatus.PASSED || status == InspectionStatus.FAILED)
                        System.currentTimeMillis() else null
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
