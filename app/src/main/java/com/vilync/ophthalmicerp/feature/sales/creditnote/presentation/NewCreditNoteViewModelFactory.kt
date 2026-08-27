package com.vilync.ophthalmicerp.feature.sales.creditnote.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vilync.ophthalmicerp.data.repository.DocumentNumberingRepository
import com.vilync.ophthalmicerp.data.repository.SalesCreditNoteRepository
import com.vilync.ophthalmicerp.data.repository.SalesRepository

class NewCreditNoteViewModelFactory(
    private val salesRepository: SalesRepository,
    private val creditNoteRepository: SalesCreditNoteRepository,
    private val numberingRepository: DocumentNumberingRepository,
    private val editingId: Long? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewCreditNoteViewModel::class.java)) {
            return NewCreditNoteViewModel(
                salesRepository = salesRepository,
                creditNoteRepository = creditNoteRepository,
                numberingRepository = numberingRepository,
                editingId = editingId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
