"""Пиксель-арт знака Ringbox — v4, сетка 64x64.

Композиция: item box стоит на земле своей зоны. Вокруг — небо с облаками,
дальние и ближние силуэты, характерный грунт и мотивы (пальмы, трубы, неон,
трещины). Вдвое больше пикселей, чем в v3, — поэтому детали читаются.

Всё рисуется кодом по координатам: правится по пикселям, без «магии».
"""

import math

W = H = 64
HORIZON = 44

GOLD = "#ffd23f"
GOLD_DARK = "#c07d05"
GOLD_LIGHT = "#fff3b0"
SCREEN = "#050b16"
SCREEN_HI = "#101a2c"

# корпус
BOX = dict(x0=12, y0=9, x1=51, y1=44)
FRAME = 4                       # толщина рамки
SCR = dict(x0=18, y0=16, x1=45, y1=37)

# кольцо
RING_CX, RING_CY = 31.5, 26.5
RING_R_OUT, RING_R_IN = 9.2, 5.2


def mix(a, b, t):
    a, b = a.lstrip("#"), b.lstrip("#")
    return "#" + "".join(
        f"{round(int(a[i*2:i*2+2], 16) + (int(b[i*2:i*2+2], 16) - int(a[i*2:i*2+2], 16)) * t):02x}"
        for i in range(3))


def shade(c, t):
    return mix(c, "#ffffff", t) if t > 0 else mix(c, "#000000", -t)


# --------------------------------------------------------------- примитивы
def blank():
    return [["." for _ in range(W)] for _ in range(H)]


def px(g, x, y, k):
    if 0 <= x < W and 0 <= y < H:
        g[y][x] = k


def fill(g, x0, y0, x1, y1, k):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px(g, x, y, k)


def dither(g, y, x0, x1, k, step=2, off=0):
    for x in range(x0, x1 + 1):
        if (x + off) % step == 0:
            px(g, x, y, k)


def line(g, x0, y0, x1, y1, k):
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx + dy
    while True:
        px(g, x0, y0, k)
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


# ------------------------------------------------------------- пейзаж
def sky(g, z):
    fill(g, 0, 0, W - 1, HORIZON, "sky")
    # мягкая дымка у горизонта
    for i, y in enumerate(range(HORIZON - 8, HORIZON)):
        dither(g, y, 0, W - 1, "skyf", 2 + (i % 3), i)


def clouds(g, spots, color="cl"):
    for cx, cy, w in spots:
        # тело облака: три «кочки»
        for dx in range(w):
            px(g, cx + dx, cy, color)
        for dx in range(2, w - 1):
            px(g, cx + dx, cy - 1, color)
        for dx in range(w // 3, w - 1):
            px(g, cx + dx, cy - 2, color)


def hills(g, base, color, amp, wl, ph=0.0, step=1.0):
    """Холмы контуром: три гармоники дают естественный рельеф, без синус-пилы."""
    for x in range(W):
        t = (x + ph) / wl
        a = amp * (0.62 + 0.24 * math.sin(t * 6.28)
                   + 0.12 * math.sin(t * 13.9 + 1.1)
                   + 0.06 * math.sin(t * 27.1 + 2.3))
        top = base - round(max(a, 0))
        for y in range(top, base):
            px(g, x, y, color)
        # светлая кромка
        px(g, x, top, "hill_hi")


def palm(g, x, base, height, leaf="mt", trunk="trunk"):
    """Пальма: ствол с наклоном и 5 листьев."""
    for i in range(height):
        px(g, x + (i // 6), base - i, trunk)
    tx, ty = x + (height // 6), base - height
    line(g, tx, ty, tx - 4, ty - 2, leaf)
    line(g, tx, ty, tx + 4, ty - 2, leaf)
    line(g, tx, ty, tx - 5, ty + 1, leaf)
    line(g, tx, ty, tx + 5, ty + 1, leaf)
    line(g, tx, ty, tx, ty - 3, leaf)
    for dx, dy in ((-4, -2), (4, -2), (-5, 1), (5, 1), (0, -3)):
        px(g, tx + dx, ty + dy + 1, leaf)


def column(g, x, y0, y1, w, color, cap):
    fill(g, x, y0, min(x + w - 1, W - 1), y1, color)
    fill(g, max(x - 1, 0), y0, min(x + w, W - 1), y0, cap)
    fill(g, max(x - 1, 0), y1 - 1, min(x + w, W - 1), y1 - 1, cap)


def pipe(g, x0, x1, y, h, color, hi, dark):
    fill(g, x0, y, x1, y + h - 1, color)
    dither(g, y, x0, x1, hi, 1)
    fill(g, x0, y + h - 1, x1, y + h - 1, dark)


def neon(g, x, y, w, h, color):
    fill(g, x, y, x + w - 1, y + h - 1, color)
    px(g, x - 1, y - 1, color)
    px(g, x + w, y + h, color)


# ------------------------------------------------------------------ грунт
def ground_checker(g, z):
    fill(g, 0, HORIZON, W - 1, H - 1, "g1")
    for y in range(HORIZON + 4, H):
        for x in range(W):
            if ((x // 8) + ((y - HORIZON) // 8)) % 2 == 0:
                px(g, x, y, "g2")
    # травяная кромка
    fill(g, 0, HORIZON, W - 1, HORIZON + 2, "gm")
    for x in range(0, W, 3):
        px(g, x, HORIZON + 3, "gm")
    fill(g, 0, HORIZON + 3, W - 1, HORIZON + 3, "gd")
    for x in range(0, W, 2):
        px(g, x, HORIZON + 3, "gm")


def ground_waves(g, z):
    fill(g, 0, HORIZON, W - 1, H - 1, "g1")
    for i, y in enumerate(range(HORIZON, H, 3)):
        dither(g, y, 0, W - 1, "g2", 3, i)
    dither(g, HORIZON, 0, W - 1, "gm", 2, 0)
    dither(g, HORIZON + 1, 0, W - 1, "gm", 4, 2)


def ground_cracks(g, z):
    fill(g, 0, HORIZON, W - 1, H - 1, "g1")
    # остывшая корка: тёмные глыбы
    for y in range(HORIZON + 4, H, 5):
        dither(g, y, 0, W - 1, "g2", 3, 1)
    # светящиеся жилы, ветвящиеся
    veins = [
        [(6, HORIZON + 1), (8, HORIZON + 4), (9, HORIZON + 8), (7, HORIZON + 12)],
        [(26, HORIZON + 2), (24, HORIZON + 6), (25, HORIZON + 11), (28, HORIZON + 14)],
        [(48, HORIZON + 1), (50, HORIZON + 5), (49, HORIZON + 9), (52, HORIZON + 13)],
        [(58, HORIZON + 3), (60, HORIZON + 7), (59, HORIZON + 12)],
    ]
    for pts in veins:
        for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
            line(g, x0, y0, x1, y1, "gm")
        # ветка
        line(g, pts[1][0], pts[1][1], pts[1][0] + 3, pts[1][1] + 2, "gm")
    # лавовая кромка у горизонта
    dither(g, HORIZON, 0, W - 1, "gm", 2, 0)


def ground_toxic(g, z):
    fill(g, 0, HORIZON, W - 1, H - 1, "g1")
    for cx, cy, r in ((6, HORIZON + 6, 3), (24, HORIZON + 10, 2),
                      (46, HORIZON + 5, 3), (56, HORIZON + 12, 2)):
        for dy in range(-r, r + 1):
            for dx in range(-r, r + 1):
                if dx * dx + dy * dy <= r * r:
                    px(g, cx + dx, cy + dy, "gm")
    dither(g, HORIZON, 0, W - 1, "g2", 4, 2)


def ground_stripes(g, z):
    fill(g, 0, HORIZON, W - 1, H - 1, "g1")
    for y in range(HORIZON, H):
        for x in range(W):
            if (x + y) % 12 < 4:
                px(g, x, y, "g2")
    dither(g, HORIZON, 0, W - 1, "gm", 2, 0)


def ground_panels(g, z):
    fill(g, 0, HORIZON, W - 1, H - 1, "g1")
    # горизонтальные стыки панелей
    for y in range(HORIZON + 8, H, 9):
        fill(g, 0, y, W - 1, y, "g2")
        dither(g, y + 1, 0, W - 1, "g2", 2, 1)
    # вертикальные стыки со сдвигом
    for i, x in enumerate(range(4, W, 21)):
        fill(g, x, HORIZON + (i % 2) * 9, x, H - 1, "g2")
    # заклёпки по углам панелей
    for y in range(HORIZON + 2, H, 9):
        for x in range(2, W, 10):
            px(g, x, y, "gm")
            px(g, x + 1, y, "gm")
    dither(g, HORIZON, 0, W - 1, "gm", 4, 1)


def ground_grid(g, z):
    fill(g, 0, HORIZON, W - 1, H - 1, "g1")
    for y in range(HORIZON, H, 5):
        fill(g, 0, y, W - 1, y, "g2")
    for x in range(0, W, 9):
        fill(g, x, HORIZON, x, H - 1, "g2")
    dither(g, HORIZON, 0, W - 1, "gm", 2, 1)


def ground_sand(g, z):
    fill(g, 0, HORIZON, W - 1, H - 1, "g1")
    for i, y in enumerate(range(HORIZON + 3, H, 5)):
        dither(g, y, 0, W - 1, "g2", 4, i * 2)
    dither(g, HORIZON, 0, W - 1, "gm", 2, 1)


GROUNDS = {
    "checker": ground_checker, "waves": ground_waves, "cracks": ground_cracks,
    "toxic": ground_toxic, "stripes": ground_stripes, "panels": ground_panels,
    "grid": ground_grid, "sand": ground_sand,
}


# ------------------------------------------------------------ сцены зон
def scene_greenhill(g, z):
    sky(g, z)
    clouds(g, [(6, 8, 12), (40, 5, 9), (52, 12, 7)])
    hills(g, HORIZON, "far1", 9, 42, 0)
    hills(g, HORIZON, "far2", 6, 26, 12)
    ground_checker(g, z)
    palm(g, 4, HORIZON - 1, 17)
    palm(g, 58, HORIZON - 1, 13)


def scene_hydrocity(g, z):
    sky(g, z)
    for i, y in enumerate(range(HORIZON - 26, HORIZON)):
        dither(g, y, 0, W - 1, "skyf", 5 + (i % 4), i)
    column(g, 3, HORIZON - 26, HORIZON, 6, "far1", "far2")
    column(g, 54, HORIZON - 20, HORIZON, 6, "far1", "far2")
    ground_waves(g, z)


def scene_chemical(g, z):
    sky(g, z)
    for x, w in ((0, 14), (18, 20), (42, 16), (60, 4)):
        fill(g, x, HORIZON - 12, min(x + w - 1, W - 1), HORIZON, "far1")
    pipe(g, 0, W - 1, 6, 5, "far2", "cl", "far1")
    pipe(g, 0, W - 1, 30, 4, "far1", "far2", "far1")
    ground_toxic(g, z)


def scene_studio(g, z):
    sky(g, z)
    for x, y, w, h in ((4, 4, 10, 4), (46, 6, 12, 3), (8, 30, 8, 3), (50, 28, 10, 4)):
        neon(g, x, y, w, h, "far2")
    for i in range(4):
        line(g, 20 + i * 8, 0, 12 + i * 12, HORIZON, "far1")
    ground_stripes(g, z)


def scene_mirage(g, z):
    sky(g, z)
    # закат полосами
    for i, y in enumerate(range(HORIZON - 18, HORIZON)):
        if i % 4 == 0:
            dither(g, y, 0, W - 1, "skyf", 2, i)
    for y in range(6, 20):
        for x in range(44, 56):
            if (x - 50) ** 2 + (y - 13) ** 2 <= 30:
                px(g, x, y, "cl")
    hills(g, HORIZON, "far1", 7, 30, 6)
    ground_sand(g, z)


def scene_press(g, z):
    sky(g, z)
    column(g, 6, HORIZON - 30, HORIZON, 5, "far2", "cl")
    column(g, 50, HORIZON - 24, HORIZON, 5, "far2", "cl")
    for x, y in ((10, 10), (16, 16), (30, 8), (44, 14), (56, 20), (22, 24), (38, 18)):
        px(g, x, y, "gm")
        px(g, x + 1, y, "gm")
        px(g, x, y + 1, "gm")
    ground_stripes(g, z)


def scene_oil(g, z):
    sky(g, z)
    for i, y in enumerate(range(HORIZON - 16, HORIZON)):
        if i % 3 == 0:
            dither(g, y, 0, W - 1, "skyf", 3, i)
    # вышка
    line(g, 10, HORIZON, 14, HORIZON - 24, "far2")
    line(g, 22, HORIZON, 18, HORIZON - 24, "far2")
    for y in range(HORIZON - 22, HORIZON, 6):
        line(g, 12, y, 20, y, "far2")
    fill(g, 48, HORIZON - 10, 60, HORIZON - 1, "far1")
    ground_toxic(g, z)


def scene_lava(g, z):
    sky(g, z)
    hills(g, HORIZON, "far1", 10, 22, 3)
    for x in (6, 24, 46, 58):
        line(g, x, HORIZON - 8, x + 4, HORIZON, "far2")
    for x, y in ((10, 14), (30, 8), (52, 20), (20, 26), (44, 6)):
        px(g, x, y, "gm")
    ground_cracks(g, z)


def scene_metallic(g, z):
    sky(g, z)
    for y in range(0, HORIZON, 8):
        for x in range(0, W, 12):
            neon(g, x, y, 10, 6, "far1")
    for x in range(0, W, 12):
        fill(g, x, 0, x, HORIZON - 1, "far2")
    ground_panels(g, z)


def scene_titanic(g, z):
    sky(g, z)
    for x in range(0, W, 10):
        fill(g, x, 0, x, HORIZON - 1, "far1")
    for y in range(4, HORIZON, 10):
        fill(g, 0, y, W - 1, y, "far1")
    column(g, 8, HORIZON - 30, HORIZON, 5, "far2", "gm")
    column(g, 51, HORIZON - 22, HORIZON, 5, "far2", "gm")
    ground_grid(g, z)


def scene_stardust(g, z):
    sky(g, z)
    for x, y in ((6, 6), (16, 12), (48, 7), (56, 16), (30, 4), (40, 14), (10, 20)):
        px(g, x, y, "gm")
        px(g, x - 1, y, "gm")
        px(g, x + 1, y, "gm")
        px(g, x, y - 1, "gm")
    for i in range(6):
        line(g, 0, 8 + i * 6, 18, 2 + i * 6, "far2")
    ground_checker(g, z)


SCENES = {
    "greenhill": scene_greenhill, "hydrocity": scene_hydrocity,
    "chemical": scene_chemical, "studio": scene_studio,
    "mirage": scene_mirage, "press": scene_press, "oil": scene_oil,
    "lava": scene_lava, "metallic": scene_metallic,
    "titanic": scene_titanic, "stardust": scene_stardust,
}


# ------------------------------------------------------------------ бокс
# металл корпуса — как в игре, одинаковый во всех зонах
METAL = dict(
    outline="#0b1120",
    base="#9fb3c8",
    hi="#e8f1fa",
    hi2="#c6d6e6",
    dark="#5d7189",
    dark2="#41536a",
)


def draw_box(g, z):
    m = METAL
    fill(g, BOX["x0"] + 1, BOX["y0"] + 1, BOX["x1"] - 1, BOX["y1"] - 1, "M")
    # внешний контур
    for x in range(BOX["x0"] + 1, BOX["x1"]):
        px(g, x, BOX["y0"], "MK")
        px(g, x, BOX["y1"], "MK")
    for y in range(BOX["y0"] + 1, BOX["y1"]):
        px(g, BOX["x0"], y, "MK")
        px(g, BOX["x1"], y, "MK")
    for cx, cy in ((BOX["x0"], BOX["y0"]), (BOX["x1"], BOX["y0"]),
                   (BOX["x0"], BOX["y1"]), (BOX["x1"], BOX["y1"])):
        px(g, cx, cy, ".")

    # скос: 2 px света сверху-слева, 2 px тени снизу-справа
    for x in range(BOX["x0"] + 1, BOX["x1"]):
        px(g, x, BOX["y0"] + 1, "MH")
        px(g, x, BOX["y0"] + 2, "MH2")
        px(g, x, BOX["y1"] - 1, "MD")
        px(g, x, BOX["y1"] - 2, "MD2")
    for y in range(BOX["y0"] + 1, BOX["y1"]):
        px(g, BOX["x0"] + 1, y, "MH")
        px(g, BOX["x0"] + 2, y, "MH2")
        px(g, BOX["x1"] - 1, y, "MD")
        px(g, BOX["x1"] - 2, y, "MD2")

    # заклёпки на рамке
    for x in range(BOX["x0"] + 6, BOX["x1"] - 4, 10):
        px(g, x, BOX["y0"] + 3, "MH")
        px(g, x, BOX["y1"] - 3, "MD")
    for y in range(BOX["y0"] + 6, BOX["y1"] - 4, 10):
        px(g, BOX["x0"] + 3, y, "MH2")
        px(g, BOX["x1"] - 3, y, "MD2")

    # экран
    for x in range(SCR["x0"] - 1, SCR["x1"] + 2):
        px(g, x, SCR["y0"] - 1, "MK")
        px(g, x, SCR["y1"] + 1, "MK")
    for y in range(SCR["y0"] - 1, SCR["y1"] + 2):
        px(g, SCR["x0"] - 1, y, "MK")
        px(g, SCR["x1"] + 1, y, "MK")
    fill(g, SCR["x0"], SCR["y0"], SCR["x1"], SCR["y1"], "S")

    # глянец стекла: диагональный блик в левом верхнем углу
    for i in range(12):
        for j in range(0, 3):
            x = SCR["x0"] + i + j
            y = SCR["y0"] + 1 + j
            if SCR["x0"] <= x <= SCR["x1"] and SCR["y0"] <= y <= SCR["y1"]:
                px(g, x, y, "SGL")
    # утопленность снизу и справа
    for x in range(SCR["x0"], SCR["x1"] + 1):
        px(g, x, SCR["y1"], "I")
    for y in range(SCR["y0"], SCR["y1"] + 1):
        px(g, SCR["x1"], y, "I")


STAND_NECK = dict(x0=28, x1=35, y0=45, y1=48)
STAND_BASE = dict(x0=21, x1=42, y0=49, y1=51)


def draw_stand(g):
    """Подставка: шея + широкая база, как у настольного монитора."""
    n, b = STAND_NECK, STAND_BASE
    # шея
    fill(g, n["x0"], n["y0"], n["x1"], n["y1"], "M")
    fill(g, n["x0"], n["y0"], n["x0"], n["y1"], "MH2")
    fill(g, n["x1"], n["y0"], n["x1"], n["y1"], "MD")
    fill(g, n["x0"], n["y1"], n["x1"], n["y1"], "MD2")
    # база
    fill(g, b["x0"], b["y0"], b["x1"], b["y1"], "M")
    fill(g, b["x0"], b["y0"], b["x1"], b["y0"], "MH")
    fill(g, b["x0"], b["y1"], b["x1"], b["y1"], "MD2")
    fill(g, b["x0"], b["y0"], b["x0"], b["y1"], "MH2")
    fill(g, b["x1"], b["y0"], b["x1"], b["y1"], "MD")
    # контур снизу
    for x in range(b["x0"], b["x1"] + 1):
        px(g, x, b["y1"] + 1, "MK")
    px(g, b["x0"] - 1, b["y0"], "MK")
    px(g, b["x1"] + 1, b["y0"], "MK")


def draw_ring(g):
    for y in range(H):
        for x in range(W):
            d = math.hypot(x + 0.5 - RING_CX, y + 0.5 - RING_CY)
            if not (RING_R_IN <= d <= RING_R_OUT):
                continue
            if not (SCR["x0"] <= x <= SCR["x1"] and SCR["y0"] <= y <= SCR["y1"]):
                continue
            proj = (x + 0.5 - RING_CX) + (y + 0.5 - RING_CY)
            if d > RING_R_OUT - 1.1 or d < RING_R_IN + 1.0:
                px(g, x, y, "O")
            elif proj < -4.2:
                px(g, x, y, "L")
            elif proj > 4.2:
                px(g, x, y, "O")
            else:
                px(g, x, y, "G")
    # блик: короткая дуга на верхней левой части кольца
    for dx in (-4, -3, -2):
        px(g, int(RING_CX) + dx, int(RING_CY) - 8, "L")


# ---------------------------------------------------------------- палитра
def pal(z):
    return {
        ".": None,
        "sky": z["sky"], "skyf": z.get("skyf", shade(z["sky"], 0.2)),
        "cl": z.get("cloud", shade(z["sky"], 0.62)),
        "g1": z["g1"], "g2": z["g2"], "gm": z["gm"],
        "gd": shade(z["g1"], -0.4),
        "mt": z.get("motif_color", z["gm"]),
        "trunk": z.get("trunk", shade(z["g1"], -0.3)),
        "hill_hi": z.get("hill_hi", shade(z["far1"], 0.22)),
        "far1": z.get("far1", shade(z["sky"], -0.3)),
        "far2": z.get("far2", shade(z["g1"], 0.08)),
        "MK": METAL["outline"], "M": METAL["base"],
        "MH": METAL["hi"], "MH2": METAL["hi2"],
        "MD": METAL["dark"], "MD2": METAL["dark2"],
        "S": SCREEN, "I": SCREEN_HI, "SGL": "#1d3350",
        "G": GOLD, "O": GOLD_DARK, "L": GOLD_LIGHT,
    }


def build_bg(z):
    g = blank()
    SCENES[z["name"] if "name" in z else z["scene"]](g, z)
    return g, pal(z)


def build_fg(z):
    g = blank()
    draw_box(g, z)
    draw_ring(g)
    draw_stand(g)
    return g, pal(z)


def build(z):
    g = blank()
    SCENES[z["name"] if "name" in z else z["scene"]](g, z)
    draw_box(g, z)
    draw_ring(g)
    draw_stand(g)
    return g, pal(z)


# ---------------------------------------------------------------- рендер
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


def svg_from(grid, colors, size=None, radius=0):
    dim = f' width="{size}" height="{size}"' if size else ""
    bg = (f'<rect width="{W}" height="{H}" rx="{radius}" fill="{colors.get("sky")}"/>'
          if radius else "")
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}"{dim} '
            'shape-rendering="crispEdges">' + bg + rects_from(grid, colors) + "</svg>")


def ascii_preview(grid, step=1):
    return "\n".join("".join("." if c == "." else c for c in row) for row in grid)
