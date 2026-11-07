# COMPUCARE SERVICE Native Android app

A native Android client for COMPUCARE SERVICE. The app uses Android native views and does not render the website inside a WebView.

## Capabilities
- Existing COMPUCARE account sign-in through the system browser and a one-time secure token handoff.
- Native customer and staff workspace screens.
- Native ticket list, service request form and staff ticket status updates.
- Local SQLite cache for previously synchronized tickets.
- Offline request/status outbox with automatic WorkManager synchronization when connectivity returns.
- Access token encrypted with Android Keystore AES-GCM; backend and database remain hosted.

## Build
- Application ID: `com.zkelite.compucareservice`
- Minimum Android: 7.0 (API 24)
- Compile/target SDK: 36
- Java 17, Android Gradle Plugin 8.10.1, Gradle 8.11.1

GitHub Actions builds and emulator-smoke-tests the APK. Debug builds are for testing, not Play Store publication. A production APK/AAB must be signed using the owner's upload key.

## Offline behavior
Previously synchronized tickets remain readable offline. New service requests and permitted staff status changes are queued locally and sent when a network is available. First sign-in, live data refresh and server-side actions require connectivity. If a session expires, the user must sign in again online before queued writes can sync.

## Authentication handoff
The website's `?native=1` sign-in return path must exchange the restored AppDeploy access token to the Android deep link `com.zkelite.compucareservice://auth`. Do not put credentials or long-lived refresh tokens in the APK.
