plugins {
    id("com.android.application") version "8.11.0"
    id("org.jetbrains.kotlin.android") version "2.2.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
}

android {
    namespace = "com.niki914.breeno.a.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.niki914.breeno.agentic"
        minSdk = 26
        targetSdk = 34
        versionName = "1.0.0"
        versionCode = 1
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // 指定你想要支持的ABIs
            abiFilters += listOf("arm64-v8a", "x86_64")
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
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf(
            "-Xno-param-assertions",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions"
        )
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }

    lint { checkReleaseBuilds = false }
}

dependencies {
    implementation(project(":composebase"))
    implementation(project(":h"))
    implementation(project(":ipc"))
    compileOnly("de.robv.android.xposed:api:82")
    implementation("org.luckypray:dexkit:2.1.0")
    implementation("androidx.annotation:annotation:1.7.0")


    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Radius
    implementation("com.github.Kyant0:Capsule:2.1.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Material & AndroidX
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.activity:activity-compose:1.4.0")

    // Compose
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.compose.material3:material3-window-size-class:1.3.2")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.4.0-alpha16")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    debugImplementation("androidx.compose.ui:ui-tooling:1.4.0")
    implementation("androidx.compose.ui:ui-tooling-preview-android:1.8.3")
}
