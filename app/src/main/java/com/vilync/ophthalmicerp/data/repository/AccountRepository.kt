package com.vilync.ophthalmicerp.data.repository

import com.vilync.ophthalmicerp.data.dao.AccountDao
import com.vilync.ophthalmicerp.data.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

class AccountRepository(
    private val accountDao: AccountDao
) {

    suspend fun insertAccount(account: AccountEntity): Long {
        return accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
    }

    fun getAllActiveAccounts(): Flow<List<AccountEntity>> {
        return accountDao.getAllActiveAccounts()
    }

    suspend fun getAccountById(id: Long): AccountEntity? {
        return accountDao.getAccountById(id)
    }

    fun getAccountsByType(type: String): Flow<List<AccountEntity>> {
        return accountDao.getAccountsByType(type)
    }

    suspend fun initializeDefaultAccounts() {
        if (!accountDao.accountExistsByName("Main Cash")) {
            insertAccount(
                AccountEntity(
                    name = "Main Cash",
                    type = "CASH",
                    initialBalance = 0.0,
                    isActive = true
                )
            )
        }
        if (!accountDao.accountExistsByName("Main Bank Account")) {
            insertAccount(
                AccountEntity(
                    name = "Main Bank Account",
                    type = "BANK",
                    initialBalance = 0.0,
                    isActive = true
                )
            )
        }
    }
}
