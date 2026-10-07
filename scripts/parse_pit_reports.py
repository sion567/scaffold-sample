#!/usr/bin/env python3
"""Parse PIT HTML reports across all modules into a flat CSV of SURVIVED mutants."""
import csv
import html as html_mod
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

ROW_RE = re.compile(r"<tr>(.*?)</tr>", re.S)
ANCHOR_RE = re.compile(r"<a name='[^']*_(\d+)'/>")
MUT_RE = re.compile(r"(\d+)\.\s*(.*?)\s*:\s*(.*?)\s*&rarr;\s*(\w+)")
SRC_RE = re.compile(r"<pre><span[^>]*>(.*?)</span></pre>", re.S)

rows = []
for report in sorted(ROOT.glob("**/target/pit-reports")):
    # 模块名 = 去掉末尾 target/pit-reports 的相对路径（聚合目录下的子模块取两级，如 common/core）
    rel = report.relative_to(ROOT).parts
    module = "/".join(rel[:-2])
    for pkg_dir in sorted(p for p in report.iterdir() if p.is_dir()):
        for cls_file in sorted(pkg_dir.glob("*.java.html")):
            cls = f"{pkg_dir.name}.{cls_file.name[:-9]}".replace(".java", "")
            text = cls_file.read_text(encoding="utf-8")
            for m in ROW_RE.finditer(text):
                row = m.group(1)
                a = ANCHOR_RE.search(row)
                muts = MUT_RE.findall(row)
                if not muts:
                    continue
                line_no = int(a.group(1)) if a else 0
                sm = SRC_RE.search(row)
                src = html_mod.unescape(re.sub(r"<[^>]+>", "", sm.group(1))).strip() if sm else ""
                for _, method, desc, status in muts:
                    desc = html_mod.unescape(desc.strip())
                    if status == "SURVIVED" and "<" not in desc:
                        rows.append({
                            "module": module,
                            "class": cls,
                            "method": method.strip(),
                            "line": line_no,
                            "mutator": desc.strip(),
                            "status": status,
                            "source": src[:200],
                        })

out = ROOT / "target" / "pit-survived.csv"
out.parent.mkdir(exist_ok=True)
with out.open("w", newline="", encoding="utf-8") as f:
    w = csv.DictWriter(f, fieldnames=["module", "class", "method", "line", "mutator", "status", "source"])
    w.writeheader()
    w.writerows(rows)

# summary
from collections import Counter
by_mod = Counter(r["module"] for r in rows)
by_cls = Counter((r["module"], r["class"]) for r in rows)
print(f"TOTAL SURVIVED: {len(rows)}  -> {out}")
print("\nSurvived by module:")
for k, v in by_mod.most_common():
    print(f"  {v:5d}  {k}")
print("\nTop 40 classes by survived:")
for (m, c), v in by_cls.most_common(40):
    print(f"  {v:4d}  {m} :: {c}")
