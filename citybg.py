"""Неоновый город — пиксель-арт фон для иконки, нарисован с нуля.

Сетка 128x128, читаемая композиция в три плана:
  1. ночное небо со звёздами,
  2. яркая линия горизонта (оранжевая с малиновым свечением),
  3. силуэты домов под ней с группами светящихся окон,
  4. тёмный пол с редкими цветными огнями.
Никаких рипов: всё нарисовано кодом, по пикселям.
"""

W = H = 128
SKY_BOTTOM = 50       # линия горизонта
FLOOR = 104           # начало пола

P = {
    ".": None,
    "sky0": "#05000f", "sky1": "#0e0033", "sky2": "#180a48", "sky3": "#241060",
    "star": "#9a7bff", "star_hi": "#ffe6ff",
    "glow_orange": "#ff7a12", "glow_yellow": "#ffc24a",
    "glow_mag": "#c000c0", "glow_mag2": "#6a006a",
    "near": "#120035", "near_hi": "#1e0a55", "near_dark": "#08001c",
    "gap": "#04000c",
    "win_mag": "#ff2bd6", "win_cyan": "#28c8ff", "win_warm": "#ffb347",
    "strip": "#ff5ea8",
    "floor0": "#10002e", "floor1": "#180844", "floor2": "#241058",
    "dot_a": "#ff5ea8", "dot_b": "#22c8ff", "dot_c": "#ffb347",
    "dot_d": "#8f6bff", "bar": "#2a1060", "bar_hi": "#22c8ff",
}


def blank():
    return [["." for _ in range(W)] for _ in range(H)]


def px(g, x, y, k):
    if 0 <= x < W and 0 <= y < H:
        g[y][x] = k


def fill(g, x0, y0, x1, y1, k):
    for y in range(max(0, y0), min(H, y1 + 1)):
        for x in range(max(0, x0), min(W, x1 + 1)):
            g[y][x] = k


def dither(g, y, x0, x1, k, step=2, off=0):
    for x in range(max(0, x0), min(W, x1 + 1)):
        if (x + off) % step == 0:
            px(g, x, y, k)


def blob(g, cx, cy, w, h, k):
    """Пятно света — прямоугольник со срезанными углами."""
    for y in range(cy, cy + h):
        for x in range(cx, cx + w):
            if (x - cx) in (0, w - 1) and (y - cy) in (0, h - 1):
                continue
            px(g, x, y, k)


# ------------------------------------------------------------------ слои
def sky(g):
    fill(g, 0, 0, W - 1, SKY_BOTTOM, "sky1")
    fill(g, 0, 0, W - 1, 14, "sky0")
    fill(g, 0, SKY_BOTTOM - 9, W - 1, SKY_BOTTOM, "sky3")
    # горизонтальные полосы перехода: каждая следующая реже
    for i, y in enumerate(range(14, 24)):
        dither(g, y, 0, W - 1, "sky0", 2, i % 2)
    for i, y in enumerate(range(24, 32)):
        dither(g, y, 0, W - 1, "sky2", 2, i % 2)
    for i, y in enumerate(range(32, SKY_BOTTOM - 9)):
        dither(g, y, 0, W - 1, "sky2", 3, i % 3)

    # звёзды: редкие, разного размера
    for x, y in ((8, 5), (26, 9), (46, 4), (63, 12), (84, 6), (103, 10), (118, 4),
                 (34, 18), (72, 20), (94, 16), (16, 22), (55, 24), (110, 22)):
        px(g, x, y, "star")
        if (x + y) % 4 == 0:
            px(g, x + 1, y, "star")
    for x, y in ((46, 4), (84, 6)):
        px(g, x, y - 1, "star_hi")
        px(g, x - 1, y, "star_hi")


def horizon(g):
    """Свечение за домами: снизу вверх — малина → оранжевый → жёлтый."""
    for i, y in enumerate(range(SKY_BOTTOM - 8, SKY_BOTTOM - 2)):
        dither(g, y, 0, W - 1, "glow_mag2", 3 + (i % 2), i)
    fill(g, 0, SKY_BOTTOM - 2, W - 1, SKY_BOTTOM - 1, "glow_mag")
    fill(g, 0, SKY_BOTTOM, W - 1, SKY_BOTTOM, "glow_orange")
    dither(g, SKY_BOTTOM + 1, 0, W - 1, "glow_yellow", 2, 0)
    fill(g, 0, SKY_BOTTOM + 2, W - 1, SKY_BOTTOM + 2, "glow_orange")


def buildings(g):
    """Дома под линией горизонта: силуэт + регулярная сетка окон."""
    spec = [
        (0, 18, 22), (20, 14, 30), (36, 20, 18), (58, 12, 26),
        (72, 22, 14), (96, 14, 32), (112, 16, 20),
    ]
    for x0, w, h in spec:
        top = SKY_BOTTOM + 4 - h // 4
        x1 = min(x0 + w - 1, W - 1)

        fill(g, x0, top, x1, FLOOR, "near")
        fill(g, x0, top, x0, FLOOR, "near_dark")      # левый торец в тень
        fill(g, x0, top, x1, top, "near_hi")          # кромка
        fill(g, x0, top + 1, x1, top + 1, "strip")    # неоновая полоса

        # окна 2x2 с шагом 4x6
        for yy in range(top + 5, FLOOR - 3, 6):
            for xx in range(x0 + 3, x1 - 2, 4):
                warm = ((xx // 4) + (yy // 6)) % 4 == 0
                px(g, xx, yy, "win_warm" if warm else "win_mag")
                px(g, xx + 1, yy, "win_warm" if warm else "win_mag")
                px(g, xx, yy + 1, "win_mag" if warm else "win_cyan")
                px(g, xx + 1, yy + 1, "win_mag" if warm else "win_cyan")

    # зазоры между домами: тонкие тёмные щели
    for x in (18, 19, 34, 35, 56, 57, 70, 71, 94, 95, 110, 111):
        if x < W:
            fill(g, x, SKY_BOTTOM + 4, x, FLOOR, "gap")


def floor_lights(g):
    fill(g, 0, FLOOR, W - 1, H - 1, "floor0")
    fill(g, 0, FLOOR, W - 1, FLOOR + 3, "floor1")
    dither(g, FLOOR + 4, 0, W - 1, "floor2", 2, 1)

    # редкие крупные пятна света по полу
    spots = [(6, FLOOR + 10, 6, 3, "dot_a"), (30, FLOOR + 14, 5, 3, "dot_b"),
             (54, FLOOR + 9, 7, 3, "dot_d"), (82, FLOOR + 15, 5, 3, "dot_c"),
             (104, FLOOR + 11, 6, 3, "dot_b")]
    for cx, cy, w, h, k in spots:
        blob(g, cx, cy, w, h, k)

    # вертикальные неоновые штрихи у самого низа
    for x in range(3, W, 11):
        fill(g, x, H - 9, x, H - 4, "bar")
        px(g, x, H - 10, "bar_hi")
    fill(g, 0, H - 3, W - 1, H - 1, "floor1")


def build():
    g = blank()
    sky(g)
    horizon(g)
    buildings(g)
    floor_lights(g)
    return g, P


def rects_from(grid, colors):
    out = []
    for y, row in enumerate(grid):
        x = 0
        while x < W:
            key = row[x]
            run = 1
            while x + run < W and row[x + run] == key:
                run += 1
            if colors.get(key):
                out.append(f'<rect x="{x}" y="{y}" width="{run}" height="1" fill="{colors[key]}"/>')
            x += run
    return "".join(out)


def svg(size=None, radius=0):
    g, c = build()
    dim = f' width="{size}" height="{size}"' if size else ""
    bg = (f'<rect width="{W}" height="{H}" rx="{radius}" fill="{c["sky1"]}"/>'
          if radius else "")
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}"{dim} '
            'shape-rendering="crispEdges">' + bg + rects_from(g, c) + "</svg>")
