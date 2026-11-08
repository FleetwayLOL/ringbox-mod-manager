#!/usr/bin/env python3
"""Генератор векторного лого RingBox. Корпус — серый, экран — чёрный, внутри золотое кольцо.

Один шаблон, разные палитры: python3 make_logo.py [light|mid|warm]
"""
import sys
import cairosvg
from pathlib import Path

PALETTES = {
    # светлый серый — как пластик старого монитора
    "light": dict(
        body=[("#E3E6EA", 0), ("#CDD2D8", .18), ("#B7BDC5", .52), ("#A2A9B2", .78), ("#8A9199", 1)],
        edge="#5E646C", bezel="#2E333B", bezel2="#1B1F26",
    ),
    # средний графит-серый
    "mid": dict(
        body=[("#B9BFC7", 0), ("#A3AAB3", .18), ("#8C939C", .52), ("#787F88", .78), ("#61686F", 1)],
        edge="#464C54", bezel="#262B32", bezel2="#15181D",
    ),
    # тёплый серый, почти кость
    "warm": dict(
        body=[("#E8E4DC", 0), ("#D6D1C7", .18), ("#C0BAB0", .52), ("#A9A39A", .78), ("#8F8A82", 1)],
        edge="#65605A", bezel="#33302C", bezel2="#1D1B18",
    ),
}


def gradient(gid, stops, x1="0", y1="0", x2="0", y2="1"):
    s = "".join(
        f'<stop offset="{o}" stop-color="{c}"/>' for c, o in stops
    )
    return f'<linearGradient id="{gid}" x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}">{s}</linearGradient>'


def build(key: str) -> str:
    p = PALETTES[key]
    body = gradient("body", p["body"])
    # кольцо: матовая сталь-золото, без «стеклянного» блеска
    ring = gradient(
        "ring",
        [("#F2C860", 0), ("#E0AE36", .38), ("#C68F1E", .72), ("#9E6E12", 1)],
        x1=".15", y1="0", x2=".85", y2="1",
    )
    stand = gradient("stand", [("#A9AFB7", 0), ("#7C838B", 1)])

    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512" fill="none">
  <title>RingBox</title>
  <defs>
    {body}
    {ring}
    {stand}
    <linearGradient id="glass" x1="0" y1="0" x2=".35" y2="1">
      <stop offset="0" stop-color="#FFFFFF" stop-opacity=".07"/>
      <stop offset=".5" stop-color="#FFFFFF" stop-opacity="0"/>
    </linearGradient>
  </defs>

  <!-- стойка -->
  <path d="M206 360 H306 L300 420 H212 Z" fill="url(#stand)"/>
  <path d="M168 462 V444 L194 420 H318 L344 444 V462 Z" fill="url(#stand)"/>
  <path d="M168 462 V444 L194 420 H318 L344 444 V462 Z" stroke="{p['edge']}" stroke-width="5" stroke-linejoin="round"/>
  <path d="M188 434 H324" stroke="{p['edge']}" stroke-opacity=".45" stroke-width="4"/>

  <!-- корпус -->
  <rect x="48" y="64" width="416" height="304" rx="34" fill="url(#body)"/>
  <rect x="48" y="64" width="416" height="304" rx="34" stroke="{p['edge']}" stroke-width="5"/>
  <!-- светлая кромка сверху -->
  <path d="M84 82 H428" stroke="#FFFFFF" stroke-opacity=".55" stroke-width="4" stroke-linecap="round"/>
  <!-- тень по низу корпуса -->
  <path d="M74 352 H438" stroke="{p['edge']}" stroke-opacity=".35" stroke-width="4" stroke-linecap="round"/>

  <!-- винты по углам -->
  <g fill="{p['edge']}" fill-opacity=".55">
    <circle cx="76" cy="92" r="4.5"/><circle cx="436" cy="92" r="4.5"/>
    <circle cx="76" cy="340" r="4.5"/><circle cx="436" cy="340" r="4.5"/>
  </g>

  <!-- утопленный экран -->
  <rect x="92" y="112" width="328" height="196" rx="16" fill="{p['bezel']}"/>
  <rect x="92" y="112" width="328" height="196" rx="16" stroke="{p['edge']}" stroke-opacity=".7" stroke-width="4"/>
  <rect x="104" y="124" width="304" height="172" rx="9" fill="{p['bezel2']}"/>
  <rect x="110" y="130" width="292" height="160" rx="6" fill="#000000"/>

  <!-- кольцо -->
  <circle cx="256" cy="210" r="46" stroke="#000000" stroke-width="26"/>
  <circle cx="256" cy="210" r="46" stroke="url(#ring)" stroke-width="22"/>
  <circle cx="256" cy="210" r="46" stroke="#FFEFC0" stroke-opacity=".55" stroke-width="5"
          stroke-dasharray="42 247" transform="rotate(-150 256 210)"/>
  <circle cx="256" cy="210" r="46" stroke="#8A5D0C" stroke-opacity=".45" stroke-width="5"
          stroke-dasharray="60 229" transform="rotate(30 256 210)"/>

  <!-- блик стекла -->
  <path d="M110 130 H402 V168 L110 244 Z" fill="url(#glass)"/>

  <!-- решётка динамика под экраном -->
  <g stroke="{p['edge']}" stroke-opacity=".45" stroke-width="3" stroke-linecap="round">
    <path d="M228 328 H284"/><path d="M228 338 H284"/><path d="M240 348 H272"/>
  </g>

  <!-- индикатор -->
  <circle cx="392" cy="332" r="4.5" fill="#8CE39A" fill-opacity=".9"/>
</svg>
'''


if __name__ == "__main__":
    keys = sys.argv[1:] or ["mid"]
    out = Path(__file__).parent
    for k in keys:
        svg = build(k)
        (out / f"ringbox-logo-{k}.svg").write_text(svg, encoding="utf-8")
        cairosvg.svg2png(bytestring=svg.encode(), write_to=str(out / f"_p_{k}.png"),
                         output_width=420, output_height=420, background_color="#0A0B0F")
        cairosvg.svg2png(bytestring=svg.encode(), write_to=str(out / f"_pl_{k}.png"),
                         output_width=420, output_height=420, background_color="#F3F4F7")
        print("готово:", k)
