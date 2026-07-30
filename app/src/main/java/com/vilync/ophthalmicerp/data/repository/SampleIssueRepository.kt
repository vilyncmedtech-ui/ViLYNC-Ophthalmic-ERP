package com.vilync.ophthalmicerp.data.repository

import com.vilync.ophthalmicerp.data.dao.SampleIssueDao
import com.vilync.ophthalmicerp.data.entity.SampleIssueEntity
import com.vilync.ophthalmicerp.data.entity.SampleIssueItemEntity
import kotlinx.coroutines.flow.Flow

class SampleIssueRepository(
    private val sampleIssueDao: SampleIssueDao
) {

    suspend fun insertSampleIssue(
        sampleIssue: SampleIssueEntity
    ): Long {
        return sampleIssueDao.insertSampleIssue(sampleIssue)
    }

    suspend fun insertSampleIssueItems(
        items: List<SampleIssueItemEntity>
    ) {
        if (items.isEmpty()) return
        sampleIssueDao.insertSampleIssueItems(items)
    }

    suspend fun updateSampleIssue(
        sampleIssue: SampleIssueEntity
    ) {
        sampleIssueDao.updateSampleIssue(sampleIssue)
    }

    suspend fun getSampleIssueById(
        sampleIssueId: Long
    ): SampleIssueEntity? {
        return sampleIssueDao.getSampleIssueById(sampleIssueId)
    }

    fun getAllSampleIssues(): Flow<List<SampleIssueEntity>> {
        return sampleIssueDao.getAllSampleIssues()
    }

    fun getSampleIssuesByFinancialYear(
        financialYearStart: Int
    ): Flow<List<SampleIssueEntity>> {
        return sampleIssueDao.getSampleIssuesByFinancialYear(
            financialYearStart
        )
    }

    fun getSampleIssuesForCustomer(
        customerId: Long
    ): Flow<List<SampleIssueEntity>> {
        return sampleIssueDao.getSampleIssuesForCustomer(customerId)
    }

    suspend fun getItemsBySampleIssueId(
        sampleIssueId: Long
    ): List<SampleIssueItemEntity> {
        return sampleIssueDao.getItemsBySampleIssueId(sampleIssueId)
    }

    suspend fun inventoryUnitHasActiveSampleIssue(
        inventoryUnitId: Long
    ): Boolean {
        return sampleIssueDao.inventoryUnitHasActiveSampleIssue(
            inventoryUnitId
        )
    }

    suspend fun markItemReturned(
        sampleIssueItemId: Long,
        returnedAt: Long
    ): Int {
        return sampleIssueDao.markItemReturned(
            sampleIssueItemId = sampleIssueItemId,
            returnedAt = returnedAt
        )
    }

    suspend fun markItemConsumed(
        sampleIssueItemId: Long,
        consumedAt: Long
    ): Int {
        return sampleIssueDao.markItemConsumed(
            sampleIssueItemId = sampleIssueItemId,
            consumedAt = consumedAt
        )
    }

    suspend fun refreshSampleIssueStatus(
        sampleIssueId: Long,
        updatedAt: Long
    ) {
        val total =
            sampleIssueDao.getTotalItemCount(sampleIssueId)

        val issued =
            sampleIssueDao.getIssuedItemCount(sampleIssueId)

        val newStatus =
            when {
                total <= 0 -> "ISSUED"
                issued == total -> "ISSUED"
                issued <= 0 -> "CLOSED"
                else -> "PARTIALLY_RETURNED"
            }

        sampleIssueDao.updateSampleIssueStatus(
            sampleIssueId = sampleIssueId,
            status = newStatus,
            updatedAt = updatedAt
        )
    }

    suspend fun sampleIssueNumberExists(
        normalizedSampleIssueNumber: String,
        financialYearStart: Int
    ): Boolean {
        return sampleIssueDao.sampleIssueNumberExists(
            normalizedSampleIssueNumber =
                normalizedSampleIssueNumber.trim(),
            financialYearStart =
                financialYearStart
        )
    }
}
