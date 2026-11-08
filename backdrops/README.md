# backdrops/ — фоны для иконок

Сюда кладётся фон, который встанет за бокс. Сам размоется и приглушится —
`render.py` всё сделает.

## Как подставить настоящий фон зоны с The Spriters Resource

1. На странице игры (PC / Computer → Sonic Mania) открой вкладку
   **Stages & Backgrounds** и скачай нужную зону — это zip.
2. Распакуй. Внутри не картинка, а слои, например у Green Hill:
   - `GHZ/Scene1-BG Outside.png` ← **вот это и нужен фон**
   - `Scene1-FG High.png` / `Scene1-FG Low.png` — передний план, не бери
   - `16x16Tiles.png`, `AniTiles.png` — тайлы, из них сцену ещё собирать
3. Переименуй файл в имя зоны и положи сюда:

```bash
cp "GHZ/Scene1-BG Outside.png" ringbox/backdrops/greenhill.png
cd ringbox && python3 render.py
```

Имена зон: `greenhill`, `hydrocity`, `chemical`, `studio`, `mirage`, `press`,
`oil`, `lava`, `metallic`, `titanic`, `stardust` (полный список — `zones.py`).

Поддерживаются `png`, `jpg`, `webp`; берётся центральный квадрат.

## Важно

Файлы в этой папке — чужая интеллектуальная собственность (Sega).
Держи их **только локально**: не коммить в git и не вкладывать в APK.
В релизном приложении фон должен быть свой — иначе жалоба и снятие проекта.

## Автоматическая сборка (если лень руками)

```bash
# 1. распакуй зипы зон сюда, папками по именам зон:
#    backdrops/raw/greenhill/...   backdrops/raw/lava/...   и т.д.
# 2. одна команда — скрипт сам найдёт фон, отбросит тайлы и FG:
python3 tools/import_backdrop.py            # все зоны
python3 tools/import_backdrop.py greenhill  # только одна
# 3. пересборка иконок
python3 render.py
```

`tools/import_backdrop.py` отбрасывает `16x16Tiles`, `AniTiles`, `FG`, `obj`,
`sprite` и берёт самый крупный файл с `BG` в названии — то есть именно
слой фона, а не конструктор.
