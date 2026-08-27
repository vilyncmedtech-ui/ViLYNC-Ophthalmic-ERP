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
    val lenses: List<SaleLensEntity>,
    val hsnFromMaster: String = "",
    val productModel: String = ""
)

data class SalesInvoiceDetailUiState(
    val isLoading: Boolean = true,
    val sale: SaleEntity? = null,
    val creditNoteNumber: String? = null,
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

                val saleWithCn =
                    requireNotNull(
                        repository.getSaleWithCreditNoteById(saleId)
                    ) {
                        "Sales Invoice could not be found."
                    }

                val saleHeader = saleWithCn.sale
                val cnNumber = saleWithCn.creditNoteNumber

                val profile =
                    companyProfileDao.getCompanyProfile()

                val items =
                    repository
                        .getSaleItems(saleId)
                        .first()

                val rawLines =
                    items.map { item ->

                        val savedLenses =
                            repository
                                .getSaleLenses(item.id)
                                .first()

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

                        val productMaster = runCatching {
                            productRepository.getProductById(item.productId)
                        }.getOrNull()

                        SalesInvoiceDetailLine(
                            item = item,
                            lenses = resolvedLenses,
                            hsnFromMaster = productMaster?.hsnCode ?: item.hsnCode,
                            productModel = productMaster?.model ?: ""
                        )
                    }

                // =====================================================
                // GROUPING BY PRODUCT + MODEL + POWER + RATE + TAX (SOURCE OF TRUTH)
                // =====================================================
                val groupedLines = rawLines.groupBy { 
                    "${it.item.productName}|${it.productModel}|${it.item.power}|${it.item.rate}|${it.item.discountPercent}|${it.item.gstPercent}|${it.hsnFromMaster}" 
                }.values.map { group ->
                    val first = group.first()
                    val totalQty = group.sumOf { it.item.quantity }
                    val totalTaxable = group.sumOf { it.item.taxableAmount }
                    val totalGst = group.sumOf { it.item.gstAmount }
                    val totalAmt = group.sumOf { it.item.totalAmount }
                    val allLenses = group.flatMap { it.lenses }
                    
                    SalesInvoiceDetailLine(
                        item = first.item.copy(
                            quantity = totalQty,
                            taxableAmount = totalTaxable,
                            gstAmount = totalGst,
                            totalAmount = totalAmt
                        ),
                        lenses = allLenses,
                        hsnFromMaster = first.hsnFromMaster,
                        productModel = first.productModel
                    )
                }

                val result = object {
                    val sale = saleHeader
                    val creditNoteNumber = cnNumber
                    val lines = groupedLines
                    val companyProfile = profile
                }
                
                result

            }.onSuccess { res ->

                _uiState.value =
                    SalesInvoiceDetailUiState(
                        isLoading = false,
                        sale = res.sale,
                        creditNoteNumber = res.creditNoteNumber,
                        lines = res.lines,
                        companyProfile = res.companyProfile
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
