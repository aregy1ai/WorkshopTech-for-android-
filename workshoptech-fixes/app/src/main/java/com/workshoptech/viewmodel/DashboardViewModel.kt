package com.workshoptech.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workshoptech.data.entity.CaseEntity
import com.workshoptech.data.entity.InventoryEntity
import com.workshoptech.data.repository.WorkshopRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * @Immutable tells the Compose compiler this class is structurally stable,
 * avoiding unnecessary recompositions when the reference changes but values
 * are equal.
 */
@Immutable
data class DashboardState(
    val activeCases:        List<CaseEntity>        = emptyList(),
    val readyCases:         List<CaseEntity>        = emptyList(),
    val lowStockItems:      List<InventoryEntity>   = emptyList(),
    val todayDeliveries:    Int                     = 0,
    val pendingInspections: Int                     = 0,
    val totalActiveCases:   Int                     = 0,
    val isLoading:          Boolean                 = true,
    val error:              String?                 = null
)

class DashboardViewModel(
    private val repository: WorkshopRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init { loadDashboard() }

    private fun loadDashboard() {
        viewModelScope.launch {
            combine(
                repository.observeCasesByStatus("IN_PROGRESS"),
                repository.observeCasesByStatus("READY_FOR_DELIVERY"),
                repository.observeLowStock()
            ) { active, ready, lowStock ->
                DashboardState(
                    activeCases        = active,
                    readyCases         = ready,
                    lowStockItems      = lowStock,
                    todayDeliveries    = ready.size,
                    pendingInspections = active.size,
                    totalActiveCases   = active.size + ready.size,
                    isLoading          = false,
                    error              = null
                )
            }
            // ✅ catch() on the flow — correctly handles Flow exceptions unlike try-catch outside collect
            .catch { e ->
                _state.value = _state.value.copy(isLoading = false, error = e.localizedMessage)
            }
            .collect { newState ->
                _state.value = newState
            }
        }
    }

    fun refresh() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        loadDashboard()
    }
}
