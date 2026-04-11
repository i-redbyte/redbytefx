# Changelog

## [Unreleased]

### Added

- **`fn` diagnostics:** compile-time validation that the function body expression matches the declared return type (`FxValueType`), with an error message that names expected AGSL type and actual expression kind.
- **`:sample` → Aurora** hero demo: iridescent rim + rotating `angularSweep`, `cosinePalette`, and `chromaticOffset`, with **PATH 00** on the home screen for quick access.
- **`:sample` → Liquid Glass** demo: stylized glass via `domainWarp` + `sampleUv`, Fresnel-style shell + `rimLight`, manual RGB channel offsets on warped UV for edge chroma, and `blendScreen` ice tint (documented as single-pass; not a multi-tap frosted blur).
- Public **`sameFloatUniformValue(Float, Float)`** for the same bitwise float equality used by runtime uniform updates.
- **`FxUniformValueTest`** and **`FxRuntimeStateTest.setFloatReturnsFalseWhenValueUnchanged`** covering uniform deduplication semantics.
- Milestone snapshot doc covering the current `v0.2-v0.6` state in one place.
- Package-level core guidance plus a clearer canonical family map across docs and sample.
- Demo-path guidance inside inspection flow and follow-up routing that points exploratory demos back toward the canonical surface.

### Changed

- **`FxInstance` setters** (`setFloat`, `setFloat2`, `setFloat3`, `setFloat4`, **`setResolution`**) now return **`Boolean`**: `true` when a new value was written and the backing implementation may refresh **`RenderEffect`**; `false` when the value was unchanged. Custom `FxInstance` implementations must be updated.
- **`FxController`** no longer mirrors uniform caches; it relies on **`FxInstance`** return values to decide when to invalidate the host view.
- **`FxDslComponents`**: shared internal **`floatSwizzle`** helpers for vector/color channel accessors.
- **`RESERVED_USER_FUNCTION_NAMES`** expanded with additional common AGSL/GLSL keywords and builtins.
- Sample wording now frames exploratory demos as extensions of the canonical path rather than parallel alternatives.
- Authoring docs now explain sampling-space choices and AGSL helper extraction more concretely.
- Several stdlib helpers now use more predictable blend/distortion intensity semantics.

### Documentation

- **`RedByteFxStdlib`**: explicit **Canonical** vs **Exploratory** KDoc sections for stdlib discovery.
- **`docs/runtime-measurement-scenario.md`**: repeatable gfxinfo/meminfo measurement checklist and notes on **`FxInstance`** invalidation vs redundant writes; pairs with **`docs/runtime-audit-v0.5.md`** for baselines.
- **`FxInstance`**: threading guidance (UI thread / **`RuntimeShader`**), performance note on **`RenderEffect`** refresh vs redundant writes.

## [v0.2-demo] - 2026-04-11

### Added

- Searchable `:sample` catalog with safer app chrome and better demo discovery.
- A separate data-driven `Circuit` demo built around nodes, routes, and animated signal flow.
- Root `qualityCheck` task and CI workflow for the v0.2 quality gate.
- Release documentation for the first public demo milestone.

### Changed

- README now reflects the actual `late v0.2-beta` / `v0.2-demo` state instead of treating `stdlib` as future work.
- Public API docs for `FxEffect`, `FxParam`, `rememberFxController(...)`, and `Modifier.redbyteFx(...)` are clearer about lifecycle and usage.
- Demo release positioning is now explicit: polished public demo first, publishing sprint later.

### Fixed

- Restored AGSL golden snapshots so `:redbytefx-core` tests can validate compiler output again.
- Marked the local golden dump helper as utility-only so it no longer pollutes the normal test suite.
- Closed several sample-level UX and visual issues during the v0.2 polish pass, including search, system bar insets, and `Circuit` stabilization.
