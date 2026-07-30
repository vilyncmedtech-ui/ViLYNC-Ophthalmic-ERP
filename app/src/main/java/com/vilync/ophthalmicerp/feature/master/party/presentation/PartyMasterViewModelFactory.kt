package com.vilync.ophthalmicerp.feature.master.party.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.vilync.ophthalmicerp.feature.master.party.data.PartyRepository
import com.vilync.ophthalmicerp.feature.master.party.gst.GstinLookupRepository


class PartyMasterViewModelFactory(

    private val repository: PartyRepository,

    private val gstinLookupRepository: GstinLookupRepository

) : ViewModelProvider.Factory {


    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {


        if (
            modelClass.isAssignableFrom(
                PartyMasterViewModel::class.java
            )
        ) {

            return PartyMasterViewModel(

                repository = repository,

                gstinLookupRepository =
                    gstinLookupRepository

            ) as T
        }


        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}