plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.budget2.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.budget2.app"
        minSdk = 23
        targetSdk = 34
        versionCode = 3
        versionName = "1.2"
    }

    signingConfigs {
        create("release") {
            val keystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
            val keystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
            val keyAliasValue = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
            val keyPasswordValue = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull

            val signingValues = listOf(
                keystorePath,
                keystorePassword,
                keyAliasValue,
                keyPasswordValue,
            )
            val configuredValueCount = signingValues.count { !it.isNullOrBlank() }

            check(configuredValueCount == 0 || configuredValueCount == signingValues.size) {
                "Release signing requires ANDROID_KEYSTORE_PATH, ANDROID_KEYSTORE_PASSWORD, " +
                    "ANDROID_KEY_ALIAS, and ANDROID_KEY_PASSWORD."
            }

            if (configuredValueCount == signingValues.size) {
                storeFile = file(requireNotNull(keystorePath))
                storePassword = keystorePassword
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (signingConfigs.getByName("release").storeFile != null) {
                signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "No release keystore configured; signing this local release APK with " +
                        "the debug key. It cannot update APKs signed with another key."
                )
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    testImplementation("junit:junit:4.13.2")
}
