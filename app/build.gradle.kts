plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.kapt")
    id("kotlin-parcelize")
}

android {
    namespace = "com.omaster.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.omaster.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 200
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        resValue("string", "app_storage_recipients", "")
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "changeme"
            keyAlias = System.getenv("KEY_ALIAS") ?: "omaster"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "changeme"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isZipAlignEnabled = true

            enableAndroidSignaturesV4()
            enableV5Signing = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            isDebuggable = false

            isCrunchPngs = true
            isCrunchResources = true
        }

        debug {
            isMinifyEnabled = false
            isDebuggable = true
            isJniDebuggable = true
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

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/NOTICE*"
        }
    }
}

dependencyLocking {
    lockAllConfigurations()
    lockMode.set(LockMode.PREFER_PROJECT)
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.0")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("com.google.code.gson:gson:2.11.0") {
        exclude(group = "com.google.errorprone", module = "annotations")
    }

    implementation("io.coil-kt:coil-compose:2.6.0") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-parcelize-runtime")
    }

    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.jakewharton.timber:timber:5.0.1")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("androidx.camera:camera-core:1.4.0-alpha05")
    implementation("androidx.camera:camera-camera2:1.4.0-alpha05")
    implementation("androidx.camera:camera-lifecycle:1.4.0-alpha05")
    implementation("androidx.camera:camera-view:1.4.0-alpha05")

    implementation("com.google.mlkit:image-labeling:17.0.8")
    implementation("com.google.android.gms:play-services-tasks:18.1.0")

    implementation("androidx.work:work-runtime-ktx:2.9.0")

    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.robolectric:robolectric:4.13")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kapt {
    correctErrorTypes = true
    arguments {
        arg("dagger.hilt.disableModulesHaveInstallInCheck", "true")
    }
}
