# Build fix v2.1.3

The previous build reached Java compilation but failed because an obsolete `MainActivity.java` still referenced `R.string.web_app_url`.

Version 2.1.3 fixes this in two ways:

1. Defines the `web_app_url` compatibility string.
2. Runs `tools/clean_android_tree.py` before verification and Gradle compilation to remove obsolete WebView Java and splash resource files left by older repository versions.

The active app remains the TWA implementation with `SplashActivity`, `HausaNovelsLauncherActivity`, and `OAuthReturnActivity`.
