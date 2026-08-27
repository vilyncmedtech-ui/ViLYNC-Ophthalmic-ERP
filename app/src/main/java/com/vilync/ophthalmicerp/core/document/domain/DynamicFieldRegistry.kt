package com.vilync.ophthalmicerp.core.document.domain

/**
 * Registry for dynamic fields available in templates.
 */
object DynamicFieldRegistry {
    private val fields = mutableMapOf<String, DynamicFieldDefinition>()

    init {
        // Core ERP Fields
        register("CustomerName", "Customer / Hospital Name")
        register("CustomerAddress", "Customer Full Address")
        register("InvoiceNo", "Invoice / Document Number")
        register("InvoiceDate", "Invoice / Document Date")
        register("GSTIN", "Taxpayer GSTIN")
        register("Logo", "Company Branding Logo")
        register("DoctorName", "Attending Doctor")
        register("PatientName", "Patient Name")
        register("HospitalName", "Hospital Name")
        register("GrandTotal", "Total Payable Amount")
        register("AmountInWords", "Grand Total in Words")
        register("Reference", "Document Reference / Remarks")
        register("ItemTable", "Main Transaction Table")

        // Supplier specific
        register("SupplierDL1", "Supplier Drug Licence 1")
        register("SupplierDL2", "Supplier Drug Licence 2")

        // Company specific
        register("CompanyDL1", "Own Drug Licence 1")
        register("CompanyDL2", "Own Drug Licence 2")
        register("CompanyMDL", "Own Medical Device Licence")

        // Barcode & QR Placeholders
        register("InvoiceBarcode", "Barcode for Invoice Number")
        register("InvoiceQRCode", "QR Code for Invoice Verification")
        register("SerialBarcode", "Barcode for Item Serial Number")
        register("BatchBarcode", "Barcode for Batch/Lot Number")
        register("CustomerBarcode", "Barcode for Customer ID")
        register("DoctorBarcode", "Barcode for Doctor ID")
        register("HospitalBarcode", "Barcode for Hospital ID")
        register("UPIQRCode", "QR Code for UPI Payment")
        register("PaymentQRCode", "Generic Payment QR Code")
    }

    fun register(id: String, description: String) {
        fields[id] = DynamicFieldDefinition(id, description)
    }

    fun getAll(): List<DynamicFieldDefinition> = fields.values.toList()
}

data class DynamicFieldDefinition(
    val id: String,
    val description: String
)
