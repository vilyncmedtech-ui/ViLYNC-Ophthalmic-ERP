package com.vilync.ophthalmicerp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteEntity
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteItemEntity
import com.vilync.ophthalmicerp.data.entity.SalesCreditNoteLensEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesCreditNoteDao {

    // =========================================================
    // CREATE
    // =========================================================

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCreditNote(
        creditNote: SalesCreditNoteEntity
    ): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCreditNoteItems(
        items: List<SalesCreditNoteItemEntity>
    ): List<Long>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCreditNoteLenses(
        lenses: List<SalesCreditNoteLensEntity>
    )

    // =========================================================
    // UPDATE
    // =========================================================

    @Update
    suspend fun updateCreditNote(
        creditNote: SalesCreditNoteEntity
    )

    // =========================================================
    // DOCUMENT / REGISTER
    // =========================================================

    @Query(
        """
        SELECT * FROM sales_credit_notes
        WHERE id = :creditNoteId
        LIMIT 1
        """
    )
    suspend fun getCreditNoteById(
        creditNoteId: Long
    ): SalesCreditNoteEntity?

    @Query(
        """
        SELECT * FROM sales_credit_notes
        ORDER BY id DESC
        """
    )
    fun getAllCreditNotes(): Flow<List<SalesCreditNoteEntity>>

    @Query(
        """
        SELECT * FROM sales_credit_notes
        WHERE financialYearStart = :financialYearStart
        ORDER BY id DESC
        """
    )
    fun getCreditNotesByFinancialYear(
        financialYearStart: Int
    ): Flow<List<SalesCreditNoteEntity>>

    @Query(
        """
        SELECT * FROM sales_credit_notes
        WHERE customerId = :customerId
        ORDER BY id DESC
        """
    )
    fun getCreditNotesForCustomer(
        customerId: Long
    ): Flow<List<SalesCreditNoteEntity>>

    // =========================================================
    // ITEMS / PHYSICAL LENSES
    // =========================================================

    @Query(
        """
        SELECT * FROM sales_credit_note_items
        WHERE creditNoteId = :creditNoteId
        ORDER BY id ASC
        """
    )
    suspend fun getItemsByCreditNoteId(
        creditNoteId: Long
    ): List<SalesCreditNoteItemEntity>

    @Query(
        """
        SELECT l.*
        FROM sales_credit_note_lenses l
        INNER JOIN sales_credit_note_items i
            ON i.id = l.creditNoteItemId
        WHERE i.creditNoteId = :creditNoteId
        ORDER BY l.id ASC
        """
    )
    suspend fun getLensesByCreditNoteId(
        creditNoteId: Long
    ): List<SalesCreditNoteLensEntity>

    // =========================================================
    // DUPLICATE NUMBER PROTECTION
    // =========================================================

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM sales_credit_notes
            WHERE normalizedCreditNoteNumber = :normalizedCreditNoteNumber
              AND financialYearStart = :financialYearStart
        )
        """
    )
    suspend fun creditNoteNumberExists(
        normalizedCreditNoteNumber: String,
        financialYearStart: Int
    ): Boolean

    // =========================================================
    // RETURN PROTECTION
    // =========================================================
    //
    // Prevents the same physical sold lens from being returned
    // to stock more than once through active Credit Notes.
    // =========================================================

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM sales_credit_note_lenses l
            INNER JOIN sales_credit_note_items i
                ON i.id = l.creditNoteItemId
            INNER JOIN sales_credit_notes c
                ON c.id = i.creditNoteId
            WHERE l.inventoryUnitId = :inventoryUnitId
              AND l.returnToStock = 1
              AND c.status != 'CANCELLED'
        )
        """
    )
    suspend fun physicalReturnAlreadyExists(
        inventoryUnitId: Long
    ): Boolean
}
