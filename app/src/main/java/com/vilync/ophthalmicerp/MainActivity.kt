package com.vilync.ophthalmicerp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vilync.ophthalmicerp.navigation.AppNavigation
import com.vilync.ophthalmicerp.ui.theme.ViLYNCERPTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            ViLYNCERPTheme {

                AppNavigation()

            }
        }
    }
}