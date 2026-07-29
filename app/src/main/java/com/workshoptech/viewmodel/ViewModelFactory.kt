package com.workshoptech.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.workshoptech.data.repository.WorkshopRepository

class ViewModelFactory(private val repository: WorkshopRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(CaseListViewModel::class.java) -> CaseListViewModel(repository) as T
        modelClass.isAssignableFrom(CreateCaseViewModel::class.java) -> CreateCaseViewModel(repository) as T
        modelClass.isAssignableFrom(CaseDetailViewModel::class.java) -> CaseDetailViewModel(repository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
