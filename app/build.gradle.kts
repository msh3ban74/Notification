import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
  val localPropsFile = rootProject.file("local.properties")
  if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { stream -> load(stream) }
  }
}

android {
  namespace = "com.notification.app"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.notification.app"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    // Read from local.properties so the real Web Client ID is never hardcoded/committed.
    // Add this line to your local.properties:
    // GOOGLE_WEB_CLIENT_ID=your-client-id.apps.googleusercontent.com
    val googleWebClientId = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")
      ?: "REPLACE_ME_GOOGLE_WEB_CLIENT_ID"
    buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")

    // Extra AI keys — from CI env (GitHub Secrets) or local.properties,
    // ALWAYS quoted here so an unset secret compiles to "" instead of
    // breaking BuildConfig generation. Never shown anywhere in the app.
    val geminiKey2 = System.getenv("GEMINI_API_KEY_2")
      ?: localProperties.getProperty("GEMINI_API_KEY_2") ?: ""
    buildConfigField("String", "GEMINI_API_KEY_2", "\"$geminiKey2\"")
    val geminiKey3 = System.getenv("GEMINI_API_KEY_3")
      ?: localProperties.getProperty("GEMINI_API_KEY_3") ?: ""
    buildConfigField("String", "GEMINI_API_KEY_3", "\"$geminiKey3\"")
    val geminiKey4 = System.getenv("GEMINI_API_KEY_4")
      ?: localProperties.getProperty("GEMINI_API_KEY_4") ?: ""
    buildConfigField("String", "GEMINI_API_KEY_4", "\"$geminiKey4\"")
    val geminiKey5 = System.getenv("GEMINI_API_KEY_5")
      ?: localProperties.getProperty("GEMINI_API_KEY_5") ?: ""
    buildConfigField("String", "GEMINI_API_KEY_5", "\"$geminiKey5\"")
    // Unlimited extra keys in ONE secret: a comma/semicolon-separated list.
    // Add as many as you like later WITHOUT any code or build change.
    val geminiKeysCsv = System.getenv("GEMINI_API_KEYS")
      ?: localProperties.getProperty("GEMINI_API_KEYS") ?: ""
    buildConfigField("String", "GEMINI_API_KEYS", "\"$geminiKeysCsv\"")

    val groqKey = System.getenv("GROQ_API_KEY")
      ?: localProperties.getProperty("GROQ_API_KEY") ?: ""
    buildConfigField("String", "GROQ_API_KEY", "\"$groqKey\"")
    val groqKeysCsv = System.getenv("GROQ_API_KEYS")
      ?: localProperties.getProperty("GROQ_API_KEYS") ?: ""
    buildConfigField("String", "GROQ_API_KEYS", "\"$groqKeysCsv\"")

    // Future providers — placeholders so adding Claude / OpenAI later is just a
    // secret + a small provider block, no schema change. Empty until set.
    val claudeKeysCsv = System.getenv("CLAUDE_API_KEYS")
      ?: localProperties.getProperty("CLAUDE_API_KEYS") ?: ""
    buildConfigField("String", "CLAUDE_API_KEYS", "\"$claudeKeysCsv\"")
    val openaiKeysCsv = System.getenv("OPENAI_API_KEYS")
      ?: localProperties.getProperty("OPENAI_API_KEYS") ?: ""
    buildConfigField("String", "OPENAI_API_KEYS", "\"$openaiKeysCsv\"")

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.auth)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
