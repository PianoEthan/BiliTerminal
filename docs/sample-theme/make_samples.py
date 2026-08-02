#!/usr/bin/env python3
"""重新生成 docs/sample-theme 下的正/负样例包。"""
import json, os, zipfile
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
BUILD = os.path.join(HERE, "build")
os.makedirs(BUILD, exist_ok=True)


def hsv2rgb(h, s, v):
    import colorsys
    r, g, b = colorsys.hsv_to_rgb(h, s, v)
    return int(r * 255), int(g * 255), int(b * 255)


def gradient(size, hue_a, hue_b, sat=120):
    w, h = size
    img = Image.new("RGB", (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            t = (x / max(1, w - 1) + y / max(1, h - 1)) / 2
            px[x, y] = hsv2rgb(hue_a + (hue_b - hue_a) * t, sat / 255.0,
                               0.55 + 0.3 * (1 - t))
    return img


def make_zip(path, entries):
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as z:
        for name, data in entries:
            z.writestr(name, data)
    print(path, os.path.getsize(path), "bytes")


preview = gradient((360, 240), 170, 210)
preview.save(os.path.join(BUILD, "aurora_preview.png"))
background = gradient((720, 1280), 165, 195, sat=150)
d = ImageDraw.Draw(background, "RGBA")
for i in range(6):
    y = 120 + i * 200
    d.ellipse((60 - i * 10, y, 660 + i * 10, y + 180), fill=(255, 255, 255, 26))
background.save(os.path.join(BUILD, "aurora_background.png"))

theme_json = {
    "format": 1,
    "id": "example.aurora",
    "meta": {"name": "极光", "author": "BiliTerminal 示例", "version": 1,
             "description": "青色种子 #008080 · 混合强度 40 · 背景图取色兜底"},
    "preview": "preview.png",
    "colors": {"mode": "auto", "seed": "#008080",
               "source": {"image": "background.png", "fallback": "#008080"},
               "contrast": 0.0, "blend": 40, "override": {"link": "#00E5FF"}},
    "background": {"image": "background.png", "fit": "centerCrop",
                   "scrim": 0.5, "scrimColor": "#000000"},
}

make_zip(os.path.join(HERE, "aurora.btheme"), [
    ("theme.json", json.dumps(theme_json, ensure_ascii=False)),
    ("preview.png", open(os.path.join(BUILD, "aurora_preview.png"), "rb").read()),
    ("background.png", open(os.path.join(BUILD, "aurora_background.png"), "rb").read()),
])
make_zip(os.path.join(HERE, "zip-slip.btheme"), [
    ("../evil.png", b"x" * 100),
    ("theme.json", json.dumps({**theme_json, "id": "evil.slip"})),
])
make_zip(os.path.join(HERE, "format99.btheme"), [
    ("theme.json", json.dumps({**theme_json, "format": 99})),
])
make_zip(os.path.join(HERE, "no-json.btheme"), [
    ("preview.png", open(os.path.join(BUILD, "aurora_preview.png"), "rb").read()),
])
blob = os.urandom(5 * 1024 * 1024)
make_zip(os.path.join(HERE, "oversize.btheme"), [
    ("theme.json", json.dumps(theme_json)),
    ("background.png", blob),
    ("background_dark.png", blob),
])
make_zip(os.path.join(HERE, "entry-too-big.btheme"), [
    ("theme.json", json.dumps(theme_json)),
    ("background.png", os.urandom(7 * 1024 * 1024)),
])
