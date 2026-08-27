package com.vilync.ophthalmicerp.core.document.domain

/**
 * Supported document types for the Universal Document Designer.
 */
enum class DesignerDocumentType {
    SALES_INVOICE,
    DELIVERY_CHALLAN,
    CREDIT_NOTE,
    DEBIT_NOTE,
    PROFORMA_INVOICE,
    SAMPLE_ISSUE,
    PURCHASE_ORDER,
    LABEL_STICKER,
    BARCODE_SLIP,
    QR_CARD,
    GENERIC_REPORT
}

/**
 * Supported output formats for the rendering engine.
 */
enum class OutputType {
    PRINT,
    PDF,
    IMAGE,
    EMAIL_ATTACHMENT,
    WHATSAPP_MEDIA,
    JSON_DATA
}

/**
 * Status of a template or version.
 */
enum class TemplateStatus {
    DRAFT,
    ACTIVE,
    ARCHIVED,
    DELETED
}
