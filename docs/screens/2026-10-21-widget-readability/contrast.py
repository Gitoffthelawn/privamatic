"""WCAG contrast of widget text/icon against the blended background, from screenshots.

Usage: python3 -I contrast.py <screens_dir>
Per <wp>-<th>: element boxes come from <wp>-<th>-ui.xml; the glyph mask and the
foreground colour come from the 100 % shot (foreground colours do not change with
opacity, only the background layer's alpha does). For every opacity, the background
is all unmasked pixels inside the element box.
"""
import re
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ELEMENTS = {
    # id: (label, WCAG minimum)
    "widget_device_name": ("device name (18sp bold)", 4.5),
    "widget_os_name": ("OS name (14sp)", 4.5),
    "widget_score_value": ("score (24sp bold, large)", 3.0),
    "widget_score_rating": ("rating (14sp)", 4.5),
    "widget_divider": ("shield icon (non-text)", 3.0),
    "widget_privacy_change_up": ("delta up (18sp)", 4.5),
    "widget_privacy_change_down": ("delta down (18sp)", 4.5),
}
Y0 = 380  # sweep PNGs are cropped to the widget band y 380–760 (full width)
OPACITIES = ["000", "025", "050", "075", "100"]
NODE_RE = re.compile(r'<node [^>]*?resource-id="[^"]*:id/(widget_\w+)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')


def lin(c):
    c = c / 255.0
    return np.where(c <= 0.04045, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)


def lum(rgb):
    r, g, b = lin(np.asarray(rgb, dtype=float)).T
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def ratio(l1, l2):
    hi, lo = np.maximum(l1, l2), np.minimum(l1, l2)
    return (hi + 0.05) / (lo + 0.05)


def hexc(rgb):
    return "#%02X%02X%02X" % tuple(int(round(v)) for v in rgb)


def dilate(mask, r=2):
    out = mask.copy()
    for dy in range(-r, r + 1):
        for dx in range(-r, r + 1):
            out |= np.roll(np.roll(mask, dy, 0), dx, 1)
    return out


def boxes(ui_path):
    found = {}
    for m in NODE_RE.finditer(ui_path.read_text(errors="replace")):
        rid, x1, y1, x2, y2 = m.group(1), *map(int, m.groups()[1:])
        if x2 > x1 and y2 > y1:
            found[rid] = (x1, y1, x2, y2)
    return found


def analyse(d, wp, th):
    ui = d / f"{wp}-{th}-ui.xml"
    ref_png = d / f"{wp}-{th}-o100.png"
    if not ui.exists() or not ref_png.exists():
        return []
    bx = boxes(ui)
    ref = np.asarray(Image.open(ref_png).convert("RGB"), dtype=float)
    rows = []
    for rid, (label, minimum) in ELEMENTS.items():
        if rid not in bx:
            continue
        x1, y1, x2, y2 = bx[rid]
        y1, y2 = y1 - Y0, y2 - Y0
        crop = ref[y1:y2, x1:x2]
        bg_med = np.median(crop.reshape(-1, 3), axis=0)
        dist = np.linalg.norm(crop - bg_med, axis=2)
        if dist.max() < 12:  # nothing drawn in this box
            continue
        glyph = dist > max(12.0, 0.25 * dist.max())
        core = dist >= np.percentile(dist[glyph], 70)
        fg = np.median(crop[core], axis=0)
        mask = dilate(glyph, 2)
        for o in OPACITIES:
            p = d / f"{wp}-{th}-o{o}.png"
            if not p.exists():
                continue
            img = np.asarray(Image.open(p).convert("RGB"), dtype=float)[y1:y2, x1:x2]
            bgpix = img[~mask]
            if len(bgpix) < 20:
                continue
            r_all = ratio(lum(fg), lum(bgpix))
            rows.append({
                "wp": wp, "th": th, "op": int(o), "el": label, "min": minimum,
                "fg": hexc(fg), "bg": hexc(np.median(bgpix, axis=0)),
                "cr_med": float(ratio(lum(fg), lum(np.median(bgpix, axis=0)))),
                "cr_p5": float(np.percentile(r_all, 5)),
            })
    return rows


def main():
    d = Path(sys.argv[1])
    rows = []
    for wp in ["orig", "light", "dark"]:
        for th in ["day", "night"]:
            rows += analyse(d, wp, th)
    print("| Wallpaper | Theme | Opacity | Element | Text/icon | Blended bg (median) | Contrast (median bg) | Worst 5 % | Min | Pass |")
    print("|---|---|---|---|---|---|---|---|---|---|")
    for r in rows:
        ok = "yes" if r["cr_p5"] >= r["min"] else ("borderline" if r["cr_med"] >= r["min"] else "**no**")
        print(f"| {r['wp']} | {r['th']} | {r['op']} % | {r['el']} | {r['fg']} | {r['bg']} | "
              f"{r['cr_med']:.2f} | {r['cr_p5']:.2f} | {r['min']} | {ok} |")


if __name__ == "__main__":
    main()
