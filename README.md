# COMPUCARE SERVICE Android app

Android WebView client for the hosted service app: https://compucare-service-sf3up4.v2.appdeploy.ai/

- Application ID: `com.zkelite.compucareservice`
- Minimum Android: 7.0 (API 24)
- Compile/target SDK: 36 (Android 16)
- Java 17, Android Gradle Plugin 8.10.1, Gradle 8.11.1

The Android client loads the live HTTPS app; backend, database and credentials remain hosted. The WebView enables JavaScript and DOM storage for the hosted app. Review the app's actual collection, transfer, retention, deletion, backup and log behavior before publishing privacy disclosures.

## Builds and signing

GitHub Actions builds a debug APK for testing and an **unsigned release AAB** as a build artifact. The debug APK is signed with an ephemeral debug key and is not for Play Store submission. The unsigned AAB is also not upload-ready. A production release must be signed using Play App Signing / an owner-controlled upload key; do not commit signing keys or passwords to this repository.

## Pre-publication checks

See [Play Store submission checklist](docs/PLAY_STORE_SUBMISSION_CHECKLIST.md) and [privacy policy draft](docs/PRIVACY_POLICY_DRAFT.md). The privacy policy is a draft only and must be verified against the real backend and hosting configuration before publication.

Test sign-in, ticket submission, photo upload, ticket closure/deletion, external contact links, and network failure behavior on physical Android devices. Some identity providers restrict embedded WebViews; use their supported system-browser/Custom Tabs integration if needed rather than weakening authentication.
