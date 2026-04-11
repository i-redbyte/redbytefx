# Runtime audit (v0.5 prep)

This note records the current runtime audit state before any deep optimization work starts.

It is intentionally practical: the goal is to capture what the sample already proves, what still
needs measurement, and which risks are platform-driven rather than obviously self-inflicted.

## Current baseline

- `FxController` owns one `FxInstance` and caches scalar/vector uniform values by param identity.
- Stable values do not re-send uniforms or increment `runtimeInvalidationTick`.
- Compose-side cache confidence now covers all uniform families plus `setResolution(...)` vs
  `syncResolution(...)`.
- Resolution has two paths:
  - `setResolution(...)` for explicit updates that should invalidate the host
  - `syncResolution(...)` for draw-time size sync without creating an extra invalidation loop
- `FxInstanceImpl` still recreates `RenderEffect` after real runtime updates because the current
  platform path does not reliably observe later `RuntimeShader` uniform changes.

## Representative demo audit

### Wave

- Effect setup is wrapped in `remember { ... }`.
- `rememberFxController(setup.effect)` is stable for the life of the demo.
- Runtime activity is simple and intentional:
  - two scalar binds
  - no time binding
  - no selection state or large shader-side branching

Conclusion: good baseline demo for coordinate/runtime sanity; no obvious controller churn.

### Composite

- Effect setup is remembered once.
- Controller lifetime is stable.
- Runtime activity is moderate but clean:
  - three scalar binds
  - no time loop
  - stdlib-heavy mask/compositing path in shader source

Conclusion: good representative demo for static stdlib-heavy rendering; no obvious lifetime risk.

### Radar

- Effect setup is remembered once.
- Controller lifetime is stable.
- Runtime activity is more realistic:
  - one time bind
  - three scalar binds
  - polar helpers and multiple layered masks in generated AGSL

Conclusion: good representative animated demo for checking redraw cadence and runtime update cost.

### Circuit

- Board spec is stable and reused.
- Effect setup is remembered once despite the heavier shader graph.
- Controller lifetime is stable.
- Runtime activity stays narrower than the visual complexity suggests:
  - one time bind
  - one route-selection bind
  - one amount bind
  - selection changes stay in uniform space instead of rebuilding the effect

Conclusion: currently the strongest sample for “heavier scene, still stable controller/effect
ownership”. No obvious duplicate-controller or effect recreation bug was found in this flow.

## What the audit says today

- The controller layer already looks intentionally conservative.
- The sample demos audited here do not appear to recreate compiled effects on ordinary UI state
  changes.
- Representative AGSL smoke coverage now exists for `Wave`, `Composite`, `Radar`, and a compact
  `Circuit`-style routing flow, which gives a low-cost regression signal inside `qualityCheck`.
- The main open runtime-quality question is still the cost/acceptability of the current
  `RenderEffect` refresh strategy after real runtime updates.

## Runtime measurement baseline

Method:

- install `:sample` with `./gradlew :sample:installDebug`
- launch a specific demo directly with the debug extra
- reset `gfxinfo`
- let the demo run for `8s`
- collect `adb shell dumpsys gfxinfo ru.redbyte.redbytefx.sample`
- collect `adb shell dumpsys meminfo ru.redbyte.redbytefx.sample`

Repro entry points:

- launch extra key: `ru.redbyte.redbytefx.sample.extra.START_DEMO`
- helper script: `scripts/measure-sample-runtime.sh`
- physical-device override: `DEVICE=<adb-serial> bash scripts/measure-sample-runtime.sh Radar Circuit`

Current emulator attached-target pass:

- date: `2026-04-11`
- device: `emulator-5554`
- model: `sdk_gphone64_x86_64`
- Android: `16`
- attached targets during this pass: `emulator-5554` only

Radar:

- total frames: `258`
- janky frames: `158` (`61.24%`)
- frame percentiles: `50th 69ms`, `90th 117ms`, `95th 150ms`, `99th 600ms`
- GPU percentiles: `50th 14ms`, `90th 22ms`, `95th 24ms`, `99th 4950ms`
- missed vsync: `114`
- slow UI thread: `158`
- slow issue draw commands: `158`
- total GPU memory usage: `6166672 bytes` (`5.88 MB`)
- mem summary: `TOTAL PSS 83650 KB`, `TOTAL RSS 177512 KB`, `swap 234 KB`

Circuit:

- total frames: `346`
- janky frames: `103` (`29.77%`)
- frame percentiles: `50th 48ms`, `90th 73ms`, `95th 117ms`, `99th 500ms`
- GPU percentiles: `50th 15ms`, `90th 24ms`, `95th 4950ms`, `99th 4950ms`
- missed vsync: `50`
- slow UI thread: `103`
- slow issue draw commands: `103`
- total GPU memory usage: `5751744 bytes` (`5.49 MB`)
- mem summary: `TOTAL PSS 83729 KB`, `TOTAL RSS 177408 KB`, `swap 234 KB`

Interpretation:

- This pass is still emulator-only, not a physical-device verdict.
- The measurement path is now device-ready, but the current workspace did not have a phone/tablet
  attached during this audit.
- Even on the emulator, the current runtime path is clearly not “free”; `Radar` remains the more
  suspicious animated case and deserves first physical-device measurement.
- `Circuit` still measures better than `Radar` despite its larger visual setup, which keeps
  attention on animation/update patterns plus the `RenderEffect` refresh path instead of scene
  size alone.
- Repeated emulator passes on the same day varied noticeably:
  - `Radar` jank landed roughly in the `38-61%` range
  - `Circuit` jank landed roughly in the `26-45%` range
  This makes the emulator useful for regressions and reproducibility, but too noisy for final
  runtime decisions.

## Physical-device baseline

Method:

- use the same launch extra and measurement script as the emulator pass
- keep the device unlocked with the sample in the foreground; some OEM builds return `Failure while dumping the app` when `gfxinfo` is requested while the app is backgrounded or keyguard is showing
- save reports under a device-specific directory inside `build/device-measurements/`

Current recorded pass:

- date: `2026-04-11`
- device: `R5CT10NF2ZP`
- model: `SM-G991B`
- Android: `15`
- reports: `build/device-measurements/r5ct10nf2zp/`

Radar:

- total frames: `954`
- janky frames: `9` (`0.94%`)
- frame percentiles: `50th 14ms`, `90th 16ms`, `95th 17ms`, `99th 25ms`
- GPU percentiles: `50th 8ms`, `90th 9ms`, `95th 10ms`, `99th 16ms`
- missed vsync: `0`
- slow UI thread: `7`
- slow issue draw commands: `9`
- total GPU memory usage: `13919552 bytes` (`13.27 MB`)
- mem summary: `TOTAL PSS 165329 KB`, `TOTAL RSS 287578 KB`, `swap 286 KB`

Circuit:

- total frames: `954`
- janky frames: `6` (`0.63%`)
- frame percentiles: `50th 13ms`, `90th 19ms`, `95th 22ms`, `99th 25ms`
- GPU percentiles: `50th 7ms`, `90th 14ms`, `95th 16ms`, `99th 18ms`
- missed vsync: `1`
- slow UI thread: `5`
- slow issue draw commands: `3`
- total GPU memory usage: `4997568 bytes` (`4.77 MB`)
- mem summary: `TOTAL PSS 154607 KB`, `TOTAL RSS 276750 KB`, `swap 287 KB`

Interpretation:

- This is the first trustworthy physical-device baseline for the current runtime path.
- On this Samsung pass, both `Radar` and `Circuit` look healthy enough that the current
  `RenderEffect` refresh strategy does not read as an urgent blocker for the project’s present
  scope.
- The physical result is much better than the emulator baseline, which confirms that emulator
  jank was directionally useful but far too pessimistic for runtime decisions.
- `Radar` still uses more GPU memory than `Circuit` in this pass, which keeps it useful as the
  “animated stress” member of the confidence quartet even though frame pacing is already good.
- Some counters are still OEM/platform-shaped rather than immediately actionable:
  - legacy jank remains much higher than modern deadline-based jank
  - `Number High input latency` is very large even during a passive scripted run
  Treat those as secondary signals unless they line up with visible problems or deadline misses.

## What still needs real measurement

- At least one more physical-device class if runtime behavior changes significantly:
  - a lower/mid-tier phone, or
  - a larger-screen/tablet configuration
- Resize and large-screen behavior through `drawWithCache -> syncResolution(size)`.
- Screenshot/smoke confidence for a small fixed set of demos instead of relying only on visual
  spot-checking.

## Recommended next pass

- Keep the current runtime architecture unless a new device pass shows a concrete problem.
- Use `Wave`, `Composite`, `Radar`, and `Circuit` as the fixed runtime confidence quartet.
- If a runtime issue appears, first determine whether it comes from:
  - controller invalidation behavior
  - `RenderEffect` refresh cost
  - sample-specific visual complexity
  - platform/runtime shader limitations
