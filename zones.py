"""Палитры и пейзажи зон Ringbox.

Каждая зона — своя сцена: небо, дальний план, грунт, мотив.
Бокс с кольцом один и тот же, тонируется под зону.
"""

ZONES = {
    "greenhill": dict(
        label="Зелёные холмы", ground="checker", motif="palm",
        sky="#4aa9e0", g1="#b06a34", g2="#8c4f22", gm="#3fbf3f",
        frame="#4fd44f", clouds=True,
        far=dict(kind="hills", amp=6, wl=17, ph=0),
        far1="#3f9a4e", far2="#2c7a3c",
    ),
    "hydrocity": dict(
        label="Аквагород", ground="waves", motif="bubbles",
        sky="#0d3a63", g1="#10557f", g2="#1f7ba6", gm="#57d8ff",
        frame="#35c6ff",
        far=dict(kind="pillars", w=4, gap=10, ph=2),
        far1="#0b2f52", far2="#0a2743",
    ),
    "chemical": dict(
        label="Химзавод", ground="toxic", motif="pipes",
        sky="#3a1a5c", g1="#12323f", g2="#1c4a56", gm="#ff4fa3",
        frame="#2fe0c8",
        far=dict(kind="blocks", seed=5),
        far1="#2a1140", far2="#3c1857",
    ),
    "studio": dict(
        label="Студия", ground="stripes", motif="stars",
        sky="#2b0b28", g1="#4a1140", g2="#6b1a55", gm="#ffd23f",
        frame="#ff7ab8",
        far=dict(kind="blocks", seed=9),
        far1="#3a0f33", far2="#52154a",
    ),
    "mirage": dict(
        label="Мираж-салун", ground="sand", motif="none",
        sky="#c2603a", g1="#d99a5c", g2="#b8763c", gm="#7a3a22",
        frame="#ffb15c", sun=(25, 4),
        far=dict(kind="hills", amp=4, wl=13, ph=3),
        far1="#a35a34", far2="#b8724a",
    ),
    "press": dict(
        label="Пресс-сад", ground="stripes", motif="stars",
        sky="#16303f", g1="#cfd9e8", g2="#9fb2c8", gm="#ff7aa8",
        frame="#8fa8d8",
        far=dict(kind="pillars", w=5, gap=12, ph=1),
        far1="#b8c6d8", far2="#cfd9e8",
    ),
    "oil": dict(
        label="Нефтеокеан", ground="toxic", motif="bubbles",
        sky="#0a1a22", g1="#123a2a", g2="#1b5236", gm="#57e0a0",
        frame="#ff8c2b",
        far=dict(kind="blocks", seed=3),
        far1="#0b2430", far2="#123243",
    ),
    "lava": dict(
        label="Лавовый риф", ground="cracks", motif="stars",
        sky="#2a0a12", g1="#3a0f1e", g2="#54182a", gm="#ff5b2e",
        frame="#ff7a2e",
        far=dict(kind="hills", amp=5, wl=11, ph=2),
        far1="#2e0d18", far2="#3d1222",
    ),
    "metallic": dict(
        label="Металл", ground="panels", motif="none",
        sky="#1a0d0d", g1="#4a1a1a", g2="#2e1010", gm="#cfd6e0",
        frame="#d94b4b",
        far=dict(kind="blocks", seed=7),
        far1="#3a1414", far2="#4a1a1a",
    ),
    "titanic": dict(
        label="Титаник", ground="grid", motif="stars",
        sky="#0b0a26", g1="#1a1440", g2="#2a2060", gm="#b06bff",
        frame="#b06bff",
        far=dict(kind="pillars", w=3, gap=8, ph=3),
        far1="#1a1450", far2="#241a63",
    ),
    "stardust": dict(
        label="Звёздная пыль", ground="checker", motif="stars",
        sky="#100a2a", g1="#2a1a6a", g2="#3a2a8a", gm="#ffd23f",
        frame="#ffd23f",
        far=dict(kind="hills", amp=4, wl=15, ph=1),
        far1="#241357", far2="#301a72",
    ),
}


# имя сцены совпадает с ключом зоны
for _k in ZONES:
    ZONES[_k]["scene"] = _k


def icon_svg(name, size=None, radius=0):
    import sprite
    grid, colors = sprite.build(ZONES[name])
    return sprite.svg_from(grid, colors, size=size, radius=radius)
