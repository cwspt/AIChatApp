import java.util.Properties

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("com.google.devtools.ksp")
}

val signingProperties = Properties().apply {
  listOf(
    rootProject.file("release-signing.properties"),
    rootProject.file("keystore.properties")
  ).filter { it.exists() }.forEach { file ->
    file.inputStream().use(::load)
  }
}

fun releaseSigningValue(key: String): String? {
  val gradleKey = "aichat.release.$key"
  val envKey = "AICHAT_RELEASE_${key.uppercase()}"
  return signingProperties.getProperty("release.$key")
    ?.takeIf { it.isNotBlank() }
    ?: signingProperties.getProperty(key)?.takeIf { it.isNotBlank() }
    ?: providers.gradleProperty(gradleKey).orNull?.takeIf { it.isNotBlank() }
    ?: System.getenv(envKey)?.takeIf { it.isNotBlank() }
}

val releaseStoreFilePath = releaseSigningValue("storeFile")
val releaseStorePassword = releaseSigningValue("storePassword")
val releaseKeyAlias = releaseSigningValue("keyAlias")
val releaseKeyPassword = releaseSigningValue("keyPassword")
val hasReleaseSigning = !releaseStoreFilePath.isNullOrBlank() &&
  !releaseStorePassword.isNullOrBlank() &&
  !releaseKeyAlias.isNullOrBlank() &&
  !releaseKeyPassword.isNullOrBlank()

android {
  namespace = "com.personal.aichat"
  compileSdk = 34

  signingConfigs {
    if (hasReleaseSigning) {
      create("release") {
        storeFile = rootProject.file(releaseStoreFilePath!!)
        storePassword = releaseStorePassword
        keyAlias = releaseKeyAlias
        keyPassword = releaseKeyPassword
      }
    }
  }

  defaultConfig {
    applicationId = "com.personal.aichat"
    minSdk = 24
    targetSdk = 34
    versionCode = 1
    versionName = "0.1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables {
      useSupportLibrary = true
    }
  }

  buildTypes {
    debug {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-debug"
    }

    release {
      isMinifyEnabled = false
      if (hasReleaseSigning) {
        signingConfig = signingConfigs.getByName("release")
      }
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
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
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.security:security-crypto:1.1.0-alpha06")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
  implementation("androidx.activity:activity-compose:1.9.1")
  implementation(platform("androidx.compose:compose-bom:2024.06.00"))
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.room:room-runtime:2.6.1")
  implementation("androidx.room:room-ktx:2.6.1")
  implementation("androidx.datastore:datastore-preferences:1.1.1")
  implementation("com.google.code.gson:gson:2.11.0")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
  implementation("org.conscrypt:conscrypt-android:2.5.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

  ksp("androidx.room:room-compiler:2.6.1")

  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")

  testImplementation("junit:junit:4.13.2")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
  testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

  androidTestImplementation("androidx.test.ext:junit:1.2.1")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
  androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
  androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
