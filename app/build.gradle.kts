import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// API keys for the sources that need one. They come from local.properties (not committed) or from an
// environment variable of the same name (the GitHub workflows pass them from repository secrets).
// A blank key is fine: that source then says so and is hidden in More.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// Signing: the same key on every build is what lets a new APK install over the old one.
// Set KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS and KEY_PASSWORD (local.properties or environment; the
// "Binder (signed)" workflow fills them from repository secrets). Without them the build is signed with
// the throwaway debug key, which still installs but cannot update an APK made with another key.
fun prop(name: String): String? = localProps.getProperty(name) ?: System.getenv(name)
val keystorePath = prop("KEYSTORE_FILE")?.takeIf { it.isNotBlank() }
val hasKeystore = keystorePath != null && file(keystorePath).exists()

fun apiKey(name: String): String =
    (localProps.getProperty(name) ?: System.getenv(name) ?: "").trim()
        .replace("\\", "").replace("\"", "") // keys never contain these; keeps the generated Java valid

android {
    namespace = "app.binder"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.binder"
        minSdk = 26
        targetSdk = 35
        // the workflow passes a rising number, so every CI build counts as newer than the one before
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 10
        versionName = "1.0.0"
        buildConfigField("String", "TMDB_KEY", "\"${apiKey("TMDB_KEY")}\"")
        buildConfigField("String", "RAWG_KEY", "\"${apiKey("RAWG_KEY")}\"")
    }
    signingConfigs {
        if (hasKeystore) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = prop("KEYSTORE_PASSWORD")
                keyAlias = prop("KEY_ALIAS")
                keyPassword = prop("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    // The release lint check can stop a build over a warning; the APK matters more here.
    lint { checkReleaseBuilds = false }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    // unit tests (they run on a normal JVM, where Android's org.json is only a stub)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
