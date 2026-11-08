# RingBox

**RingBox** is an Android mod manager for the **Sonic Mania RSDKv5 decompilation**. It is not the official mobile/Netflix release and does not include the game, `Data.rsdk`, or mod archives.

## Features

- Live GameBanana catalog for Sonic Mania (GameBanana game ID `6045`), with online search, category/tag filters, and 30-mod pages.
- Install a catalog mod or choose a local `.zip` archive.
- Read the decompilation's `RSDK/v5/mods/` folder and recognize manually installed mods through GameBanana.
- Persist the selected game folder and app preferences across launches.
- Multilingual interface: automatic language selection plus 15 languages.
- Local cover-image cache with bounded memory use.

## Requirements

- Android 8.0 or newer.
- The Android build of the Sonic Mania RSDKv5 decompilation.
- A copy of the game and its data files that you are entitled to use. RingBox does not provide them.

RingBox can use Android's all-files access for direct discovery of `RSDK/v5`; SAF folder selection is also available as a fallback. Grant only the access needed for the game folder.

## Build

Use JDK 17 and Android SDK Platform 34 / Build Tools 34.0.0. Import `app/android/` in Android Studio, or run Gradle 8.7 from that directory:

```sh
cd app/android
printf 'sdk.dir=/path/to/android-sdk\n' > local.properties
gradle :app:assembleDebug
```

The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`. Do not commit generated APKs, signing keys, `local.properties`, or Gradle build output.

Current source version: **1.0.0** (`applicationId`: `app.ringbox`, `versionCode`: `10`).

## GameBanana connectivity

The app tries the selected access channel and, in direct mode, has a fallback chain: direct request, DoH providers, a user-configured proxy, a user-configured mirror, and a last-resort public proxy. AdGuard, Google, and Quad9 DoH are tried before Cloudflare; Cloudflare is best-effort only and is **not** considered a reliable solution in Russia. A user-provided proxy/VPN or mirror may be required depending on the provider.

The catalog and mod metadata come from GameBanana. The app does not host mod downloads.

## Project layout

- `app/android/` — Android app source.
- `live/`, `app/` — UI prototype and preview sources.
- `pack/`, `docs/` — project notes and documentation.
- `art/`, `icon/`, `logo/` — RingBox artwork and icon sources.

See [CONTRIBUTING.md](CONTRIBUTING.md) before submitting changes. Code and documentation are licensed under the MIT License in [LICENSE](LICENSE). Game files, SEGA trademarks, and third-party mod content are not covered by that license.

---

## Кратко по-русски

RingBox — менеджер модов для Android-билда **декомпиляции** Sonic Mania на RSDKv5, а не официальной мобильной версии. В каталоге есть живой поиск GameBanana, фильтры по категориям и тегам, загрузка страниц по 30 модов; в «Моих» отображаются и распознаются папки, положенные в `mods/` вручную. Для сборки нужны JDK 17 и Android SDK 34. Игра, `Data.rsdk` и архивы модов в проект не входят.
