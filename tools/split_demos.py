#!/usr/bin/env python3
"""Split Demos.kt into package demos/*.kt with per-file import filtering."""

from __future__ import annotations

import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
SRC = ROOT / "sample/src/main/java/ru/redbyte/redbytefx/sample/ui/Demos.kt"
OUT = ROOT / "sample/src/main/java/ru/redbyte/redbytefx/sample/ui/demos"

# Line numbers of `fun DemoXxx` in Demos.kt (1-based)
_FUN_LINES = [
    262,
    305,
    402,
    441,
    475,
    509,
    558,
    643,
    723,
    764,
    854,
    923,
    992,
    1057,
    1148,
    1237,
    1331,
    1419,
    1499,
    1602,
    1697,
    1820,
    1928,
    2041,
    2149,
    2272,
    2398,
]

_NAMES = [
    "DemoFlip",
    "DemoMirror",
    "DemoRotate",
    "DemoScale",
    "DemoOffset",
    "DemoWave",
    "DemoPulse",
    "DemoSignal",
    "DemoPosterize",
    "DemoFilm",
    "DemoGrade",
    "DemoWarp",
    "DemoPrism",
    "DemoSpotlight",
    "DemoBeacon",
    "DemoComposite",
    "DemoFrame",
    "DemoCorner",
    "DemoReveal",
    "DemoSweep",
    "DemoRadar",
    "DemoHalo",
    "DemoAurora",
    "DemoLiquidGlass",
    "DemoSigil",
    "DemoGlitch",
    "DemoDuotone",
]

_SETUP: list[tuple[str, list[tuple[int, int]] | None]] = [
    ("DemoMirror", [(79, 88)]),
    ("DemoWave", [(89, 94)]),
    ("DemoPulse", [(95, 101)]),
    ("DemoSignal", [(102, 108)]),
    ("DemoRadar", [(109, 116)]),
    ("DemoHalo", [(117, 124)]),
    ("DemoAurora", [(125, 133)]),
    ("DemoLiquidGlass", [(134, 143)]),
    ("DemoSigil", [(144, 150)]),
    ("DemoDuotone", [(151, 156)]),
    ("DemoPosterize", [(157, 162)]),
    ("DemoFilm", [(163, 170)]),
    ("DemoGrade", [(171, 177)]),
    ("DemoWarp", [(178, 185)]),
    ("DemoPrism", [(186, 192)]),
    ("DemoSpotlight", [(193, 199)]),
    ("DemoBeacon", [(200, 207)]),
    ("DemoComposite", [(208, 214)]),
    ("DemoFrame", [(215, 222)]),
    ("DemoCorner", [(223, 230)]),
    ("DemoReveal", [(232, 238), (256, 256)]),
    ("DemoSweep", [(240, 246)]),
    ("DemoGlitch", [(248, 254)]),
]

# `by` delegates need getValue/setValue; the names do not appear as identifiers in source text.
DELEGATE_IMPORTS: list[str] = [
    "import androidx.compose.runtime.getValue",
    "import androidx.compose.runtime.setValue",
]

SAMPLE_UI_IMPORTS: dict[str, str] = {
    "DemoLayout": "import ru.redbyte.redbytefx.sample.ui.DemoLayout",
    "DemoPreviewStage": "import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage",
    "DemoLiquidGlassPreviewStage": (
        "import ru.redbyte.redbytefx.sample.ui.DemoLiquidGlassPreviewStage"
    ),
    "SliderRow": "import ru.redbyte.redbytefx.sample.ui.SliderRow",
    "SwitchRow": "import ru.redbyte.redbytefx.sample.ui.SwitchRow",
    "RadioRow": "import ru.redbyte.redbytefx.sample.ui.RadioRow",
}


def _import_simple_matches(simple: str, body: str) -> bool:
    if simple == "bindFloat":
        return bool(re.search(r"\bbindFloat\b(?!\d)", body))
    return bool(re.search(r"\b" + re.escape(simple) + r"\b", body))


def filter_imports(import_lines: list[str], body: str) -> list[str]:
    kept: list[str] = []
    for line in import_lines:
        s = line.strip()
        if not s.startswith("import "):
            continue
        rest = s[7:].strip()
        if rest.endswith(".*"):
            kept.append(line)
            continue
        simple = rest.split(".")[-1]
        if _import_simple_matches(simple, body):
            kept.append(line)
    return kept


def sample_ui_import_lines(body: str) -> list[str]:
    lines_out: list[str] = []
    for symbol, imp in sorted(SAMPLE_UI_IMPORTS.items(), key=lambda x: x[1]):
        if _import_simple_matches(symbol, body):
            lines_out.append(imp)
    return lines_out


def _ensure_delegate_imports(body: str, lines: list[str]) -> None:
    if " by " not in body:
        return
    for imp in DELEGATE_IMPORTS:
        if imp not in lines:
            lines.insert(0, imp)


def main() -> None:
    all_lines = SRC.read_text().splitlines()
    # Original Demos.kt: lines 3–77 (imports only)
    import_lines = all_lines[2:78]

    OUT.mkdir(parents=True, exist_ok=True)

    shared = """package ru.redbyte.redbytefx.sample.ui.demos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.redbyte.redbytefx.FxEffect

@Composable
internal fun rememberGeneratedAgsl(effect: FxEffect): String =
    remember(effect) { effect.agslSource() }
"""
    (OUT / "DemoShared.kt").write_text(shared + "\n")

    setup_map = dict(_SETUP)

    for i, name in enumerate(_NAMES):
        start = _FUN_LINES[i] - 1
        if i + 1 < len(_FUN_LINES):
            end = _FUN_LINES[i + 1] - 3
        else:
            end = len(all_lines)

        parts_body: list[str] = []
        setup = setup_map.get(name)
        if setup:
            for a, b in setup:
                parts_body.append("\n".join(all_lines[a - 1 : b]))
        parts_body.append("\n".join(all_lines[start - 1 : end]))
        body = "\n\n".join(parts_body) + "\n"

        filtered = filter_imports(import_lines, body)
        _ensure_delegate_imports(body, filtered)
        ui_extra = sample_ui_import_lines(body)

        header = (
            "package ru.redbyte.redbytefx.sample.ui.demos\n\n"
            + "\n".join(filtered)
            + ("\n\n" + "\n".join(ui_extra) if ui_extra else "")
            + "\n"
        )
        (OUT / f"{name}.kt").write_text(header + "\n" + body)

    SRC.unlink()


if __name__ == "__main__":
    main()
