import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val signingProperties = Properties().apply {
    val propertiesFile = rootProject.file("signing.properties")
    if (propertiesFile.exists()) propertiesFile.inputStream().use { load(it) }
}

fun signingValue(name: String): String? =
    providers.environmentVariable(name).orNull ?: signingProperties.getProperty(name.lowercase().replace('_', '.'))

android {
    namespace = "com.reader.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.reader.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            val keystorePath = signingValue("READER_KEYSTORE")
            val keystorePassword = signingValue("READER_STORE_PASSWORD")
            val keyAlias = signingValue("READER_KEY_ALIAS")
            val keyPassword = signingValue("READER_KEY_PASSWORD")

            if (keystorePath != null && keystorePassword != null && keyAlias != null && keyPassword != null) {
                signingConfig = signingConfigs.create("release") {
                    storeFile = rootProject.file(keystorePath)
                    storePassword = keystorePassword
                    this.keyAlias = keyAlias
                    this.keyPassword = keyPassword
                }
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("com.google.code.gson:gson:2.11.0")

    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
