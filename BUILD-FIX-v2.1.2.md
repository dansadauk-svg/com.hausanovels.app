# Android resource-linking fix — v2.1.2

The GitHub build was failing in `:app:processDebugResources` because older files left in the repository referenced resources that were not defined:

- `brand_gold`
- `progress_track`
- `splash_background`
- `splash_background_end`
- `text_primary`
- `text_secondary`
- `brand_green_soft`
- `splash_tagline`
- `loading_preparing`

Version 2.1.2 defines every compatibility resource while keeping the active splash screen minimal: icon, “Hausa Novels”, progress bar and percentage only.

The GitHub workflow also runs `tools/verify_android_resources.py` before Gradle so missing resources are reported clearly before Android resource linking.

For the cleanest result, delete the old repository contents except `.git`, then upload the contents of this project folder. Uploading over an old project can leave obsolete source files behind.
