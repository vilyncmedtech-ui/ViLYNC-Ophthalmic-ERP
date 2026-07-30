package com.vilync.ophthalmicerp.feature.purchase.validation

import com.vilync.ophthalmicerp.feature.purchase.model.PurchaseItem

object PurchaseValidator {

    fun validateItem(
        item: PurchaseItem,
        serialNumberRequired: Boolean
    ): String? {

        // =====================================================
        // QUANTITY
        // =====================================================

        if (item.quantity <= 0) {
            return "Quantity must be greater than zero."
        }


        // =====================================================
        // PURCHASE RATE
        // =====================================================

        if (item.purchaseRate < 0.0) {
            return "Purchase rate cannot be negative."
        }


        // =====================================================
        // DISCOUNT
        // =====================================================

        if (
            item.discountPercent < 0.0 ||
            item.discountPercent > 100.0
        ) {
            return "Discount must be between 0 and 100."
        }


        // =====================================================
        // GST
        // =====================================================

        if (
            item.gstPercent < 0.0 ||
            item.gstPercent > 100.0
        ) {
            return "GST must be between 0 and 100."
        }


        // =====================================================
        // SERIAL-TRACKED PRODUCTS SUCH AS IOL
        // =====================================================

        if (serialNumberRequired) {

            /*
             * New structure:
             *
             * One physical IOL =
             * One Serial Number + One Expiry
             *
             * Therefore quantity must always match
             * lensDetails.size.
             */

            if (item.lensDetails.isEmpty()) {
                return "Please add at least one physical lens."
            }


            // =================================================
            // QUANTITY MUST MATCH PHYSICAL LENS COUNT
            // =================================================

            if (
                item.lensDetails.size !=
                item.quantity
            ) {
                return "Quantity and physical lens count must be equal."
            }


            // =================================================
            // SERIAL NUMBER VALIDATION
            // =================================================

            val cleanedSerialNumbers =
                item.lensDetails.map { lensDetail ->

                    lensDetail.serialNumber.trim()
                }


            if (
                cleanedSerialNumbers.any {
                    it.isBlank()
                }
            ) {
                return "Serial number cannot be blank."
            }


            // =================================================
            // DUPLICATE SERIAL NUMBERS INSIDE CURRENT ITEM
            // =================================================

            val normalizedSerialNumbers =
                cleanedSerialNumbers.map {
                    it.uppercase()
                }


            if (
                normalizedSerialNumbers
                    .toSet()
                    .size !=
                normalizedSerialNumbers.size
            ) {
                return "Duplicate Serial Number found."
            }


            // =================================================
            // INDIVIDUAL EXPIRY VALIDATION
            // =================================================

            item.lensDetails.forEachIndexed {
                    index,
                    lensDetail ->

                val expiry =
                    lensDetail.expiryDate.trim()


                if (expiry.isBlank()) {

                    return "Lens ${index + 1}: Expiry cannot be blank."
                }


                /*
                 * Required format:
                 *
                 * MMYY
                 *
                 * Example:
                 * 1229
                 * 0630
                 */

                if (
                    expiry.length != 4 ||
                    !expiry.all { it.isDigit() }
                ) {

                    return "Lens ${index + 1}: Expiry must be in MMYY format."
                }


                val month =
                    expiry
                        .take(2)
                        .toIntOrNull()


                if (
                    month == null ||
                    month !in 1..12
                ) {

                    return "Lens ${index + 1}: Expiry month must be between 01 and 12."
                }
            }
        }


        // =====================================================
        // VALID
        // =====================================================

        return null
    }
}