#!/usr/bin/env python3
"""Собирает preview.html: все темы Ringbox в одном файле, без внешних ресурсов."""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import base64
from zones import ZONES


def png_data(name, size=192):
    """PNG как data-URI — витрина работает без внешних файлов."""
    path = os.path.join(HERE, "icons", f"ringbox-{name}-{size}.png")
    with open(path, "rb") as f:
        return "data:image/png;base64," + base64.b64encode(f.read()).decode()


def img(name, size, radius_ratio=0.22, label=None):
    src = png_data(name, 192 if size > 96 else 48)
    style = (f"width:{size}px;height:{size}px;"
             f"border-radius:{round(size * radius_ratio)}px;")
    alt = label or name
    return f'<img src="{src}" alt="{alt}" style="{style}">'

HERE = os.path.dirname(os.path.abspath(__file__))

CSS = """
  * { box-sizing: border-box; }
  body { margin: 0; padding: 32px 22px 64px; background: #0b0b13; color: #e9ecf5;
         font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
  .wrap { max-width: 1000px; margin: 0 auto; }
  h1 { font-size: 21px; letter-spacing: -0.02em; margin: 0 0 6px; }
  .sub { color: #7d849c; font-size: 13px; margin: 0 0 28px; line-height: 1.6; max-width: 660px; }
  .lockup { display: flex; align-items: center; gap: 20px; padding: 18px 22px 18px 18px;
            border-radius: 20px; width: fit-content; background: #131521;
            border: 1px solid #202334; margin-bottom: 10px; }
  .lockup svg { width: 92px; height: 92px; display: block; image-rendering: pixelated;
                border-radius: 20%; }
  .lockup .word { font-size: 32px; font-weight: 800; letter-spacing: -0.03em; }
  .lockup .tag { font-size: 12px; color: #7d849c; margin-top: 3px; }
  h2 { font-size: 12px; font-weight: 600; letter-spacing: .09em; text-transform: uppercase;
       color: #6a7090; margin: 38px 0 15px; }
  .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(132px, 1fr)); gap: 16px; }
  .card { padding: 14px; border-radius: 16px; background: #131521; border: 1px solid #202334;
          text-align: center; }
  .card svg { width: 100%; height: auto; display: block; image-rendering: pixelated;
              border-radius: 20%; }
  .card .name { margin-top: 10px; font-size: 12px; color: #aab0c6; }
  .card .key { margin-top: 3px; font-size: 10px; color: #5b6178;
               font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }
  .row { display: flex; align-items: flex-end; gap: 24px; flex-wrap: wrap; padding: 20px;
         border-radius: 16px; background: #131521; border: 1px solid #202334; }
  .row svg { display: block; image-rendering: pixelated; border-radius: 20%; }
  .cell { text-align: center; }
  .cell span { display: block; margin-top: 9px; font-size: 10px; color: #5b6178; }
  .row.plain { background: #eef1f7; border-color: #eef1f7; }
  .row.plain span { color: #8a90a4; }
  .note { margin-top: 30px; font-size: 12px; color: #6a7090; line-height: 1.7; max-width: 680px; }
  .note b { color: #96a0bd; font-weight: 600; }
  .flag { display: inline-block; margin-top: 4px; padding: 3px 8px; border-radius: 6px;
          background: #2a1d10; color: #d9a15b; font-size: 11px; }
"""


def build():
    cards = "\n".join(
        f'''    <div class="card">{img(name, 148, 0.22)}
      <div class="name">{z["label"]}</div>
      <div class="key">z-{name}</div>
    </div>''' for name, z in ZONES.items())

    smalls = "".join(
        f'<div class="cell">{img("greenhill", s)}<span>{s}</span></div>'
        for s in (192, 96, 64, 48, 32))

    light = "".join(
        f'<div class="cell">{img(n, 64)}<span>{ZONES[n]["label"]}</span></div>'
        for n in ("greenhill", "studio", "hydrocity", "lava", "stardust"))

    html = f"""<!DOCTYPE html>
<html lang="ru">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Ringbox — пиксель-арт знак</title>
<style>{CSS}</style>
</head>
<body>
<div class="wrap">
  <h1>Ringbox</h1>
  <p class="sub">Знак — item box из игры: монитор с рамкой и золотым кольцом на экране.
  Пиксель-арт 32×32, нарисован по пикселям, без сглаживания.
  Меняется только палитра корпуса — кольцо всегда золотое, как в оригинале.</p>

  <div class="lockup">
    {img("greenhill", 92, 0.2)}
    <div><div class="word">Ringbox</div><div class="tag">моды в один тап</div></div>
  </div>

  <h2>Зоны</h2>
  <div class="grid">
{cards}
  </div>

  <h2>Масштаб</h2>
  <div class="row">{smalls}</div>

  <h2>На светлом фоне</h2>
  <div class="row plain">{light}</div>

  <p class="note">
    Арт собран кодом (<b>sprite.py</b>): корпус — точные координаты, кольцо — маска 12×12,
    свет и тень считаются по диагонали. Правка любого пикселя — одна строка.
    <b>Новая зона</b> — строка в <b>zones.py</b>, дальше
    <b>python3 build_icons.py &amp;&amp; python3 build_preview.py</b>.
    <br>Фон зоны размывается в низком разрешении и растягивается пикселями —
    поэтому он мягкий, но не мыльный. Свой фон можно положить в <b>backdrops/</b>.
    <br><span class="flag">арт свой, в стиле игры — не рипнутые спрайты Sega</span>
  </p>
</div>
</body>
</html>
"""
    with open(os.path.join(HERE, "gallery.html"), "w", encoding="utf-8") as f:
        f.write(html)
    print(f"gallery.html: {len(ZONES)} тем")


if __name__ == "__main__":
    build()
