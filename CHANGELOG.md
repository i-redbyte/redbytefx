# Changelog

## [Unreleased]

- No tracked changes yet.

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
