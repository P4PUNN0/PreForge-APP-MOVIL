import java.util.Properties

plugins {
    id("com.google.gms.google-services")
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

val propiedadesLocales = Properties().apply {
    val archivoPropiedadesLocales = rootProject.file("local.properties")
    if (archivoPropiedadesLocales.exists()) {
        archivoPropiedadesLocales.inputStream().use { entrada -> load(entrada) }
    }
}

val claveApiGemini = providers.gradleProperty("GEMINI_API_KEY")
    .orElse(providers.environmentVariable("GEMINI_API_KEY"))
    .orElse(propiedadesLocales.getProperty("GEMINI_API_KEY", ""))
    .get()
val claveApiGeminiEscapada = claveApiGemini
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

android {
    namespace = "com.example.preforge"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.preforge"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "GEMINI_API_KEY", "\"$claveApiGeminiEscapada\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    val versionRoom = "2.7.2"
    implementation("androidx.room:room-runtime:$versionRoom")
    implementation("androidx.room:room-ktx:$versionRoom")
    ksp("androidx.room:room-compiler:$versionRoom")

    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303")
    testImplementation("org.robolectric:robolectric:4.16.1")
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
