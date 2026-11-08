# Changelog

## 1.0.0

- Live GameBanana catalog search for Sonic Mania (6045).
- Catalog pages of 30 mods, page counters, category/tag filters, and search in installed mods.
- GameBanana matching for mods placed in `mods/` outside RingBox.
- Persistent app preferences and selected game folder.
- File scanning moved off the UI thread; cover decoding and in-memory caching are bounded for smoother scrolling.
- The previous tested APK, `RingBox-0.5.3.apk`, is retained separately as a rollback build.

## 0.5.3

- Reduced catalog scrolling load with bounded thumbnail decoding, limited concurrent cover requests, and a flat background.
