package com.vilync.ophthalmicerp.feature.inventory.opening.presentation

import com.vilync.ophthalmicerp.data.entity.OpeningStockEntity

data class OpeningStockUiState(
    val isLoading: Boolean = false,
    val openingStocks: List<OpeningStockEntity> = emptyList(),
    val searchQuery: String = "",
    
    // Entry/Edit fields
    val id: Long = 0L,
    val entryNumber: String = "",
    val entryDate: String = "",
    val remarks: String = "",
    val status: String = "DRAFT",
    val items: List<OpeningStockUiItem> = emptyList(),
    
    val isSaving: Boolean = false,
    val isPosted: Boolean = false,
    val isCancelled: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    
    val isEditMode: Boolean = false,
    val isDirty: Boolean = false
)
