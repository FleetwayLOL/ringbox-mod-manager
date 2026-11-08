#!/usr/bin/env python3
"""live/index.html = app/ui-v4.html + app/live.css (полноэкранный режим на телефоне)."""
import pathlib
root = pathlib.Path(__file__).parent
proto = (root / "app/ui-v4.html").read_text()
css = (root / "app/live.css").read_text()
i = proto.index("\n</style>")
out = proto[:i] + "\n/* ===== LIVE-режим " + "=" * 37 + " */\n" + css + proto[i:]
(root / "live/index.html").write_text(out)
print("live/index.html пересобран:", len(out), "байт")
