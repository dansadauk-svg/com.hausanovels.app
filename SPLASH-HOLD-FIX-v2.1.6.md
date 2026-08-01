# HausaNovels v2.1.7 splash hold fix

This package keeps the same Android identity:

- Package: `ng.hausanovels.app`
- Version name: `2.1.7`
- Version code: `217`

Only `SplashActivity.java` was changed. The native splash now remains visible while the app checks whether `hausanovels.ng` is reachable, then opens the Trusted Web Activity. It holds the splash for at least 3.6 seconds and waits up to 15 seconds for the site to respond before launching, preventing the immediate dark/empty transition gap seen between the splash and the page.

A Trusted Web Activity does not expose a true website `onPageFinished` callback to the native wrapper, so this is implemented as a website readiness gate plus a longer branded splash hold.
