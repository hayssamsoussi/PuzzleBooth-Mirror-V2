import org.jetbrains.kotlin.storage.CacheResetOnProcessCanceled.enabled

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.puzzlebooth.server"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        targetSdk = 34
        versionCode = 5
        versionName = "2.3"

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
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }

    flavorDimensions("version")
    productFlavors {
        create("remote") {
            dimension = "version"
            applicationId = "com.puzzlebooth.remote"
            versionCode = 6
            versionName = "2.3"
        }

        create("server") {
            dimension = "version"
            applicationId = "com.puzzlebooth.server"
            versionCode = 6
            versionName = "2.3"
        }
    }
}

dependencies {

    implementation("com.google.firebase:firebase-storage-ktx:20.3.0")
    implementation("androidx.activity:activity:1.8.0")
    implementation("com.google.firebase:firebase-crashlytics:19.0.3")
    val nav_version = "2.7.4"

    // Java language implementation
    implementation("androidx.navigation:navigation-fragment:$nav_version")
    implementation("androidx.navigation:navigation-ui:$nav_version")

    // Kotlin
    implementation("androidx.navigation:navigation-fragment-ktx:$nav_version")
    implementation("androidx.navigation:navigation-ui-ktx:$nav_version")
    implementation("org.greenrobot:eventbus:3.3.1")

    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Feature module Support
    implementation("androidx.navigation:navigation-dynamic-features-fragment:$nav_version")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.github.kittinunf.fuel:fuel-gson:2.3.1")

    implementation("io.github.pilgr:paperdb:2.7.2")
    implementation("com.github.kenglxn.QRGen:android:3.0.1")

    // Testing Navigation
    androidTestImplementation("androidx.navigation:navigation-testing:$nav_version")
    implementation("com.github.kittinunf.fuel:fuel:2.3.1")
    implementation("com.google.code.gson:gson:2.8.6")    // Navigation library
    implementation("com.konaire.numeric-keyboard:numeric-keyboard:1.2.0")
    implementation("com.google.android.gms:play-services-nearby:18.5.0")
    // Jetpack Compose Integration
    implementation("androidx.navigation:navigation-compose:$nav_version")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("io.reactivex.rxjava2:rxjava:2.2.10")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("com.google.zxing:core:3.4.0")
    implementation("io.github.g00fy2.quickie:quickie-bundled:1.8.0")
    implementation("com.otaliastudios:cameraview:2.7.2")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("com.linkedin.dexmaker:dexmaker:2.28.3")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.kongzue.dialogx:DialogX:0.0.48")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}