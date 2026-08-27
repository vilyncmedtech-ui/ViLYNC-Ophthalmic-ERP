package com.vilync.ophthalmicerp.feature.gst.model

data class GstReportSnapshot(
    val financialYearStart: Int,
    val sales: List<GstDocumentRow>,
    val purchases: List<GstDocumentRow>,
    val creditNotes: List<GstDocumentRow>,
    val hsnRows: List<GstHsnRow>
) {
    val b2bSales: List<GstDocumentRow>
        get() = sales.filter { it.gstin.isNotBlank() }

    val b2cSales: List<GstDocumentRow>
        get() = sales.filter { it.gstin.isBlank() }

    val outputTaxable: Double
        get() = sales.sumOf { it.taxableAmount }

    val outputCgst: Double
        get() = sales.sumOf { it.cgstAmount }

    val outputSgst: Double
        get() = sales.sumOf { it.sgstAmount }

    val outputIgst: Double
        get() = sales.sumOf { it.igstAmount }

    val outputGst: Double
        get() = outputCgst + outputSgst + outputIgst

    val inputTaxable: Double
        get() = purchases.sumOf { it.taxableAmount }

    val inputGst: Double
        get() = purchases.sumOf { it.totalGst }

    val creditNoteTaxable: Double
        get() = creditNotes.sumOf { it.taxableAmount }

    val creditNoteGst: Double
        get() = creditNotes.sumOf { it.totalGst }

    val adjustedOutputGst: Double
        get() = outputGst - creditNoteGst

    val netGstPosition: Double
        get() = adjustedOutputGst - inputGst
}

data class GstDocumentRow(
    val id: Long,
    val documentNumber: String,
    val documentDate: String,
    val partyName: String,
    val gstin: String,
    val taxableAmount: Double,
    val cgstAmount: Double,
    val sgstAmount: Double,
    val igstAmount: Double,
    val totalAmount: Double,
    val status: String = ""
) {
    val totalGst: Double
        get() = cgstAmount + sgstAmount + igstAmount
}

data class GstHsnRow(
    val hsnCode: String,
    val description: String,
    val quantity: Int,
    val taxableAmount: Double,
    val gstPercent: Double,
    val gstAmount: Double,
    val totalAmount: Double
)

enum class GstReportType(
    val routeKey: String,
    val title: String
) {
    GSTR1("gstr1", "GSTR-1"),
    GSTR3B("gstr3b", "GSTR-3B Working"),
    SALES_REGISTER("sales_register", "Sales GST Register"),
    PURCHASE_REGISTER("purchase_register", "Purchase GST Register"),
    HSN("hsn", "HSN Summary"),
    B2B("b2b", "B2B Sales"),
    B2C("b2c", "B2C Sales"),
    RETURNS("returns", "Returns / Adjustments"),
    ITC("itc", "ITC Summary"),
    OUTPUT_TAX("output_tax", "Output Tax Liability"),
    TAX_HEADS("tax_heads", "CGST / SGST / IGST Summary");

    companion object {
        fun fromRouteKey(value: String): GstReportType =
            entries.firstOrNull { it.routeKey == value } ?: GSTR1
    }
}