# WebView and Chrome parity

The app uses the phone's current Android System WebView rendering engine. The following settings are applied to keep the website's visual scale close to mobile Chrome:

- device-width viewport support;
- wide viewport enabled;
- overview zoom disabled;
- text zoom fixed at 100%;
- normal layout algorithm;
- default mobile font sizes;
- a Chrome-style user agent without an app-only suffix;
- no duplicate app-specific safe-area scaling.

Android System WebView and the full Chrome browser are separate applications, so their browser controls and some engine-version details cannot be literally identical. Keeping Chrome and Android System WebView updated on the phone gives the closest result.
