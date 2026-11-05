plugins { id("com.android.application") }

android {
    namespace = "com.zkelite.compucareservice"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.zkelite.compucareservice"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
