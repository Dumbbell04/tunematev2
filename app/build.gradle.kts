plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.tunematev2"
    compileSdk = 34

    // BuildConfig 기능을 활성화
    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.tunematev2"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        // 서버 URL을 정의 (여기서 민감한 정보를 관리할 수 있음)
        buildConfigField("String", "BASE_URL", "\"http://3.26.61.213:5000\"") // 환경에 따라 URL을 변경 가능

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.0")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
