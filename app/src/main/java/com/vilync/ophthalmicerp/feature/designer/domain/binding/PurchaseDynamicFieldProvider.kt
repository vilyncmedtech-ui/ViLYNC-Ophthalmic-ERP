package com.vilync.ophthalmicerp.feature.designer.domain.binding

import com.vilync.ophthalmicerp.core.document.engine.BindingValue
import com.vilync.ophthalmicerp.core.document.engine.DocumentBindingContext
import com.vilync.ophthalmicerp.core.document.engine.DynamicFieldProvider
import com.vilync.ophthalmicerp.data.repository.PurchaseRepository
import com.vilync.ophthalmicerp.feature.designer.domain.model.DesignerDocumentType

/**
 * Provides dynamic fields for Purchase-related documents.
 */
class PurchaseDynamicFieldProvider(
    private val repository: PurchaseRepository
) : DynamicFieldProvider {

    override val name: String = "PurchaseDataProvider"

    override val supportedTypes: Set<DesignerDocumentType> = setOf(
        DesignerDocumentType.DEBIT_NOTE // Purchase Return
    )

    override suspend fun getFields(context: DocumentBindingContext): Map<String, BindingValue> {
        val fields = mutableMapOf<String, BindingValue>()
        val purchase = repository.getPurchaseById(context.entityId) ?: return emptyMap()

        fields["CustomerName"] = BindingValue.Text(purchase.supplierName) // Supplier in Purchase context
        fields["InvoiceNo"] = BindingValue.Text(purchase.invoiceNumber)
        fields["InvoiceDate"] = BindingValue.Text(purchase.invoiceDate)
        fields["GrandTotal"] = BindingValue.Number(purchase.grandTotal)

        fields["InvoiceBarcode"] = BindingValue.Barcode(purchase.invoiceNumber)
        
        return fields
    }
}
