# HausaNovels Android — GitHub-ready v1.2.0

Android WebView app for `https://hausanovels.ng/`.

## Included improvements

- Android 16 / API level 36 target.
- Professional two-stage splash experience.
- Transparent HausaNovels logo with real page-load percentage.
- WebView viewport and typography tuned to match mobile Chrome closely.
- Chrome-style user agent without the old app suffix.
- Android 16 edge-to-edge and safe-area handling.
- Back navigation and predictive back support.
- File upload chooser.
- Paystack checkout inside the app.
- Google sign-in opens securely in the phone browser and returns through the website app link.
- GitHub Actions builds a debug APK and, when signing secrets are added, a signed APK and AAB.

## Upload to GitHub

1. Create a new empty GitHub repository.
2. Extract this ZIP.
3. Upload the contents of this folder to the repository root.
4. Open **Actions → Build HausaNovels Android → Run workflow**.
5. Download the `hausanovels-android-v1.2.0` artifact.

## Release signing secrets

Add these under **GitHub repository → Settings → Secrets and variables → Actions**:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Convert a keystore to base64 before saving `KEYSTORE_BASE64`:

```bash
base64 -w 0 hausanovels-release-key.jks
```

On macOS:

```bash
base64 hausanovels-release-key.jks | tr -d '\n'
```

The repository deliberately does not contain a private keystore.

## Main configuration

Website URL and in-app payment hosts:

`app/src/main/res/values/strings.xml`

Package name, API level and version:

`app/build.gradle`

Transparent splash logo:

`app/src/main/res/drawable/hausanovels_logo_transparent.png`

## Splash progress

The Android system splash displays first. The app then shows a branded loading screen with the transparent logo, loading message, progress bar and actual WebView loading percentage. It fades out after the first page becomes ready.

## Chrome-like page sizing

The app fixes WebView text zoom at 100%, uses a wide device viewport, disables automatic overview zoom and removes the old `HausaNovelsApp` user-agent suffix. This prevents the website from rendering larger app-only text.

Android System WebView is not the complete Chrome browser, so browser controls and the exact installed engine version can differ. Update both Chrome and Android System WebView on test phones for the closest rendering match.

## Google sign-in return link

The manifest supports verified links for:

- `https://hausanovels.ng/...`
- `https://www.hausanovels.ng/...`

For automatic return to the app, publish a valid Digital Asset Links file at:

`https://hausanovels.ng/.well-known/assetlinks.json`

## Play Console

This project targets Android 16:

```gradle
compileSdk 36
targetSdk 36
```

Before every Play Console upload, increase `versionCode` in `app/build.gradle` above the version code of the previous release.

See `docs/PLAY-CONSOLE.md` for release details.
