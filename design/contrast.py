#!/usr/bin/env python3
"""Print the WCAG ratio of every color pair the spec claims, parsed from SpindleTokens.kt.

Text pairs need >= 4.5, UI pairs >= 3. Exits 1 if any pair fails.
Run from the repo root: python3 design/contrast.py
"""
import pathlib
import re
import sys

TOKENS = pathlib.Path(__file__).resolve().parent.parent / (
    "app/src/main/java/com/markfoundry/spindle/ui/theme/SpindleTokens.kt"
)


def parse():
    palettes, current = {}, None
    for line in TOKENS.read_text().splitlines():
        m = re.match(r"\s*object (\w+)", line)
        if m:
            current = palettes.setdefault(m.group(1), {})
            continue
        m = re.match(r"\s*val (\w+) = Color\(0x([0-9A-Fa-f]{8})\)", line)
        if m and current is not None:
            argb = int(m.group(2), 16)
            current[m.group(1)] = ((argb >> 24) / 255, (argb >> 16) & 255, (argb >> 8) & 255, argb & 255)
    return palettes


def over(fg, bg):
    """Composite fg (with alpha) over an opaque bg; returns an opaque color."""
    a = fg[0]
    return (1.0,) + tuple(round(a * f + (1 - a) * b) for f, b in zip(fg[1:], bg[1:]))


def lum(c):
    def ch(v):
        v /= 255
        return v / 12.92 if v <= 0.03928 else ((v + 0.055) / 1.055) ** 2.4
    r, g, b = (ch(v) for v in c[1:])
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def ratio(a, b):
    hi, lo = sorted((lum(a), lum(b)), reverse=True)
    return (hi + 0.05) / (lo + 0.05)


WHITE = (1.0, 255, 255, 255)

# (palette, fg, bg, kind, note). bg "Tray@White" = translucent tray composited over white art.
PAIRS = [
    ("AnalogDayPalette", "Text", "Tray", "text", "title"),
    ("AnalogDayPalette", "Text2", "Tray", "text", "artist/meta/time"),
    ("AnalogDayPalette", "Ink", "Ground", "text", "gate copy"),
    ("AnalogDayPalette", "Accent", "Tray", "ui", "seek fill"),
    ("AnalogDayPalette", "PlayIcon", "PlayFill", "ui", "play icon"),
    ("AnalogDayPalette", "PlayRim", "Tray", "ui", "play rim (fill vs tray is 1.97)"),
    ("AnalogDayPalette", "SeekTrack", "Tray", "ui", "seek track"),
    ("AnalogDayPalette", "SeekThumb", "Tray", "ui", "seek thumb"),
    ("AnalogDayPalette", "CtaLabel", "CtaFill", "text", "gate CTA"),
    ("AnalogNightPalette", "Text", "Tray", "text", "title"),
    ("AnalogNightPalette", "Text2", "Tray", "text", "artist/meta/time"),
    ("AnalogNightPalette", "Ink", "Ground", "text", "gate copy"),
    ("AnalogNightPalette", "Accent", "Tray", "ui", "seek fill"),
    ("AnalogNightPalette", "PlayIcon", "PlayFill", "ui", "play icon"),
    ("AnalogNightPalette", "PlayFill", "Tray", "ui", "play fill"),
    ("AnalogNightPalette", "SeekTrack", "Tray", "ui", "seek track"),
    ("AnalogNightPalette", "SeekThumb", "Tray", "ui", "seek thumb"),
    ("AnalogNightPalette", "CtaLabel", "CtaFill", "text", "gate CTA"),
    ("FluxPalette", "Text", "Tray@White", "text", "title, worst case"),
    ("FluxPalette", "Text2", "Tray@White", "text", "artist, worst case"),
    ("FluxPalette", "Text2", "Ground", "text", "artist over void"),
    ("FluxPalette", "Ink", "Ground", "text", "gate copy"),
    ("FluxPalette", "Accent", "Ground", "ui", "ring / seek fill"),
    ("FluxPalette", "PlayIcon", "Accent", "ui", "play icon on cyan stop"),
    ("FluxPalette", "PlayIcon", "Violet", "ui", "play icon on violet stop"),
    ("FluxPalette", "PlayIcon", "Magenta", "ui", "play icon on magenta stop"),
    ("FluxPalette", "SeekTrack", "Tray@White", "ui", "seek track, worst case"),
    ("FluxPalette", "SeekTrack", "Ground", "ui", "seek track over void"),
    ("FluxPalette", "SeekThumb", "Tray@White", "ui", "seek thumb, worst case"),
    ("FluxPalette", "CtaLabel", "CtaFill", "text", "gate CTA"),
]

MIN = {"text": 4.5, "ui": 3.0}


def main():
    pal = parse()
    failed = 0
    for name, fg_key, bg_key, kind, note in PAIRS:
        p = pal[name]
        bg_name, _, base = bg_key.partition("@")
        bg = over(p[bg_name], WHITE if base == "White" else p.get("Ground", WHITE))
        fg = over(p[fg_key], bg)
        r = ratio(fg, bg)
        ok = r >= MIN[kind]
        failed += not ok
        print(f"{'PASS' if ok else 'FAIL'} {r:5.2f} >= {MIN[kind]:.1f} {kind:4} {name[:-7]:12} {fg_key}/{bg_key}  ({note})")
    print(f"{len(PAIRS) - failed}/{len(PAIRS)} pairs pass")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
