import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

fun escapeForBuildConfig(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

val localProps = Properties()
listOf(
    rootProject.file("config.local.properties"),
    rootProject.file("../config.local.properties"),
).firstOrNull { it.exists() }?.inputStream()?.use { localProps.load(it) }


android {
    namespace = "com.yuyue.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yuyue.app"
        minSdk = 29
        targetSdk = 35
        versionCode = 17
        versionName = "1.0.15+" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd.HHmm"))
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
        // Legacy cloud API base (unused by local-first CaptureKernel). Emulator: 10.0.2.2
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"${escapeForBuildConfig(localProps.getProperty("API_BASE_URL", "http://10.0.2.2:8080"))}\"",
        )
        // 密钥只打进 APK，不进设置页（来自根目录 config.local.properties）
        buildConfigField(
            "String",
            "DEFAULT_LLM_API_KEY",
            "\"${escapeForBuildConfig(localProps.getProperty("LLM_API_KEY", ""))}\"",
        )
        buildConfigField(
            "String",
            "DEFAULT_LLM_BASE_URL",
            "\"${escapeForBuildConfig(localProps.getProperty("LLM_BASE_URL", "https://api.minimaxi.com"))}\"",
        )
        buildConfigField(
            "String",
            "DEFAULT_LLM_MODEL",
            "\"${escapeForBuildConfig(localProps.getProperty("LLM_MODEL", "MiniMax-M2.7"))}\"",
        )
        buildConfigField("String", "DEFAULT_EMBEDDING_MODEL", "\"embo-01\"")
        buildConfigField(
            "String",
            "SILICONFLOW_API_KEY",
            "\"${escapeForBuildConfig(localProps.getProperty("SILICONFLOW_API_KEY", ""))}\"",
        )
        buildConfigField(
            "String",
            "DEFAULT_FAST_LLM_BASE_URL",
            "\"${escapeForBuildConfig(localProps.getProperty("FAST_LLM_BASE_URL", "https://api.siliconflow.cn"))}\"",
        )
        buildConfigField(
            "String",
            "DEFAULT_FAST_LLM_MODEL",
            "\"${escapeForBuildConfig(localProps.getProperty("FAST_LLM_MODEL", "Qwen/Qwen2.5-7B-Instruct"))}\"",
        )
        // Task 5: Navigation Compose shell (Home/Time/Recall/Vault + global Capture).
        // Default true when the new shell compiles; set false to force LegacyNavHost.
        buildConfigField("boolean", "FEATURE_REBUILD_SHELL", "true")
        // Task 10: dual-write new captures/entries to yuyue-domain.db (QA/debug default on).
        buildConfigField("boolean", "FEATURE_DOMAIN_WRITE", "true")
        manifestPlaceholders["usesCleartextTraffic"] = "true"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    buildTypes {
        getByName("debug") {
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        create("qa") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            isDebuggable = true
            matchingFallbacks += listOf("debug")
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        getByName("release") {
            isMinifyEnabled = false
            manifestPlaceholders["usesCleartextTraffic"] = "false"
            buildConfigField("String", "DEFAULT_LLM_API_KEY", "\"\"")
            buildConfigField("String", "SILICONFLOW_API_KEY", "\"\"")
            // Keep dual-write off for store release until QA cutover is signed off.
            buildConfigField("boolean", "FEATURE_DOMAIN_WRITE", "false")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    androidResources {
        noCompress += "onnx"
    }
}

val voskZip = file("src/main/assets/vosk-model-small-cn-0.22.zip")
val bgeDir = file("src/main/assets/bge-small-zh-v1.5")
val bgeOnnx = file("$bgeDir/model_quantized.onnx")
val bgeVocab = file("$bgeDir/vocab.txt")

fun downloadUrl(urls: List<String>, dest: File, minBytes: Long) {
    if (dest.exists() && dest.length() > minBytes) return
    dest.parentFile.mkdirs()
    var last: Exception? = null
    for (url in urls) {
        try {
            val conn = URI(url).toURL().openConnection()
            conn.setRequestProperty("User-Agent", "Yuyue/1.0")
            conn.connectTimeout = 60_000
            conn.readTimeout = 180_000
            conn.getInputStream().use { input -> dest.outputStream().use { input.copyTo(it) } }
            if (dest.length() > minBytes) return
        } catch (e: Exception) {
            last = e
            dest.delete()
        }
    }
    throw last ?: IllegalStateException("download failed: $dest")
}

tasks.register("downloadVoskModel") {
    outputs.file(voskZip)
    doLast {
        if (voskZip.exists() && voskZip.length() > 1_000_000L) return@doLast
        voskZip.parentFile.mkdirs()
        URI("https://alphacephei.com/vosk/models/vosk-model-small-cn-0.22.zip").toURL().openStream().use { input ->
            voskZip.outputStream().use { input.copyTo(it) }
        }
    }
}
tasks.register("downloadBgeModel") {
    outputs.files(bgeOnnx, bgeVocab)
    doLast {
        try {
            downloadUrl(
                listOf(
                    "https://huggingface.co/Xenova/bge-small-zh-v1.5/resolve/main/onnx/model_quantized.onnx",
                    "https://hf-mirror.com/Xenova/bge-small-zh-v1.5/resolve/main/onnx/model_quantized.onnx",
                ),
                bgeOnnx,
                1_000_000L,
            )
            downloadUrl(
                listOf(
                    "https://huggingface.co/Xenova/bge-small-zh-v1.5/resolve/main/vocab.txt",
                    "https://hf-mirror.com/Xenova/bge-small-zh-v1.5/resolve/main/vocab.txt",
                ),
                bgeVocab,
                1_000L,
            )
        } catch (e: Exception) {
            logger.warn("BGE model download skipped: ${e.message}")
        }
    }
}
tasks.configureEach {
        if (name == "preDebugBuild" || name == "preReleaseBuild" || name == "preQaBuild") {
            dependsOn("downloadVoskModel", "downloadBgeModel")
        }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.security.crypto)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.coroutines.android)
    implementation("com.alphacephei:vosk-android:0.3.75@aar")
    implementation("net.java.dev.jna:jna:5.17.0@aar")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.20.0")
    debugImplementation(libs.compose.ui.tooling.preview)
    testImplementation("junit:junit:4.13.2")
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation("org.xerial:sqlite-jdbc:3.47.1.0")
}
