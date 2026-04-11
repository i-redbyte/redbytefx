# Milestone snapshot

Date: April 11, 2026

This is a short honest snapshot of where RedByteFX stands right now. It is not a release note and not a publication contract.

## Current level

Primary active level: `v0.2 closed / early v0.3`

That is still the most important framing:

- the library surface has now closed the `v0.2 Shader stdlib` milestone well enough to stop front-loading stdlib curation work
- the sample/app/tooling work is already meaningfully inside `v0.3` and should become the main active focus
- `v0.4-v0.6` now have real groundwork in docs, sample guidance, and runtime audit material, but they are still backlog/progression work, not “done later milestones”

## Version snapshot

### v0.2 Shader stdlib

Status: effectively closed as a curated first stdlib milestone

What is already solid:

- `core` vs `stdlib` split is documented more clearly
- canonical helper families are now visible in package docs, README, sample metadata, and cookbook/examples
- exploratory helpers are still available, but they are framed more honestly as secondary/style-oriented surface
- representative compiler/golden/smoke coverage is in place
- blend/compositing-style intensity semantics are more consistent and predictable

What is still open after closure:

- some helper-surface width/noise can still improve, but that is now better treated as ongoing shaping than as unfinished `v0.2`
- future surface work should favor consistency and teaching quality over adding more helpers

### v0.3 Tooling

Status: real and already useful

What is already solid:

- searchable sample catalog with sections, filters, starter routes, canonical family map, and related-demo routing
- preview, controls, DSL snippet, generated AGSL, copy flow, and debug/comparison guidance
- compact-phone layout is substantially better on real hardware
- runtime measurement flow exists and was run on emulator plus a physical Samsung device

What is still open:

- there is still room to polish presentation consistency and a few remaining wording/inspection details
- sample is a strong adoption demo now, but should keep resisting IDE-like sprawl

### v0.4 Authoring UX

Status: started, no longer just an idea

What is already solid:

- better author-facing wording around same-effect uniform binding
- better unsupported-argument / bad-literal guidance
- `AGSL vs redbytefx` doc exists and is materially useful
- cookbook now includes coordinate-space, `sample(...)` vs `sampleUv(...)`, compositing, polar, and `fn(...)` extraction rewrites

What is still open:

- diagnostics are still exception-shaped, not a richer structured system
- cookbook/examples are useful, but still small compared to the long-term authoring goal

### v0.5 Runtime quality

Status: grounded backlog with first real evidence

What is already solid:

- controller/runtime audit exists
- representative AGSL smoke coverage exists
- `Radar` / `Circuit` measurement flow is reproducible
- physical-device measurements show the current runtime path looks healthy enough on real hardware

What is still open:

- runtime confidence still needs more breadth than one physical-device baseline
- lifecycle/resize/screenshot confidence is not yet “boring enough” everywhere

### v0.6 Library shaping

Status: started in guidance, not finished in surface design

What is already solid:

- package docs point to canonical starter paths first
- sample home screen exposes canonical families instead of only listing demos
- demo inspection flow explicitly labels `START HERE`, `CANONICAL`, `FOUNDATION`, and `EXPLORATORY`
- follow-up demo routing and demo wording now reinforce the canonical path instead of treating every helper equally

What is still open:

- the actual public helper surface still needs more shaping, not just better labels
- canonical vs exploratory is a documented/guided split today, not a final mature taxonomy

## What is intentionally still out of scope

- `maven-publish`
- signing
- artifact pipeline
- release contract / compatibility promises

## Recommended next step

Keep the main focus where it already belongs:

1. shift the main focus to `v0.3` and keep strengthening the sample as an adoption tool
2. treat further stdlib work as selective shaping, not renewed helper-surface expansion
3. keep `v0.4-v0.6` honest as real groundwork and shaping work while the library remains raw
