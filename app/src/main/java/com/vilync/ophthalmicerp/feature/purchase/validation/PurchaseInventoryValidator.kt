package com.vilync.ophthalmicerp.feature.purchase.validation

import com.vilync.ophthalmicerp.data.repository.InventoryRepository

class PurchaseInventoryValidator(
    private val inventoryRepository: InventoryRepository
) {

    suspend fun validateSerialNumbers(
        serialNumbers: List<String>
    ): String? {

        val cleanedSerialNumbers = serialNumbers
            .map { it.trim() }
            .filter { it.isNotBlank() }

        for (serialNumber in cleanedSerialNumbers) {

            val alreadyExists =
                inventoryRepository.serialNumberExists(serialNumber)

            if (alreadyExists) {
                return "Serial Number $serialNumber already exists in inventory."
            }
        }

        return null
    }
}