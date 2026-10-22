"""WCAG contrast of every widget on one home-screen shot (several widgets, top to bottom).

Usage: python3 -I widget_contrast.py <png> <ui.xml> <label,label,...> <wp> <theme> [<mask.png>]
Labels name the widgets from top to bottom: an opacity in percent ("76") or "0" for the
no-background style. Method as docs/screens/2026-10-21-widget-readability/contrast.py:
per element box, the glyph mask is every pixel far from the box's median colour, the text colour
is the median of the glyph's core, and the background is every pixel outside the dilated mask.
"Worst 5 %" is the 5th percentile of the contrast against those background pixels.

No-background widgets: the text colour is fixed (white, or the light tint), so the glyph is the
pixels near the brightest core colour. On a white wallpaper that can't separate text from
wallpaper, so <mask.png> (the same widgets on the Black wallpaper) gives the glyph mask; the text
colour is then the median of the shot's own pixels under the mask core. Two backgrounds are reported:
  - "with shadow": the 1–4 px ring around the glyph, where the shadow sits;
  - "wallpaper": pixels at least 12 px from any glyph in the widget, i.e. the wallpaper alone.
"""
import re
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ELEMENTS = {
    "widget_device_name": ("device name (18sp bold)", 4.5),
    "widget_os_name": ("OS name (14sp)", 4.5),
    "widget_score_value": ("score (24sp bold)", 4.5),
    "widget_score_delta": ("change (20sp bold)", 4.5),
    "widget_score_rating": ("rating (14sp)", 4.5),
    "widget_divider": ("shield icon", 3.0),
}
NODE_RE = re.compile(r'<node [^>]*?resource-id="[^"]*:id/(widget_\w+)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')


def lin(c):
    c = np.asarray(c, dtype=float) / 255.0
    return np.where(c <= 0.04045, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)


def lum(rgb):
    r, g, b = np.moveaxis(lin(rgb), -1, 0)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def ratio(l1, l2):
    return (np.maximum(l1, l2) + 0.05) / (np.minimum(l1, l2) + 0.05)


def hexc(rgb):
    return "#%02X%02X%02X" % tuple(int(round(v)) for v in rgb)


def dilate(mask, r):
    out = mask.copy()
    for dy in range(-r, r + 1):
        for dx in range(-r, r + 1):
            out |= np.roll(np.roll(mask, dy, 0), dx, 1)
    return out


def widgets(ui_text):
    """[{id: box}] per widget, top to bottom, grouped by the widget_root that contains each node."""
    nodes = [(m.group(1), tuple(map(int, m.groups()[1:]))) for m in NODE_RE.finditer(ui_text)]
    roots = sorted([b for rid, b in nodes if rid == "widget_root"], key=lambda b: b[1])
    out = []
    for rx1, ry1, rx2, ry2 in roots:
        d = {"widget_root": (rx1, ry1, rx2, ry2)}
        for rid, (x1, y1, x2, y2) in nodes:
            if rid in ELEMENTS and x2 > x1 and y2 > y1 and rx1 <= x1 and x2 <= rx2 and ry1 <= y1 and y2 <= ry2:
                d[rid] = (x1, y1, x2, y2)
        out.append(d)
    return out


def card_row(img, box):
    x1, y1, x2, y2 = box
    crop = img[y1:y2, x1:x2]
    med = np.median(crop.reshape(-1, 3), axis=0)
    dist = np.linalg.norm(crop - med, axis=2)
    if dist.max() < 12:
        return None
    glyph = dist > max(12.0, 0.25 * dist.max())
    core = dist >= np.percentile(dist[glyph], 70)
    fg = np.median(crop[core], axis=0)
    bg = crop[~dilate(glyph, 2)]
    r = ratio(lum(fg), lum(bg))
    return hexc(fg), hexc(np.median(bg, axis=0)), float(np.percentile(r, 5)), float(ratio(lum(fg), lum(np.median(bg, axis=0))))


def bare_glyph(crop):
    """Glyph pixels of light text: near the brightest core colour of the box."""
    l = lum(crop)
    core = l >= np.percentile(l, 97)
    fg = np.median(crop[core], axis=0)
    return np.linalg.norm(crop - fg, axis=2) < 40, fg


def bare_rows(img, w, ref):
    rx1, ry1, rx2, ry2 = w["widget_root"]
    root = img[ry1:ry2, rx1:rx2]
    all_glyphs = np.zeros(root.shape[:2], bool)
    found = {}
    for rid in ELEMENTS:
        if rid not in w:
            continue
        x1, y1, x2, y2 = w[rid]
        crop = img[y1:y2, x1:x2]
        g, _ = bare_glyph(ref[y1:y2, x1:x2])
        l = lum(ref[y1:y2, x1:x2])
        core = g & (l >= np.percentile(l[g], 50)) if g.any() else g
        fg = np.median(crop[core], axis=0)
        found[rid] = (g, fg)
        all_glyphs[y1 - ry1:y2 - ry1, x1 - rx1:x2 - rx1] |= g
    far = ~dilate(all_glyphs, 12)
    wallpaper = root[far]
    rows = []
    for rid, (g, fg) in found.items():
        x1, y1, x2, y2 = w[rid]
        crop = img[y1:y2, x1:x2]
        ring = dilate(g, 4) & ~dilate(g, 1)
        rp = crop[ring]
        r_ring = ratio(lum(fg), lum(rp))
        r_wp = ratio(lum(fg), lum(wallpaper))
        rows.append((rid, hexc(fg), hexc(np.median(rp, axis=0)), float(np.median(r_ring)), float(np.percentile(r_ring, 5)),
                     hexc(np.median(wallpaper, axis=0)), float(np.median(r_wp)), float(np.percentile(r_wp, 5))))
    return rows


def main():
    png, ui, labels = Path(sys.argv[1]), Path(sys.argv[2]), sys.argv[3].split(",")
    wp, th = sys.argv[4], sys.argv[5]
    img = np.asarray(Image.open(png).convert("RGB"), dtype=float)
    ref = np.asarray(Image.open(sys.argv[6]).convert("RGB"), dtype=float) if len(sys.argv) > 6 else img
    ws = widgets(ui.read_text(errors="replace"))
    if len(ws) != len(labels):
        sys.exit(f"{len(ws)} widgets in {ui}, {len(labels)} labels")
    for label, w in zip(labels, ws):
        if label == "0":
            for rid, fg, ringc, r_med, r_p5, wpc, w_med, w_p5 in bare_rows(img, w, ref):
                name, need = ELEMENTS[rid]
                print(f"| {wp} | {th} | none | {name} | {fg} | {ringc} | {r_med:.2f} | {r_p5:.2f} | {wpc} | {w_med:.2f} | {w_p5:.2f} | {need} |")
            continue
        for rid, (name, need) in ELEMENTS.items():
            if rid not in w:
                continue
            row = card_row(img, w[rid])
            if row is None:
                continue
            fg, bg, p5, med = row
            ok = "yes" if p5 >= need else "**no**"
            print(f"| {wp} | {th} | {label} % | {name} | {fg} | {bg} | {med:.2f} | {p5:.2f} | {need} | {ok} |")


if __name__ == "__main__":
    main()
