package com.vilync.ophthalmicerp.feature.purchase.returnentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.entity.PurchaseReturnEntity
import com.vilync.ophthalmicerp.data.repository.PurchaseReturnRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PurchaseReturnSavedRegisterViewModel(
    private val repository: PurchaseReturnRepository
) : ViewModel() {

    // =========================================================
    // SEARCH
    // =========================================================

    private val _searchQuery =
        MutableStateFlow("")

    val searchQuery: StateFlow<String> =
        _searchQuery.asStateFlow()


    // =========================================================
    // CANCEL / DELETE STATE
    // =========================================================

    private val _isCancelling =
        MutableStateFlow(false)

    val isCancelling: StateFlow<Boolean> =
        _isCancelling.asStateFlow()


    private val _actionMessage =
        MutableStateFlow<String?>(null)

    val actionMessage: StateFlow<String?> =
        _actionMessage.asStateFlow()


    // =========================================================
    // PURCHASE RETURN REGISTER
    // =========================================================

    val returns: StateFlow<List<PurchaseReturnEntity>> =
        combine(
            _searchQuery,
            repository.getAllPurchaseReturns()
        ) { query, allReturns ->

            val keyword =
                query.trim()

            if (keyword.isBlank()) {

                allReturns

            } else {

                allReturns.filter { purchaseReturn ->

                    purchaseReturn.creditNoteNumber.contains(
                        keyword,
                        ignoreCase = true
                    ) ||
                            purchaseReturn.originalInvoiceNumber.contains(
                                keyword,
                                ignoreCase = true
                            ) ||
                            purchaseReturn.supplierName.contains(
                                keyword,
                                ignoreCase = true
                            )
                }
            }

        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )


    // =========================================================
    // SEARCH UPDATE
    // =========================================================

    fun updateSearchQuery(
        value: String
    ) {
        _searchQuery.value =
            value
    }


    // =========================================================
    // CANCEL PURCHASE RETURN
    // =========================================================

    /**
     * UI may show this action as Delete / Cancel.
     *
     * We intentionally do NOT hard-delete the Purchase Return.
     *
     * The existing repository cancellation flow marks the
     * Purchase Return as CANCELLED so accounting / audit
     * history is preserved.
     */
    fun cancelPurchaseReturn(
        purchaseReturnId: Long,
        reason: String
    ) {

        if (_isCancelling.value) {
            return
        }

        if (purchaseReturnId <= 0L) {

            _actionMessage.value =
                "Invalid Purchase Return."

            return
        }

        val cleanReason =
            reason.trim()

        if (cleanReason.isBlank()) {

            _actionMessage.value =
                "Cancellation reason is required."

            return
        }


        viewModelScope.launch {

            _isCancelling.value =
                true

            _actionMessage.value =
                null

            try {

                val purchaseReturn =
                    repository.getPurchaseReturnById(
                        purchaseReturnId =
                            purchaseReturnId
                    )

                if (purchaseReturn == null) {

                    _actionMessage.value =
                        "Purchase Return not found."

                    return@launch
                }


                if (
                    purchaseReturn.status ==
                    PurchaseReturnEntity.STATUS_CANCELLED
                ) {

                    _actionMessage.value =
                        "This Purchase Return is already cancelled."

                    return@launch
                }


                val cancelled =
                    repository.cancelPostedPurchaseReturn(
                        purchaseReturnId =
                            purchaseReturnId,
                        reason =
                            cleanReason
                    )


                _actionMessage.value =
                    if (cancelled) {

                        "Purchase Return cancelled successfully."

                    } else {

                        "Unable to cancel Purchase Return."
                    }

            } catch (
                exception: Exception
            ) {

                _actionMessage.value =
                    exception.message
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Unable to cancel Purchase Return."

            } finally {

                _isCancelling.value =
                    false
            }
        }
    }


    // =========================================================
    // MESSAGE CONSUMED
    // =========================================================

    fun clearActionMessage() {

        _actionMessage.value =
            null
    }
}