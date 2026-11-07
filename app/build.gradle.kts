plugins { id("com.android.application") }

android {
    namespace = "com.zkelite.compucareservice"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.zkelite.compucareservice"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "2.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.work:work-runtime:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
