#!/data/data/com.termux/files/usr/bin/python3
"""烤制 NotoSansCJK SC → mpl BakedFace JSON(无 matplotlib 依赖,直接读 TTC)"""
import json, sys
from fontTools.ttLib import TTFont
from fontTools.pens.recordingPen import RecordingPen

TTC = "/system/fonts/NotoSansCJK-Regular.ttc"
OUT = sys.argv[1] if len(sys.argv) > 1 else "/data/data/com.termux/files/home/.cache/jtest/noto-sans-cjk.json"

t = TTFont(TTC, fontNumber=2)  # SC Regular
order = t.getGlyphOrder()
gid = {n: i for i, n in enumerate(order)}
cmap = t.getBestCmap()

# 字符集:ASCII + CJK 标点 + 基本汉字区 + 全角 + 常用符号
ranges = [(0x20, 0x7E), (0x3000, 0x303F), (0x4E00, 0x9FA5), (0xFF00, 0xFFEF),
          (0x2013, 0x2026), (0x2190, 0x2193), (0x00B0, 0x00B1), (0x00B7, 0x00B7), (0x00D7, 0x00D7)]
chars = [c for c in (chr(c) for a, b in ranges for c in range(a, b + 1)) if ord(c) in cmap]
print(f"字符集: {len(chars)}")

gs = t.getGlyphSet()
glyphs, outlines = {}, {}
n = 0
for c in chars:
    name = cmap[ord(c)]
    g = gid[name]
    pen = RecordingPen()
    gs[name].draw(pen)
    contours, cur = [], None
    for op, cargs in pen.value:
        if op == "moveTo":
            cur = [int(cargs[0][0]), int(cargs[0][1]), 1]
        elif op == "lineTo":
            cur += [int(cargs[0][0]), int(cargs[0][1]), 1]
        elif op == "curveTo":
            (a2, b2, c2) = cargs
            cur += [int(a2[0]), int(a2[1]), 2, int(b2[0]), int(b2[1]), 2, int(c2[0]), int(c2[1]), 1]
        elif op in ("closePath", "endPath"):
            if len(cur) > 3 and cur[-3:-1] == cur[0:2] and cur[-1] == 1:
                cur = cur[:-3]
            contours.append(cur); cur = None
    xs = [cc[i] for cc in contours for i in range(0, len(cc), 3)]
    ys = [cc[i + 1] for cc in contours for i in range(0, len(cc), 3)]
    box = [min(xs), max(xs), min(ys), max(ys)] if xs else [0, 0, 0, 0]
    glyphs[str(g)] = [t["hmtx"][name][0], *box]
    outlines[str(g)] = contours
    n += 1
    if n % 2000 == 0:
        print(f"  {n} 字形烤制中...")

cover = []
for cp in sorted(cmap):
    if cover and cover[-1][1] == cp - 1:
        cover[-1][1] = cp
    else:
        cover.append([cp, cp])

if "OS/2" in t:
    typo = [t["OS/2"].sTypoAscender, -t["OS/2"].sTypoDescender, t["OS/2"].sTypoLineGap]
else:
    typo = [t["hhea"].ascent, -t["hhea"].descent, t["hhea"].lineGap]

data = dict(family="Noto Sans CJK SC", weight=400, upm=t["head"].unitsPerEm, typo=typo,
            cmap={c: gid[cmap[ord(c)]] for c in chars}, cover=cover, glyphs=glyphs,
            outlines=outlines, offsets={}, kern={}, gsub=dict(order=[], lookups={}, gclass={}))
body = json.dumps(data, ensure_ascii=False, separators=(",", ":"))
open(OUT, "w").write(body)
print(f"写出 {OUT}: {len(body)//1024} KB")
