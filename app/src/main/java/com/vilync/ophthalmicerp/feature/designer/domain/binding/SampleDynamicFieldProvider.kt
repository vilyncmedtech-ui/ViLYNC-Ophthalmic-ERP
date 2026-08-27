package com.vilync.ophthalmicerp.feature.designer.domain.binding

import com.vilync.ophthalmicerp.core.document.domain.DesignerDocumentType
import com.vilync.ophthalmicerp.core.document.engine.BindingValue
import com.vilync.ophthalmicerp.core.document.engine.DocumentBindingContext
import com.vilync.ophthalmicerp.core.document.engine.DynamicFieldProvider
import com.vilync.ophthalmicerp.core.document.engine.binding.PlaceholderRegistry

/**
 * Provides static mock data to facilitate live preview in the Document Designer.
 */
class SampleDynamicFieldProvider : DynamicFieldProvider {

    override val name: String = "SampleDataProvider"

    override val supportedTypes: Set<DesignerDocumentType> = DesignerDocumentType.values().toSet()

    override suspend fun getFields(context: DocumentBindingContext): Map<String, BindingValue> {
        return mapOf(
            // COMPANY
            PlaceholderRegistry.COMPANY_NAME to BindingValue.Text("ViLYNC MedTech Pvt Ltd"),
            PlaceholderRegistry.COMPANY_ADDRESS to BindingValue.Text("Industrial Area, Phase 1, Chandigarh, 160002"),
            PlaceholderRegistry.COMPANY_GSTIN to BindingValue.Text("04AABCV1234A1Z1"),
            PlaceholderRegistry.COMPANY_MOBILE to BindingValue.Text("+91 98765 43210"),

            // IDENTITY
            PlaceholderRegistry.INVOICE_NO to BindingValue.Text("INV/2026/001"),
            PlaceholderRegistry.INVOICE_DATE to BindingValue.Text("07-08-2026"),
            PlaceholderRegistry.SALESMAN to BindingValue.Text("John Doe"),
            PlaceholderRegistry.PAYMENT_MODE to BindingValue.Text("Bank Transfer"),

            // CUSTOMER
            PlaceholderRegistry.CUSTOMER_NAME to BindingValue.Text("Apollo Eye Hospital"),
            PlaceholderRegistry.CUSTOMER_ADDRESS to BindingValue.Text("Sarita Vihar, New Delhi, 110076"),
            PlaceholderRegistry.CUSTOMER_GSTIN to BindingValue.Text("07AAAAA0000A1Z5"),
            PlaceholderRegistry.CUSTOMER_MOBILE to BindingValue.Text("+91 11 2692 5858"),

            // FINANCIALS
            PlaceholderRegistry.SUB_TOTAL to BindingValue.Number(10000.00),
            PlaceholderRegistry.DISCOUNT to BindingValue.Number(500.00),
            PlaceholderRegistry.CGST to BindingValue.Number(855.00),
            PlaceholderRegistry.SGST to BindingValue.Number(855.00),
            PlaceholderRegistry.IGST to BindingValue.Number(0.00),
            PlaceholderRegistry.ROUND_OFF to BindingValue.Number(0.00),
            PlaceholderRegistry.GRAND_TOTAL to BindingValue.Number(11210.00),
            PlaceholderRegistry.AMOUNT_IN_WORDS to BindingValue.Text("Eleven Thousand Two Hundred and Ten Only"),

            // VISUALS
            PlaceholderRegistry.BARCODE to BindingValue.Barcode("INV2026001"),
            PlaceholderRegistry.QR_CODE to BindingValue.QrCode("upi://pay?pa=vilync@upi&am=11210"),

            // ERP INVOICE TABLE (REPEATER)
            "ItemTable" to BindingValue.Table(
                headers = listOf("S.No", "Description", "HSN", "Qty", "Rate", "Amount"),
                rows = listOf(
                    listOf(BindingValue.Text("1"), BindingValue.Text("Spirant Autofocus Pro (IOL)"), BindingValue.Text("9021"), BindingValue.Text("10"), BindingValue.Number(6500.0), BindingValue.Number(65000.0)),
                    listOf(BindingValue.Text("2"), BindingValue.Text("ViscoElastic Solution 2ml"), BindingValue.Text("3004"), BindingValue.Text("5"), BindingValue.Number(450.0), BindingValue.Number(2250.0)),
                    listOf(BindingValue.Text("3"), BindingValue.Text("Disposable Syringe (Box of 100) - 2ml with Needle"), BindingValue.Text("9018"), BindingValue.Text("2"), BindingValue.Number(1200.0), BindingValue.Number(2400.0))
                )
            )
        )
    }
}
