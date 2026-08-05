# HausaNovels Android v2.1.8 Gesture Navigation Fix

## Fixed

- Android 13+ system edge-swipe back now routes through WebView history instead of closing the app.
- Android back buttons and system gestures use one shared navigation handler.
- Left-edge swipe works on devices using three-button navigation.
- A page with no native WebView history returns to the HausaNovels home page before exit.
- The home page requires a second back action within two seconds, preventing accidental app closure.
- Pull-to-refresh now checks actual upward WebView scroll capability and uses a longer trigger distance to reduce accidental refreshes.

## Version

- Version name: `2.1.8`
- Version code: `218`
