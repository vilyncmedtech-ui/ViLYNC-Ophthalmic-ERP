package com.vilync.ophthalmicerp.data.repository

import com.vilync.ophthalmicerp.core.security.SessionManager
import com.vilync.ophthalmicerp.data.dao.AuditTrailDao
import com.vilync.ophthalmicerp.data.entity.AuditTrailEntity
import kotlinx.coroutines.flow.Flow


class AuditTrailRepository(
    private val auditTrailDao: AuditTrailDao
) {


    // =========================================================
    // RECORD AUDIT EVENT
    // =========================================================

    /**
     * Creates a central ERP audit record.
     *
     * Logged-in user information is automatically taken from
     * SessionManager so individual modules do not need to pass
     * userId / username / role manually.
     */
    suspend fun recordEvent(
        module: String,
        action: String,
        recordId: Long? = null,
        referenceNumber: String? = null,
        financialYear: String? = null,
        description: String? = null,
        fieldName: String? = null,
        oldValue: String? = null,
        newValue: String? = null
    ): Long {

        val currentUser =
            SessionManager.currentUser.value


        val auditTrail =
            AuditTrailEntity(

                module =
                    module.trim().uppercase(),

                action =
                    action.trim().uppercase(),

                recordId =
                    recordId,

                referenceNumber =
                    referenceNumber
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank()
                        },

                userId =
                    currentUser?.id,

                username =
                    currentUser?.username,

                userDisplayName =
                    currentUser?.displayName,

                userRole =
                    currentUser?.role,

                financialYear =
                    financialYear
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank()
                        },

                description =
                    description
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank()
                        },

                fieldName =
                    fieldName
                        ?.trim()
                        ?.takeIf {
                            it.isNotBlank()
                        },

                oldValue =
                    oldValue,

                newValue =
                    newValue
            )


        return auditTrailDao.insertAuditTrail(
            auditTrail
        )
    }


    // =========================================================
    // DIRECT INSERT
    // =========================================================

    /**
     * Primarily useful for system-generated events where the
     * complete AuditTrailEntity is intentionally constructed
     * outside this repository.
     */
    suspend fun insertAuditTrail(
        auditTrail: AuditTrailEntity
    ): Long {

        return auditTrailDao.insertAuditTrail(
            auditTrail
        )
    }


    // =========================================================
    // ALL AUDIT TRAIL
    // =========================================================

    fun getAllAuditTrail():
            Flow<List<AuditTrailEntity>> =

        auditTrailDao.getAllAuditTrail()


    // =========================================================
    // AUDIT BY ID
    // =========================================================

    suspend fun getAuditTrailById(
        auditId: Long
    ): AuditTrailEntity? =

        auditTrailDao.getAuditTrailById(
            auditId
        )


    // =========================================================
    // MODULE
    // =========================================================

    fun getAuditTrailByModule(
        module: String
    ): Flow<List<AuditTrailEntity>> =

        auditTrailDao.getAuditTrailByModule(
            module.trim().uppercase()
        )


    // =========================================================
    // ACTION
    // =========================================================

    fun getAuditTrailByAction(
        action: String
    ): Flow<List<AuditTrailEntity>> =

        auditTrailDao.getAuditTrailByAction(
            action.trim().uppercase()
        )


    // =========================================================
    // USER
    // =========================================================

    fun getAuditTrailByUser(
        userId: Long
    ): Flow<List<AuditTrailEntity>> =

        auditTrailDao.getAuditTrailByUser(
            userId
        )


    // =========================================================
    // FINANCIAL YEAR
    // =========================================================

    fun getAuditTrailByFinancialYear(
        financialYear: String
    ): Flow<List<AuditTrailEntity>> =

        auditTrailDao.getAuditTrailByFinancialYear(
            financialYear.trim()
        )


    // =========================================================
    // RECORD HISTORY
    // =========================================================

    fun getAuditTrailForRecord(
        module: String,
        recordId: Long
    ): Flow<List<AuditTrailEntity>> =

        auditTrailDao.getAuditTrailForRecord(
            module =
                module.trim().uppercase(),

            recordId =
                recordId
        )


    // =========================================================
    // REFERENCE NUMBER
    // =========================================================

    fun getAuditTrailByReferenceNumber(
        referenceNumber: String
    ): Flow<List<AuditTrailEntity>> =

        auditTrailDao
            .getAuditTrailByReferenceNumber(
                referenceNumber.trim()
            )


    // =========================================================
    // SEARCH
    // =========================================================

    fun searchAuditTrail(
        query: String
    ): Flow<List<AuditTrailEntity>> {

        val normalizedQuery =
            query.trim()


        return if (
            normalizedQuery.isBlank()
        ) {

            auditTrailDao
                .getAllAuditTrail()

        } else {

            auditTrailDao
                .searchAuditTrail(
                    normalizedQuery
                )
        }
    }
}