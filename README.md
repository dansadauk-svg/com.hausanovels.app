# HausaNovels Web To APK

GitHub-ready Android WebView APK project for `https://hausanovels.ng/`.

## What is included

- Native Android WebView wrapper.
- GitHub Actions APK build workflow.
- PNG app icon resources.
- Splash screen with only the icon on the splash background.
- Camera cutout and notch safe-area padding.
- Back button support.
- File picker support for upload fields.
- Paystack checkout hosts allowed inside the WebView.
- External links open in the user's browser or related app.

## Main settings

Change these when needed:

`app/src/main/res/values/strings.xml`

```xml
<string name="app_name">HausaNovels</string>
<string name="web_app_url">https://hausanovels.ng/</string>
```

Package name:

`app/build.gradle`

```gradle
applicationId "ng.hausanovels.app"
```

If you change the package name, also change the Java folder and the first line in:

`app/src/main/java/ng/hausanovels/app/MainActivity.java`

## Splash screen

The splash screen is handled in:

- `app/src/main/res/drawable/splash_screen.xml`
- `app/src/main/res/drawable/splash_icon.png`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values-v31/styles.xml`

The splash background is:

```xml
<color name="splash_background">#050816</color>
```

Only the PNG icon is centered on the splash screen.

## Camera cutout and safe area

The app avoids the top camera cutout by applying Android window insets in:

`app/src/main/java/ng/hausanovels/app/MainActivity.java`

It pads the WebView away from:

- status bar
- navigation bar
- display cutout
- camera notch/hole area

The WordPress theme already has:

```html
viewport-fit=cover
```

So the Android shell and the website are both prepared for safe-area handling.

## Build APK With GitHub

1. Create a new GitHub repository.
2. Upload all files from this folder to the repository root.
3. Go to the repository on GitHub.
4. Open **Actions**.
5. Run **Build APK**.
6. Download the artifact named `hausanovels-apks`.

The debug APK is easiest for direct phone testing:

`app-debug.apk`

The release APK created by this workflow is unsigned. For Play Store upload, you will need a signed release or Android App Bundle later.

## Testing while DNS is still propagating

You can build and install the APK while DNS is still propagating. If `hausanovels.ng` is not resolving yet on a phone, the app will show a connection or website loading issue until DNS finishes.

For best testing, open the site in Chrome on the same phone first. If Chrome can open it, the APK should open it too.
