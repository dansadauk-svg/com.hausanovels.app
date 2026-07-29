# Build fix v2.1.4

The prior workflow tried to read `$HOME/.android/debug.keystore` directly. That path is not guaranteed to exist in every GitHub runner configuration, even when a debug APK was produced.

Version 2.1.4 permanently removes that dependency:

- The debug certificate fingerprint is read from the built APK with Android `apksigner`.
- The release fingerprint is also read from the actual signed release APK.
- The workflow starts with `clean assembleDebug`.
- No Android source or resource files are deleted during the build.
- A validator rejects stale WebView files and unresolved local resources before Gradle runs.
- The package remains `ng.hausanovels.app`.
- Release builds still use the existing GitHub secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, and `KEY_PASSWORD`.

To avoid mixing old and new source files, delete the old repository contents except GitHub Secrets, then upload the contents of this folder.
