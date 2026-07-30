package com.vilync.ophthalmicerp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vilync.ophthalmicerp.data.entity.AuditTrailEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface AuditTrailDao {


    // =========================================================
    // INSERT AUDIT EVENT
    // =========================================================

    /**
     * Audit records are append-only.
     *
     * Normal ERP operations should create new audit records
     * instead of modifying existing audit history.
     */
    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertAuditTrail(
        auditTrail: AuditTrailEntity
    ): Long


    // =========================================================
    // ALL AUDIT TRAIL
    // =========================================================

    /**
     * Latest activity first.
     */
    @Query(
        """
        SELECT *
        FROM audit_trail
        ORDER BY createdAt DESC, id DESC
        """
    )
    fun getAllAuditTrail():
            Flow<List<AuditTrailEntity>>


    // =========================================================
    // AUDIT BY ID
    // =========================================================

    @Query(
        """
        SELECT *
        FROM audit_trail
        WHERE id = :auditId
        LIMIT 1
        """
    )
    suspend fun getAuditTrailById(
        auditId: Long
    ): AuditTrailEntity?


    // =========================================================
    // MODULE-WISE AUDIT
    // =========================================================

    /**
     * Examples:
     *
     * PURCHASE
     * SALES
     * PURCHASE_RETURN
     * PRODUCT_MASTER
     */
    @Query(
        """
        SELECT *
        FROM audit_trail
        WHERE module = :module
        ORDER BY createdAt DESC, id DESC
        """
    )
    fun getAuditTrailByModule(
        module: String
    ): Flow<List<AuditTrailEntity>>


    // =========================================================
    // ACTION-WISE AUDIT
    // =========================================================

    /**
     * Examples:
     *
     * CREATE
     * UPDATE
     * DELETE
     * RESTORE
     * LOGIN
     * LOGOUT
     */
    @Query(
        """
        SELECT *
        FROM audit_trail
        WHERE action = :action
        ORDER BY createdAt DESC, id DESC
        """
    )
    fun getAuditTrailByAction(
        action: String
    ): Flow<List<AuditTrailEntity>>


    // =========================================================
    // USER-WISE AUDIT
    // =========================================================

    @Query(
        """
        SELECT *
        FROM audit_trail
        WHERE userId = :userId
        ORDER BY createdAt DESC, id DESC
        """
    )
    fun getAuditTrailByUser(
        userId: Long
    ): Flow<List<AuditTrailEntity>>


    // =========================================================
    // FINANCIAL YEAR-WISE AUDIT
    // =========================================================

    @Query(
        """
        SELECT *
        FROM audit_trail
        WHERE financialYear = :financialYear
        ORDER BY createdAt DESC, id DESC
        """
    )
    fun getAuditTrailByFinancialYear(
        financialYear: String
    ): Flow<List<AuditTrailEntity>>


    // =========================================================
    // RECORD-WISE AUDIT HISTORY
    // =========================================================

    /**
     * Gives the complete history of a particular ERP record.
     *
     * Example:
     *
     * module = PURCHASE
     * recordId = 15
     *
     * This can show:
     *
     * CREATE
     * UPDATE
     * DELETE
     * RESTORE
     */
    @Query(
        """
        SELECT *
        FROM audit_trail
        WHERE module = :module
          AND recordId = :recordId
        ORDER BY createdAt DESC, id DESC
        """
    )
    fun getAuditTrailForRecord(
        module: String,
        recordId: Long
    ): Flow<List<AuditTrailEntity>>


    // =========================================================
    // REFERENCE NUMBER
    // =========================================================

    /**
     * Useful for invoice / debit note / credit note history.
     */
    @Query(
        """
        SELECT *
        FROM audit_trail
        WHERE referenceNumber = :referenceNumber
        ORDER BY createdAt DESC, id DESC
        """
    )
    fun getAuditTrailByReferenceNumber(
        referenceNumber: String
    ): Flow<List<AuditTrailEntity>>


    // =========================================================
    // SEARCH AUDIT TRAIL
    // =========================================================

    /**
     * General search for Settings -> Audit Trail.
     *
     * Searches:
     *
     * Module
     * Action
     * Reference Number
     * Username
     * Display Name
     * Description
     */
    @Query(
        """
        SELECT *
        FROM audit_trail
        WHERE module LIKE '%' || :query || '%'
           OR action LIKE '%' || :query || '%'
           OR COALESCE(referenceNumber, '') LIKE '%' || :query || '%'
           OR COALESCE(username, '') LIKE '%' || :query || '%'
           OR COALESCE(userDisplayName, '') LIKE '%' || :query || '%'
           OR COALESCE(description, '') LIKE '%' || :query || '%'
        ORDER BY createdAt DESC, id DESC
        """
    )
    fun searchAuditTrail(
        query: String
    ): Flow<List<AuditTrailEntity>>
}