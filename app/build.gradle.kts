plugins {
    alias(libs.plugins.android.application)

    // 앱 푸시알림 설정
    id("com.google.gms.google-services")
}

android {
    viewBinding {
        enable = true
    }
    namespace = "com.example.fixsiheung"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }


    defaultConfig {
        applicationId = "com.example.fixsiheung"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
        }

        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // 이미지 로딩 라이브러리
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // HTTP 통신 라이브러리
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // 앱 푸시알림 설정
    implementation("com.google.firebase:firebase-messaging:23.4.0")

    // 카카오 맵 SDK 추가 (버전은 최신으로 확인 필요)
    implementation("com.kakao.maps.open:android:2.13.1")
    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.0.1")
}