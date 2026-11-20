plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "app.liveisland"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.liveisland"
        minSdk = 29
        targetSdk = 35
        versionCode = (System.getenv("LI_VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("LI_VERSION_NAME") ?: "0.1.0"
    }

    // Release signing comes from CI secrets (or your local env). Without them the release APK is
    // signed with the debug key so it still installs; publish only properly signed builds.
    val ks = System.getenv("LI_KEYSTORE")
    signingConfigs {
        create("release") {
            if (ks != null && file(ks).exists()) {
                storeFile = file(ks)
                storePassword = System.getenv("LI_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("LI_KEY_ALIAS")
                keyPassword = System.getenv("LI_KEY_PASSWORD")
            } else {
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.savedstate)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.palette)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}
