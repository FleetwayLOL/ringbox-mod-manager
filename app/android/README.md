# RingBox — Android-проект

Каркас приложения. Пока живой один экран — **Папка**: выбор папки игры через SAF,
проверка `Data.rsdk`, чтение списка `mods/`. Каталог и установка — следующие шаги.

## Открыть

1. Android Studio → **Open** → папка `ringbox/app/android`.
2. Дать Gradle синхронизироваться (Compose BOM 2024.06, AGP 8.5.2, Kotlin 1.9.24).
3. Run на телефоне с Android 8.0+ (minSdk 26).

Собрать без студии:

```bash
cd ringbox/app/android
./gradlew assembleDebug   # нужен gradle wrapper: gradle wrapper --gradle-version 8.7
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Структура

```
android/
├── settings.gradle.kts, build.gradle.kts
└── app/
    ├── build.gradle.kts            applicationId app.ringbox
    └── src/main/
        ├── AndroidManifest.xml     INTERNET + portrait, без MANAGE_EXTERNAL_STORAGE
        ├── res/mipmap-*/           иконки из make_icon.py
        └── java/app/ringbox/
            ├── MainActivity.kt     лого/счётчик, 3 таба, экран папки
            ├── data/SafeStorage.kt SAF-дерево, Data.rsdk, mods/, порядок загрузки
            └── ui/theme/Theme.kt   палитра Mania, шахматка, пиксельная рамка
```

## Что уже работает

- **Экран настроек** (`ui/SettingsScreen.kt`): «Папка игры» с иконкой и переходом на экран папки,
  проверка обновлений, кэш; блок **персонализации** — тема, 6 акцентов, скругление карточек.
  Акцент и радиус живые: `LocalAccent` / `LocalRadius` в `ui/theme/Theme.kt`.

- Выбор папки через `OpenDocumentTree`, persistable-право — переживает перезапуск.
- Проверка, что в папке лежит `Data.rsdk` (иначе красная плашка).
- Список папок в `mods/` как «установленные моды» и счётчик в шапке.
- `SafeStorage.modDir(name, create = true)` — готово под установку мода.
- Палитра и рамки из прототипа `app/prototype.html`.

## Следующее

1. **Каталог**: GameBanana API — `apiv11/Mod/Index?_aFilters[Generic_Game]=6045&_nPerpage=50&_nPage=N`.
   Список, обложки, поиск. Без `_sSort` (даёт `UNKNOWN_SORT`).
2. **Экран мода**: `apiv11/Mod/{id}/DownloadPage` → `_aFiles[]` (`_sFile`, `_sDownloadUrl`, `_sMd5Checksum`).
3. **Установщик**: качать `gamebanana.com/dl/{fileId}` с браузерным User-Agent → распаковать в
   `mods/<Имя>/` через SAF → проверить `mod.ini` и `TargetVersion=5`.
4. **Порядок загрузки**: перетаскивание, движок читает `mods/` снизу вверх.
5. **Пиксельный шрифт**: положить ttf в `res/font/` и заменить `Mania.pixel`.

## Правила проекта

- Это менеджер для **декомпиляции** Sonic Mania (RSDKv5), не форк движка.
- Данные игры не трогаем, только `mods/`.
- Ассеты SEGA не бандлим; названия модов и обложки — с GameBanana.
