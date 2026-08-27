package com.vilync.ophthalmicerp.core.document.engine.binding

/**
 * Strongly typed registry of all supported ERP document placeholders.
 */
object PlaceholderRegistry {

    // COMPANY INFO
    const val COMPANY_NAME = "CompanyName"
    const val COMPANY_ADDRESS = "CompanyAddress"
    const val COMPANY_GSTIN = "CompanyGSTIN"
    const val COMPANY_MOBILE = "CompanyMobile"
    const val COMPANY_EMAIL = "CompanyEmail"
    const val COMPANY_TERMS = "CompanyTerms"
    const val LOGO = "Logo"

    // INVOICE / IDENTITY
    const val INVOICE_NO = "InvoiceNo"
    const val INVOICE_DATE = "InvoiceDate"
    const val SALESMAN = "Salesman"
    const val PAYMENT_MODE = "PaymentMode"

    // CUSTOMER INFO
    const val CUSTOMER_NAME = "CustomerName"
    const val CUSTOMER_ADDRESS = "CustomerAddress"
    const val CUSTOMER_GSTIN = "CustomerGSTIN"
    const val CUSTOMER_MOBILE = "CustomerMobile"

    // SHIP TO INFO
    const val SHIP_TO_NAME = "ShipToName"
    const val SHIP_TO_ADDRESS = "ShipToAddress"
    const val SHIP_TO_GSTIN = "ShipToGstin"

    // FINANCIALS
    const val SUB_TOTAL = "SubTotal"
    const val DISCOUNT = "Discount"
    const val TAXABLE_AMOUNT = "TaxableAmount"
    const val GST_TOTAL = "GST"
    const val CGST = "CGST"
    const val SGST = "SGST"
    const val IGST = "IGST"
    const val ROUND_OFF = "RoundOff"
    const val GRAND_TOTAL = "GrandTotal"
    const val AMOUNT_IN_WORDS = "AmountInWords"
    const val REFERENCE = "Reference"
    const val ITEM_TABLE = "ItemTable"

    // SUPPLIER INFO (For PO/Returns)
    const val SUPPLIER_DL1 = "SupplierDL1"
    const val SUPPLIER_DL2 = "SupplierDL2"

    // COMPANY REGULATORY
    const val COMPANY_DL1 = "CompanyDL1"
    const val COMPANY_DL2 = "CompanyDL2"
    const val COMPANY_MDL = "CompanyMDL"

    // VISUAL IDENTIFIERS
    const val BARCODE = "Barcode"
    const val QR_CODE = "QRCode"
    const val FOOTER_TEXT = "FooterText"

    /**
     * Returns a set of all registered placeholder IDs.
     */
    fun getAllPlaceholders(): Set<String> = setOf(
        COMPANY_NAME, COMPANY_ADDRESS, COMPANY_GSTIN, COMPANY_MOBILE, COMPANY_EMAIL, COMPANY_TERMS, LOGO,
        COMPANY_DL1, COMPANY_DL2, COMPANY_MDL,
        INVOICE_NO, INVOICE_DATE, SALESMAN, PAYMENT_MODE, REFERENCE,
        CUSTOMER_NAME, CUSTOMER_ADDRESS, CUSTOMER_GSTIN, CUSTOMER_MOBILE,
        SHIP_TO_NAME, SHIP_TO_ADDRESS, SHIP_TO_GSTIN,
        SUPPLIER_DL1, SUPPLIER_DL2,
        SUB_TOTAL, DISCOUNT, TAXABLE_AMOUNT, GST_TOTAL, CGST, SGST, IGST, 
        ROUND_OFF, GRAND_TOTAL, AMOUNT_IN_WORDS, ITEM_TABLE,
        BARCODE, QR_CODE, FOOTER_TEXT
    )

    /**
     * Wraps a key in the standard ERP placeholder syntax.
     */
    fun wrap(key: String): String = "\${$key}"
}
