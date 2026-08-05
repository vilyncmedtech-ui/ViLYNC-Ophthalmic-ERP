package com.vilync.ophthalmicerp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vilync.ophthalmicerp.data.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY name COLLATE NOCASE ASC")
    fun getAllActiveAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE type = :type AND isActive = 1 ORDER BY name COLLATE NOCASE ASC")
    fun getAccountsByType(type: String): Flow<List<AccountEntity>>

    @Query("SELECT COUNT(*) FROM accounts WHERE type = :type")
    suspend fun countAccountsByType(type: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM accounts WHERE name = :name)")
    suspend fun accountExistsByName(name: String): Boolean
}
