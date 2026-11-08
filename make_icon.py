#!/usr/bin/env python3
"""Собирает ОДНУ иконку Ringbox: фон из uploads/ + металлический item box.

Никаких зон и вариантов — один знак. Фон берётся из картинки, которую
пользователь положил в uploads/, размывается, приглушается, уходит в виньетку.
Бокс с кольцом остаётся резким и сидит на мягком неоновом ореоле.
"""
import io
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import cairosvg
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter

import citybg
import sprite
from zones import ZONES

HERE = os.path.dirname(os.path.abspath(__file__))
BG_SRC = None  # фон рисуем сами (citybg.py), рипы не используются
OUT = os.path.join(HERE, "icon")
SIZES = (1024, 512, 192, 96, 48)

RENDER = 1024
CROP_X = 1500          # где вырезать квадрат из широкого фона
BLUR = 16.0            # расфокус фона в пикселях 1024-й сетки (умеренное боке)
DARKEN = 0.86          # приглушение фона
SATUR = 1.06
BOX_SCALE = 0.58       # ширина монитора относительно иконки
BOTTOM = 0.905         # где стоит подставка (доля высоты иконки)
GLOW = "#b02bff"       # неоновый ореол за боксом


def render_fg(size):
    grid, colors = sprite.build_fg(ZONES["titanic"])
    buf = io.BytesIO()
    cairosvg.svg2png(bytestring=sprite.svg_from(grid, colors).encode(),
                     write_to=buf, output_width=size, output_height=size)
    buf.seek(0)
    return Image.open(buf).convert("RGBA")


def make_background():
    """Фон: свой пиксель-арт город (citybg.py) — без чужих ассетов."""
    buf = io.BytesIO()
    cairosvg.svg2png(bytestring=citybg.svg().encode(), write_to=buf,
                     output_width=RENDER, output_height=RENDER)
    buf.seek(0)
    im = Image.open(buf).convert("RGB")

    if BLUR:
        im = im.filter(ImageFilter.GaussianBlur(BLUR))
    im = ImageEnhance.Brightness(im).enhance(DARKEN)
    im = ImageEnhance.Color(im).enhance(SATUR)

    # мягкое затемнение по краям — центр остаётся самым светлым
    grad = Image.new("L", (1, RENDER))
    for y in range(RENDER):
        t = abs(y / RENDER - 0.5) * 2
        grad.putpixel((0, y), int(255 * min(1.0, 0.70 + 0.30 * (1 - t ** 2))))
    grad = grad.resize((RENDER, RENDER))
    darker = ImageEnhance.Brightness(im).enhance(0.55)
    im = Image.composite(im, darker, grad)

    mask = Image.new("L", (RENDER, RENDER), 0)
    d = ImageDraw.Draw(mask)
    d.ellipse((-RENDER * 0.22, -RENDER * 0.22, RENDER * 1.22, RENDER * 1.22), fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(RENDER * 0.14))
    darker2 = ImageEnhance.Brightness(im).enhance(0.62)
    return Image.composite(im, darker2, mask).convert("RGBA")


def compose():
    bg = make_background()

    # исходник обрезаем по контуру: у спрайта вокруг монитора прозрачные поля
    raw = render_fg(RENDER)
    bbox = raw.split()[3].getbbox()
    fg = raw.crop(bbox)

    # ширина по BOX_SCALE, высота — по пропорции
    target_w = round(RENDER * BOX_SCALE)
    target_h = round(fg.height * target_w / fg.width)
    fg = fg.resize((target_w, target_h), Image.LANCZOS)

    # ставим так, чтобы подставка стояла на нужной линии
    cx = (RENDER - target_w) // 2
    cy = round(RENDER * BOTTOM) - target_h

    # неоновый ореол
    glow_mask = fg.split()[3].filter(ImageFilter.GaussianBlur(max(2, target_w * 0.06)))
    glow = Image.new("RGBA", fg.size, (0, 0, 0, 0))
    glow.paste(Image.new("RGBA", fg.size, GLOW + "cc"), (0, 0), glow_mask)

    # мягкая тень
    shadow_mask = fg.split()[3].filter(ImageFilter.GaussianBlur(max(1, target_w * 0.03)))
    shadow = Image.new("RGBA", fg.size, (0, 0, 0, 0))
    shadow.paste(Image.new("RGBA", fg.size, (0, 0, 0, 160)), (0, 0), shadow_mask)

    out = bg.copy()
    out.alpha_composite(glow, (cx, cy))
    out.alpha_composite(shadow, (cx, cy + round(target_h * 0.012)))
    out.alpha_composite(fg, (cx, cy))
    return out


def save_android(img):
    """mipmap-* для Android + круглая версия."""
    from PIL import ImageDraw
    base = os.path.join(OUT, "android")
    mip = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
    for name, size in mip.items():
        d = os.path.join(base, f"mipmap-{name}")
        os.makedirs(d, exist_ok=True)
        small = img.resize((size, size), Image.LANCZOS)
        small.save(os.path.join(d, "ic_launcher.png"))
        mask = Image.new("L", (size, size), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
        rnd = small.convert("RGBA")
        rnd.putalpha(mask)
        rnd.save(os.path.join(d, "ic_launcher_round.png"))


def main():
    os.makedirs(OUT, exist_ok=True)
    img = compose()
    for size in SIZES:
        img.resize((size, size), Image.LANCZOS).save(os.path.join(OUT, f"ringbox-{size}.png"))
    img.save(os.path.join(HERE, "ringbox-icon.png"))
    save_android(img)

    # исходник арта: чистый спрайт, без фона и без размытия
    art_dir = os.path.join(HERE, "art")
    os.makedirs(art_dir, exist_ok=True)
    bg_only = make_background()
    bg_only.convert("RGB").save(os.path.join(art_dir, "city-background.png"))
    src = render_fg(1024)
    src.save(os.path.join(art_dir, "source-transparent.png"))
    preview = Image.new("RGBA", src.size, "#0d0b18")
    preview.alpha_composite(src)
    preview.save(os.path.join(art_dir, "source-on-dark.png"))

    print(f"готово: ringbox-icon.png + icon/ ({', '.join(map(str, SIZES))})")
    print("исходник арта: art/source-transparent.png, art/source-on-dark.png")


if __name__ == "__main__":
    main()
