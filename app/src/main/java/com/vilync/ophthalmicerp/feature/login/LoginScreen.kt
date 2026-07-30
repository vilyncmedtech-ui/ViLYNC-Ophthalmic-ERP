package com.vilync.ophthalmicerp.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
fun LoginScreen(

    viewModel: LoginViewModel,

    onLoginSuccess: () -> Unit

) {

    val uiState by
    viewModel.uiState.collectAsState()


    // =========================================================
    // LOGIN SUCCESS
    // =========================================================

    LaunchedEffect(
        uiState.isLoginSuccessful
    ) {

        if (
            uiState.isLoginSuccessful
        ) {

            viewModel.consumeLoginSuccess()

            onLoginSuccess()
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
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )


            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )


            Text(
                text = "Secure Login",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )


            Spacer(
                modifier =
                    Modifier.height(32.dp)
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

                singleLine = true,

                enabled =
                    !uiState.isLoggingIn,

                modifier =
                    Modifier.fillMaxWidth()
            )


            Spacer(
                modifier =
                    Modifier.height(16.dp)
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

                singleLine = true,

                enabled =
                    !uiState.isLoggingIn,

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
            // LOGIN BUTTON
            // =================================================

            Button(
                onClick =
                    viewModel::login,

                enabled =
                    !uiState.isLoggingIn,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
            ) {

                if (
                    uiState.isLoggingIn
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
                        text = "LOGIN",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}