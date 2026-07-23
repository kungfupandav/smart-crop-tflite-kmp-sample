plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.metro)
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // Vendored TensorFlowLiteC (C API) xcframework, used by the iOS SaliencyEngine.
    // Run `./scripts/fetch-tflitec.sh` once to download it (git-ignored).
    val tflcXcframework = "$projectDir/nativeInterop/TensorFlowLiteC.xcframework"
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        // Pick the matching xcframework slice for this Kotlin/Native target.
        val slice = if (target.name == "iosArm64") "ios-arm64" else "ios-arm64_x86_64-simulator"
        val frameworkDir = "$tflcXcframework/$slice"

        target.compilations.getByName("main").cinterops.create("tflitec") {
            defFile(project.file("src/nativeInterop/cinterop/tflitec.def"))
            compilerOpts("-F$frameworkDir")
        }

        target.binaries.framework {
            baseName = "shared"
            isStatic = true
            // The final link into the app also needs these (see iosApp/project.yml),
            // but declaring them keeps Kotlin's own tooling consistent.
            linkerOpts("-F$frameworkDir", "-framework", "TensorFlowLiteC", "-lc++")
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)

            // Navigation
            implementation(libs.navigation.compose)

            // Lifecycle / ViewModel
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)

            // Kotlinx
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)

            // Coil
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            // Room (Multiplatform)
            implementation(libs.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)

            // TFLite
            implementation(libs.tflite)
            implementation(libs.tflite.gpu)
            implementation(libs.tflite.support)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosX64", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

android {
    namespace = "com.smartcrop.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
