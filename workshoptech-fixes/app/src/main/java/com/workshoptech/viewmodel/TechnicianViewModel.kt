package com.workshoptech.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workshoptech.data.entity.TechnicianEntity
import com.workshoptech.data.repository.WorkshopRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TechnicianState(
    val technicians: List<TechnicianEntity> = emptyList(),
    val isLoading: Boolean                  = true,
    val error: String?                      = null
)

class TechnicianViewModel(
    private val repository: WorkshopRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TechnicianState())
    val state: StateFlow<TechnicianState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAllTechnicians()
                .catch { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message)
                }
                .collect { list ->
                    _state.value = _state.value.copy(technicians = list, isLoading = false)
                }
        }
    }

    fun upsert(technician: TechnicianEntity) {
        viewModelScope.launch { repository.upsertTechnician(technician) }
    }
}
