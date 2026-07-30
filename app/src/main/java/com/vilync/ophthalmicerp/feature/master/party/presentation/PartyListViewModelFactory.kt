package com.vilync.ophthalmicerp.feature.master.party.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository


class PartyListViewModelFactory(
    private val repository: com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (
            modelClass.isAssignableFrom(
                _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.PartyListViewModel::class.java
            )
        ) {

            return _root_ide_package_.com.vilync.ophthalmicerp.feature.master.party.presentation.PartyListViewModel(
                repository = repository
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}