import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

val localProperties = Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) {
        FileInputStream(localFile).use {load(it)}
    }
}

val NAVER_CLIENT_ID: String = (localProperties["NAVER_CLIENT_ID"] as? String) ?: ""
val NAVER_CLIENT_SECRET: String = (localProperties["NAVER_CLIENT_SECRET"] as? String) ?: ""
val KAKAO_NATIVE_APP_KEY: String = (localProperties["KAKAO_NATIVE_APP_KEY"] as? String) ?: ""
val GOOGLE_CLIENT_ID: String = (localProperties["GOOGLE_CLIENT_ID"] as? String) ?: ""

android {
    namespace = "com.leeandyun.moduchongmu"
    compileSdk = 35


    defaultConfig {
        applicationId = "com.leeandyun.moduchongmu"
        manifestPlaceholders["appId"] = applicationId ?: ""
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        buildConfigField("String", "NAVER_CLIENT_ID", "\"$NAVER_CLIENT_ID\"")
        buildConfigField("String", "NAVER_CLIENT_SECRET", "\"$NAVER_CLIENT_SECRET\"")
        buildConfigField("String", "KAKAO_NATIVE_APP_KEY", "\"$KAKAO_NATIVE_APP_KEY\"")
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$GOOGLE_CLIENT_ID\"")
        resValue("string", "KAKAO_REDIRECT_KEY", "kakao\"$KAKAO_NATIVE_APP_KEY\"")
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.lottie)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.oauth) // jdk 11
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.v2.user) // 카카오 로그인 API 모듈
    implementation(libs.v2.share) // 카카오톡 공유 API 모듈
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
}


