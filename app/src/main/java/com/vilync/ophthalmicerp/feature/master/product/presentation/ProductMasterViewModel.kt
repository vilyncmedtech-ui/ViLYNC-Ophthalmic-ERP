package com.vilync.ophthalmicerp.feature.master.product.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vilync.ophthalmicerp.feature.master.product.data.ProductMasterRepository
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import com.vilync.ophthalmicerp.feature.master.party.model.PartyType
import com.vilync.ophthalmicerp.feature.master.product.model.ProductMaster
import com.vilync.ophthalmicerp.feature.master.product.model.ProductUnit
import com.vilync.ophthalmicerp.feature.product.model.ProductCategory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


class ProductMasterViewModel(
    private val repository: ProductMasterRepository,
    private val partyRepository: PartyRepository
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            ProductMasterUiState()
        )

    val uiState: StateFlow<ProductMasterUiState> =
        _uiState.asStateFlow()


    private val _isSaving =
        MutableStateFlow(false)

    val isSaving: StateFlow<Boolean> =
        _isSaving.asStateFlow()


    private val _saveSuccess =
        MutableStateFlow(false)

    val saveSuccess: StateFlow<Boolean> =
        _saveSuccess.asStateFlow()


    // =========================================================
    // MANUFACTURER SEARCH JOB
    // =========================================================

    private var manufacturerSearchJob: Job? =
        null


    // =========================================================
    // INITIAL LOAD
    // =========================================================

    init {
        observeManufacturerSuggestions()
    }


    // =========================================================
    // LOAD SAVED MANUFACTURERS
    // =========================================================

    private fun observeManufacturerSuggestions() {

        viewModelScope.launch {

            partyRepository
                .getAllActiveParties()
                .collect { parties ->

                    val cleanList =
                        parties
                            .filter { party ->
                                party.partyType == PartyType.VENDOR ||
                                        party.partyType == PartyType.BOTH
                            }
                            .map {
                                it.partyName.trim()
                            }
                            .filter {
                                it.isNotBlank()
                            }
                            .distinctBy {
                                it.lowercase()
                            }
                            .sortedBy {
                                it.lowercase()
                            }

                    _uiState.value =
                        _uiState.value.copy(
                            manufacturerSuggestions =
                                cleanList
                        )
                }
        }
    }


    // =========================================================
    // PRODUCT NAME
    // =========================================================

    fun updateProductName(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                productName = value,
                errorMessage = null
            )

        _saveSuccess.value = false
    }


    // =========================================================
    // COMPANY / MANUFACTURER
    // =========================================================
    //
    // FINAL RULE:
    //
    // L    -> search
    // Li   -> search
    // Lif  -> search
    // Life -> narrower search
    //
    // Search starts from FIRST character.
    //
    // =========================================================

    fun updateBrand(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                brand = value,
                errorMessage = null
            )

        _saveSuccess.value =
            false

        manufacturerSearchJob
            ?.cancel()


        val query =
            value.trim()


        // -----------------------------------------------------
        // EMPTY FIELD
        // -----------------------------------------------------

        if (query.isBlank()) {

            _uiState.value =
                _uiState.value.copy(
                    filteredManufacturerSuggestions =
                        emptyList(),
                    showManufacturerSuggestions =
                        false
                )

            return
        }


        // -----------------------------------------------------
        // IMMEDIATE LOCAL FILTER
        // -----------------------------------------------------
        //
        // This makes suggestions appear instantly from already
        // loaded Room manufacturer names.
        //
        // -----------------------------------------------------

        val immediateResults =
            filterManufacturers(
                manufacturers =
                    _uiState.value
                        .manufacturerSuggestions,
                query =
                    query
            )


        _uiState.value =
            _uiState.value.copy(
                filteredManufacturerSuggestions =
                    immediateResults,
                showManufacturerSuggestions =
                    immediateResults.isNotEmpty()
            )


        // -----------------------------------------------------
        // ROOM SEARCH
        // -----------------------------------------------------
        //
        // DAO search runs from first typed character.
        //
        // -----------------------------------------------------

        manufacturerSearchJob =
            viewModelScope.launch {

                try {

                    val databaseResults =
                        partyRepository
                            .searchParties(
                                query = query
                            )
                            .first()
                            .filter { party ->
                                party.partyType == PartyType.VENDOR ||
                                        party.partyType == PartyType.BOTH
                            }
                            .map {
                                it.partyName.trim()
                            }
                            .filter {
                                it.isNotBlank()
                            }
                            .distinctBy {
                                it.lowercase()
                            }
                            .take(10)


                    /*
                     * User may type another character while
                     * Room query is running.
                     *
                     * Do not show stale results.
                     */
                    if (
                        _uiState.value
                            .brand
                            .trim()
                            .equals(
                                query,
                                ignoreCase = true
                            )
                    ) {

                        val combinedResults =
                            (
                                    immediateResults +
                                            databaseResults
                                    )
                                .map {
                                    it.trim()
                                }
                                .filter {
                                    it.isNotBlank()
                                }
                                .distinctBy {
                                    it.lowercase()
                                }
                                .sortedWith(
                                    compareBy<String> {

                                        if (
                                            it.startsWith(
                                                query,
                                                ignoreCase = true
                                            )
                                        ) {
                                            0
                                        } else {
                                            1
                                        }
                                    }.thenBy {
                                        it.lowercase()
                                    }
                                )
                                .take(10)


                        _uiState.value =
                            _uiState.value.copy(
                                filteredManufacturerSuggestions =
                                    combinedResults,
                                showManufacturerSuggestions =
                                    combinedResults.isNotEmpty()
                            )
                    }

                } catch (
                    exception: Exception
                ) {

                    /*
                     * Local suggestions remain usable even if
                     * Room search unexpectedly fails.
                     */
                }
            }
    }


    // =========================================================
    // FILTER MANUFACTURERS
    // =========================================================

    private fun filterManufacturers(
        manufacturers: List<String>,
        query: String
    ): List<String> {

        if (query.isBlank()) {
            return emptyList()
        }

        return manufacturers
            .filter { manufacturer ->

                manufacturer.contains(
                    query,
                    ignoreCase = true
                )
            }
            .distinctBy {
                it.lowercase()
            }
            .sortedWith(
                compareBy<String> {

                    if (
                        it.startsWith(
                            query,
                            ignoreCase = true
                        )
                    ) {
                        0
                    } else {
                        1
                    }

                }.thenBy {
                    it.lowercase()
                }
            )
            .take(10)
    }


    // =========================================================
    // SHOW MANUFACTURER SUGGESTIONS
    // =========================================================

    fun showManufacturerSuggestions() {

        val query =
            _uiState.value
                .brand
                .trim()

        if (query.isBlank()) {

            _uiState.value =
                _uiState.value.copy(
                    filteredManufacturerSuggestions =
                        emptyList(),
                    showManufacturerSuggestions =
                        false
                )

            return
        }


        val results =
            filterManufacturers(
                manufacturers =
                    _uiState.value
                        .manufacturerSuggestions,
                query =
                    query
            )


        _uiState.value =
            _uiState.value.copy(
                filteredManufacturerSuggestions =
                    results,
                showManufacturerSuggestions =
                    results.isNotEmpty()
            )
    }


    // =========================================================
    // HIDE MANUFACTURER SUGGESTIONS
    // =========================================================

    fun hideManufacturerSuggestions() {

        _uiState.value =
            _uiState.value.copy(
                showManufacturerSuggestions =
                    false
            )
    }


    // =========================================================
    // SELECT MANUFACTURER
    // =========================================================

    fun selectManufacturer(
        manufacturer: String
    ) {

        manufacturerSearchJob
            ?.cancel()

        _uiState.value =
            _uiState.value.copy(
                brand =
                    manufacturer.trim(),
                filteredManufacturerSuggestions =
                    emptyList(),
                showManufacturerSuggestions =
                    false,
                errorMessage =
                    null
            )

        _saveSuccess.value =
            false
    }


    // =========================================================
    // MODEL
    // =========================================================

    fun updateModel(
        value: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                model = value,
                errorMessage = null
            )

        _saveSuccess.value = false
    }


    // =========================================================
    // CATEGORY
    // =========================================================

    fun updateCategory(
        value: ProductCategory
    ) {

        if (value == ProductCategory.IOL) {

            _uiState.value =
                _uiState.value.copy(
                    category = value,
                    hsnCode = "9021",
                    gstPercent = "5",
                    powerApplicable = true,
                    batchApplicable = true,
                    expiryApplicable = true,
                    serialNumberRequired = true,
                    errorMessage = null
                )

        } else {

            _uiState.value =
                _uiState.value.copy(
                    category = value,
                    hsnCode = "",
                    gstPercent = "0",
                    powerApplicable = false,
                    batchApplicable = false,
                    expiryApplicable = false,
                    serialNumberRequired = false,
                    serialPrefix = "",
                    errorMessage = null
                )
        }

        recalculatePrices()

        _saveSuccess.value = false
    }


    // =========================================================
    // UNIT
    // =========================================================

    fun updateUnit(
        value: ProductUnit
    ) {

        _uiState.value =
            _uiState.value.copy(
                unit = value,
                errorMessage = null
            )

        _saveSuccess.value = false
    }


    // =========================================================
    // HSN
    // =========================================================

    fun updateHsnCode(
        value: String
    ) {

        val digitsOnly =
            value.filter {
                it.isDigit()
            }

        if (digitsOnly.length <= 8) {

            _uiState.value =
                _uiState.value.copy(
                    hsnCode = digitsOnly,
                    errorMessage = null
                )

            _saveSuccess.value = false
        }
    }


    // =========================================================
    // GST
    // =========================================================

    fun updateGstPercent(
        value: String
    ) {

        if (
            value.isBlank() ||
            value.matches(
                Regex("^\\d*\\.?\\d*$")
            )
        ) {

            val gst =
                value.toDoubleOrNull()
                    ?: 0.0

            if (gst <= 100.0) {

                _uiState.value =
                    _uiState.value.copy(
                        gstPercent = value,
                        errorMessage = null
                    )

                recalculatePrices()

                _saveSuccess.value = false
            }
        }
    }


    // =========================================================
    // PURCHASE PRICE
    // =========================================================

    fun updatePurchasePrice(
        value: String
    ) {

        if (
            value.isBlank() ||
            value.matches(
                Regex("^\\d*\\.?\\d*$")
            )
        ) {

            _uiState.value =
                _uiState.value.copy(
                    purchasePrice = value,
                    errorMessage = null
                )

            recalculatePrices()

            _saveSuccess.value = false
        }
    }


    // =========================================================
    // RETAIL PRICE
    // =========================================================

    fun updateRetailPrice(
        value: String
    ) {

        if (
            value.isBlank() ||
            value.matches(
                Regex("^\\d*\\.?\\d*$")
            )
        ) {

            _uiState.value =
                _uiState.value.copy(
                    retailPrice = value,
                    errorMessage = null
                )

            recalculatePrices()

            _saveSuccess.value = false
        }
    }


    // =========================================================
    // MRP
    // =========================================================

    fun updateMrp(
        value: String
    ) {

        if (
            value.isBlank() ||
            value.matches(
                Regex("^\\d*\\.?\\d*$")
            )
        ) {

            _uiState.value =
                _uiState.value.copy(
                    mrp = value,
                    errorMessage = null
                )

            _saveSuccess.value = false
        }
    }


    // =========================================================
    // RECALCULATE PRICES
    // =========================================================

    private fun recalculatePrices() {

        val currentState =
            _uiState.value


        val gstPercent =
            currentState
                .gstPercent
                .toDoubleOrNull()
                ?: 0.0


        val purchasePrice =
            currentState
                .purchasePrice
                .toDoubleOrNull()
                ?: 0.0


        val purchaseGstAmount =
            purchasePrice *
                    gstPercent /
                    100.0


        val netPurchasePrice =
            purchasePrice +
                    purchaseGstAmount


        val retailPrice =
            currentState
                .retailPrice
                .toDoubleOrNull()
                ?: 0.0


        val retailGstAmount =
            retailPrice *
                    gstPercent /
                    100.0


        val netRetailPrice =
            retailPrice +
                    retailGstAmount


        _uiState.value =
            _uiState.value.copy(
                purchaseGstAmount =
                    purchaseGstAmount,
                netPurchasePrice =
                    netPurchasePrice,
                retailGstAmount =
                    retailGstAmount,
                netRetailPrice =
                    netRetailPrice
            )
    }


    // =========================================================
    // POWER
    // =========================================================

    fun updatePowerApplicable(
        value: Boolean
    ) {

        _uiState.value =
            _uiState.value.copy(
                powerApplicable = value,
                errorMessage = null
            )

        _saveSuccess.value = false
    }


    // =========================================================
    // BATCH
    // =========================================================

    fun updateBatchApplicable(
        value: Boolean
    ) {

        _uiState.value =
            _uiState.value.copy(
                batchApplicable = value,
                errorMessage = null
            )

        _saveSuccess.value = false
    }


    // =========================================================
    // EXPIRY
    // =========================================================

    fun updateExpiryApplicable(
        value: Boolean
    ) {

        _uiState.value =
            _uiState.value.copy(
                expiryApplicable = value,
                errorMessage = null
            )

        _saveSuccess.value = false
    }


    // =========================================================
    // SERIAL NUMBER
    // =========================================================

    fun updateSerialNumberRequired(
        value: Boolean
    ) {

        _uiState.value =
            _uiState.value.copy(
                serialNumberRequired = value,
                serialPrefix = if (value) _uiState.value.serialPrefix else "",
                errorMessage = null
            )

        _saveSuccess.value = false
    }


    // =========================================================
    // SERIAL PREFIX
    // =========================================================

    fun updateSerialPrefix(
        value: String
    ) {

        val normalized =
            value
                .filter { it.isLetterOrDigit() }
                .uppercase()

        _uiState.value =
            _uiState.value.copy(
                serialPrefix = normalized,
                errorMessage = null
            )

        _saveSuccess.value = false
    }


    // =========================================================
    // ACTIVE
    // =========================================================

    fun updateIsActive(
        value: Boolean
    ) {

        _uiState.value =
            _uiState.value.copy(
                isActive = value,
                errorMessage = null
            )

        _saveSuccess.value = false
    }


    // =========================================================
    // LOAD PRODUCT FOR EDIT
    // =========================================================

    fun loadProduct(
        productId: Long
    ) {

        if (productId <= 0L) {

            setError(
                "Invalid Product ID."
            )

            return
        }


        manufacturerSearchJob
            ?.cancel()


        _saveSuccess.value =
            false


        _uiState.value =
            _uiState.value.copy(
                isLoadingProduct = true,
                errorMessage = null,
                showManufacturerSuggestions = false
            )


        viewModelScope.launch {

            try {

                val product =
                    repository.getProductById(
                        productId
                    )


                if (product == null) {

                    _uiState.value =
                        _uiState.value.copy(
                            isLoadingProduct = false,
                            errorMessage =
                                "Product not found."
                        )

                    return@launch
                }


                val manufacturers =
                    _uiState.value
                        .manufacturerSuggestions


                _uiState.value =
                    ProductMasterUiState(

                        productId =
                            product.id,

                        productName =
                            product.productName,

                        brand =
                            product.brand,

                        model =
                            product.model,

                        category =
                            product.category,

                        unit =
                            product.unit,

                        manufacturerSuggestions =
                            manufacturers,

                        filteredManufacturerSuggestions =
                            emptyList(),

                        showManufacturerSuggestions =
                            false,

                        hsnCode =
                            product.hsnCode,

                        gstPercent =
                            formatNumber(
                                product.gstPercent
                            ),

                        purchasePrice =
                            formatNumber(
                                product.purchasePrice
                            ),

                        purchaseGstAmount =
                            product.purchaseGstAmount,

                        netPurchasePrice =
                            product.netPurchasePrice,

                        retailPrice =
                            formatNumber(
                                product.retailPrice
                            ),

                        retailGstAmount =
                            product.retailGstAmount,

                        netRetailPrice =
                            product.netRetailPrice,

                        mrp =
                            formatNumber(
                                product.mrp
                            ),

                        powerApplicable =
                            product.powerApplicable,

                        batchApplicable =
                            product.batchApplicable,

                        expiryApplicable =
                            product.expiryApplicable,

                        serialNumberRequired =
                            product.serialNumberRequired,

                        serialPrefix =
                            product.serialPrefix,

                        isActive =
                            product.isActive,

                        isEditMode =
                            true,

                        isLoadingProduct =
                            false,

                        errorMessage =
                            null
                    )

            } catch (
                exception: Exception
            ) {

                _uiState.value =
                    _uiState.value.copy(
                        isLoadingProduct = false,
                        errorMessage =
                            exception.message
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: "Unable to load product."
                    )
            }
        }
    }


    // =========================================================
    // START NEW PRODUCT
    // =========================================================

    fun startNewProduct() {

        resetForm()
    }


    // =========================================================
    // FORMAT NUMBER
    // =========================================================

    private fun formatNumber(
        value: Double
    ): String {

        return if (
            value % 1.0 == 0.0
        ) {

            value.toLong()
                .toString()

        } else {

            value.toString()
        }
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    fun validateProduct(): Boolean {

        val state =
            _uiState.value


        if (state.productName.isBlank()) {

            setError(
                "Product Name is required."
            )

            return false
        }


        if (state.hsnCode.isBlank()) {

            setError(
                "HSN Code is required."
            )

            return false
        }


        val gst =
            state.gstPercent
                .toDoubleOrNull()


        if (
            gst == null ||
            gst < 0.0 ||
            gst > 100.0
        ) {

            setError(
                "Please enter a valid GST percentage."
            )

            return false
        }


        val purchasePrice =
            state.purchasePrice
                .toDoubleOrNull()


        if (
            purchasePrice == null ||
            purchasePrice < 0.0
        ) {

            setError(
                "Please enter a valid Purchase Price."
            )

            return false
        }


        val retailPrice =
            state.retailPrice
                .toDoubleOrNull()


        if (
            retailPrice == null ||
            retailPrice < 0.0
        ) {

            setError(
                "Please enter a valid Retail Price."
            )

            return false
        }


        val mrp =
            state.mrp
                .toDoubleOrNull()


        if (
            mrp == null ||
            mrp < 0.0
        ) {

            setError(
                "Please enter a valid MRP."
            )

            return false
        }


        if (
            state.serialNumberRequired &&
            state.serialPrefix.isBlank()
        ) {

            setError(
                "Serial Prefix is required when Serial No. tracking is enabled."
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
    // CREATE PRODUCT MASTER
    // =========================================================

    fun createProductMaster():
            ProductMaster? {

        if (!validateProduct()) {
            return null
        }


        recalculatePrices()


        val state =
            _uiState.value


        return ProductMaster(

            id =
                state.productId,

            productName =
                state.productName.trim(),

            brand =
                state.brand.trim(),

            model =
                state.model.trim(),

            category =
                state.category,

            unit =
                state.unit,

            hsnCode =
                state.hsnCode.trim(),

            gstPercent =
                state.gstPercent
                    .toDoubleOrNull()
                    ?: 0.0,

            purchasePrice =
                state.purchasePrice
                    .toDoubleOrNull()
                    ?: 0.0,

            purchaseGstAmount =
                state.purchaseGstAmount,

            netPurchasePrice =
                state.netPurchasePrice,

            retailPrice =
                state.retailPrice
                    .toDoubleOrNull()
                    ?: 0.0,

            retailGstAmount =
                state.retailGstAmount,

            netRetailPrice =
                state.netRetailPrice,

            mrp =
                state.mrp
                    .toDoubleOrNull()
                    ?: 0.0,

            powerApplicable =
                state.powerApplicable,

            batchApplicable =
                state.batchApplicable,

            expiryApplicable =
                state.expiryApplicable,

            serialNumberRequired =
                state.serialNumberRequired,

            serialPrefix =
                state.serialPrefix.trim().uppercase(),

            isActive =
                state.isActive
        )
    }


    // =========================================================
    // SAVE PRODUCT
    // =========================================================

    fun saveProduct(
        onSuccess: (Long) -> Unit = {}
    ) {

        if (_isSaving.value) {
            return
        }


        manufacturerSearchJob
            ?.cancel()


        hideManufacturerSuggestions()


        val product =
            createProductMaster()
                ?: return


        _isSaving.value =
            true

        _saveSuccess.value =
            false


        viewModelScope.launch {

            try {

                val result =
                    repository.saveProduct(
                        product
                    )


                when (result) {

                    is ProductMasterRepository
                    .SaveResult
                    .Success -> {

                        _saveSuccess.value =
                            true


                        _uiState.value =
                            _uiState.value.copy(
                                errorMessage =
                                    null
                            )


                        onSuccess(
                            result.productId
                        )
                    }


                    ProductMasterRepository
                        .SaveResult
                        .DuplicateProduct -> {

                        _saveSuccess.value =
                            false


                        setError(
                            "This product already exists. " +
                                    "Product Name must be unique."
                        )
                    }


                    is ProductMasterRepository
                    .SaveResult
                    .Error -> {

                        _saveSuccess.value =
                            false


                        setError(
                            result.message
                        )
                    }
                }

            } catch (
                exception: Exception
            ) {

                _saveSuccess.value =
                    false


                setError(
                    exception.message
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Unable to save product. Please try again."
                )

            } finally {

                _isSaving.value =
                    false
            }
        }
    }


    // =========================================================
    // CLEAR SAVE SUCCESS
    // =========================================================

    fun clearSaveSuccess() {

        _saveSuccess.value =
            false
    }


    // =========================================================
    // ERROR
    // =========================================================

    private fun setError(
        message: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                errorMessage =
                    message
            )
    }


    fun clearError() {

        _uiState.value =
            _uiState.value.copy(
                errorMessage =
                    null
            )
    }


    // =========================================================
    // DELETE PRODUCT
    // =========================================================

    fun deleteProduct(
        onSuccess: () -> Unit = {}
    ) {

        val productId =
            _uiState.value.productId


        if (productId <= 0L) {

            setError(
                "Product cannot be deleted because it has not been saved yet."
            )

            return
        }


        if (_isSaving.value) {
            return
        }


        manufacturerSearchJob
            ?.cancel()


        hideManufacturerSuggestions()


        _isSaving.value =
            true

        _saveSuccess.value =
            false


        viewModelScope.launch {

            try {

                val deleted =
                    repository.deleteProduct(
                        productId
                    )


                if (deleted) {

                    onSuccess()

                } else {

                    setError(
                        "Unable to delete product."
                    )
                }

            } catch (
                exception: Exception
            ) {

                setError(
                    exception.message
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Unable to delete product."
                )

            } finally {

                _isSaving.value =
                    false
            }
        }
    }


    // =========================================================
    // RESET FORM
    // =========================================================

    fun resetForm() {

        manufacturerSearchJob
            ?.cancel()


        val manufacturers =
            _uiState.value
                .manufacturerSuggestions


        _uiState.value =
            ProductMasterUiState(
                manufacturerSuggestions =
                    manufacturers
            )


        _saveSuccess.value =
            false

        _isSaving.value =
            false
    }
}