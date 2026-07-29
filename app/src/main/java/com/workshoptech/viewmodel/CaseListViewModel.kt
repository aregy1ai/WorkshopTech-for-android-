package com.workshoptech.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.workshoptech.data.entity.CaseEntity
import com.workshoptech.data.repository.WorkshopRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CaseListViewModel(private val repository: WorkshopRepository) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private val _activeFilter = MutableStateFlow<String?>(null)

    private val _cases: Flow<PagingData<CaseEntity>> = combine(_searchQuery.debounce(300), _activeFilter) { query, filter ->
        when { !query.isNullOrBlank() -> repository.searchCasesPaged(query); !filter.isNullOrBlank() -> repository.getCasesPagedByStatus(filter); else -> repository.getCasesPaged() }
    }.flatMapLatest { it }.cachedIn(viewModelScope)
    val cases: Flow<PagingData<CaseEntity>> = _cases

    private val _dashboardStats = MutableStateFlow<WorkshopRepository.DashboardStats?>(null)
    val dashboardStats: StateFlow<WorkshopRepository.DashboardStats?> = _dashboardStats.asStateFlow()

    init { viewModelScope.launch { loadDashboardStats() } }
    fun setQuery(query: String) { _searchQuery.value = query }
    fun setFilter(status: String?) { _activeFilter.value = status }
    fun clearFilters() { _searchQuery.value = ""; _activeFilter.value = null }
    fun refreshDashboardStats() { viewModelScope.launch { loadDashboardStats() } }
    private suspend fun loadDashboardStats() { _dashboardStats.value = repository.getDashboardStats() }
}
