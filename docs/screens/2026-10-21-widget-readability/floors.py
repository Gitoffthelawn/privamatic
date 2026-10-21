"""Theoretical contrast of a fixed foreground over the widget card at opacity a,
composited on the worst wallpapers (pure white and pure black).

Material tones are modelled as neutral greys (L* = tone); compositing is in
gamma-encoded sRGB, which matches the measured screenshots (see contrast.py).
"floor" = lowest opacity from which every higher opacity passes on both wallpapers.

Usage: python3 -I floors.py
"""


def tone_to_srgb(t):
    y = ((t + 16) / 116) ** 3 if t > 8 else t / 903.3
    return 12.92 * y if y <= 0.0031308 else 1.055 * y ** (1 / 2.4) - 0.055


def hex_to_srgb(h):
    return [int(h[i:i + 2], 16) / 255 for i in (1, 3, 5)]


def lin(c):
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def lum(rgb):
    return sum(w * lin(v) for w, v in zip((0.2126, 0.7152, 0.0722), rgb))


def ratio(a, b):
    hi, lo = max(a, b), min(a, b)
    return (hi + 0.05) / (lo + 0.05)


def worst(fg, card, a):
    """fg, card: sRGB triples. Worst contrast over white and black wallpaper."""
    on_white = [a * c + (1 - a) for c in card]
    on_black = [a * c for c in card]
    return min(ratio(lum(fg), lum(on_white)), ratio(lum(fg), lum(on_black)))


def floor(fg, card, target):
    ok = [worst(fg, card, i / 100) >= target for i in range(101)]
    return next((i for i in range(101) if all(ok[i:])), None)


def grey(t):
    return [tone_to_srgb(t)] * 3


def row(name, fg, card, target):
    cells = " | ".join(f"{worst(fg, card, a / 100):.1f}" for a in (0, 25, 50, 75, 100))
    print(f"| {name} | {target} | {cells} | {floor(fg, card, target)} |")


print("| Pair | Target | 0 % | 25 % | 50 % | 75 % | 100 % | Floor % |")
print("|---|---|---|---|---|---|---|---|")
print("| **Today (layout-v31 / layout-night-v31)** | | | | | | | |")
row("light: name/score accent1_600 (T40) on accent2_50 (T95)", grey(40), grey(95), 4.5)
row("light: OS/rating accent1_800 (T20) on T95", grey(20), grey(95), 4.5)
row("light: shield neutral1_300 (T70) on T95", grey(70), grey(95), 3.0)
row("dark: name/score accent1_100 (T90) on accent2_800 (T20)", grey(90), grey(20), 4.5)
row("dark: OS/rating accent1_200 (T80) on T20", grey(80), grey(20), 4.5)
row("dark: shield neutral1_500 (T50) on T20", grey(50), grey(20), 3.0)
row("light: delta #BA1A1A on T95", hex_to_srgb("#BA1A1A"), grey(95), 4.5)
print("| **Physical limit (pure black / white text)** | | | | | | | |")
row("black text on T95 card", [0, 0, 0], grey(95), 4.5)
row("white text on T10 card", [1, 1, 1], grey(10), 4.5)
print("| **Proposed card tokens** | | | | | | | |")
row("light: primary T10 on T95", grey(10), grey(95), 4.5)
row("light: secondary T20 on T95", grey(20), grey(95), 4.5)
row("light: shield T30 on T95", grey(30), grey(95), 3.0)
row("dark: primary T95 on T10", grey(95), grey(10), 4.5)
row("dark: secondary T90 on T10", grey(90), grey(10), 4.5)
row("dark: shield T80 on T10", grey(80), grey(10), 3.0)
print("| **Proposed rating / delta tints** | | | | | | | |")
for name, h, t in [("green #004225", "#004225", 95), ("teal #224C58", "#224C58", 95), ("red #93000A", "#93000A", 95)]:
    row(f"light: {name} on T95 (score, large)", hex_to_srgb(h), grey(t), 3.0)
    row(f"light: {name} on T95 (delta, 18sp)", hex_to_srgb(h), grey(t), 4.5)
for name, h in [("green #93D5AA", "#93D5AA"), ("teal #A3CDDB", "#A3CDDB"), ("red #FFB4AB", "#FFB4AB")]:
    row(f"dark: {name} on T10 (score, large)", hex_to_srgb(h), grey(10), 3.0)
    row(f"dark: {name} on T10 (delta, 18sp)", hex_to_srgb(h), grey(10), 4.5)
