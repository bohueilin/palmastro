import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Release signing (PRD §35 item 10). keystore.properties lives at the repo root and is
// never committed (see keystore.properties.template). The keystore it points to is the
// Play App Signing UPLOAD key; Google Play holds the actual app signing key.
// When the file is absent (CI without signing secrets, fresh clones) the release build
// proceeds UNSIGNED so the pipeline stays green; the resulting artifact cannot be
// uploaded to Play until it is signed.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}
val hasReleaseSigning = keystorePropertiesFile.exists()
if (!hasReleaseSigning) {
    logger.warn(
        "keystore.properties not found at ${keystorePropertiesFile.path} - " +
            "release-type builds will be UNSIGNED. Copy keystore.properties.template " +
            "to keystore.properties and fill in the upload-key credentials to sign.",
    )
}

android {
    namespace = "com.palmastro.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.palmastro.app"
        minSdk = 26
        targetSdk = 35
        versionCode = (project.findProperty("versionCode") as? String)?.toInt() ?: 1
        versionName = (project.findProperty("versionName") as? String) ?: "0.1.0"
        // Launch UI languages only (PRD §43): English + Traditional Chinese.
        resConfigs("en", "zh-rTW")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                // storeFile in keystore.properties is relative to the app module dir
                // (e.g. ../keystore/palmastro-upload.jks -> <repo>/keystore/).
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        // PRD §68 build types. Both mirror release (minified, shrunk, signed with the
        // upload key when available) and stay installable next to production via
        // distinct application ids.
        create("closedTest") {
            initWith(getByName("release"))
            applicationIdSuffix = ".ct"
            matchingFallbacks += listOf("release")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        create("partnerDemo") {
            initWith(getByName("release"))
            applicationIdSuffix = ".demo"
            matchingFallbacks += listOf("release")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    implementation(project(":contracts"))
    implementation(project(":engine-scan-quality"))
    implementation(project(":engine-palm-features"))
    implementation(project(":engine-astro"))
    implementation(project(":engine-scoring"))
    implementation(project(":engine-content"))
    implementation(project(":svc-analytics"))
    implementation(project(":data-room"))

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    val cameraVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraVersion")
    implementation("androidx.camera:camera-camera2:$cameraVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraVersion")
    implementation("androidx.camera:camera-view:$cameraVersion")

    implementation("com.google.mediapipe:tasks-vision:0.10.9")

    implementation("androidx.work:work-runtime-ktx:2.9.0")

    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}


tasks.withType<Test> {
    useJUnitPlatform()
}
