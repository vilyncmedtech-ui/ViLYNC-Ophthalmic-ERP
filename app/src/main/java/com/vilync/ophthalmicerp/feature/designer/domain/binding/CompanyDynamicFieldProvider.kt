package com.vilync.ophthalmicerp.feature.designer.domain.binding

import com.vilync.ophthalmicerp.core.document.domain.DesignerDocumentType
import com.vilync.ophthalmicerp.core.document.engine.BindingValue
import com.vilync.ophthalmicerp.core.document.engine.DocumentBindingContext
import com.vilync.ophthalmicerp.core.document.engine.DynamicFieldProvider
import com.vilync.ophthalmicerp.core.document.engine.binding.PlaceholderRegistry
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileRepository

/**
 * Resolves Company-related placeholders from the official ERP profile.
 */
class CompanyDynamicFieldProvider(
    private val repository: CompanyProfileRepository
) : DynamicFieldProvider {

    override val name: String = "CompanyProfileProvider"

    override val supportedTypes: Set<DesignerDocumentType> = DesignerDocumentType.values().toSet()

    override suspend fun getFields(context: DocumentBindingContext): Map<String, BindingValue> {
        val fields = mutableMapOf<String, BindingValue>()
        
        try {
            val profile = repository.getCompanyProfile()
            if (profile != null) {
                fields[PlaceholderRegistry.COMPANY_NAME] = BindingValue.Text(profile.legalName)
                
                val address = listOf(
                    profile.addressLine1,
                    profile.addressLine2,
                    profile.city,
                    profile.state,
                    profile.pinCode
                ).filter { it.isNotBlank() }.joinToString(", ")
                
                fields[PlaceholderRegistry.COMPANY_ADDRESS] = BindingValue.Text(address)
                fields[PlaceholderRegistry.COMPANY_GSTIN] = BindingValue.Text(profile.gstin)
                fields[PlaceholderRegistry.COMPANY_MOBILE] = BindingValue.Text(profile.phone)
                fields[PlaceholderRegistry.COMPANY_EMAIL] = BindingValue.Text(profile.email)
                fields[PlaceholderRegistry.COMPANY_TERMS] = BindingValue.Text(profile.defaultTermsAndConditions)

                fields[PlaceholderRegistry.COMPANY_DL1] = BindingValue.Text(profile.drugLicenceNo1)
                fields[PlaceholderRegistry.COMPANY_DL2] = BindingValue.Text(profile.drugLicenceNo2)
                fields[PlaceholderRegistry.COMPANY_MDL] = BindingValue.Text(profile.medicalDeviceLicenceNo)
                
                if (profile.logoUri.isNotBlank()) {
                    fields[PlaceholderRegistry.LOGO] = BindingValue.Image(uri = profile.logoUri)
                }

                fields[PlaceholderRegistry.FOOTER_TEXT] = BindingValue.Text(
                    "This is a system generated document and does not require any physical signature."
                )
            }
        } catch (e: Exception) {
            // Logged by PlaceholderResolver
        }

        return fields
    }
}
