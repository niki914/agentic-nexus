plugins {
    id("com.android.application") version "8.11.0"
    id("org.jetbrains.kotlin.android") version "2.2.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
}

android {
    namespace = "com.niki914.nexus.agentic"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.niki914.nexus.agentic"
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

    // Android root
    compileOnly("de.robv.android.xposed:api:82")
    implementation("org.luckypray:dexkit:2.1.0")

    // LLM & networking
    implementation("com.github.niki914:S3ss10n:2.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Third-party UI
    implementation("com.github.Kyant0:Capsule:2.1.0")

    // Material & AndroidX
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.activity:activity-compose:1.4.0")
    implementation("androidx.annotation:annotation:1.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Compose
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.compose.material3:material3-window-size-class:1.3.2")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.4.0-alpha16")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    debugImplementation("androidx.compose.ui:ui-tooling:1.4.0")
    implementation("androidx.compose.ui:ui-tooling-preview-android:1.8.3")
}

// 获取 ADB 路径
fun getAdbPath(): String {
    val sdkDir = project.extensions.getByType<com.android.build.gradle.BaseExtension>().sdkDirectory
    val adbFile = File(sdkDir, "platform-tools/adb")
    return if (adbFile.exists()) adbFile.absolutePath else "adb"
}

// 注册 adb reverse 任务
val adbReverse = tasks.register("adbReverse") {
    group = "custom"
    description = "Execute adb reverse for port 8788 and 1234"
    
    doLast {
        val adbPath = getAdbPath()
        println("Using ADB path: $adbPath")
        try {
            project.exec {
                commandLine(adbPath, "reverse", "tcp:8788", "tcp:8788")
                isIgnoreExitValue = true
            }
            project.exec {
                commandLine(adbPath, "reverse", "tcp:1234", "tcp:1234")
                isIgnoreExitValue = true
            }
            println("ADB reverse successful.")
        } catch (e: Exception) {
            println("Failed to execute adb reverse: ${e.message}")
        }
    }
}

// 注册启动 Python Server 任务
val startServer = tasks.register("startServer") {
    group = "custom"
    description = "Start simple python server"
    
    doFirst {
        println("Starting Python Server in background...")
        val pythonCmds = listOf("python3", "/usr/bin/python3", "/usr/local/bin/python3", "/opt/homebrew/bin/python3")
        var started = false
        
        for (cmd in pythonCmds) {
            try {
                ProcessBuilder(cmd, "server.py")
                    .directory(file("../server"))
                    .inheritIO()
                    .start()
                println("Python Server started successfully using: $cmd")
                started = true
                break
            } catch (e: Exception) {
                // Continue to next command
            }
        }
        
        if (!started) {
            println("Failed to start Python Server: python3 not found in common paths.")
        }
    }
}

// 自动挂载
tasks.configureEach {
    // 只要是执行安装或者构建，就尝试运行这两个任务
    if (name.startsWith("install") || name.startsWith("assemble")) {
        dependsOn(adbReverse)
//        dependsOn(startServer)
    }
}
