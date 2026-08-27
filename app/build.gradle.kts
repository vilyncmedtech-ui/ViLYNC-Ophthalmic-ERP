import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}


// =============================================================
// LOCAL PROPERTIES
// =============================================================

val localProperties = Properties()

val localPropertiesFile =
    rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use {
        localProperties.load(it)
    }
}

val gstApiKey =
    localProperties.getProperty(
        "GST_API_KEY",
        ""
    )

val googleWebClientId =
    localProperties.getProperty(
        "GOOGLE_WEB_CLIENT_ID",
        ""
    )


android {

    namespace = "com.vilync.ophthalmicerp"

    compileSdk = 37


    defaultConfig {

        applicationId = "com.vilync.ophthalmicerp"

        minSdk = 31
        targetSdk = 36

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"


        // =====================================================
        // GST API KEY
        // =====================================================

        buildConfigField(
            "String",
            "GST_API_KEY",
            "\"${gstApiKey.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        )

        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${googleWebClientId.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        )
    }


    buildTypes {

        release {

            optimization {
                enable = false
            }
        }
    }


    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }


    buildFeatures {

        compose = true

        // Required for BuildConfig.GST_API_KEY
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE.txt"
        }
    }
}


dependencies {

    implementation(
        platform(
            libs.androidx.compose.bom
        )
    )

    implementation(
        libs.androidx.activity.compose
    )

    implementation(
        libs.androidx.compose.material3
    )

    implementation(
        libs.androidx.compose.material.icons.core
    )

    implementation(
        libs.androidx.compose.material.icons.extended
    )

    implementation(
        libs.androidx.compose.ui
    )

    implementation(
        libs.androidx.compose.ui.graphics
    )

    implementation(
        libs.androidx.compose.ui.tooling.preview
    )

    implementation(
        libs.androidx.core.ktx
    )

    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )

    implementation(
        libs.androidx.navigation.compose
    )


    // =========================================================
    // ROOM DATABASE
    // =========================================================

    implementation(
        libs.androidx.room.runtime
    )

    implementation(
        libs.androidx.room.ktx
    )

    ksp(
        libs.androidx.room.compiler
    )


    // =========================================================
    // GOOGLE IDENTITY / CREDENTIALS
    // =========================================================

    implementation(
        libs.androidx.identity.credentials
    )

    implementation(
        libs.androidx.identity.play
    )

    implementation(
        libs.google.identity.googleid
    )

    implementation(
        libs.googleauth
    )


    // =========================================================
    // GOOGLE DRIVE API
    // =========================================================

    implementation(
        libs.google.drive.services
    )

    implementation(
        libs.google.drive.client
    )

    implementation(
        libs.google.drive.http
    )


    // =========================================================
    // WORK MANAGER
    // =========================================================

    implementation(
        libs.androidx.work.runtime
    )

    // =========================================================
    // BARCODE / QR GENERATION
    // =========================================================

    implementation("com.google.zxing:core:3.5.3")


    // =========================================================
    // TESTING
    // =========================================================

    testImplementation(
        libs.junit
    )

    androidTestImplementation(
        platform(
            libs.androidx.compose.bom
        )
    )

    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        libs.androidx.junit
    )

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )
}