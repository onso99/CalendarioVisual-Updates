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
        versionCode = 1
        versionName = "1.0"

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

    // ★★★ DEPENDENCIAS AÑADIDAS PARA LA ACTIVIDAD DE CONFIGURACIÓN DEL WIDGET ★★★
    implementation("androidx.appcompat:appcompat:1.6.1") // Para AppCompatActivity
    implementation("androidx.preference:preference-ktx:1.2.1") // Para PreferenceFragmentCompat y extensiones ktx
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
    implementation("androidx.compose.ui:ui:1.6.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.0")
    implementation("androidx.compose.foundation:foundation:1.6.0")
    implementation("androidx.compose.material3:material3:1.2.0") // Ya tienes libs.androidx.material3, verifica si son la misma o diferentes versiones/artefactos
    implementation("androidx.activity:activity-compose:1.9.0") // Ya tienes libs.androidx.activity.compose
    implementation("androidx.activity:activity-ktx:1.8.2")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0") // O verifica la última versión estable en developer.android.com
    implementation(platform("androidx.compose:compose-bom:2024.06.00")) // O la versión de BOM que estés usando
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core") // Para iconos básicos
    implementation("androidx.compose.material:material-icons-extended") // ¡PARA Brightness4 y Brightness7!

}
