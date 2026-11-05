# COMPUCARE SERVICE — Google Play submission checklist

## Project baseline
- Package: `com.zkelite.compucareservice`
- Version currently in source: `1.0.0` (versionCode 1)
- Minimum Android: API 24
- Target/compile SDK: API 36
- Java 17, AGP 8.10.1, Gradle 8.11.1
- Web app: https://compucare-service-sf3up4.v2.appdeploy.ai/
- Support email: info@compucare.zkelite.store

## Listing draft
**App name:** COMPUCARE SERVICE

**Short description (draft):** Request product repairs, submit fault photos, and manage service tickets.

**Full description (draft):**
COMPUCARE SERVICE helps customers submit product repair and service requests. Provide your name, address and contact details, describe the issue, and attach photographs of the faulty product. The service team can use the submitted details to review the request, contact you and coordinate service.

The app requires an internet connection to access the hosted service. The operator states that customer-submitted ticket details and faulty-product photographs are removed from the server when the ticket is closed. This retention statement must be verified against backups, logs and service-provider copies before publication.

## Required before submission
- [ ] Verify Google Play developer account type and complete identity verification in Play Console.
- [ ] Pay Google's one-time developer registration fee.
- [ ] Confirm whether the business/service model requires an Organization account rather than Personal.
- [ ] Run API 36 build and resolve CI failures.
- [ ] Test sign-in, ticket submission, photo upload, ticket closure/deletion, support links, and offline/error states on physical Android devices.
- [ ] Verify privacy policy against actual data flows, subprocessors, backups and logs; publish an approved policy URL.
- [ ] Complete Data Safety based on actual SDK/backend behavior. Include owner-described name, address, contact information and photos, and review any additional diagnostics/identifiers.
- [ ] Complete app access instructions, content rating, target audience, ads declaration and other Play Console app-content sections accurately.
- [ ] Prepare store assets: 512×512 app icon, feature graphic (1024×500), phone screenshots and any other requested screenshots.
- [ ] Configure secure production upload key / Play App Signing. Never put keystore files or passwords in Git.
- [ ] Build and verify a production-signed Android App Bundle (AAB). CI artifact is unsigned and not upload-ready.
- [ ] Complete required testing track and any personal-account closed-testing requirements shown in Play Console.
- [ ] Submit for Google review; approval is not guaranteed.

## Important quality risk
The Android client is a WebView wrapper for a hosted website. Review Google Play's minimum functionality and user-experience requirements carefully and ensure the app provides a reliable, useful mobile experience beyond a thin website wrapper. Google makes the final review decision.
