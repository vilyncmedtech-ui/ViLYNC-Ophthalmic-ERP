package com.vilync.ophthalmicerp.feature.designer.domain.binding

import com.vilync.ophthalmicerp.core.document.engine.BindingValue
import com.vilync.ophthalmicerp.core.document.engine.DocumentBindingContext
import com.vilync.ophthalmicerp.core.document.engine.DynamicFieldProvider
import com.vilync.ophthalmicerp.data.repository.SalesRepository
import com.vilync.ophthalmicerp.feature.designer.domain.model.DesignerDocumentType
import kotlinx.coroutines.flow.first

/**
 * Provides dynamic fields for Sales-related documents (Invoice, Challan, etc.).
 */
class SalesDynamicFieldProvider(
    private val repository: SalesRepository
) : DynamicFieldProvider {

    override val name: String = "SalesDataProvider"

    override val supportedTypes: Set<DesignerDocumentType> = setOf(
        DesignerDocumentType.SALES_INVOICE,
        DesignerDocumentType.DELIVERY_CHALLAN,
        DesignerDocumentType.CREDIT_NOTE,
        DesignerDocumentType.PROFORMA_INVOICE,
        DesignerDocumentType.SAMPLE_ISSUE
    )

    override suspend fun getFields(context: DocumentBindingContext): Map<String, BindingValue> {
        val fields = mutableMapOf<String, BindingValue>()
        val sale = repository.getSaleById(context.entityId) ?: return emptyMap()

        // 1. Basic Identity Fields
        fields["CustomerName"] = BindingValue.Text(sale.customerName)
        fields["CustomerAddress"] = BindingValue.Text(sale.billToAddress)
        fields["InvoiceNo"] = BindingValue.Text(sale.invoiceNumber)
        fields["InvoiceDate"] = BindingValue.Text(sale.invoiceDate)
        fields["GSTIN"] = BindingValue.Text(sale.billToGstin)
        
        // 2. Financial Summary
        fields["GrandTotal"] = BindingValue.Number(sale.totalAmount)

        // 3. Barcode & QR Resolution
        fields["InvoiceBarcode"] = BindingValue.Barcode(sale.invoiceNumber)
        fields["InvoiceQRCode"] = BindingValue.QrCode(sale.invoiceNumber)
        
        // 4. Item Table (Logical Decomposition)
        val items = repository.getSaleItems(sale.id).first()
        fields["ItemTable"] = BindingValue.Table(
            headers = listOf("Product", "Qty", "Rate", "Amount"),
            rows = items.map { item ->
                listOf(
                    BindingValue.Text(item.productName),
                    BindingValue.Number(item.quantity.toDouble()),
                    BindingValue.Number(item.rate),
                    BindingValue.Number(item.totalAmount)
                )
            }
        )

        return fields
    }
}
