package com.vilync.ophthalmicerp.feature.dashboard

import com.vilync.ophthalmicerp.data.database.AppDatabase
import kotlinx.coroutines.flow.first

data class SearchResult(
    val id: Long,
    val title: String,
    val subtitle: String,
    val category: String,
    val route: String
)

class GlobalSearchUseCase(
    private val database: AppDatabase
) {
    suspend fun execute(query: String): List<SearchResult> {
        val q = query.trim()
        if (q.length < 2) return emptyList()

        val results = mutableListOf<SearchResult>()

        // 1. Products
        val products = database.productDao().getAllProducts().first()
            .filter { it.productName.contains(q, ignoreCase = true) || it.hsnCode.contains(q, ignoreCase = true) }
        results.addAll(products.take(5).map {
            SearchResult(it.id, it.productName, "HSN: ${it.hsnCode} • ${it.brandName}", "Products", "product_master/${it.id}")
        })

        // 2. Serials
        val serials = database.inventoryDao().findBySerialSuffix(q)
        results.addAll(serials.take(5).map {
            SearchResult(it.id, it.serialNumber, "Status: ${it.status} • Power: ${it.power}", "Serials", "serial_movement_history/${it.id}")
        })

        // 3. Parties (Customers/Suppliers)
        val parties = database.partyDao().searchParties(q).first()
        results.addAll(parties.take(5).map {
            SearchResult(it.id, it.partyName, "${it.partyType} • ${it.city}", "Parties", "party_master/${it.id}")
        })

        // 4. Sales Invoices
        val sales = database.salesDao().searchSalesList(q)
        results.addAll(sales.take(5).map {
            SearchResult(it.id, it.invoiceNumber, "${it.customerName} • ${it.invoiceDate}", "Sales Invoices", "sales_invoice_detail/${it.id}")
        })

        // 5. Purchases
        val purchases = database.purchaseDao().searchPurchasesList(q)
        results.addAll(purchases.take(5).map {
            SearchResult(it.id, it.invoiceNumber, "${it.supplierName} • ${it.invoiceDate}", "Purchases", "purchase_detail/${it.id}")
        })

        // 6. Challans
        val challans = database.challanDao().searchChallans(q)
        results.addAll(challans.take(5).map {
            SearchResult(it.id, it.challanNumber, "${it.customerName} • ${it.challanDate}", "Challans", "sales_pending_module/Challan Detail/Challan detail view is coming soon.")
        })

        // 7. Credit Notes
        val cns = database.salesCreditNoteDao().searchCreditNotes(q)
        results.addAll(cns.take(5).map {
            SearchResult(it.id, it.creditNoteNumber, "${it.customerName} • ${it.creditNoteDate}", "Credit Notes", "sales_pending_module/Credit Note Detail/Credit Note detail view is coming soon.")
        })

        // 8. Proforma
        val proformas = database.proformaInvoiceDao().searchProforma(q)
        results.addAll(proformas.take(5).map {
            SearchResult(it.id, it.proformaNumber, "${it.customerName} • ${it.proformaDate}", "Proforma Invoices", "sales_pending_module/Proforma Detail/Proforma detail view is coming soon.")
        })

        // 9. Samples
        val samples = database.sampleIssueDao().searchSamples(q)
        results.addAll(samples.take(5).map {
            SearchResult(it.id, it.sampleIssueNumber, "${it.customerName} • ${it.sampleIssueDate}", "Sample Issues", "sales_pending_module/Sample Detail/Sample detail view is coming soon.")
        })

        return results
    }
}
