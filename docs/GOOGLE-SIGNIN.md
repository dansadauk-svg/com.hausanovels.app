# Google Sign-In return flow

1. The TWA opens Google OAuth in Chrome, which is required for secure browser-based authentication.
2. Google redirects to the HTTPS WordPress callback:

```text
https://hausanovels.ng/?hn_google_callback=1
```

3. WordPress creates the authenticated session in Chrome's cookie jar.
4. The callback page opens:

```text
hausanovels://oauth/callback?url=https%3A%2F%2Fhausanovels.ng%2Faccount%2F
```

5. `OAuthReturnActivity` validates the URL and reopens the Account page through the TWA.

The custom scheme is not a Google redirect URI. It is used only after the secure HTTPS callback has finished.
