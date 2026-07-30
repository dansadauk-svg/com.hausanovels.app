# HausaNovels TWA v2.1.4 transition background fix

This build keeps the same package, version name, version code, signing-secret names, Google return flow, icon, splash screen and TWA setup.

Only the launch transition background was changed so the app no longer flashes a dark blank page between the native splash screen and the website/TWA surface.

Changed areas:

- TWA activity window background: light cream `#FFF8EF`
- TWA status/navigation bar fallback colors: light cream
- OAuth return trampoline background: light cream
- Trusted Web Activity status/navigation metadata: light cream

Unchanged:

- Package: `ng.hausanovels.app`
- Version name: `2.1.4`
- Version code: `214`
- Signing secret names
- Website URL
- Digital Asset Links setup
- Splash screen design and timing
