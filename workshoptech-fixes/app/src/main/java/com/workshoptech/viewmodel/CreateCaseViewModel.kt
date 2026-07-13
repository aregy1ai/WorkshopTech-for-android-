package com.workshoptech.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workshoptech.data.entity.CaseEntity
import com.workshoptech.data.entity.CustomerEntity
import com.workshoptech.data.repository.WorkshopRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class CreateCaseState(
    val customerPhone: String    = "",
    val customerName: String     = "",
    val licensePlate: String     = "",
    val make: String             = "",
    val model: String            = "",
    val year: String             = "",
    val color: String            = "",
    val notes: String            = "",
    val existingCustomer: CustomerEntity? = null,
    val isSaving: Boolean        = false,
    val createdCaseId: String?   = null,
    val error: String?           = null
)

class CreateCaseViewModel(
    private val repository: WorkshopRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CreateCaseState())
    val state: StateFlow<CreateCaseState> = _state.asStateFlow()

    fun onPhoneChanged(phone: String) {
        _state.value = _state.value.copy(customerPhone = phone)
        lookupCustomer(phone)
    }

    private fun lookupCustomer(phone: String) {
        if (phone.length < 7) return
        viewModelScope.launch {
            val customer = repository.findCustomerByPhone(phone)
            _state.value = _state.value.copy(
                existingCustomer = customer,
                customerName     = customer?.name ?: _state.value.customerName
            )
        }
    }

    fun onFieldChanged(
        name: String?  = null,
        plate: String? = null,
        make: String?  = null,
        model: String? = null,
        year: String?  = null,
        color: String? = null,
        notes: String? = null
    ) {
        _state.value = _state.value.copy(
            customerName = name  ?: _state.value.customerName,
            licensePlate = plate ?: _state.value.licensePlate,
            make         = make  ?: _state.value.make,
            model        = model ?: _state.value.model,
            year         = year  ?: _state.value.year,
            color        = color ?: _state.value.color,
            notes        = notes ?: _state.value.notes
        )
    }

    fun createCase() {
        val s = _state.value
        if (s.licensePlate.isBlank()) {
            _state.value = s.copy(error = "رقم اللوحة مطلوب")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            try {
                val customerId = s.existingCustomer?.customerId ?: run {
                    val id = UUID.randomUUID().toString()
                    repository.upsertCustomer(
                        CustomerEntity(
                            customerId = id,
                            name       = s.customerName,
                            phone      = s.customerPhone,
                            email      = null,
                            createdAt  = System.currentTimeMillis()
                        )
                    )
                    id
                }

                val caseId = UUID.randomUUID().toString()
                repository.upsertCase(
                    CaseEntity(
                        caseId       = caseId,
                        customerId   = customerId,
                        licensePlate = s.licensePlate,
                        make         = s.make,
                        model        = s.model,
                        year         = s.year.toIntOrNull(),
                        color        = s.color,
                        status       = "NEW",
                        notes        = s.notes.takeIf { it.isNotBlank() },
                        createdAt    = System.currentTimeMillis(),
                        updatedAt    = System.currentTimeMillis()
                    )
                )
                _state.value = _state.value.copy(isSaving = false, createdCaseId = caseId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.message)
            }
        }
    }
}
