package com.vilync.ophthalmicerp.feature.companyprofile.presentation

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileEntity
import com.vilync.ophthalmicerp.feature.companyprofile.data.CompanyProfileRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyProfileScreen(
    repository: CompanyProfileRepository,
    onBack: () -> Unit
) {
    val savedProfile by repository
        .observeCompanyProfile()
        .collectAsState(initial = null)

    var form by remember {
        mutableStateOf(CompanyProfileEntity())
    }

    var loadedFromDatabase by remember {
        mutableStateOf(false)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    val scope = rememberCoroutineScope()


    val context = LocalContext.current

    val logoPickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { selectedUri: Uri? ->
            selectedUri?.let { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                }

                form = form.copy(
                    logoUri = uri.toString()
                )
            }
        }

    val signaturePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { selectedUri: Uri? ->
            selectedUri?.let { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                }

                form = form.copy(
                    signatureUri = uri.toString()
                )
            }
        }

    LaunchedEffect(savedProfile, loadedFromDatabase) {
        if (!loadedFromDatabase) {
            savedProfile?.let { profile ->
                form = profile
                loadedFromDatabase = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Company Profile",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack
                    ) {
                        Text("Back")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Own Company Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "These details will be used as the company identity for ERP documents and future printable templates.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CompanySection(
                title = "Business Identity"
            ) {
                CompanyField(
                    value = form.legalName,
                    onValueChange = {
                        form = form.copy(
                            legalName = it
                        )
                    },
                    label = "Legal Name *"
                )

                CompanyField(
                    value = form.tradeName,
                    onValueChange = {
                        form = form.copy(
                            tradeName = it
                        )
                    },
                    label = "Trade Name"
                )
            }

            CompanySection(
                title = "Business Address"
            ) {
                CompanyField(
                    value = form.addressLine1,
                    onValueChange = {
                        form = form.copy(
                            addressLine1 = it
                        )
                    },
                    label = "Address Line 1"
                )

                CompanyField(
                    value = form.addressLine2,
                    onValueChange = {
                        form = form.copy(
                            addressLine2 = it
                        )
                    },
                    label = "Address Line 2"
                )

                TwoCompanyFields(
                    leftValue = form.city,
                    leftChange = {
                        form = form.copy(
                            city = it
                        )
                    },
                    leftLabel = "City",
                    rightValue = form.district,
                    rightChange = {
                        form = form.copy(
                            district = it
                        )
                    },
                    rightLabel = "District"
                )

                TwoCompanyFields(
                    leftValue = form.state,
                    leftChange = {
                        form = form.copy(
                            state = it
                        )
                    },
                    leftLabel = "State",
                    rightValue = form.pinCode,
                    rightChange = {
                        form = form.copy(
                            pinCode = it
                        )
                    },
                    rightLabel = "PIN Code",
                    rightKeyboardType = KeyboardType.Number
                )
            }

            CompanySection(
                title = "Tax & Registration"
            ) {
                TwoCompanyFields(
                    leftValue = form.gstin,
                    leftChange = {
                        form = form.copy(
                            gstin = it
                        )
                    },
                    leftLabel = "GSTIN",
                    rightValue = form.pan,
                    rightChange = {
                        form = form.copy(
                            pan = it
                        )
                    },
                    rightLabel = "PAN"
                )
            }

            CompanySection(
                title = "Contact Details"
            ) {
                TwoCompanyFields(
                    leftValue = form.phone,
                    leftChange = {
                        form = form.copy(
                            phone = it
                        )
                    },
                    leftLabel = "Phone",
                    leftKeyboardType = KeyboardType.Phone,
                    rightValue = form.email,
                    rightChange = {
                        form = form.copy(
                            email = it
                        )
                    },
                    rightLabel = "Email",
                    rightKeyboardType = KeyboardType.Email
                )

                CompanyField(
                    value = form.website,
                    onValueChange = {
                        form = form.copy(
                            website = it
                        )
                    },
                    label = "Website",
                    keyboardType = KeyboardType.Uri
                )
            }

            CompanySection(
                title = "Bank Details"
            ) {
                CompanyField(
                    value = form.accountHolderName,
                    onValueChange = {
                        form = form.copy(
                            accountHolderName = it
                        )
                    },
                    label = "Account Holder Name"
                )

                TwoCompanyFields(
                    leftValue = form.bankName,
                    leftChange = {
                        form = form.copy(
                            bankName = it
                        )
                    },
                    leftLabel = "Bank Name",
                    rightValue = form.bankBranch,
                    rightChange = {
                        form = form.copy(
                            bankBranch = it
                        )
                    },
                    rightLabel = "Branch"
                )

                TwoCompanyFields(
                    leftValue = form.accountNumber,
                    leftChange = {
                        form = form.copy(
                            accountNumber = it
                        )
                    },
                    leftLabel = "Account Number",
                    rightValue = form.ifscCode,
                    rightChange = {
                        form = form.copy(
                            ifscCode = it
                        )
                    },
                    rightLabel = "IFSC Code"
                )

                CompanyField(
                    value = form.upiId,
                    onValueChange = {
                        form = form.copy(
                            upiId = it
                        )
                    },
                    label = "UPI ID"
                )
            }

            CompanySection(
                title = "Regulatory / Licence Details"
            ) {
                CompanyField(
                    value = form.drugLicenceNo1,
                    onValueChange = {
                        form = form.copy(
                            drugLicenceNo1 = it
                        )
                    },
                    label = "Drug Licence No. 1"
                )

                CompanyField(
                    value = form.drugLicenceNo2,
                    onValueChange = {
                        form = form.copy(
                            drugLicenceNo2 = it
                        )
                    },
                    label = "Drug Licence No. 2"
                )

                CompanyField(
                    value = form.medicalDeviceLicenceNo,
                    onValueChange = {
                        form = form.copy(
                            medicalDeviceLicenceNo = it
                        )
                    },
                    label = "Medical Device Licence No."
                )
            }

            CompanySection(
                title = "Print Identity"
            ) {
                CompanyField(
                    value = form.authorisedSignatoryName,
                    onValueChange = {
                        form = form.copy(
                            authorisedSignatoryName = it
                        )
                    },
                    label = "Authorised Signatory Name"
                )

                CompanyPngSelector(
                    title = "Company Logo",
                    uriValue = form.logoUri,
                    chooseText = if (form.logoUri.isBlank()) "Choose Logo PNG" else "Replace Logo PNG",
                    onChoose = {
                        logoPickerLauncher.launch(arrayOf("image/png"))
                    },
                    onRemove = {
                        form = form.copy(logoUri = "")
                    }
                )

                CompanyPngSelector(
                    title = "Authorised Signature",
                    uriValue = form.signatureUri,
                    chooseText = if (form.signatureUri.isBlank()) "Choose Signature PNG" else "Replace Signature PNG",
                    onChoose = {
                        signaturePickerLauncher.launch(arrayOf("image/png"))
                    },
                    onRemove = {
                        form = form.copy(signatureUri = "")
                    }
                )

                CompanyField(
                    value = form.defaultTermsAndConditions,
                    onValueChange = {
                        form = form.copy(
                            defaultTermsAndConditions = it
                        )
                    },
                    label = "Default Terms & Conditions",
                    singleLine = false,
                    minLines = 4
                )
            }

            Button(
                onClick = {
                    if (form.legalName.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "Legal Name is required."
                            )
                        }
                    } else {
                        isSaving = true

                        scope.launch {
                            try {
                                repository.saveCompanyProfile(
                                    form
                                )

                                loadedFromDatabase = true

                                snackbarHostState.showSnackbar(
                                    "Company Profile saved successfully."
                                )
                            } catch (error: Exception) {
                                snackbarHostState.showSnackbar(
                                    error.message
                                        ?: "Unable to save Company Profile."
                                )
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text =
                        if (isSaving) {
                            "Saving..."
                        } else {
                            "Save Company Profile"
                        }
                )
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

@Composable
private fun CompanyPngSelector(
    title: String,
    uriValue: String,
    chooseText: String,
    onChoose: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current

    val bitmap = remember(uriValue) {
        if (uriValue.isBlank()) {
            null
        } else {
            try {
                context.contentResolver
                    .openInputStream(Uri.parse(uriValue))
                    ?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
            } catch (_: Exception) {
                null
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )

        bitmap?.let { preview ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Image(
                    bitmap = preview.asImageBitmap(),
                    contentDescription = "$title preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                )
            }
        }

        if (uriValue.isNotBlank() && bitmap == null) {
            Text(
                text = "PNG selected, but preview is unavailable. You can replace or remove it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onChoose,
                modifier = Modifier.weight(1f)
            ) {
                Text(chooseText)
            }

            if (uriValue.isNotBlank()) {
                TextButton(onClick = onRemove) {
                    Text("Remove")
                }
            }
        }

        Text(
            text = "PNG only",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun CompanySection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            content()
        }
    }
}

@Composable
private fun CompanyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(label)
        },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        modifier = modifier
    )
}

@Composable
private fun TwoCompanyFields(
    leftValue: String,
    leftChange: (String) -> Unit,
    leftLabel: String,
    rightValue: String,
    rightChange: (String) -> Unit,
    rightLabel: String,
    leftKeyboardType: KeyboardType = KeyboardType.Text,
    rightKeyboardType: KeyboardType = KeyboardType.Text
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CompanyField(
            value = leftValue,
            onValueChange = leftChange,
            label = leftLabel,
            keyboardType = leftKeyboardType,
            modifier = Modifier.weight(1f)
        )

        CompanyField(
            value = rightValue,
            onValueChange = rightChange,
            label = rightLabel,
            keyboardType = rightKeyboardType,
            modifier = Modifier.weight(1f)
        )
    }
}
