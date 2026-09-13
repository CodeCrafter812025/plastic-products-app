import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

// Real signing values live only in the developer's local, gitignored
// android/keystore.properties (storeFile, storePassword, keyAlias, keyPassword).
// When that file is absent — e.g. building in a fresh checkout or CI without
// secrets — this stays an empty Properties() with placeholder fallbacks below,
// so the build configures successfully instead of failing with a confusing
// missing-file error. The resulting release build just won't be validly signed
// until a real keystore.properties is provided.
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "ir.codecrafter.plasticproducts"
    compileSdk = 35

    defaultConfig {
        applicationId = "ir.codecrafter.plasticproducts"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            storeFile = keystoreProperties.getProperty("storeFile")?.let { file(it) }
            storePassword = keystoreProperties.getProperty("storePassword") ?: "placeholder"
            keyAlias = keystoreProperties.getProperty("keyAlias") ?: "placeholder"
            keyPassword = keystoreProperties.getProperty("keyPassword") ?: "placeholder"
        }
    }

    buildTypes {
        debug {
            // Temporarily pointed at a physical device via `adb reverse` instead of
            // the emulator's 10.0.2.2 host alias.
            buildConfigField("String", "BASE_URL", "\"http://127.0.0.1:8000/api/v1/\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
            // TODO: placeholder until the production API domain is decided
            buildConfigField("String", "BASE_URL", "\"https://api.plasticproducts.example.com/api/v1/\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // checkTestSources=false (the default) only suppresses *reporting* issues in
        // test sources — lint still analyzes them, which is what crashes on
        // PersianDateFormatterTest.kt. ignoreTestSources skips analyzing test sources
        // entirely, avoiding the crash. Present in AGP's Lint DSL since 7.0, so it's
        // available on this project's AGP 8.7.2 (see gradle/libs.versions.toml).
        ignoreTestSources = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.security.crypto)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.coil.compose)

    testImplementation(libs.junit)
}
