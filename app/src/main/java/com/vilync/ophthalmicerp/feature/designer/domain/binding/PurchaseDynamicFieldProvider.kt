package com.vilync.ophthalmicerp.feature.designer.domain.binding

import com.vilync.ophthalmicerp.core.document.domain.DesignerDocumentType
import com.vilync.ophthalmicerp.core.document.engine.BindingValue
import com.vilync.ophthalmicerp.core.document.engine.DocumentBindingContext
import com.vilync.ophthalmicerp.core.document.engine.DynamicFieldProvider
import com.vilync.ophthalmicerp.core.document.engine.binding.PlaceholderRegistry
import com.vilync.ophthalmicerp.data.repository.ProductRepository
import com.vilync.ophthalmicerp.data.repository.PurchaseRepository
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import kotlinx.coroutines.flow.first
import java.util.Locale

/**
 * Provides dynamic fields for Purchase-related documents.
 */
class PurchaseDynamicFieldProvider(
    private val repository: PurchaseRepository,
    private val partyRepository: PartyRepository,
    private val productRepository: ProductRepository
) : DynamicFieldProvider {

    override val name: String = "PurchaseDataProvider"

    override val supportedTypes: Set<DesignerDocumentType> = setOf(
        DesignerDocumentType.DEBIT_NOTE, // Purchase Return
        DesignerDocumentType.PURCHASE_ORDER
    )

    override suspend fun getFields(context: DocumentBindingContext): Map<String, BindingValue> {
        val fields = mutableMapOf<String, BindingValue>()
        val purchase = repository.getPurchaseById(context.entityId) ?: return emptyMap()

        // 1. Common Identity Fields
        fields[PlaceholderRegistry.INVOICE_NO] = BindingValue.Text(purchase.invoiceNumber)
        fields[PlaceholderRegistry.INVOICE_DATE] = BindingValue.Text(purchase.invoiceDate)
        fields[PlaceholderRegistry.REFERENCE] = BindingValue.Text(purchase.reference)

        // 2. Supplier Details (from Master data if ID available)
        val vendor = if (purchase.supplierId > 0) {
            partyRepository.getPartyById(purchase.supplierId)
        } else null

        fields[PlaceholderRegistry.CUSTOMER_NAME] = BindingValue.Text(vendor?.partyName ?: purchase.supplierName)
        fields[PlaceholderRegistry.CUSTOMER_GSTIN] = BindingValue.Text(vendor?.gstin ?: purchase.supplierGstin)
        
        val address = vendor?.let { v ->
            listOf(v.addressLine1, v.addressLine2, v.city, v.state, v.pinCode).filter { it.isNotBlank() }.joinToString(", ")
        } ?: ""
        fields[PlaceholderRegistry.CUSTOMER_ADDRESS] = BindingValue.Text(address)
        
        // Purchase Order specific: Add DL details (if they existed in master, for now empty strings)
        if (context.documentType == DesignerDocumentType.PURCHASE_ORDER) {
            fields[PlaceholderRegistry.SUPPLIER_DL1] = BindingValue.Text("") 
            fields[PlaceholderRegistry.SUPPLIER_DL2] = BindingValue.Text("")
        }

        // 3. Financial Summary - Formatted for professional output
        fields[PlaceholderRegistry.TAXABLE_AMOUNT] = BindingValue.Text(money(purchase.taxableAmount))
        fields[PlaceholderRegistry.CGST] = BindingValue.Text(money(purchase.cgstAmount))
        fields[PlaceholderRegistry.SGST] = BindingValue.Text(money(purchase.sgstAmount))
        fields[PlaceholderRegistry.IGST] = BindingValue.Text(money(purchase.igstAmount))
        fields[PlaceholderRegistry.GST_TOTAL] = BindingValue.Text(money(purchase.cgstAmount + purchase.sgstAmount + purchase.igstAmount))
        fields[PlaceholderRegistry.GRAND_TOTAL] = BindingValue.Text(money(purchase.grandTotal))

        // 4. Item Table (No Serials for PO)
        val items = repository.getPurchaseItems(purchase.id).first()
        fields[PlaceholderRegistry.ITEM_TABLE] = BindingValue.Table(
            headers = listOf("Sr. No.", "Product", "Model", "Power", "HSN", "Qty", "Rate", "Total"),
            rows = items.mapIndexed { index, item ->
                val product = productRepository.getProductById(item.productId)
                listOf(
                    BindingValue.Text((index + 1).toString()),
                    BindingValue.Text(product?.productName ?: "Unknown"),
                    BindingValue.Text(product?.model ?: "-"),
                    BindingValue.Text(item.power),
                    BindingValue.Text(item.hsnCode),
                    BindingValue.Text(item.quantity.toString()),
                    BindingValue.Text(money(item.purchaseRate)),
                    BindingValue.Text(money(item.lineTotal))
                )
            }
        )

        fields[PlaceholderRegistry.BARCODE] = BindingValue.Barcode(purchase.invoiceNumber)
        
        return fields
    }

    private fun money(value: Double) = String.format(Locale.US, "%,.2f", value)
}
