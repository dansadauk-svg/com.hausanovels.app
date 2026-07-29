# HausaNovels TWA v2.1.4 — Runtime repair

This is the repaired v2.1.4 source. It keeps:

- package/application ID `ng.hausanovels.app`
- version code `214`
- version name `2.1.4`
- existing GitHub signing secret names

Runtime changes:

- The splash launches the TWA with `FLAG_ACTIVITY_NEW_TASK`, matching Android Browser Helper's launcher flow.
- The TWA launcher no longer uses `singleTask`, avoiding the trampoline/relaunch conflict.
- TWA launch exceptions fall back to an external browser instead of terminating the app.
- Browser fallback explicitly excludes `ng.hausanovels.app`, preventing a recursive App Link loop.
- The unused FileProvider and splash-transfer metadata were removed because this project uses its own native splash screen.
- Google OAuth return uses the same crash-safe launch path.
- Both `hausanovels.ng` and `www.hausanovels.ng` are accepted.

The GitHub workflow still reads release signing from:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`
