package com.vilync.ophthalmicerp.feature.inventory.serialstock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.repository.SerialStockRepository
import com.vilync.ophthalmicerp.data.repository.StockMovementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch


class SerialMovementHistoryViewModel(

    private val inventoryUnitId: Long,

    private val serialStockRepository:
    SerialStockRepository,

    private val stockMovementRepository:
    StockMovementRepository

) : ViewModel() {


    // =========================================================
    // UI STATE
    // =========================================================

    private val _uiState =
        MutableStateFlow(
            SerialMovementHistoryUiState(
                inventoryUnitId =
                    inventoryUnitId
            )
        )


    val uiState:
            StateFlow<SerialMovementHistoryUiState> =
        _uiState.asStateFlow()


    // =========================================================
    // INITIAL LOAD
    // =========================================================

    init {

        loadMovementHistory()
    }


    // =========================================================
    // REFRESH
    // =========================================================

    fun refresh() {

        loadMovementHistory()
    }


    // =========================================================
    // LOAD COMPLETE SERIAL INFORMATION + HISTORY
    // =========================================================

    private fun loadMovementHistory() {

        viewModelScope.launch {

            // =================================================
            // VALIDATE INVENTORY UNIT ID
            // =================================================

            if (inventoryUnitId <= 0L) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        errorMessage =
                            "Invalid Inventory Unit ID."
                    )

                return@launch
            }


            // =================================================
            // START LOADING
            // =================================================

            _uiState.value =
                _uiState.value.copy(
                    isLoading = true,
                    errorMessage = null
                )


            // =================================================
            // LOAD SERIAL STOCK INFORMATION
            // =================================================

            try {

                val serialStock =
                    serialStockRepository
                        .getSerialStockById(
                            inventoryUnitId =
                                inventoryUnitId
                        )


                if (serialStock == null) {

                    _uiState.value =
                        _uiState.value.copy(
                            isLoading = false,
                            errorMessage =
                                "Serial Stock record not found."
                        )

                    return@launch
                }


                // =============================================
                // STORE SERIAL INFORMATION
                // =============================================

                _uiState.value =
                    _uiState.value.copy(

                        inventoryUnitId =
                            serialStock.inventoryUnitId,

                        serialNumber =
                            serialStock.serialNumber,

                        serialPrefix =
                            serialStock.serialPrefix,

                        productName =
                            serialStock.productName,

                        brandName =
                            serialStock.brandName,

                        model =
                            serialStock.model,

                        category =
                            serialStock.category,

                        power =
                            serialStock.power,

                        batchNumber =
                            serialStock.batchNumber,

                        expiryDate =
                            serialStock.expiryDate,

                        receivedDate =
                            serialStock.receivedDate,

                        supplierName =
                            serialStock.supplierName,

                        purchaseInvoiceNumber =
                            serialStock.purchaseInvoiceNumber,

                        currentStatus =
                            serialStock.status,

                        errorMessage =
                            null
                    )


                // =============================================
                // OBSERVE MOVEMENT HISTORY
                // =============================================

                stockMovementRepository
                    .getMovementHistoryByUnit(
                        inventoryUnitId =
                            inventoryUnitId
                    )
                    .catch { throwable ->

                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                errorMessage =
                                    throwable.message
                                        ?: "Unable to load movement history."
                            )
                    }
                    .collect { movements ->

                        _uiState.value =
                            _uiState.value.copy(
                                isLoading = false,
                                movements =
                                    movements,
                                errorMessage =
                                    null
                            )
                    }

            } catch (throwable: Throwable) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoading = false,
                        errorMessage =
                            throwable.message
                                ?: "Unable to load Serial Movement History."
                    )
            }
        }
    }
}