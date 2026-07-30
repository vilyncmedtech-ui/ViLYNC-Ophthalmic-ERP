package com.vilync.ophthalmicerp.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun FirstAdminSetupScreen(

    viewModel: FirstAdminSetupViewModel,

    onAdminCreated: () -> Unit

) {

    val uiState by
    viewModel.uiState.collectAsState()


    // =========================================================
    // SUCCESS NAVIGATION
    // =========================================================

    LaunchedEffect(
        uiState.isCreatedSuccessfully
    ) {

        if (
            uiState.isCreatedSuccessfully
        ) {

            onAdminCreated()
        }
    }


    Surface(
        modifier =
            Modifier.fillMaxSize()
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(24.dp),

            verticalArrangement =
                Arrangement.Center,

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {


            // =================================================
            // TITLE
            // =================================================

            Text(
                text = "ViLYNC ERP",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            Text(
                text = "First Administrator Setup",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            Text(
                text =
                    "Create the first administrator account for this ERP installation.",

                fontSize = 14.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )


            Spacer(
                modifier =
                    Modifier.height(28.dp)
            )


            // =================================================
            // ADMINISTRATOR NAME
            // =================================================

            OutlinedTextField(
                value =
                    uiState.displayName,

                onValueChange =
                    viewModel::updateDisplayName,

                label = {
                    Text(
                        text = "Administrator Name"
                    )
                },

                singleLine = true,

                enabled =
                    !uiState.isSaving,

                modifier =
                    Modifier.fillMaxWidth()
            )


            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )


            // =================================================
            // USERNAME
            // =================================================

            OutlinedTextField(
                value =
                    uiState.username,

                onValueChange =
                    viewModel::updateUsername,

                label = {
                    Text(
                        text = "Username"
                    )
                },

                supportingText = {
                    Text(
                        text = "Minimum 4 characters"
                    )
                },

                singleLine = true,

                enabled =
                    !uiState.isSaving,

                modifier =
                    Modifier.fillMaxWidth()
            )


            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )


            // =================================================
            // PASSWORD
            // =================================================

            OutlinedTextField(
                value =
                    uiState.password,

                onValueChange =
                    viewModel::updatePassword,

                label = {
                    Text(
                        text = "Password"
                    )
                },

                supportingText = {
                    Text(
                        text = "Minimum 8 characters"
                    )
                },

                singleLine = true,

                enabled =
                    !uiState.isSaving,

                visualTransformation =
                    PasswordVisualTransformation(),

                modifier =
                    Modifier.fillMaxWidth()
            )


            Spacer(
                modifier =
                    Modifier.height(14.dp)
            )


            // =================================================
            // CONFIRM PASSWORD
            // =================================================

            OutlinedTextField(
                value =
                    uiState.confirmPassword,

                onValueChange =
                    viewModel::updateConfirmPassword,

                label = {
                    Text(
                        text = "Confirm Password"
                    )
                },

                singleLine = true,

                enabled =
                    !uiState.isSaving,

                visualTransformation =
                    PasswordVisualTransformation(),

                modifier =
                    Modifier.fillMaxWidth()
            )


            // =================================================
            // ERROR MESSAGE
            // =================================================

            uiState.errorMessage
                ?.let { message ->

                    Spacer(
                        modifier =
                            Modifier.height(16.dp)
                    )


                    Text(
                        text = message,

                        color =
                            MaterialTheme
                                .colorScheme
                                .error
                    )
                }


            Spacer(
                modifier =
                    Modifier.height(24.dp)
            )


            // =================================================
            // CREATE ADMINISTRATOR BUTTON
            // =================================================

            Button(
                onClick =
                    viewModel::createFirstAdmin,

                enabled =
                    !uiState.isSaving,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
            ) {

                if (
                    uiState.isSaving
                ) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.height(
                                24.dp
                            ),

                        strokeWidth = 2.dp
                    )

                } else {

                    Text(
                        text =
                            "CREATE ADMINISTRATOR",

                        fontWeight =
                            FontWeight.SemiBold
                    )
                }
            }
        }
    }
}