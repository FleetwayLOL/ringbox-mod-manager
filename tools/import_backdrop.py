#!/usr/bin/env python3
"""Собирает фон для иконки из папки со спрайтами/слоями зоны.

Что делает:
  1. ищет в backdrops/raw/<зона>/ все png,
  2. отбрасывает тайлы, анимации, передний план (tile, ani, fg, obj...),
  3. из оставшихся берёт самый крупный фон,
  4. кадрирует по центру, гонит через render.py.

Запуск:
    python3 tools/import_backdrop.py            # все зоны, где есть папка
    python3 tools/import_backdrop.py greenhill  # одна зона
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from PIL import Image

HERE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(HERE, "backdrops", "raw")
OUT = os.path.join(HERE, "backdrops")

# что не считаем фоном
BAD = ("tile", "ani", "fg", "obj", "sprite", "sheet", "icon", "spr")


def pick(folder):
    """Выбрать самый подходящий файл-фон в папке."""
    cands = []
    for root, _, files in os.walk(folder):
        for f in files:
            if not f.lower().endswith(".png"):
                continue
            low = f.lower()
            path = os.path.join(root, f)
            try:
                with Image.open(path) as im:
                    w, h = im.size
            except Exception:
                continue
            if w < 64 or h < 64:
                continue                      # обрезки и мелочь не нужны
            score = 0
            if "bg" in low or "background" in low:
                score += 1000
            score -= sum(500 for bad in BAD if bad in low)
            cands.append((score, w * h, path))
    if not cands:
        return None
    cands.sort(reverse=True)
    return cands[0][2]


def prepare(src, zone):
    img = Image.open(src).convert("RGBA")
    side = min(img.size)
    left = (img.width - side) // 2
    top = (img.height - side) // 2
    img = img.crop((left, top, left + side, top + side)).resize((512, 512), Image.LANCZOS)
    dst = os.path.join(OUT, f"{zone}.png")
    img.save(dst)
    return dst


def main():
    want = sys.argv[1:] or None
    if not os.path.isdir(RAW):
        print(f"нет папки {RAW}")
        print("положи распакованные зоны туда: backdrops/raw/<зона>/...")
        return

    zones = [z for z in os.listdir(RAW) if os.path.isdir(os.path.join(RAW, z))]
    if want:
        zones = [z for z in zones if z in want]

    for zone in sorted(zones):
        src = pick(os.path.join(RAW, zone))
        if not src:
            print(f"✗ {zone}: подходящего фона не нашёл")
            continue
        dst = prepare(src, zone)
        print(f"✓ {zone}: {os.path.relpath(src, RAW)} -> {os.path.basename(dst)}")

    if zones:
        print("\nтеперь: python3 render.py")


if __name__ == "__main__":
    main()
