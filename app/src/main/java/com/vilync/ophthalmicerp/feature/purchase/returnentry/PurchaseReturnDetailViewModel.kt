package com.vilync.ophthalmicerp.feature.purchase.returnentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnItemEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnLensEntity
import com.vilync.ophthalmicerp.data.repository.PurchaseReturnRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class PurchaseReturnDetailUiState(
    val isLoading: Boolean = true,
    val purchaseReturn: PurchaseReturnEntity? = null,
    val items: List<PurchaseReturnItemEntity> = emptyList(),
    val lensesByItemId: Map<Long, List<PurchaseReturnLensEntity>> = emptyMap(),
    val errorMessage: String? = null
)

class PurchaseReturnDetailViewModel(
    private val repository: PurchaseReturnRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseReturnDetailUiState())
    val uiState: StateFlow<PurchaseReturnDetailUiState> = _uiState.asStateFlow()

    fun loadPurchaseReturn(purchaseReturnId: Long) {
        viewModelScope.launch {
            _uiState.value = PurchaseReturnDetailUiState(isLoading = true)
            try {
                val purchaseReturn = repository.getPurchaseReturnById(purchaseReturnId)
                    ?: throw IllegalStateException("Purchase return not found.")
                val items = repository.getPurchaseReturnItems(purchaseReturnId).first()
                val lensesByItemId = items.associate { item ->
                    item.id to repository.getPurchaseReturnLenses(item.id).first()
                }
                _uiState.value = PurchaseReturnDetailUiState(
                    isLoading = false,
                    purchaseReturn = purchaseReturn,
                    items = items,
                    lensesByItemId = lensesByItemId
                )
            } catch (error: Exception) {
                _uiState.value = PurchaseReturnDetailUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Unable to load purchase return."
                )
            }
        }
    }
}
