package com.workshoptech.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workshoptech.data.entity.InventoryEntity
import com.workshoptech.data.repository.WorkshopRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@Immutable
data class InventoryState(
    val items: List<InventoryEntity>    = emptyList(),
    val lowStockItems: List<InventoryEntity> = emptyList(),
    val isLoading: Boolean              = true,
    val error: String?                  = null
)

class InventoryViewModel(
    private val repository: WorkshopRepository
) : ViewModel() {

    private val _state = MutableStateFlow(InventoryState())
    val state: StateFlow<InventoryState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeInventory(),
                repository.observeLowStock()
            ) { all, low ->
                InventoryState(items = all, lowStockItems = low, isLoading = false)
            }.catch { e ->
                _state.value = _state.value.copy(isLoading = false, error = e.localizedMessage)
            }.collect { s -> _state.value = s }
        }
    }

    fun upsert(item: InventoryEntity) {
        viewModelScope.launch { repository.upsertInventoryItem(item) }
    }

    fun decrement(itemId: String, amount: Int = 1) {
        viewModelScope.launch { repository.decrementInventory(itemId, amount) }
    }
}
