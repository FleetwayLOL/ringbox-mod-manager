# Contributing to RingBox

Thanks for helping with RingBox.

## Scope

RingBox manages mods for the Sonic Mania RSDKv5 decompilation. Do not add game files, copyrighted game assets, or bundled third-party mods to the repository. Keep credentials, signing keys, local Gradle configuration, and generated APKs out of commits.

## Changes

- Keep the Android app under `app/android/`.
- Use `applicationId app.ringbox` and increment `versionCode` for each distributed build.
- Test a debug build with `:app:assembleDebug` before submitting changes.
- Describe user-visible changes and any device-specific limitations in the pull request.

## License

Code and documentation are licensed under the MIT License in `LICENSE`. Game assets, trademarks, and third-party mod files are excluded.
