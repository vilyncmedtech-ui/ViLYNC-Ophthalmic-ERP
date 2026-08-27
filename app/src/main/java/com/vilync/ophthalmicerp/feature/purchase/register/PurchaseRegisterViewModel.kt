package com.vilync.ophthalmicerp.feature.purchase.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.core.financialyear.FinancialYearManager
import com.vilync.ophthalmicerp.data.entity.PurchaseEntity
import com.vilync.ophthalmicerp.data.repository.PurchaseRepository
import com.vilync.ophthalmicerp.data.repository.PurchaseReturnRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


data class PurchaseRegisterUiState(

    val searchQuery: String = "",

    val purchases: List<PurchaseEntity> = emptyList(),

    // Example:
    // 2025 = FY 2025-26
    val financialYearStart: Int =
        FinancialYearManager
            .activeFinancialYear
            .value
            .startYear
)


// =============================================================
// PURCHASE DELETE RESULT
// =============================================================

sealed class PurchaseDeleteResult {

    data class Success(
        val message: String
    ) : PurchaseDeleteResult()

    data class Blocked(
        val message: String
    ) : PurchaseDeleteResult()

    data class Error(
        val message: String
    ) : PurchaseDeleteResult()
}


class PurchaseRegisterViewModel(

    private val purchaseRepository: PurchaseRepository,

    /*
     * Nullable temporarily so the existing navigation/factory
     * continues compiling until the Purchase Register screen
     * wiring is updated in the next controlled step.
     *
     * IMPORTANT:
     * Delete is BLOCKED if this repository has not been supplied.
     */
    private val purchaseReturnRepository: PurchaseReturnRepository? = null,

    private val requiredStatus: String = "POSTED"

) : ViewModel() {


    private val searchQuery =
        MutableStateFlow("")


    // =========================================================
    // DELETE RESULT
    // =========================================================

    private val _deleteResult =
        MutableStateFlow<PurchaseDeleteResult?>(null)

    val deleteResult: StateFlow<PurchaseDeleteResult?> =
        _deleteResult.asStateFlow()


    // =========================================================
    // PURCHASES FOR ACTIVE WORKING FINANCIAL YEAR
    // =========================================================

    /*
     * Whenever Working FY changes, flatMapLatest cancels the
     * previous FY database observation and starts observing
     * the newly selected FY.
     *
     * Room therefore loads only the required Financial Year.
     */
    private val purchasesForActiveFinancialYear =
        FinancialYearManager
            .activeFinancialYear
            .flatMapLatest { financialYear ->

                purchaseRepository
                    .getPurchasesByStatusAndFy(
                        status = requiredStatus,
                        financialYearStart =
                            financialYear.startYear
                    )
            }


    // =========================================================
    // UI STATE
    // =========================================================

    val uiState: StateFlow<PurchaseRegisterUiState> =

        combine(

            purchasesForActiveFinancialYear,

            FinancialYearManager.activeFinancialYear,

            searchQuery

        ) { purchases, financialYear, query ->

            val q =
                query.trim()


            val filteredPurchases =

                if (q.isBlank()) {

                    purchases

                } else {

                    purchases.filter { purchase ->

                        purchase.invoiceNumber.contains(
                            q,
                            ignoreCase = true
                        ) ||
                                purchase.supplierName.contains(
                                    q,
                                    ignoreCase = true
                                )
                    }
                }


            PurchaseRegisterUiState(

                searchQuery =
                    query,

                purchases =
                    filteredPurchases,

                financialYearStart =
                    financialYear.startYear
            )
        }
            .stateIn(

                scope =
                    viewModelScope,

                started =
                    SharingStarted
                        .WhileSubscribed(
                            5_000
                        ),

                initialValue =
                    PurchaseRegisterUiState()
            )


    // =========================================================
    // SEARCH
    // =========================================================

    fun updateSearchQuery(
        value: String
    ) {

        searchQuery.value =
            value
    }


    // =========================================================
    // DELETE PURCHASE
    // =========================================================

    /**
     * Protected Purchase deletion.
     *
     * CURRENT RULE:
     *
     * A Purchase cannot be deleted if ANY Purchase Return
     * document exists against that Purchase.
     *
     * This deliberately includes historical/cancelled returns
     * because the original Purchase is part of that document
     * history.
     *
     * FUTURE RULE:
     *
     * When Sales Module is developed, dependency checks for:
     *
     * - Sales Invoice
     * - Sales Challan
     * - Serial-number usage
     * - Other stock-linked transactions
     *
     * must be added here BEFORE deleteCompletePurchase().
     */
    fun deletePurchase(
        purchaseId: Long
    ) {

        if (purchaseId <= 0L) {

            _deleteResult.value =
                PurchaseDeleteResult.Error(
                    message = "Invalid Purchase ID."
                )

            return
        }


        viewModelScope.launch {

            try {

                // -------------------------------------------------
                // VERIFY PURCHASE EXISTS
                // -------------------------------------------------

                val purchase =
                    purchaseRepository
                        .getPurchaseById(
                            purchaseId = purchaseId
                        )


                if (purchase == null) {

                    _deleteResult.value =
                        PurchaseDeleteResult.Error(
                            message =
                                "Purchase not found. It may already have been deleted."
                        )

                    return@launch
                }


                // -------------------------------------------------
                // SAFETY: DEPENDENCY REPOSITORY MUST BE AVAILABLE
                // -------------------------------------------------

                val returnRepository =
                    purchaseReturnRepository


                if (returnRepository == null) {

                    _deleteResult.value =
                        PurchaseDeleteResult.Blocked(
                            message =
                                "Purchase delete protection is not fully connected yet. " +
                                        "Purchase has not been deleted."
                        )

                    return@launch
                }


                // -------------------------------------------------
                // PURCHASE RETURN DEPENDENCY CHECK
                // -------------------------------------------------

                val linkedReturns =
                    returnRepository
                        .getReturnsForPurchase(
                            originalPurchaseId = purchaseId
                        )
                        .first()


                if (linkedReturns.isNotEmpty()) {

                    val references =
                        linkedReturns
                            .map {
                                it.creditNoteNumber.trim()
                            }
                            .filter {
                                it.isNotBlank()
                            }
                            .distinct()


                    val referenceText =

                        if (references.isEmpty()) {

                            "a Purchase Return"

                        } else {

                            references.joinToString(
                                separator = ", "
                            )
                        }


                    _deleteResult.value =
                        PurchaseDeleteResult.Blocked(
                            message =
                                "Purchase cannot be deleted.\n\n" +
                                        "Invoice: ${purchase.invoiceNumber}\n\n" +
                                        "This Purchase is already linked with Purchase Return: " +
                                        referenceText +
                                        ".\n\n" +
                                        "The original Purchase must be preserved for transaction history."
                        )

                    return@launch
                }


                // -------------------------------------------------
                // FUTURE SALES / CHALLAN / SERIAL DEPENDENCIES
                // -------------------------------------------------

                /*
                 * IMPORTANT PERMANENT ERP RULE:
                 *
                 * Before this delete call, future Sales Module
                 * must verify that none of this Purchase's serial
                 * numbers / quantities are used in:
                 *
                 * Sales Invoice
                 * Sales Challan
                 * Sales Return
                 * Transfer
                 * Replacement
                 * or any other downstream stock transaction.
                 *
                 * If used anywhere, deletion MUST be blocked and
                 * the exact document/reference shown to the user.
                 */


                // -------------------------------------------------
                // SAFE PHYSICAL DELETE
                // -------------------------------------------------

                purchaseRepository
                    .deleteCompletePurchase(
                        purchaseId = purchaseId
                    )


                _deleteResult.value =
                    PurchaseDeleteResult.Success(
                        message =
                            "Purchase ${purchase.invoiceNumber} deleted successfully."
                    )


            } catch (error: Exception) {

                _deleteResult.value =
                    PurchaseDeleteResult.Error(
                        message =
                            error.message
                                ?: "Unable to delete Purchase."
                    )
            }
        }
    }


    // =========================================================
    // CLEAR DELETE RESULT
    // =========================================================

    fun clearDeleteResult() {

        _deleteResult.value =
            null
    }
}


// =============================================================
// VIEW MODEL FACTORY
// =============================================================

class PurchaseRegisterViewModelFactory(

    private val purchaseRepository:
    PurchaseRepository,

    /*
     * Optional temporarily for backward-compatible compilation.
     *
     * In the Purchase Register navigation step we will supply
     * the actual PurchaseReturnRepository.
     */
    private val purchaseReturnRepository:
    PurchaseReturnRepository? = null,

    private val requiredStatus: String = "POSTED"

) : ViewModelProvider.Factory {


    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {


        if (
            modelClass.isAssignableFrom(
                PurchaseRegisterViewModel::class.java
            )
        ) {

            return PurchaseRegisterViewModel(
                purchaseRepository =
                    purchaseRepository,
                purchaseReturnRepository =
                    purchaseReturnRepository,
                requiredStatus = requiredStatus
            ) as T
        }


        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}