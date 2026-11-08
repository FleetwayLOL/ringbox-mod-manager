#!/usr/bin/env python3
"""Рендер иконок Ringbox: бокс в фокусе, зона — в расфокусе.

Два пути:
1. Свой пейзаж зоны (sprite.build_bg) размывается гауссом.
2. Если в backdrops/ лежит <зона>.png — берётся он (твой рип спрайтов),
   кадрируется, размывается и подставляется вместо пейзажа.
   Эти файлы НЕ в репозитории и НЕ распространяются.
"""
import io
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import cairosvg
from PIL import Image, ImageEnhance, ImageFilter

import sprite
from zones import ZONES

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "icons")
DROP = os.path.join(HERE, "backdrops")
SIZES = (512, 192, 96, 48)
RENDER = 512
LOW = 128          # фон рендерится в низком разрешении...
BLUR = 2           # ...размывается на этом масштабе...
DARKEN = 0.85      # ...и приглушается, чтобы бокс читался
SHADOW = 7         # мягкая тень под боксом


def to_png(svg, size=RENDER):
    buf = io.BytesIO()
    cairosvg.svg2png(bytestring=svg.encode(), write_to=buf,
                     output_width=size, output_height=size)
    buf.seek(0)
    return Image.open(buf).convert("RGBA")


def backdrop(name, z):
    """Фон: свой пейзаж или подложенный пользователем спрайт."""
    custom = None
    for ext in (".png", ".jpg", ".jpeg", ".webp"):
        path = os.path.join(DROP, f"{name}{ext}")
        if os.path.exists(path):
            custom = path
            break

    if custom:
        img = Image.open(custom).convert("RGBA")
        # центральный квадратный кроп
        side = min(img.size)
        left = (img.width - side) // 2
        top = (img.height - side) // 2
        img = img.crop((left, top, left + side, top + side))
        img = img.resize((LOW, LOW), Image.LANCZOS)
        return img, f"backdrops/{os.path.basename(custom)}"

    grid, colors = sprite.build_bg(z)
    return to_png(sprite.svg_from(grid, colors), LOW), "свой пейзаж"


def compose(name, z):
    bg, source = backdrop(name, z)

    # размываем на низком разрешении и растягиваем пикселями:
    # фон остаётся «пиксельным», но в расфокусе
    bg = bg.filter(ImageFilter.GaussianBlur(BLUR))
    bg = ImageEnhance.Brightness(bg).enhance(DARKEN)
    bg = ImageEnhance.Color(bg).enhance(1.10)
    bg = bg.resize((RENDER, RENDER), Image.NEAREST)

    grid, colors = sprite.build_fg(z)
    fg = to_png(sprite.svg_from(grid, colors))

    # мягкая тень под боксом — чтобы он «стоял» над размытой зоной
    alpha = fg.split()[3].filter(ImageFilter.GaussianBlur(SHADOW))
    shadow = Image.new("RGBA", fg.size, (0, 0, 0, 0))
    shadow.putalpha(alpha.point(lambda a: int(a * 0.5)))
    frame = Image.new("RGBA", fg.size, (0, 0, 0, 0))
    frame.paste(shadow, (0, 12), shadow)

    frame.alpha_composite(bg)
    out = Image.new("RGBA", fg.size, (0, 0, 0, 0))
    out.alpha_composite(frame)
    out.alpha_composite(fg)
    return out, source


def combined_svg(name, z):
    """Один SVG: размытый фон через feGaussianBlur + резкий бокс."""
    bg_grid, bg_colors = sprite.build_bg(z)
    fg_grid, fg_colors = sprite.build_fg(z)
    custom = any(os.path.exists(os.path.join(DROP, f"{name}{e}"))
                 for e in (".png", ".jpg", ".jpeg", ".webp"))
    note = ("<!-- фон: backdrops/%s (пользовательский) -->" % name) if custom else ""
    return (
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32" '
        'shape-rendering="crispEdges">'
        + note +
        '<defs><filter id="dof" x="-20%" y="-20%" width="140%" height="140%">'
        '<feGaussianBlur stdDeviation="1.7"/></filter></defs>'
        f'<g filter="url(#dof)">{sprite.rects_from(bg_grid, bg_colors)}</g>'
        f'<g>{sprite.rects_from(fg_grid, fg_colors)}</g>'
        "</svg>"
    )


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, z in ZONES.items():
        img, source = compose(name, z)
        for size in SIZES:
            img.resize((size, size), Image.LANCZOS).save(
                os.path.join(OUT, f"ringbox-{name}-{size}.png"))
        with open(os.path.join(OUT, f"ringbox-{name}.svg"), "w", encoding="utf-8") as f:
            f.write(combined_svg(name, z) + "\n")
        if name == "greenhill":
            img.resize((512, 512), Image.LANCZOS).save(
                os.path.join(HERE, "ringbox-final-greenhill.png"))
        print(f"ok  {name:10s} фон: {source}")


if __name__ == "__main__":
    main()
