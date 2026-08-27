package com.vilync.ophthalmicerp.feature.designer.domain

import com.vilync.ophthalmicerp.core.document.domain.*
import com.vilync.ophthalmicerp.core.document.template.DocumentTemplateRepository
import com.vilync.ophthalmicerp.core.document.engine.binding.PlaceholderRegistry
import java.util.UUID

/**
 * Idempotent bootstrap for the professional B2B Purchase Order template.
 */
object PurchaseOrderTemplateBootstrap {

    private const val TARGET_VERSION = 5 // Reverting to last known stable baseline

    suspend fun bootstrap(repository: DocumentTemplateRepository) {
        val existing = repository.getTemplatesByContext(DesignerDocumentType.PURCHASE_ORDER, 0, 0)
        
        if (existing.isNotEmpty()) {
            val current = existing.first()
            if (current.activeVersion >= TARGET_VERSION) return
            
            // Perform clean upgrade for development phase
            repository.deleteTemplate(current.templateId)
        }

        val templateId = UUID.randomUUID().toString()
        val template = DocumentTemplate(
            templateId = templateId,
            templateName = "Professional Purchase Order",
            documentType = DesignerDocumentType.PURCHASE_ORDER,
            description = "Canonical B2B Purchase Order for Manufacturer / Supplier.",
            activeVersion = TARGET_VERSION,
            status = TemplateStatus.ACTIVE,
            isDefault = true
        )

        val layout = createDefaultLayout()
        repository.saveTemplate(template, layout)
        repository.setDefaultTemplate(templateId, true)
    }

    private fun createDefaultLayout(): TemplateLayout {
        val page = TemplatePage(
            index = 0,
            width = 210f, // A4
            height = 297f,
            objects = listOf(
                // 1. Header Box
                LayoutObject(
                    id = "HeaderBox", type = "RECTANGLE", x = 10f, y = 10f, width = 190f, height = 32f,
                    styles = mapOf("fillColor" to "#14233C")
                ),
                LayoutObject(
                    id = "CompanyLogo", type = "IMAGE", x = 15f, y = 14f, width = 24f, height = 24f,
                    bindings = mapOf("sourceUri" to PlaceholderRegistry.LOGO)
                ),
                LayoutObject(
                    id = "CompanyName", type = "TEXT", x = 45f, y = 14f, width = 100f, height = 8f,
                    properties = mapOf("text" to PlaceholderRegistry.wrap(PlaceholderRegistry.COMPANY_NAME)),
                    styles = mapOf("fontSize" to "14", "fontStyle" to "BOLD", "color" to "#FFFFFF")
                ),
                LayoutObject(
                    id = "CompanyAddress", type = "TEXT", x = 45f, y = 23f, width = 95f, height = 15f,
                    properties = mapOf("text" to PlaceholderRegistry.wrap(PlaceholderRegistry.COMPANY_ADDRESS)),
                    styles = mapOf("fontSize" to "8", "color" to "#CCCCCC")
                ),
                LayoutObject(
                    id = "DocTitle", type = "TEXT", x = 140f, y = 14f, width = 55f, height = 10f,
                    properties = mapOf("text" to "PURCHASE ORDER"),
                    styles = mapOf("fontSize" to "18", "fontStyle" to "BOLD", "color" to "#D4AF37", "horizontalAlignment" to "RIGHT")
                ),

                // 2. Info Grid
                LayoutObject(
                    id = "OrderToTitle", type = "TEXT", x = 10f, y = 50f, width = 90f, height = 5f,
                    properties = mapOf("text" to "ORDER TO:"),
                    styles = mapOf("fontSize" to "9", "fontStyle" to "BOLD", "color" to "#14233C")
                ),
                LayoutObject(
                    id = "VendorName", type = "TEXT", x = 10f, y = 57f, width = 90f, height = 6f,
                    properties = mapOf("text" to PlaceholderRegistry.wrap(PlaceholderRegistry.CUSTOMER_NAME)),
                    styles = mapOf("fontSize" to "11", "fontStyle" to "BOLD")
                ),
                LayoutObject(
                    id = "VendorAddress", type = "TEXT", x = 10f, y = 64f, width = 90f, height = 15f,
                    properties = mapOf("text" to PlaceholderRegistry.wrap(PlaceholderRegistry.CUSTOMER_ADDRESS)),
                    styles = mapOf("fontSize" to "9")
                ),
                LayoutObject(
                    id = "VendorGSTIN", type = "TEXT", x = 10f, y = 80f, width = 90f, height = 5f,
                    properties = mapOf("text" to "GSTIN: " + PlaceholderRegistry.wrap(PlaceholderRegistry.CUSTOMER_GSTIN)),
                    styles = mapOf("fontSize" to "9")
                ),

                LayoutObject(
                    id = "OrderDetailsTitle", type = "TEXT", x = 110f, y = 50f, width = 90f, height = 5f,
                    properties = mapOf("text" to "ORDER DETAILS:"),
                    styles = mapOf("fontSize" to "9", "fontStyle" to "BOLD", "color" to "#14233C")
                ),
                LayoutObject(
                    id = "PONoLabel", type = "TEXT", x = 110f, y = 57f, width = 30f, height = 5f,
                    properties = mapOf("text" to "PO Number:"),
                    styles = mapOf("fontSize" to "9", "color" to "#666666")
                ),
                LayoutObject(
                    id = "PONoValue", type = "TEXT", x = 140f, y = 57f, width = 60f, height = 5f,
                    properties = mapOf("text" to PlaceholderRegistry.wrap(PlaceholderRegistry.INVOICE_NO)),
                    styles = mapOf("fontSize" to "9", "fontStyle" to "BOLD")
                ),
                LayoutObject(
                    id = "DateLabel", type = "TEXT", x = 110f, y = 64f, width = 30f, height = 5f,
                    properties = mapOf("text" to "Order Date:"),
                    styles = mapOf("fontSize" to "9", "color" to "#666666")
                ),
                LayoutObject(
                    id = "DateValue", type = "TEXT", x = 140f, y = 64f, width = 60f, height = 5f,
                    properties = mapOf("text" to PlaceholderRegistry.wrap(PlaceholderRegistry.INVOICE_DATE)),
                    styles = mapOf("fontSize" to "9", "fontStyle" to "BOLD")
                ),

                // 3. Item Table
                LayoutObject(
                    id = "ItemTable", type = "INVOICE_TABLE", x = 10f, y = 95f, width = 190f, height = 120f,
                    bindings = mapOf("data" to PlaceholderRegistry.ITEM_TABLE),
                    styles = mapOf("fontSize" to "9", "borderWidth" to "0.2")
                ),

                // 4. Summary
                LayoutObject(
                    id = "TotalLabel", type = "TEXT", x = 140f, y = 230f, width = 30f, height = 6f,
                    properties = mapOf("text" to "Grand Total:"),
                    styles = mapOf("fontSize" to "12", "fontStyle" to "BOLD", "horizontalAlignment" to "RIGHT")
                ),
                LayoutObject(
                    id = "TotalValue", type = "TEXT", x = 170f, y = 230f, width = 30f, height = 6f,
                    properties = mapOf("text" to "₹ " + PlaceholderRegistry.wrap(PlaceholderRegistry.GRAND_TOTAL)),
                    styles = mapOf("fontSize" to "13", "fontStyle" to "BOLD", "horizontalAlignment" to "RIGHT", "color" to "#345FA8")
                ),

                // 5. Remarks
                LayoutObject(
                    id = "RemarksLabel", type = "TEXT", x = 10f, y = 230f, width = 30f, height = 5f,
                    properties = mapOf("text" to "REMARKS:"),
                    styles = mapOf("fontSize" to "8", "fontStyle" to "BOLD", "color" to "#666666")
                ),
                LayoutObject(
                    id = "RemarksValue", type = "TEXT", x = 10f, y = 236f, width = 120f, height = 10f,
                    properties = mapOf("text" to PlaceholderRegistry.wrap(PlaceholderRegistry.REFERENCE)),
                    styles = mapOf("fontSize" to "9")
                )
            )
        )
        return TemplateLayout(pages = listOf(page))
    }
}
