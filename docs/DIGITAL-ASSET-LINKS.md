# Digital Asset Links

Publish valid JSON at:

```text
https://hausanovels.ng/.well-known/assetlinks.json
```

Requirements:

- HTTPS only
- no redirect
- content type `application/json`
- package name `ng.hausanovels.app`
- uppercase SHA-256 certificate fingerprint matching the installed build

For Play Store installs, use the **Play App Signing certificate** fingerprint from Play Console, not only the upload-key fingerprint.

The GitHub workflow generates fingerprint files for debug and signed upload-key builds. Add all fingerprints that you actively test to the WordPress PWA settings, one per line.
