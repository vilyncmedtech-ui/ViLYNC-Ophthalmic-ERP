package com.vilync.ophthalmicerp.feature.purchase.detail

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.core.document.domain.DesignerDocumentType
import com.vilync.ophthalmicerp.core.document.engine.DocumentRuntime
import com.vilync.ophthalmicerp.core.document.engine.PrintRenderResult
import com.vilync.ophthalmicerp.core.util.ShareUtils
import com.vilync.ophthalmicerp.data.entity.PurchaseEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseItemEntity
import com.vilync.ophthalmicerp.data.entity.PurchaseLensEntity
import com.vilync.ophthalmicerp.data.repository.PurchaseRepository
import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Locale


// =============================================================
// PURCHASE DETAIL ITEM
// =============================================================

data class PurchaseDetailItem(

    val purchaseItem: PurchaseItemEntity,

    val productName: String = "",

    val brand: String = "",

    val model: String = "",

    val category: String = "",

    val hsnCode: String = "",

    val lenses: List<PurchaseLensEntity> = emptyList()
)


// =============================================================
// UI STATE
// =============================================================

data class PurchaseDetailUiState(

    val isLoading: Boolean = true,

    val purchase: PurchaseEntity? = null,

    val items: List<PurchaseDetailItem> = emptyList(),

    val errorMessage: String? = null
)


// =============================================================
// VIEW MODEL
// =============================================================

class PurchaseDetailViewModel(

    private val purchaseId: Long,

    private val purchaseRepository: PurchaseRepository,

    private val productRepository: ProductMasterRepository,

    private val documentRuntime: DocumentRuntime? = null

) : ViewModel() {


    private val _uiState =
        MutableStateFlow(
            PurchaseDetailUiState()
        )

    val uiState: StateFlow<PurchaseDetailUiState> =
        _uiState.asStateFlow()


    init {

        loadPurchaseDetail()
    }

    fun refresh() {
        loadPurchaseDetail()
    }

    fun printOrder(context: Context, title: String) {
        val runtime = documentRuntime ?: return
        viewModelScope.launch {
            val result = runtime.print(purchaseId, DesignerDocumentType.PURCHASE_ORDER, title)
            if (result is PrintRenderResult && result.isSuccess) {
                val adapter = result.printAdapter ?: return@launch
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                printManager.print(
                    title,
                    adapter,
                    PrintAttributes.Builder().build()
                )
            }
        }
    }

    fun generateOrderPdf(outputFile: File, onResult: (Boolean) -> Unit) {
        val runtime = documentRuntime ?: return
        viewModelScope.launch {
            val result = runtime.generatePdf(purchaseId, DesignerDocumentType.PURCHASE_ORDER, outputFile)
            onResult(result.isSuccess)
        }
    }

    fun exportExcelAndShare(context: Context): Result<Unit> = runCatching {
        val state = _uiState.value
        val purchase = state.purchase ?: throw Exception("Order data not loaded.")
        
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, "PO_${purchase.invoiceNumber.replace("/", "_")}.xls")

        OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8).use { w ->
            w.write("""<?xml version="1.0" encoding="UTF-8"?>""")
            w.write("""<?mso-application progid="Excel.Sheet"?>""")
            w.write("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">""")
            w.write("""<Worksheet ss:Name="PurchaseOrder"><Table>""")
            
            fun row(vararg values: String) {
                w.write("<Row>")
                values.forEach { v ->
                    val escaped = v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
                    w.write("<Cell><Data ss:Type=\"String\">$escaped</Data></Cell>")
                }
                w.write("</Row>")
            }

            row("PURCHASE ORDER", purchase.invoiceNumber)
            row("Date", purchase.invoiceDate)
            row("Vendor", purchase.supplierName)
            row("Status", purchase.status)
            row()
            row("Product", "Model", "Power", "HSN", "Qty", "Rate", "GST %", "Total")
            state.items.forEach { itm ->
                val pi = itm.purchaseItem
                row(
                    itm.productName,
                    itm.model,
                    pi.power,
                    itm.hsnCode,
                    pi.quantity.toString(),
                    "%.2f".format(pi.purchaseRate),
                    "${pi.gstPercent}%",
                    "%.2f".format(pi.lineTotal)
                )
            }
            row()
            row("Taxable Amount", "%.2f".format(purchase.taxableAmount))
            row("GST Total", "%.2f".format(purchase.igstAmount))
            row("Grand Total", "%.2f".format(purchase.grandTotal))
            row()
            row("Remarks", purchase.reference)
            
            w.write("</Table></Worksheet></Workbook>")
        }
        ShareUtils.shareFile(context, file, "application/vnd.ms-excel", "Share Purchase Order Excel")
    }


    // =========================================================
    // LOAD PURCHASE DETAIL
    // =========================================================

    private fun loadPurchaseDetail() {

        viewModelScope.launch {

            _uiState.value =
                PurchaseDetailUiState(
                    isLoading = true
                )

            try {

                // -------------------------------------------------
                // PURCHASE HEADER
                // -------------------------------------------------

                val purchase =
                    purchaseRepository
                        .getPurchaseById(
                            purchaseId
                        )

                if (purchase == null) {

                    _uiState.value =
                        PurchaseDetailUiState(
                            isLoading = false,
                            errorMessage =
                                "Purchase invoice not found."
                        )

                    return@launch
                }


                // -------------------------------------------------
                // PURCHASE ITEMS
                // -------------------------------------------------

                val purchaseItems =
                    purchaseRepository
                        .getPurchaseItems(
                            purchaseId
                        )
                        .first()


                // -------------------------------------------------
                // RESOLVE PRODUCT + PHYSICAL LENSES
                // -------------------------------------------------

                val detailItems =
                    purchaseItems.map { item ->

                        val product =
                            productRepository
                                .getProductById(
                                    item.productId
                                )

                        val lenses =
                            purchaseRepository
                                .getPurchaseLenses(
                                    item.id
                                )
                                .first()


                        PurchaseDetailItem(

                            purchaseItem = item,

                            productName =
                                product?.productName
                                    ?: "Unknown Product",

                            brand =
                                product?.brand
                                    ?: "",

                            model =
                                product?.model
                                    ?: "",

                            category =
                                product?.category
                                    ?.name
                                    ?: "",

                            hsnCode =
                                product?.hsnCode
                                    ?: "",

                            lenses =
                                lenses
                        )
                    }


                // -------------------------------------------------
                // SUCCESS
                // -------------------------------------------------

                _uiState.value =
                    PurchaseDetailUiState(

                        isLoading = false,

                        purchase = purchase,

                        items = detailItems,

                        errorMessage = null
                    )

            } catch (e: Exception) {

                _uiState.value =
                    PurchaseDetailUiState(

                        isLoading = false,

                        errorMessage =
                            e.message
                                ?: "Unable to load purchase details."
                    )
            }
        }
    }
}


// =============================================================
// VIEW MODEL FACTORY
// =============================================================

class PurchaseDetailViewModelFactory(

    private val purchaseId: Long,

    private val purchaseRepository: PurchaseRepository,

    private val productRepository: ProductMasterRepository,

    private val documentRuntime: DocumentRuntime? = null

) : ViewModelProvider.Factory {


    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                PurchaseDetailViewModel::class.java
            )
        ) {

            return PurchaseDetailViewModel(

                purchaseId =
                    purchaseId,

                purchaseRepository =
                    purchaseRepository,

                productRepository =
                    productRepository,

                documentRuntime = documentRuntime

            ) as T
        }


        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
