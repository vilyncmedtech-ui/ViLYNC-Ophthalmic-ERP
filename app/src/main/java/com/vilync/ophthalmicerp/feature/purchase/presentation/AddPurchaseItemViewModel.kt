package com.vilync.ophthalmicerp.feature.purchase.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.data.repository.PurchaseRepository
import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository
import com.vilync.ophthalmicerp.feature.master.product.model.ProductMaster
import com.vilync.ophthalmicerp.feature.purchase.model.IolLensDetail
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddPurchaseItemViewModel(
    private val productRepository: ProductMasterRepository,
    private val purchaseRepository: PurchaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AddPurchaseItemUiState()
    )

    val uiState: StateFlow<AddPurchaseItemUiState> =
        _uiState.asStateFlow()


    private val _productSuggestions =
        MutableStateFlow<List<ProductMaster>>(
            emptyList()
        )

    val productSuggestions: StateFlow<List<ProductMaster>> =
        _productSuggestions.asStateFlow()


    private var productSearchJob: Job? = null

    private val serialCheckJobs =
        mutableMapOf<Int, Job>()


    // =========================================================
    // PRODUCT
    // =========================================================

    fun updateProductId(value: Long) {

        _uiState.value = _uiState.value.copy(
            productId = value,
            errorMessage = null
        )
    }


    fun updateProductName(value: String) {

        _uiState.value = _uiState.value.copy(
            productId = 0L,
            productName = value,
            errorMessage = null
        )

        searchProducts(
            query = value
        )
    }


    private fun searchProducts(
        query: String
    ) {

        productSearchJob?.cancel()

        productSearchJob =
            viewModelScope.launch {

                val flow =
                    if (
                        query.trim().isBlank()
                    ) {
                        productRepository
                            .getAllActiveProducts()
                    } else {
                        productRepository
                            .searchProducts(
                                query = query
                            )
                    }

                flow.collect { products ->

                    _productSuggestions.value =
                        products
                            .filter {
                                it.isActive
                            }
                            .take(20)
                }
            }
    }


    fun loadProductSuggestions() {

        searchProducts(
            query = _uiState.value.productName
        )
    }


    fun selectProduct(
        product: ProductMaster
    ) {

        _uiState.value =
            _uiState.value.copy(
                productId = product.id,
                productName = product.productName,
                model = product.model,
                category = product.category.name,
                hsnCode = product.hsnCode,
                gstPercent = formatNumber(product.gstPercent),
                purchaseRate = formatNumber(product.purchasePrice),
                errorMessage = null
            )

        _productSuggestions.value =
            emptyList()
    }


    fun clearProductSuggestions() {

        _productSuggestions.value =
            emptyList()
    }


    private fun formatNumber(
        value: Double
    ): String {

        return if (
            value % 1.0 == 0.0
        ) {
            value.toLong().toString()
        } else {
            value.toString()
        }
    }


    fun updateModel(value: String) {

        _uiState.value = _uiState.value.copy(
            model = value,
            errorMessage = null
        )
    }


    fun updateCategory(value: String) {

        _uiState.value = _uiState.value.copy(
            category = value,
            errorMessage = null
        )
    }


    // =========================================================
    // HSN CODE
    // =========================================================

    fun updateHsnCode(value: String) {

        val cleanValue =
            value.filter {
                it.isLetterOrDigit()
            }

        _uiState.value = _uiState.value.copy(
            hsnCode = cleanValue.uppercase(),
            errorMessage = null
        )
    }


    // =========================================================
    // POWER
    // =========================================================

    fun updatePower(value: String) {

        val cleanValue =
            value
                .uppercase()
                .replace("D", "")
                .trim()

        if (
            cleanValue.isBlank() ||
            cleanValue.matches(
                Regex("^\\d*\\.?\\d*$")
            )
        ) {

            _uiState.value =
                _uiState.value.copy(
                    power = cleanValue,
                    errorMessage = null
                )
        }
    }


    // =========================================================
    // FORMAT POWER WITH D
    // =========================================================

    fun formatPower() {

        val currentPower =
            _uiState.value
                .power
                .uppercase()
                .replace("D", "")
                .trim()

        if (currentPower.isBlank()) {
            return
        }

        val numericPower =
            currentPower.toDoubleOrNull()

        if (numericPower == null) {

            setError(
                "Please enter a valid lens power."
            )

            return
        }

        val formattedNumber =
            if (
                numericPower % 1.0 == 0.0
            ) {
                numericPower
                    .toInt()
                    .toString()
            } else {
                numericPower.toString()
            }

        _uiState.value =
            _uiState.value.copy(
                power = "${formattedNumber}D",
                errorMessage = null
            )
    }


    // =========================================================
    // QUANTITY
    // =========================================================

    /*
     * For IOL:
     *
     * Quantity is controlled automatically by physical
     * lens rows.
     *
     * 1 physical IOL =
     * 1 Serial Number + 1 Expiry
     *
     * Therefore:
     *
     * quantity = lensDetails.size
     *
     * For non-IOL products, manual quantity remains available.
     */

    fun updateQuantity(value: String) {

        if (
            _uiState.value.category.equals(
                "IOL",
                ignoreCase = true
            )
        ) {
            return
        }

        if (value.isBlank()) {

            _uiState.value =
                _uiState.value.copy(
                    quantity = "",
                    errorMessage = null
                )

            return
        }

        if (
            !value.all {
                it.isDigit()
            }
        ) {
            return
        }

        val quantity =
            value.toIntOrNull()
                ?: return

        if (quantity > 100) {

            setError(
                "Maximum quantity allowed in one item is 100."
            )

            return
        }

        _uiState.value =
            _uiState.value.copy(
                quantity = value,
                errorMessage = null
            )
    }


    // =========================================================
    // ADD PHYSICAL IOL LENS
    // =========================================================

    fun addLens() {

        val currentLensDetails =
            _uiState.value.lensDetails

        if (currentLensDetails.size >= 100) {

            setError(
                "Maximum quantity allowed in one item is 100."
            )

            return
        }

        val updatedLensDetails =
            currentLensDetails +
                    IolLensDetail()

        _uiState.value =
            _uiState.value.copy(
                lensDetails = updatedLensDetails,
                quantity =
                    updatedLensDetails.size.toString(),
                errorMessage = null
            )
    }


    // =========================================================
    // REMOVE PHYSICAL IOL LENS
    // =========================================================

    fun removeLens(index: Int) {

        val currentLensDetails =
            _uiState.value
                .lensDetails
                .toMutableList()

        if (
            index !in currentLensDetails.indices
        ) {
            return
        }

        currentLensDetails.removeAt(index)

        _uiState.value =
            _uiState.value.copy(
                lensDetails = currentLensDetails,
                quantity =
                    currentLensDetails.size.toString(),
                errorMessage = null
            )
    }


    // =========================================================
    // UPDATE PHYSICAL IOL SERIAL NUMBER
    // =========================================================

    fun updateLensSerialNumber(
        index: Int,
        value: String
    ) {
        val currentLensDetails =
            _uiState.value.lensDetails.toMutableList()

        if (index !in currentLensDetails.indices) {
            return
        }

        val cleanValue = value.trim()

        currentLensDetails[index] =
            currentLensDetails[index].copy(
                serialNumber = cleanValue
            )

        serialCheckJobs[index]?.cancel()

        val checking =
            _uiState.value.checkingSerialIndexes - index

        val duplicates =
            calculateCurrentItemDuplicateIndexes(
                lensDetails = currentLensDetails
            )

        _uiState.value =
            _uiState.value.copy(
                lensDetails = currentLensDetails,
                quantity = currentLensDetails.size.toString(),
                checkingSerialIndexes = checking,
                duplicateSerialIndexes = duplicates,
                errorMessage = null
            )

        if (cleanValue.isBlank()) {
            return
        }

        serialCheckJobs[index] =
            viewModelScope.launch {
                _uiState.value =
                    _uiState.value.copy(
                        checkingSerialIndexes =
                            _uiState.value.checkingSerialIndexes + index
                    )

                delay(350)

                try {
                    val existsInDatabase =
                        purchaseRepository.purchaseLensSerialExists(
                            serialNumber = cleanValue
                        )

                    val latestState = _uiState.value
                    val latestSerial =
                        latestState.lensDetails
                            .getOrNull(index)
                            ?.serialNumber
                            ?.trim()
                            .orEmpty()

                    if (!latestSerial.equals(cleanValue, ignoreCase = true)) {
                        return@launch
                    }

                    val currentItemDuplicates =
                        calculateCurrentItemDuplicateIndexes(
                            lensDetails = latestState.lensDetails
                        )

                    val finalDuplicates =
                        if (existsInDatabase) {
                            currentItemDuplicates + index
                        } else {
                            currentItemDuplicates
                        }

                    _uiState.value =
                        latestState.copy(
                            checkingSerialIndexes =
                                latestState.checkingSerialIndexes - index,
                            duplicateSerialIndexes = finalDuplicates
                        )

                } catch (_: Exception) {
                    _uiState.value =
                        _uiState.value.copy(
                            checkingSerialIndexes =
                                _uiState.value.checkingSerialIndexes - index
                        )
                }
            }
    }


    private fun calculateCurrentItemDuplicateIndexes(
        lensDetails: List<IolLensDetail>
    ): Set<Int> {
        val grouped =
            lensDetails
                .mapIndexedNotNull { index, lens ->
                    val serial = lens.serialNumber.trim().uppercase()
                    if (serial.isBlank()) null else index to serial
                }
                .groupBy { it.second }

        return grouped.values
            .filter { it.size > 1 }
            .flatten()
            .map { it.first }
            .toSet()
    }



    // =========================================================
    // UPDATE PHYSICAL IOL EXPIRY
    // =========================================================

    fun updateLensExpiry(
        index: Int,
        value: String
    ) {

        val currentLensDetails =
            _uiState.value
                .lensDetails
                .toMutableList()

        if (
            index !in currentLensDetails.indices
        ) {
            return
        }

        val digitsOnly =
            value.filter {
                it.isDigit()
            }

        if (
            digitsOnly.length > 4
        ) {
            return
        }

        val currentLens =
            currentLensDetails[index]

        currentLensDetails[index] =
            currentLens.copy(
                expiryDate = digitsOnly
            )

        _uiState.value =
            _uiState.value.copy(
                lensDetails = currentLensDetails,
                quantity =
                    currentLensDetails.size.toString(),
                errorMessage = null
            )
    }


    // =========================================================
    // PURCHASE RATE
    // =========================================================

    fun updatePurchaseRate(value: String) {

        if (
            value.isBlank() ||
            value.matches(
                Regex("^\\d*\\.?\\d*$")
            )
        ) {

            _uiState.value =
                _uiState.value.copy(
                    purchaseRate = value,
                    errorMessage = null
                )
        }
    }


    // =========================================================
    // DISCOUNT
    // =========================================================

    fun updateDiscountPercent(value: String) {

        if (
            value.isBlank() ||
            value.matches(
                Regex("^\\d*\\.?\\d*$")
            )
        ) {

            _uiState.value =
                _uiState.value.copy(
                    discountPercent = value,
                    errorMessage = null
                )
        }
    }


    // =========================================================
    // GST
    // =========================================================

    fun updateGstPercent(value: String) {

        if (
            value.isBlank() ||
            value.matches(
                Regex("^\\d*\\.?\\d*$")
            )
        ) {

            _uiState.value =
                _uiState.value.copy(
                    gstPercent = value,
                    errorMessage = null
                )
        }
    }


    // =========================================================
    // BATCH / LOT
    // =========================================================

    fun updateBatchNumber(value: String) {

        _uiState.value =
            _uiState.value.copy(
                batchNumber = value,
                errorMessage = null
            )
    }


    // =========================================================
    // VALIDATE
    // =========================================================

    fun validateCurrentItem(): Boolean {

        /*
         * Standardize IOL Power first.
         */

        if (
            _uiState.value.category.equals(
                "IOL",
                ignoreCase = true
            )
        ) {

            formatPower()
        }

        val state =
            _uiState.value


        if (state.checkingSerialIndexes.isNotEmpty()) {
            setError(
                "Please wait while Serial Number is being checked."
            )
            return false
        }

        if (state.duplicateSerialIndexes.isNotEmpty()) {
            setError(
                "Duplicate Serial Number found. Please enter a unique Serial Number."
            )
            return false
        }


        // -----------------------------------------------------
        // PRODUCT
        // -----------------------------------------------------

        if (
            state.productName.isBlank()
        ) {

            setError(
                "Please enter Product Name."
            )

            return false
        }


        // -----------------------------------------------------
        // HSN
        // -----------------------------------------------------

        if (
            state.hsnCode.isBlank()
        ) {

            setError(
                "Please enter HSN Code."
            )

            return false
        }


        // -----------------------------------------------------
        // IOL VALIDATION
        // -----------------------------------------------------

        if (
            state.category.equals(
                "IOL",
                ignoreCase = true
            )
        ) {

            // -------------------------------------------------
            // POWER
            // -------------------------------------------------

            if (
                state.power.isBlank()
            ) {

                setError(
                    "Please enter IOL Power."
                )

                return false
            }

            if (
                !state.power.endsWith(
                    "D",
                    ignoreCase = true
                )
            ) {

                setError(
                    "Please enter a valid IOL Power."
                )

                return false
            }


            // -------------------------------------------------
            // PHYSICAL LENS COUNT
            // -------------------------------------------------

            if (
                state.lensDetails.isEmpty()
            ) {

                setError(
                    "Please add at least one IOL lens."
                )

                return false
            }

            if (
                state.lensDetails.size > 100
            ) {

                setError(
                    "Maximum quantity allowed in one item is 100."
                )

                return false
            }


            // -------------------------------------------------
            // QUANTITY MUST MATCH PHYSICAL LENS ROWS
            // -------------------------------------------------

            val quantity =
                state.quantity.toIntOrNull()

            if (
                quantity != state.lensDetails.size
            ) {

                setError(
                    "IOL quantity must match physical lens count."
                )

                return false
            }


            // -------------------------------------------------
            // EVERY IOL MUST HAVE SERIAL NUMBER
            // -------------------------------------------------

            val cleanedSerialNumbers =
                state.lensDetails.map {
                    it.serialNumber.trim()
                }

            if (
                cleanedSerialNumbers.any {
                    it.isBlank()
                }
            ) {

                setError(
                    "Every IOL must have a Serial Number."
                )

                return false
            }


            // -------------------------------------------------
            // DUPLICATE SERIAL INSIDE CURRENT ITEM
            // -------------------------------------------------

            val normalizedSerialNumbers =
                cleanedSerialNumbers.map {
                    it.uppercase()
                }

            if (
                normalizedSerialNumbers
                    .distinct()
                    .size !=
                normalizedSerialNumbers.size
            ) {

                setError(
                    "Duplicate Serial Number found."
                )

                return false
            }


            // -------------------------------------------------
            // EVERY PHYSICAL IOL MUST HAVE EXPIRY
            // -------------------------------------------------

            if (
                state.lensDetails.any {
                    it.expiryDate.isBlank()
                }
            ) {

                setError(
                    "Every IOL must have an Expiry."
                )

                return false
            }


            // -------------------------------------------------
            // EXPIRY MMYY VALIDATION FOR EACH IOL
            // -------------------------------------------------

            state.lensDetails.forEachIndexed {
                    index,
                    lensDetail ->

                val expiry =
                    lensDetail.expiryDate.trim()

                if (
                    expiry.length != 4 ||
                    !expiry.all {
                        it.isDigit()
                    }
                ) {

                    setError(
                        "Lens ${index + 1}: Expiry must be in MMYY format."
                    )

                    return false
                }

                val month =
                    expiry
                        .take(2)
                        .toIntOrNull()

                if (
                    month == null ||
                    month !in 1..12
                ) {

                    setError(
                        "Lens ${index + 1}: Expiry month must be between 01 and 12."
                    )

                    return false
                }
            }

        } else {

            // -------------------------------------------------
            // NON-IOL QUANTITY
            // -------------------------------------------------

            val quantity =
                state.quantity.toIntOrNull()

            if (
                quantity == null ||
                quantity <= 0
            ) {

                setError(
                    "Quantity must be greater than zero."
                )

                return false
            }

            if (
                quantity > 100
            ) {

                setError(
                    "Maximum quantity allowed in one item is 100."
                )

                return false
            }
        }


        // -----------------------------------------------------
        // PURCHASE RATE
        // -----------------------------------------------------

        val purchaseRate =
            state.purchaseRate
                .toDoubleOrNull()

        if (
            purchaseRate == null ||
            purchaseRate < 0.0
        ) {

            setError(
                "Please enter a valid Purchase Rate."
            )

            return false
        }


        // -----------------------------------------------------
        // DISCOUNT
        // -----------------------------------------------------

        val discount =
            state.discountPercent
                .toDoubleOrNull()
                ?: 0.0

        if (
            discount < 0.0 ||
            discount > 100.0
        ) {

            setError(
                "Discount must be between 0 and 100."
            )

            return false
        }


        // -----------------------------------------------------
        // GST
        // -----------------------------------------------------

        val gst =
            state.gstPercent
                .toDoubleOrNull()
                ?: 0.0

        if (
            gst < 0.0 ||
            gst > 100.0
        ) {

            setError(
                "GST must be between 0 and 100."
            )

            return false
        }


        _uiState.value =
            _uiState.value.copy(
                errorMessage = null
            )

        return true
    }


    // =========================================================
    // ERROR
    // =========================================================

    private fun setError(message: String) {

        _uiState.value =
            _uiState.value.copy(
                errorMessage = message
            )
    }


    fun clearError() {

        _uiState.value =
            _uiState.value.copy(
                errorMessage = null
            )
    }


    // =========================================================
    // LOAD EXISTING PURCHASE ITEM FOR EDIT
    // =========================================================

    fun loadPurchaseItem(
        item: com.vilync.ophthalmicerp.feature.purchase.model.PurchaseItem
    ) {

        productSearchJob?.cancel()

        val isIol =
            item.category.equals(
                "IOL",
                ignoreCase = true
            )

        val quantity =
            if (isIol) {
                item.lensDetails.size.toString()
            } else {
                item.quantity.toString()
            }

        _uiState.value =
            AddPurchaseItemUiState(
                productId = item.productId,
                productName = item.productName,
                model = item.model,
                category = item.category,
                hsnCode = item.hsnCode,
                power = item.power,
                quantity = quantity,
                purchaseRate = formatNumber(item.purchaseRate),
                discountPercent = formatNumber(item.discountPercent),
                gstPercent = formatNumber(item.gstPercent),
                batchNumber = item.batchNumber,
                lensDetails = item.lensDetails.map { lensDetail ->
                    lensDetail.copy(
                        serialNumber = lensDetail.serialNumber.trim(),
                        expiryDate = lensDetail.expiryDate.trim()
                    )
                },
                errorMessage = null
            )

        _productSuggestions.value =
            emptyList()
    }


    // =========================================================
    // RESET
    // =========================================================

    fun resetForm() {

        productSearchJob?.cancel()

        serialCheckJobs.values.forEach { it.cancel() }
        serialCheckJobs.clear()

        _uiState.value =
            AddPurchaseItemUiState()

        _productSuggestions.value =
            emptyList()
    }
}