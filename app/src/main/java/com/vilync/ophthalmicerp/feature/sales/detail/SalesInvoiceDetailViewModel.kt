package com.vilync.ophthalmicerp.feature.sales.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.entity.SaleEntity
import com.vilync.ophthalmicerp.data.entity.SaleItemEntity
import com.vilync.ophthalmicerp.data.entity.SaleLensEntity
import com.vilync.ophthalmicerp.data.repository.InventoryRepository
import com.vilync.ophthalmicerp.data.repository.ProductRepository
import com.vilync.ophthalmicerp.data.repository.SalesRepository
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileDao
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class SalesInvoiceDetailLine(
    val item: SaleItemEntity,
    val lenses: List<SaleLensEntity>
)

data class SalesInvoiceDetailUiState(
    val isLoading: Boolean = true,
    val sale: SaleEntity? = null,
    val lines: List<SalesInvoiceDetailLine> = emptyList(),
    val companyProfile: CompanyProfileEntity? = null,
    val errorMessage: String? = null
)

class SalesInvoiceDetailViewModel(
    private val saleId: Long,
    private val repository: SalesRepository,
    private val inventoryRepository: InventoryRepository,
    private val productRepository: ProductRepository,
    private val companyProfileDao: CompanyProfileDao
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(SalesInvoiceDetailUiState())

    val uiState: StateFlow<SalesInvoiceDetailUiState> =
        _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        load()
    }

    private fun load() {
        viewModelScope.launch {

            _uiState.value =
                SalesInvoiceDetailUiState(
                    isLoading = true
                )

            runCatching {

                require(saleId > 0L) {
                    "Valid Sales Invoice is required."
                }

                // =====================================================
                // SALES INVOICE
                // =====================================================

                val sale =
                    requireNotNull(
                        repository.getSaleById(saleId)
                    ) {
                        "Sales Invoice could not be found."
                    }

                // =====================================================
                // COMPANY PROFILE
                // =====================================================
                //
                // Company Profile is deliberately loaded as a one-time
                // snapshot for this invoice detail state.
                //
                // The same snapshot will later be supplied to the
                // Print/PDF renderer so company identity, GSTIN,
                // bank details, UPI, terms and signatory information
                // remain sourced from the existing Company Profile.
                // =====================================================

                val companyProfile =
                    companyProfileDao.getCompanyProfile()

                // =====================================================
                // SALES ITEMS
                // =====================================================

                val items =
                    repository
                        .getSaleItems(saleId)
                        .first()

                // =====================================================
                // SALES ITEMS + PHYSICAL LENSES
                // =====================================================

                val lines =
                    items.map { item ->

                        val savedLenses =
                            repository
                                .getSaleLenses(item.id)
                                .first()

                        // =============================================
                        // RESOLVE DISPLAY SERIAL NUMBER
                        // =============================================
                        //
                        // Preserve the existing saved Sales Lens data.
                        //
                        // Where Inventory still contains the physical
                        // unit, prefer its serial number.
                        //
                        // Product Master Serial Prefix is applied only
                        // when the stored/base serial is numeric and
                        // does not already contain the prefix.
                        //
                        // No database mutation is performed here.
                        // This is display/export resolution only.
                        // =============================================

                        val resolvedLenses =
                            savedLenses.map { lens ->

                                val inventoryUnit =
                                    runCatching {
                                        inventoryRepository
                                            .getById(
                                                lens.inventoryUnitId
                                            )
                                    }.getOrNull()

                                val inventorySerial =
                                    inventoryUnit
                                        ?.serialNumber
                                        ?.trim()
                                        .orEmpty()

                                val savedSerial =
                                    lens
                                        .serialNumber
                                        .trim()

                                val baseSerial =
                                    inventorySerial.ifBlank {
                                        savedSerial
                                    }

                                val serialPrefix =
                                    inventoryUnit
                                        ?.let { unit ->

                                            runCatching {

                                                productRepository
                                                    .getProductById(
                                                        unit.productId
                                                    )
                                                    ?.serialPrefix
                                                    ?.trim()
                                                    .orEmpty()

                                            }.getOrDefault("")
                                        }
                                        .orEmpty()

                                val resolvedSerial =
                                    when {

                                        baseSerial.isBlank() ->
                                            baseSerial

                                        serialPrefix.isBlank() ->
                                            baseSerial

                                        baseSerial.startsWith(
                                            prefix = serialPrefix,
                                            ignoreCase = true
                                        ) ->
                                            baseSerial

                                        baseSerial.all {
                                            it.isDigit()
                                        } ->
                                            serialPrefix + baseSerial

                                        else ->
                                            baseSerial
                                    }

                                if (
                                    resolvedSerial.isNotBlank() &&
                                    resolvedSerial !=
                                    lens.serialNumber
                                ) {

                                    lens.copy(
                                        serialNumber =
                                            resolvedSerial
                                    )

                                } else {

                                    lens
                                }
                            }

                        SalesInvoiceDetailLine(
                            item = item,
                            lenses = resolvedLenses
                        )
                    }

                // =====================================================
                // RESULT
                // =====================================================

                Triple(
                    sale,
                    lines,
                    companyProfile
                )

            }.onSuccess {
                    (
                        sale,
                        lines,
                        companyProfile
                    ) ->

                _uiState.value =
                    SalesInvoiceDetailUiState(
                        isLoading = false,
                        sale = sale,
                        lines = lines,
                        companyProfile =
                            companyProfile
                    )

            }.onFailure { error ->

                _uiState.value =
                    SalesInvoiceDetailUiState(
                        isLoading = false,
                        errorMessage =
                            error.message
                                ?: "Unable to load Sales Invoice."
                    )
            }
        }
    }
}

class SalesInvoiceDetailViewModelFactory(
    private val saleId: Long,
    private val repository: SalesRepository,
    private val inventoryRepository: InventoryRepository,
    private val productRepository: ProductRepository,
    private val companyProfileDao: CompanyProfileDao
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                SalesInvoiceDetailViewModel::class.java
            )
        ) {

            @Suppress("UNCHECKED_CAST")
            return SalesInvoiceDetailViewModel(
                saleId = saleId,
                repository = repository,
                inventoryRepository =
                    inventoryRepository,
                productRepository =
                    productRepository,
                companyProfileDao =
                    companyProfileDao
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}