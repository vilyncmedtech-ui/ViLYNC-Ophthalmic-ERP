package com.vilync.ophthalmicerp.data.repository

import com.vilync.ophthalmicerp.data.dao.FinancialTransactionDao
import com.vilync.ophthalmicerp.data.entity.FinancialTransactionEntity
import kotlinx.coroutines.flow.Flow

class FinancialTransactionRepository(
    private val financialTransactionDao: FinancialTransactionDao
) {

    suspend fun insertTransaction(transaction: FinancialTransactionEntity): Long {
        return financialTransactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: FinancialTransactionEntity) {
        financialTransactionDao.updateTransaction(transaction)
    }

    fun getAllTransactions(): Flow<List<FinancialTransactionEntity>> {
        return financialTransactionDao.getAllTransactions()
    }

    suspend fun getTransactionById(id: Long): FinancialTransactionEntity? {
        return financialTransactionDao.getTransactionById(id)
    }

    fun getTransactionsByParty(partyId: Long): Flow<List<FinancialTransactionEntity>> {
        return financialTransactionDao.getTransactionsByParty(partyId)
    }

    fun getTransactionsByAccount(accountId: Long): Flow<List<FinancialTransactionEntity>> {
        return financialTransactionDao.getTransactionsByAccount(accountId)
    }

    suspend fun getAccountBalance(accountId: Long): Double {
        return financialTransactionDao.getAccountBalance(accountId) ?: 0.0
    }

    suspend fun getTotalAmountByPartyAndType(partyId: Long, type: String): Double {
        return financialTransactionDao.getTotalAmountByPartyAndType(partyId, type) ?: 0.0
    }

    suspend fun getTodayTotalByType(type: String, date: String): Double {
        return financialTransactionDao.getTodayTotalByType(type, date) ?: 0.0
    }

    suspend fun getAccountTypeBalance(accountType: String): Double {
        return financialTransactionDao.getAccountTypeBalance(accountType) ?: 0.0
    }
}
