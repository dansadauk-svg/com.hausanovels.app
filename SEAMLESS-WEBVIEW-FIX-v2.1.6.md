# HausaNovels v2.1.6 Seamless WebView Fix

This package keeps the app identity unchanged:

- Package: `ng.hausanovels.app`
- Version name: `2.1.6`
- Version code: `216`
- Existing GitHub signing secret names are unchanged.

## Why this build exists

The previous TWA build hands the user from the Android launcher activity to the browser provider. When Digital Asset Links is not fully verified, or while Chrome is preparing the TWA surface, the user can see a browser header, a blank page, or a brief error screen. A TWA wrapper cannot keep its splash screen above Chrome until the web page finishes rendering.

This build uses a native Android WebView for the normal HausaNovels reading experience. The splash screen and WebView live inside the same Activity, so the splash is hidden only after the first HausaNovels page becomes visible.

## Google Sign-In

Google authentication is still opened in the secure system browser. After Google completes the HTTPS callback, the existing WordPress Google Sign-In plugin returns to:

`hausanovels://oauth/callback`

The app receives that deep link and loads the one-time app login URL inside the WebView so the WordPress login cookie is created inside the app.

## Installation

Replace the GitHub repository contents with everything inside this folder, preserving repository secrets.

Do not use the old TWA files in the same repository. This package includes cleanup scripts to remove old TWA/WebView leftovers before building.
