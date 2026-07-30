# HausaNovels Android TWA v2.1.6

GitHub-ready Trusted Web Activity for `https://hausanovels.ng`.

## Included fixes

- Uses one clean TWA source tree with no old WebView `MainActivity.java` or obsolete splash resources.
- Validates every local Android resource reference before Gradle starts.
- Reads debug and release certificate fingerprints from the APK files actually built, so it does not depend on `$HOME/.android/debug.keystore`.
- Google Sign-In returns from Chrome through the package-restricted `hausanovels://oauth/callback` deep link.
- The Android return activity validates the destination and reopens only `https://hausanovels.ng` inside the TWA.
- The app header uses normal mobile-Chrome spacing without duplicated safe-area padding.
- The adaptive launcher icon keeps all important artwork inside Android's safe zone, preventing cropping.
- The splash screen contains only the icon, **Hausa Novels**, a progress bar, and percentage.
- `compileSdk` and `targetSdk` are API 36.

## Project settings

- Package: `ng.hausanovels.app`
- Version: `2.1.6` (`versionCode 216`)

## Important GitHub update method

Upload this as a clean replacement. Do not only add the new files over an old project without deleting obsolete files. Version 2.1.6 is a clean source tree. The workflow no longer deletes project files during the build. It validates the source, builds from a clean Gradle state, and reads certificate fingerprints from the APKs that were actually produced.
- Minimum Android: API 23
- Compile SDK: API 36
- Target SDK: API 36
- Java: 17
- Start URL: `https://hausanovels.ng/?utm_source=twa&twa=1`

## WordPress updates

Install the packages inside `wordpress/` before testing the new APK:

1. `hausanovels-pwa-v0.1.1.zip`
2. `hausanovels-google-signin-v0.1.3-twa-return-fix.zip`
3. `hausanovels-dark-v0.1.45-twa-header-icon-fix.zip`

The existing Wallet and Paystack packages are retained for convenience.

Google Cloud must keep this exact authorized redirect URI:

```text
https://hausanovels.ng/?hn_google_callback=1
```

Do not add the `hausanovels://` URI to Google Cloud. Google returns to the HTTPS callback first; WordPress then opens the installed Android app.

## Digital Asset Links

The site must serve:

```text
https://hausanovels.ng/.well-known/assetlinks.json
```

Add the SHA-256 certificate fingerprint for the build actually installed. For Play Store installs, use the **Play App Signing certificate SHA-256** from Play Console. The upload-key fingerprint is different.

## GitHub Actions

Push the project contents to the root of a GitHub repository, then run:

`Actions → Build HausaNovels TWA v2.1.6 → Run workflow`

The workflow always produces a debug APK. To create signed release APK/AAB files, configure these repository secrets:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

The build artifacts include the certificate fingerprint and an `assetlinks.json` template.

## Testing Google return

1. Install the new APK.
2. Install the WordPress updates.
3. Add the installed build's SHA-256 fingerprint in **Hausa Books → PWA & Android TWA**.
4. Open `https://hausanovels.ng/.well-known/assetlinks.json` and confirm it returns JSON directly with no redirect.
5. Start Google Sign-In from the app.
6. After Google completes, Android should return to the app Account page.

If a browser page remains visible, tap **Return to HausaNovels** once. That button uses an explicit package-restricted Android intent and is the reliable fallback when Chrome blocks an automatic custom-scheme launch.

## Notes

The percentage on the custom splash represents Android launch preparation. A TWA does not expose the actual website loading percentage to the wrapper.

## v2.1.6 runtime repair

This package keeps `ng.hausanovels.app` and the original v2.1.6 version identifiers. Replace the repository contents with this clean source while preserving the repository's GitHub Actions secrets. The launcher now has a safe external-browser fallback and cannot route that fallback back into the HausaNovels app.

## Legacy-file-safe builds

This v2.1.6 package automatically removes obsolete files left by older GitHub uploads
before validation and before Gradle's `preBuild`. You no longer need to manually delete
`app/src/main/res/xml/filepaths.xml` or the old WebView/splash resources for the workflow
to succeed.


## v2.1.6 Seamless WebView Fix

This build replaces the visible TWA browser hand-off with a native WebView shell. The package name, version name, version code and signing secret names remain unchanged. The splash overlay stays in the same Activity and disappears only after the first HausaNovels page is visible. Google Sign-In still opens in the secure system browser and returns through the existing `hausanovels://oauth/callback` app bridge.
