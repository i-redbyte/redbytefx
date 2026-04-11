# Changelog

## [Unreleased]

### Added

- Milestone snapshot doc covering the current `v0.2-v0.6` state in one place.
- Package-level core guidance plus a clearer canonical family map across docs and sample.
- Demo-path guidance inside inspection flow and follow-up routing that points exploratory demos back toward the canonical surface.

### Changed

- Sample wording now frames exploratory demos as extensions of the canonical path rather than parallel alternatives.
- Authoring docs now explain sampling-space choices and AGSL helper extraction more concretely.
- Several stdlib helpers now use more predictable blend/distortion intensity semantics.

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
