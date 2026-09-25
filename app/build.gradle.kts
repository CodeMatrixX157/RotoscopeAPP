plugins {
    id("com.android.application")
    // REMOVED: id("org.jetbrains.kotlin.android") — AGP 9.0 has built-in Kotlin
}

android {
    namespace = "com.example.rotoscope"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.rotoscope"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-module1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // REMOVED: kotlinOptions { jvmTarget = "17" } — migrated to the kotlin block below

    buildFeatures {
        compose = true
    }

    // NOTE: With Kotlin 2.x + AGP 9.0, you should use the Compose Compiler Gradle plugin
    // instead of the old composeOptions.kotlinCompilerExtensionVersion.
    // See the "Important Note" below for the alternative.

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// NEW: Migrated from kotlinOptions. This sets the JVM target for Kotlin compilation[citation:17].
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}