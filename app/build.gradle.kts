import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
//    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.translationapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.translationapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }

        val apiKey = localProperties.getProperty("AZURE_TRANSLATOR_API_KEY") ?: ""
        val subscriptionKey = localProperties.getProperty("AZURE_TRANSLATOR_SUBSCRIPTION_KEY") ?: ""
        val region = localProperties.getProperty("AZURE_TRANSLATOR_REGION") ?: ""
        val endpoint = localProperties.getProperty("AZURE_TRANSLATOR_ENDPOINT") ?: ""

        buildConfigField("String", "AZURE_TRANSLATOR_API_KEY", "\"$apiKey\"")
        buildConfigField("String", "AZURE_TRANSLATOR_SUBSCRIPTION_KEY", "\"$subscriptionKey\"")
        buildConfigField("String", "AZURE_TRANSLATOR_REGION", "\"$region\"")
        buildConfigField("String", "AZURE_TRANSLATOR_ENDPOINT", "\"$endpoint\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.mlkit:text-recognition-chinese:16.0.0")
    implementation("com.google.mlkit:text-recognition-japanese:16.0.0")
    implementation("com.google.mlkit:text-recognition-korean:16.0.0")
    implementation("com.google.mlkit:text-recognition-devanagari:16.0.0")
    implementation("androidx.camera:camera-core:1.1.0")
    implementation("androidx.camera:camera-camera2:1.1.0")
    implementation("androidx.camera:camera-lifecycle:1.1.0")
    implementation("androidx.camera:camera-view:1.1.0")
    implementation("androidx.camera:camera-extensions:1.1.0")
    implementation("androidx.core:core-ktx:1.6.0")
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("com.google.android.material:material:1.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("com.microsoft.cognitiveservices.speech:client-sdk:1.24.0")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}