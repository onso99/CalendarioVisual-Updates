plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Configuración de Kotlin
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

// Configuración de Android usando la API moderna recomendada (ApplicationExtension)
extensions.configure<com.android.build.api.dsl.ApplicationExtension> {
    namespace = "com.example.calendario"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.calendario"
        minSdk = 29
        targetSdk = 37
        versionCode = 2
        versionName = "1.98.76"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,DEPENDENCIES,INDEX.LIST}"
        }
    }

    @Suppress("UnstableApiUsage")
    androidResources {
        localeFilters.addAll(listOf("es", "gl", "eu", "ca", "en", "fr", "de", "it", "pt", "zh", "ru", "ja"))
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // ViewModel dependencies
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Actividad de configuración del widget
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.preference.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.foundation)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.google.gson)
    implementation(libs.androidx.work.runtime.ktx) 
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    // Google Drive & Auth
    implementation(libs.playServicesAuth)
    implementation(libs.googleApiClient)
    implementation(libs.googleDriveApi)
    implementation(libs.googleHttpClient)
    implementation(libs.kotlinx.coroutines.play.services)
}
