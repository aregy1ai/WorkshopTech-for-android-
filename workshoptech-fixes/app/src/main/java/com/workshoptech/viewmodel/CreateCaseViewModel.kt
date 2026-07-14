package com.workshoptech.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workshoptech.data.entity.CaseEntity
import com.workshoptech.data.entity.CustomerEntity
import com.workshoptech.data.repository.WorkshopRepository
import com.workshoptech.util.InputValidator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

@Immutable
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

        // ── Input validation ──────────────────────────────────────────────────
        val plateResult = InputValidator.validatePlate(s.licensePlate)
        if (!plateResult.isValid) {
            _state.value = s.copy(error = plateResult.errorMessage); return
        }
        val nameResult = if (s.existingCustomer == null)
            InputValidator.validateName(s.customerName) else null
        if (nameResult != null && !nameResult.isValid) {
            _state.value = s.copy(error = nameResult.errorMessage); return
        }
        val yearResult = InputValidator.validateYear(s.year)
        if (!yearResult.isValid) {
            _state.value = s.copy(error = yearResult.errorMessage); return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            try {
                val customerId = s.existingCustomer?.customerId ?: run {
                    val id = UUID.randomUUID().toString()
                    repository.upsertCustomer(
                        CustomerEntity(
                            customerId = id,
                            name       = nameResult!!.valueOrNull() ?: s.customerName,
                            phone      = s.customerPhone.trim(),
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
                        licensePlate = plateResult.valueOrNull() ?: s.licensePlate,
                        make         = InputValidator.sanitizeText(s.make),
                        model        = InputValidator.sanitizeText(s.model),
                        year         = yearResult.valueOrNull()?.toIntOrNull(),
                        color        = InputValidator.sanitizeText(s.color),
                        status       = "NEW",
                        notes        = s.notes.takeIf { it.isNotBlank() }
                            ?.let { InputValidator.sanitizeText(it) },
                        createdAt    = System.currentTimeMillis(),
                        updatedAt    = System.currentTimeMillis()
                    )
                )
                _state.value = _state.value.copy(isSaving = false, createdCaseId = caseId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSaving = false, error = e.localizedMessage)
            }
        }
    }
}
