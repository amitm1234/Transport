plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.transport1"
    compileSdk = 36

    defaultConfig {  
        applicationId = "com.example.transport1"  
        minSdk = 24  
        targetSdk = 36  
        versionCode = 1  
        versionName = "1.0"  

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
        sourceCompatibility = JavaVersion.VERSION_17  
        targetCompatibility = JavaVersion.VERSION_17  
    }  

    kotlinOptions {  
        jvmTarget = "17"  
    }  

    buildFeatures {  
        viewBinding = true  
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.7.0")  
    implementation("com.google.android.material:material:1.10.0")  
    implementation("androidx.activity:activity-ktx:1.8.0")  
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")  

    // Firebase
    implementation("com.google.firebase:firebase-auth:22.3.1")
    implementation("com.google.firebase:firebase-database:20.3.1")
    implementation("com.google.firebase:firebase-storage:20.3.0")

    // Glide  
    implementation("com.github.bumptech.glide:glide:4.16.0")  
    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // Testing  
    testImplementation("junit:junit:4.13.2")  
    androidTestImplementation("androidx.test.ext:junit:1.1.5")  
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}