plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose) // Nota: Si tu actividad de configuración NO usa Compose, este plugin a nivel de módulo podría no ser estrictamente necesario para *esa* actividad, pero no daña tenerlo si otras partes de tu app sí usan Compose.
}

android {
    namespace = "com.example.calendario"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.calendario"
        minSdk = 29
        targetSdk = 36
        versionCode = 2
        versionName = "1.8.945"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true // Mantener si usas Compose en otras partes o planeas hacerlo.
    }
    @Suppress("UnstableApiUsage")
    androidResources {
        localeFilters.addAll(listOf("es", "gl", "eu", "ca", "en", "fr", "de", "it", "pt", "zh", "ru", "ja"))
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose) // Para Activity con Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3) // Material 3 para Compose

    // ViewModel dependencies
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // ★★★ DEPENDENCIAS AÑADIDAS PARA LA ACTIVIDAD DE CONFIGURACIÓN DEL WIDGET ★★★
    implementation(libs.androidx.appcompat) // Para AppCompatActivity
    implementation(libs.androidx.preference.ktx) // Para PreferenceFragmentCompat y extensiones ktx
    // ★★★ FIN DE DEPENDENCIAS AÑADIDAS ★★★

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Estas son dependencias específicas de Compose, mantenlas si las usas.
    // Algunas pueden estar ya cubiertas por el BOM de Compose o libs.
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.activity.ktx)

    implementation(libs.google.gson)
    implementation(libs.androidx.work.runtime.ktx) 
    implementation(libs.androidx.material.icons.core) // Para iconos básicos
    implementation(libs.androidx.material.icons.extended) // ¡PARA Brightness4 y Brightness7!

}