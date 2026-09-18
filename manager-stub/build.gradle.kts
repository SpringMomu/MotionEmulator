plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.serialization)
}

android {
    namespace = "com.zhufucdev.me"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
    sourceSets.getByName("main") {
        java.setSrcDirs(listOf("../ws-plugin/sdk/stub/src/main/java"))
        manifest.srcFile("../ws-plugin/sdk/stub/src/main/AndroidManifest.xml")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.google.guava)
    implementation(libs.aventrix.jnanoid)
    implementation(libs.redempt.crunch)
    implementation(libs.google.gms.maps)
    implementation(libs.google.maps.utils)
}
