import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.baselineprofile)
}

// 默认签名直接用仓库根目录内置的 melodia-release.jks（别名与密码均为 android）
// —— CI 与本地开箱即用，无需任何额外配置。
// 想换签名时用 local.properties（本地）或同名环境变量（CI）覆盖；
// 自定义 keystore 必须四项给全，否则视为未配置，release 退回调试签名。
val keystoreProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(key: String, env: String): String? =
    (keystoreProps.getProperty(key) ?: System.getenv(env))?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("RELEASE_STORE_FILE", "RELEASE_STORE_FILE")
        ?: "melodia-release.jks".takeIf { rootProject.file(it).exists() }
// 只有内置 keystore 才套用固定别名/密码，自定义 keystore 必须显式配置四项
val bundledDefault: String? = "android".takeIf { releaseStoreFile == "melodia-release.jks" }
val releaseStorePassword = signingValue("RELEASE_STORE_PASSWORD", "RELEASE_STORE_PASSWORD") ?: bundledDefault
val releaseKeyAlias = signingValue("RELEASE_KEY_ALIAS", "RELEASE_KEY_ALIAS") ?: bundledDefault
val releaseKeyPassword = signingValue("RELEASE_KEY_PASSWORD", "RELEASE_KEY_PASSWORD") ?: bundledDefault

val hasReleaseSigning = releaseStoreFile != null &&
        releaseStorePassword != null &&
        releaseKeyAlias != null &&
        releaseKeyPassword != null &&
        rootProject.file(releaseStoreFile).exists()

android {
    namespace = "com.lin0721.linmusic"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.lin0721.linmusic"
        minSdk = 26
        targetSdk = 36
        // CI 打 tag 发布时通过 -PreleaseVersionCode/-PreleaseVersionName 覆盖，本地构建使用 fork 默认版本
        versionCode = (project.findProperty("releaseVersionCode") as String?)?.toIntOrNull() ?: 2010199
        versionName = (project.findProperty("releaseVersionName") as String?) ?: "2.1.1-rainac.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // USB 独占输出的原生层：libusb 与等时输出流
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                // 未配置签名时沿用调试签名，保证 assembleRelease 始终可产出可安装包
                signingConfigs.getByName("debug")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }
    packaging {
        dex {
            useLegacyPackaging = false
        }
    }
    testOptions {
        unitTests {
            // AppLogger 内部调用 android.util.Log，纯 JVM 单测下未做该 mock 会直接抛异常
            isReturnDefaultValues = true
        }
    }
}

// Room Gradle 插件在 AGP 9 下会重复注册 copyRoomSchemas，改用 KSP 参数导出 schema
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// 自定义输出 APK 文件名：Melodia-v{versionName}-{buildType}.apk
base {
    archivesName.set("Melodia-v${android.defaultConfig.versionName}")
}

baselineProfile {
    automaticGenerationDuringBuild = false
    saveInSrc = true
}

// Room Gradle 插件在 AGP 9 下会重复注册 copyRoomSchemas，改用 KSP 参数导出 schema
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Network
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)

    // DI - Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Image loading - Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Media3 & Media
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.documentfile)
    implementation(libs.taglib)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.haze)
    implementation(libs.android.image.cropper)
    implementation(libs.zxing.core)
    implementation(libs.bouncycastle)
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.org.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}