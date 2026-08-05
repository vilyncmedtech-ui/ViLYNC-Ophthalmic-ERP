package com.vilync.ophthalmicerp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vilync.ophthalmicerp.data.entity.FinancialTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialTransactionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: FinancialTransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: FinancialTransactionEntity)

    @Query("SELECT * FROM financial_transactions ORDER BY id DESC")
    fun getAllTransactions(): Flow<List<FinancialTransactionEntity>>

    @Query("SELECT * FROM financial_transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): FinancialTransactionEntity?

    @Query("SELECT * FROM financial_transactions WHERE partyId = :partyId ORDER BY id DESC")
    fun getTransactionsByParty(partyId: Long): Flow<List<FinancialTransactionEntity>>

    @Query("SELECT * FROM financial_transactions WHERE accountId = :accountId ORDER BY id DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<FinancialTransactionEntity>>

    @Query("SELECT SUM(amount) FROM financial_transactions WHERE accountId = :accountId AND status = 'POSTED'")
    suspend fun getAccountBalance(accountId: Long): Double?

    @Query("SELECT SUM(amount) FROM financial_transactions WHERE partyId = :partyId AND type = :type AND status = 'POSTED'")
    suspend fun getTotalAmountByPartyAndType(partyId: Long, type: String): Double?

    @Query("SELECT SUM(amount) FROM financial_transactions WHERE type = :type AND transactionDate = :date AND status = 'POSTED'")
    suspend fun getTodayTotalByType(type: String, date: String): Double?

    @Query("""
        SELECT SUM(CASE 
            WHEN ft.type IN ('CUSTOMER_RECEIPT', 'OPENING_BALANCE') THEN ft.amount 
            WHEN ft.type = 'SUPPLIER_PAYMENT' THEN -ft.amount 
            ELSE 0 
        END) 
        FROM financial_transactions ft
        INNER JOIN accounts a ON ft.accountId = a.id
        WHERE a.type = :accountType AND ft.status = 'POSTED'
    """)
    suspend fun getAccountTypeBalance(accountType: String): Double?

    // =========================================================
    // PERIOD REPORTING QUERIES
    // =========================================================

    @Query("""
        SELECT SUM(amount) FROM financial_transactions 
        WHERE partyId = :partyId AND type = :type AND status = 'POSTED'
          AND (substr(transactionDate, 7, 4) || '-' || substr(transactionDate, 4, 2) || '-' || substr(transactionDate, 1, 2)) < :startDate
    """)
    suspend fun getOpeningBalanceByPartyAndType(partyId: Long, type: String, startDate: String): Double?

    @Query("""
        SELECT * FROM financial_transactions 
        WHERE partyId = :partyId AND status = 'POSTED'
          AND (substr(transactionDate, 7, 4) || '-' || substr(transactionDate, 4, 2) || '-' || substr(transactionDate, 1, 2)) BETWEEN :startDate AND :endDate
    """)
    suspend fun getTransactionsForPartyPeriod(partyId: Long, startDate: String, endDate: String): List<FinancialTransactionEntity>

    @Query("""
        SELECT SUM(CASE WHEN type = 'CUSTOMER_RECEIPT' THEN amount ELSE -amount END) 
        FROM financial_transactions 
        WHERE accountId = :accountId AND status = 'POSTED'
          AND (substr(transactionDate, 7, 4) || '-' || substr(transactionDate, 4, 2) || '-' || substr(transactionDate, 1, 2)) < :startDate
    """)
    suspend fun getAccountOpeningBalance(accountId: Long, startDate: String): Double?

    @Query("""
        SELECT * FROM financial_transactions 
        WHERE accountId = :accountId AND status = 'POSTED'
          AND (substr(transactionDate, 7, 4) || '-' || substr(transactionDate, 4, 2) || '-' || substr(transactionDate, 1, 2)) BETWEEN :startDate AND :endDate
    """)
    suspend fun getAccountTransactionsForPeriod(accountId: Long, startDate: String, endDate: String): List<FinancialTransactionEntity>
}
