# COMPUCARE SERVICE Android wrapper

Android WebView shell for the existing hosted app: https://compucare-service-sf3up4.v2.appdeploy.ai/

- Application ID: com.zkelite.compucareservice
- Minimum Android: 7.0 (API 24)
- Target SDK: 35
- Java 17, Android Gradle Plugin 8.7.3, Gradle 8.9

The Android client loads the live HTTPS app; backend, database and credentials remain hosted. No SMTP/database/API secrets are embedded. JavaScript and DOM storage are enabled for the React app; file/content access and cleartext HTTP are disabled.

Builds are produced by the GitHub Actions workflow. Debug APKs are signed with the ephemeral Android debug key and are for testing only. A production release requires a company-controlled release keystore and signing process.

Test AppDeploy sign-in on physical Android devices: some identity providers restrict embedded WebViews. If the provider blocks this flow, use its supported system-browser/Custom Tabs integration rather than weakening authentication.
