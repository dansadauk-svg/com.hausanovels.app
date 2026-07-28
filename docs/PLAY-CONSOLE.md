# Google Play Console release notes

## Target Android 16

This project is configured with:

```gradle
compileSdk 36
targetSdk 36
```

The Android Gradle Plugin is 8.11.1 and GitHub Actions uses Gradle 8.13 with Java 17.

## Version code

The included build uses:

```gradle
versionCode 120
versionName "1.2.0"
```

Google Play requires every uploaded APK/AAB to have a version code higher than the previous release. Increase `versionCode` before building when 120 has already been used.

## Signed AAB

Create these GitHub repository secrets:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

The workflow always builds a debug APK. It builds the signed release APK and AAB when all signing secrets are present.
