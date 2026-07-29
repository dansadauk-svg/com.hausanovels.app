# Android resource-linking fix — v2.1.1

The v2.1.0 GitHub build failed at `:app:processDebugResources` because merged API 28 resources referenced these colour names:

- `@color/app_background`
- `@color/brand_green`

They were not defined in the application resources. Version 2.1.1 adds those compatibility aliases in:

`app/src/main/res/values/colors.xml`

They map to the existing HausaNovels background and green colours. No WordPress update is required for this build-only correction.
