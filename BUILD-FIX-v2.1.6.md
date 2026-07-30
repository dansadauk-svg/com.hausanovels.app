# HausaNovels TWA v2.1.6 — repository cleanup and runtime repair

Package identity remains unchanged:

- Application ID: `ng.hausanovels.app`
- Version name: `2.1.6`
- Version code: `216`

## Permanent repository cleanup

GitHub's browser uploader overwrites matching paths, but it does not delete old files
that are absent from a replacement ZIP. Older HausaNovels files such as
`res/xml/filepaths.xml`, `MainActivity.java`, and obsolete splash drawables can therefore
remain and break a newer build.

This package fixes that in two places:

1. GitHub Actions runs `tools/prepare_clean_source.py` before source validation.
2. Gradle runs `purgeLegacyHausaNovelsSources` before every local or CI `preBuild`.

The cleanup is idempotent: it succeeds whether the obsolete files exist or not.

## Current source set

The current app uses only:

- `SplashActivity`
- `HausaNovelsLauncherActivity`
- `OAuthReturnActivity`
- `BrowserFallback`
- the minimal `activity_splash.xml` splash screen

No FileProvider is required, so `res/xml/filepaths.xml` must not participate in the build.
