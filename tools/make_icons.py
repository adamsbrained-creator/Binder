#!/usr/bin/env python3
"""Builds the launcher icon foregrounds for Binder and Catalog.

Takes the variable Outfit font (SIL OFL 1.1, see FONT-LICENSES/Outfit-OFL.txt), pins it to
weight 500 (Medium), sets each word, applies the font's own kerning, and writes the outlines
as Android vector pathData. The vectors contain no text, so the icons need no font at run time.

Usage:
    pip install fonttools
    python3 tools/make_icons.py "path/to/Outfit[wght].ttf"

Both words use the same font size and the same baseline. The size is the largest one for which
the widest word (and its descender) stays inside the 66dp safe zone of the 108dp canvas, minus
a small margin (MARGIN dp on every side).
"""
import sys
from pathlib import Path

from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

WORDS = {"ic_launcher_foreground": "Binder", "ic_launcher_catalog_foreground": "Catalog"}
WEIGHT = 500
CANVAS = 108.0
SAFE = 66.0
MARGIN = 1.0          # breathing room inside the safe zone
INK = "#1C1C1C"
OUT = Path(__file__).resolve().parent.parent / "app/src/main/res/drawable"


def kerning(font):
    """Returns f(left, right) -> x advance adjustment from the font's GPOS 'kern' feature."""
    gpos = font["GPOS"].table
    idx = set()
    for fr in gpos.FeatureList.FeatureRecord:
        if fr.FeatureTag == "kern":
            idx.update(fr.Feature.LookupListIndex)
    subs = []
    for i in sorted(idx):
        for st in gpos.LookupList.Lookup[i].SubTable:
            if st.LookupType == 9:
                st = st.ExtSubTable
            if st.LookupType == 2:
                subs.append(st)

    def val(v):
        return getattr(v, "XAdvance", 0) or 0 if v is not None else 0

    def pair(l, r):
        for st in subs:
            cov = st.Coverage.glyphs
            if l not in cov:
                continue
            if st.Format == 1:
                for pvr in st.PairSet[cov.index(l)].PairValueRecord:
                    if pvr.SecondGlyph == r:
                        return val(pvr.Value1)
            else:
                c1 = st.ClassDef1.classDefs.get(l, 0)
                c2 = st.ClassDef2.classDefs.get(r, 0)
                rec = st.Class1Record[c1].Class2Record[c2]
                v = val(rec.Value1)
                if v:
                    return v
        return 0

    return pair


def layout(font, text, pair):
    cmap = font.getBestCmap()
    hmtx = font["hmtx"]
    names = [cmap[ord(c)] for c in text]
    x, placed = 0, []
    for i, n in enumerate(names):
        placed.append((n, x))
        x += hmtx[n][0]
        if i + 1 < len(names):
            x += pair(n, names[i + 1])
    return placed


def fmt(v):
    s = f"{v:.2f}".rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


def number_round(path):
    out, cur = [], ""
    for ch in path:
        if ch.isalpha():
            if cur:
                out.append(cur)
            out.append(ch)
            cur = ""
        elif ch in " ,":
            if cur:
                out.append(cur)
            cur = ""
        elif ch == "-" and cur:
            out.append(cur)
            cur = ch
        else:
            cur += ch
    if cur:
        out.append(cur)
    res = []
    for t in out:
        res.append(t if t.isalpha() else fmt(float(t)))
    s = ""
    for t in res:
        s += t if (t.isalpha() or not s or s[-1].isalpha() or t.startswith("-")) else " " + t
    return s


def main():
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    font = instancer.instantiateVariableFont(TTFont(sys.argv[1]), {"wght": WEIGHT})
    upm = font["head"].unitsPerEm
    glyphs = font.getGlyphSet()
    pair = kerning(font)
    os2 = font["OS/2"]
    cap = os2.sCapHeight

    # measure each word in font units
    measured = {}
    for key, word in WORDS.items():
        placed = layout(font, word, pair)
        xmin, xmax, ymin, ymax = 1e9, -1e9, 1e9, -1e9
        from fontTools.pens.boundsPen import BoundsPen
        for n, x in placed:
            bp = BoundsPen(glyphs)
            glyphs[n].draw(TransformPen(bp, (1, 0, 0, 1, x, 0)))
            if bp.bounds:
                xmin, ymin = min(xmin, bp.bounds[0]), min(ymin, bp.bounds[1])
                xmax, ymax = max(xmax, bp.bounds[2]), max(ymax, bp.bounds[3])
        measured[key] = (placed, xmin, xmax, ymin, ymax)

    room = SAFE - 2 * MARGIN
    # one scale for both words: the tightest of width and height limits
    scale = min(
        min(room / (m[2] - m[1]) for m in measured.values()),
        # ink above the baseline is the cap/ascender height; below is the descender; the block
        # is centred on the cap height, so the larger side decides
        min(room / (2 * max(m[4] - cap / 2, cap / 2 - m[3]) ) for m in measured.values()),
    )
    size = scale * upm
    print(f"font size {size:.2f}dp (scale {scale:.5f}), cap height {cap * scale:.2f}dp")

    for key, (placed, xmin, xmax, ymin, ymax) in measured.items():
        word = WORDS[key]
        width = (xmax - xmin) * scale
        left = (CANVAS - width) / 2 - xmin * scale
        base = CANVAS / 2 + cap * scale / 2        # baseline y, cap height centred
        pen = SVGPathPen(glyphs, ntos=lambda v: f"{v:.2f}")
        for n, x in placed:
            glyphs[n].draw(TransformPen(pen, (scale, 0, 0, -scale, left + x * scale, base)))
        d = number_round(pen.getCommands())
        top = base - ymax * scale
        bottom = base - ymin * scale
        lo, hi = (CANVAS - SAFE) / 2, (CANVAS + SAFE) / 2
        assert lo <= left + xmin * scale and left + xmax * scale <= hi, f"{word} too wide"
        assert lo <= top and bottom <= hi, f"{word} too tall"
        print(f"{word}: x {left + xmin * scale:.2f}..{left + xmax * scale:.2f}, y {top:.2f}..{bottom:.2f} (safe {lo}..{hi})")
        xml = (
            '<?xml version="1.0" encoding="utf-8"?>\n'
            f'<!-- "{word}" in Outfit Medium (500), turned into outlines by tools/make_icons.py. No text, no font needed. -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp" android:height="108dp"\n'
            '    android:viewportWidth="108" android:viewportHeight="108">\n'
            f'    <path android:fillColor="{INK}" android:pathData="{d}" />\n'
            '</vector>\n'
        )
        (OUT / f"{key}.xml").write_text(xml)


if __name__ == "__main__":
    main()
