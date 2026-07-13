package com.workshoptech.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workshoptech.data.entity.CustomerEntity
import com.workshoptech.data.repository.WorkshopRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomerState(
    val customers: List<CustomerEntity> = emptyList(),
    val query: String                   = "",
    val isLoading: Boolean              = true,
    val error: String?                  = null
)

@OptIn(FlowPreview::class)
class CustomerViewModel(
    private val repository: WorkshopRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _state = MutableStateFlow(CustomerState())
    val state: StateFlow<CustomerState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _query.debounce(300)
                .flatMapLatest { q ->
                    repository.observeCustomers(q.takeIf { it.isNotBlank() })
                }
                .catch { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message)
                }
                .collect { list ->
                    _state.value = _state.value.copy(customers = list, isLoading = false)
                }
        }
    }

    fun onSearch(q: String) {
        _query.value = q
        _state.value = _state.value.copy(query = q)
    }

    fun upsert(customer: CustomerEntity) {
        viewModelScope.launch { repository.upsertCustomer(customer) }
    }
}
