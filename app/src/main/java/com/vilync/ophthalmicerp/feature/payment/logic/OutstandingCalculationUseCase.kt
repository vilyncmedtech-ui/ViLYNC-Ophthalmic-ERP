package com.vilync.ophthalmicerp.feature.payment.logic

import com.vilync.ophthalmicerp.data.repository.FinancialTransactionRepository
import com.vilync.ophthalmicerp.data.repository.PurchaseRepository
import com.vilync.ophthalmicerp.data.repository.SalesRepository
import com.vilync.ophthalmicerp.data.repository.SalesCreditNoteRepository
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class OutstandingCalculationUseCase(
    private val salesRepository: SalesRepository,
    private val purchaseRepository: PurchaseRepository,
    private val salesCreditNoteRepository: SalesCreditNoteRepository,
    private val financialTransactionRepository: FinancialTransactionRepository,
    private val partyRepository: PartyRepository
) {

    suspend fun getCustomerOutstanding(customerId: Long): Double {
        val totalInvoiced = salesRepository.getTotalSaleAmountForCustomer(customerId)
        val totalCreditNotes = salesCreditNoteRepository.getTotalCreditNoteAmountForCustomer(customerId)
        val totalReceived = financialTransactionRepository.getTotalAmountByPartyAndType(customerId, "CUSTOMER_RECEIPT")
        
        return (totalInvoiced - totalCreditNotes - totalReceived).coerceAtLeast(0.0)
    }

    suspend fun getCustomerOverdue(customerId: Long, thresholdDays: Int): Double {
        val today = LocalDate.now()
        val thresholdDate = today.minusDays(thresholdDays.toLong())
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        
        val sales = salesRepository.getSalesByCustomer(customerId).first()
            .filter { it.status == "POSTED" }
        
        val overdueInvoiced = sales.filter { 
            runCatching { LocalDate.parse(it.invoiceDate, formatter).isBefore(thresholdDate) }.getOrDefault(false)
        }.sumOf { it.totalAmount }
        
        val totalCredits = salesCreditNoteRepository.getTotalCreditNoteAmountForCustomer(customerId)
        val totalReceived = financialTransactionRepository.getTotalAmountByPartyAndType(customerId, "CUSTOMER_RECEIPT")
        
        return (overdueInvoiced - totalCredits - totalReceived).coerceAtLeast(0.0)
    }

    suspend fun getSupplierOutstanding(supplierId: Long): Double {
        val totalBilled = purchaseRepository.getTotalPurchaseAmountForSupplier(supplierId)
        val totalPaid = financialTransactionRepository.getTotalAmountByPartyAndType(supplierId, "SUPPLIER_PAYMENT")
        
        return (totalBilled - totalPaid).coerceAtLeast(0.0)
    }

    suspend fun getSupplierOverdue(supplierId: Long, thresholdDays: Int): Double {
        val today = LocalDate.now()
        val thresholdDate = today.minusDays(thresholdDays.toLong())
        val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        
        val party = partyRepository.getPartyById(supplierId) ?: return 0.0
        val purchases = purchaseRepository.getPurchasesBySupplier(party.partyName).first()
        
        val overdueBilled = purchases.filter {
            runCatching { LocalDate.parse(it.invoiceDate, formatter).isBefore(thresholdDate) }.getOrDefault(false)
        }.sumOf { it.grandTotal }
        
        val totalPaid = financialTransactionRepository.getTotalAmountByPartyAndType(supplierId, "SUPPLIER_PAYMENT")
        
        return (overdueBilled - totalPaid).coerceAtLeast(0.0)
    }
}
