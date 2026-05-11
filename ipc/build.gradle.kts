plugins {
    id("com.android.library") version "8.11.0"
    id("org.jetbrains.kotlin.android") version "2.2.0"
}

android {
    namespace = "com.niki914.nexus.ipc"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("com.google.android.material:material:1.12.0")
}
