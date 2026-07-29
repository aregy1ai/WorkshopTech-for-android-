package com.workshoptech.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workshoptech.data.entity.CaseEntity
import com.workshoptech.data.repository.WorkshopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class CreateCaseState(val country: String = "LY", val plateText: String = "", val makeId: String? = null, val modelId: String? = null, val year: Int? = null, val freeText: String? = null, val colorName: String? = null, val isSubmitting: Boolean = false, val error: String? = null)

class CreateCaseViewModel(private val repository: WorkshopRepository) : ViewModel() {
    private val _state = MutableStateFlow(CreateCaseState())
    val state: StateFlow<CreateCaseState> = _state
    fun setCountry(country: String) { _state.value = _state.value.copy(country = country) }
    fun setPlateText(text: String) { _state.value = _state.value.copy(plateText = text) }
    fun setMake(make: String) { _state.value = _state.value.copy(makeId = make.ifBlank { null }) }
    fun setModel(model: String) { _state.value = _state.value.copy(modelId = model.ifBlank { null }) }
    fun setYear(year: String) { _state.value = _state.value.copy(year = year.toIntOrNull()) }
    fun setFreeText(text: String) { _state.value = _state.value.copy(freeText = text.ifBlank { null }) }
    fun setColor(color: String) { _state.value = _state.value.copy(colorName = color.ifBlank { null }) }

    fun submit(onSuccess: (String) -> Unit) {
        val s = _state.value
        if (s.plateText.isBlank()) { _state.value = s.copy(error = "رقم اللوحة مطلوب"); return }
        viewModelScope.launch {
            _state.value = s.copy(isSubmitting = true, error = null)
            try {
                val caseId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                repository.upsertCase(CaseEntity(caseId = caseId, plateCountry = s.country, plateText = s.plateText, plateTextRaw = s.plateText, customerId = null, vehicleMakeId = s.makeId, vehicleModelId = s.modelId, vehicleYear = s.year, vehicleFreeText = s.freeText, colorCode = null, colorName = s.colorName, status = "NEW", createdAt = now, updatedAt = now, estimatedCost = null, actualCost = null, estimatedHours = null, actualHours = null, technicianId = null))
                _state.value = _state.value.copy(isSubmitting = false)
                onSuccess(caseId)
            } catch (e: Exception) { _state.value = _state.value.copy(isSubmitting = false, error = e.message) }
        }
    }
}
