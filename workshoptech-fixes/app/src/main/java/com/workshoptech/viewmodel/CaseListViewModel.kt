package com.workshoptech.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workshoptech.data.entity.CaseEntity
import com.workshoptech.data.repository.WorkshopRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CaseListState(
    val cases: List<CaseEntity>  = emptyList(),
    val query: String            = "",
    val statusFilter: String?    = null,
    val isLoading: Boolean       = true,
    val error: String?           = null
)

@OptIn(FlowPreview::class)
class CaseListViewModel(
    private val repository: WorkshopRepository
) : ViewModel() {

    private val _query        = MutableStateFlow("")
    private val _statusFilter = MutableStateFlow<String?>(null)

    private val _state = MutableStateFlow(CaseListState())
    val state: StateFlow<CaseListState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _query.debounce(300)
                .combine(_statusFilter) { q, s -> q to s }
                .flatMapLatest { (q, s) ->
                    when {
                        s != null -> repository.observeCasesByStatus(s)
                        else      -> repository.observeCases(q.takeIf { it.isNotBlank() })
                    }
                }
                .catch { e ->
                    _state.value = _state.value.copy(isLoading = false, error = e.message)
                }
                .collect { cases ->
                    _state.value = _state.value.copy(
                        cases     = cases,
                        isLoading = false
                    )
                }
        }
    }

    fun onSearch(query: String) {
        _query.value = query
        _state.value = _state.value.copy(query = query)
    }

    fun onStatusFilter(status: String?) {
        _statusFilter.value = status
        _state.value = _state.value.copy(statusFilter = status)
    }
}
