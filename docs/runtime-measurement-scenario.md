# Runtime measurement scenario

This document is a **repeatable checklist** for measuring RedByteFX runtime behavior on a device or emulator. For audit notes, baseline numbers, and interpretation, see [runtime-audit-v0.5.md](runtime-audit-v0.5.md).

## Goals

- Compare **frame pacing** and **jank** across demos under the same conditions.
- Capture **memory** (`meminfo`) alongside **gfxinfo** so regressions in GPU allocations are visible.
- Keep runs comparable: same duration, same launch path, same foreground state.

## Prerequisites

- `:sample` installed: `./gradlew :sample:installDebug`
- Optional: `scripts/measure-sample-runtime.sh` (see audit doc for `DEVICE=...` and demo names)

## Steps

1. **Pick a target** — fixed demo name (`Wave`, `Composite`, `Radar`, `Circuit`, or newer demos as they join the confidence set).
2. **Launch the demo** using the debug extra `ru.redbyte.redbytefx.sample.extra.START_DEMO` (or the measure script) so navigation is not part of the noise.
3. **Reset counters** — `adb shell dumpsys gfxinfo ru.redbyte.redbytefx.sample reset` (or follow your shell script if it wraps this).
4. **Run foreground** — keep the sample in the foreground, device unlocked, for a fixed window (e.g. **8s** as in the audit pass).
5. **Collect**:
   - `adb shell dumpsys gfxinfo ru.redbyte.redbytefx.sample`
   - `adb shell dumpsys meminfo ru.redbyte.redbytefx.sample`
6. **Record** — date, device model, Android version, demo id, and attach raw output or a path under `build/device-measurements/<device-id>/` if you use that layout.

## What to watch in app code (invalidation path)

When correlating measurements with code:

- **`FxInstance` setters** return `Boolean`: `true` means the value changed and the implementation may refresh **`RenderEffect`**; redundant writes should return `false` and avoid extra work.
- **`FxController`** uses those return values to decide host invalidation; duplicate uniform caches should not be needed in controller code.
- After **real** uniform or resolution updates, expect a **`RenderEffect`** refresh on the current platform path; see KDoc on **`FxInstance`** and the audit section on refresh cost.

## Emulator vs physical device

- Use the emulator for **regression** and script **reproducibility**; treat jank percentages as noisy (see audit ranges).
- Use at least one **physical device** class for decisions about shipping behavior; numbers in [runtime-audit-v0.5.md](runtime-audit-v0.5.md) illustrate the gap.
