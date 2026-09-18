plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.zhufucdev.ws_plugin"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.zhufucdev.ws_plugin"
        minSdk = 24
        targetSdk = 34
        versionCode = 5
        versionName = "1.2.4"
    }
    buildFeatures { buildConfig = true }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(libs.corektx)
    implementation(libs.kotlinx.coroutine)
    ksp(libs.yukihook.ksp)
    implementation(libs.yukihook)
    compileOnly(libs.xposed)
    // Build the fixed SDK sources in this repository, never the old Maven binaries.
    implementation(project(":stub"))
    implementation(project(":plugin"))
    implementation(project(":xposed"))
}

// YukiHook 1.2 writes its entry-point descriptors into src/main during KSP.
// On a fresh checkout, merging assets before KSP silently produces an APK that
// LSPosed cannot load. Order both descriptors' packaging after their generation.
for (variant in listOf("Debug", "Release")) {
    tasks.matching {
        it.name == "merge${variant}Assets" || it.name == "process${variant}JavaRes"
    }.configureEach {
        dependsOn("ksp${variant}Kotlin")
    }
}
