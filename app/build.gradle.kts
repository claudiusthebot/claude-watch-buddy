plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "rocks.claudiusthebot.watchbuddy"
    compileSdk = 34

    defaultConfig {
        applicationId = "rocks.claudiusthebot.watchbuddy"
        minSdk = 30
        targetSdk = 34
        versionCode = 2
        versionName = "0.2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
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
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/versions/9/**",
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties"
            )
        }
    }
}

dependencies {
    // Wear OS Compose
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
    implementation("androidx.compose.foundation:foundation:1.6.8")
    implementation("androidx.compose.material:material-icons-core:1.6.8")
    implementation("androidx.wear.compose:compose-material:1.3.1")
    implementation("androidx.wear.compose:compose-foundation:1.3.1")
    implementation("androidx.wear.compose:compose-navigation:1.3.1")
    implementation("androidx.wear:wear:1.3.0")
    implementation("androidx.wear:wear-input:1.2.0-alpha02")
    implementation("androidx.wear:wear-remote-interactions:1.0.0")

    // Activity + lifecycle
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-service:2.8.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // DataStore for settings
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // JSON — use Android's built-in org.json.JSONObject (no extra dep needed)

    debugImplementation("androidx.compose.ui:ui-tooling:1.6.8")

    testImplementation("junit:junit:4.13.2")
}
