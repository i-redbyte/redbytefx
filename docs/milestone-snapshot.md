# Milestone snapshot

Date: April 11, 2026

This is a short honest snapshot of where RedByteFX stands right now. It is not a release note and not a publication contract.

## Current level

Primary active level: `v0.2–v0.6 first-pass documentation milestones closed; ongoing work is incremental`

Framing:

- `v0.2 Shader stdlib` is closed as a first milestone; further stdlib work is selective shaping
- `v0.3` sample tooling, `v0.4` authoring docs, `v0.5` runtime baseline, and `v0.6` canonical guidance each have a **first-pass closure** documented in-repo
- the library remains pre-publication; incremental work continues without reopening those milestones from zero

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

Status: **first complete tooling milestone closed** (see [v0.3-tooling.md](v0.3-tooling.md))

What is already solid:

- searchable sample catalog with sections, filters, starter routes, canonical family map, and related-demo routing
- shared **DemoLayout**: preview, controls, metadata + inspection, prev/next, follow-ups
- DSL + generated AGSL panels with **COPY** and **SHARE**, plus compact **COMPARE CODE** dialog on phones
- wide layout from **~840dp**: deliberate two-column preview | controls on tablets/desktops
- per-demo debug checklist aligned with path signals

What can still evolve incrementally:

- presentation tweaks on new form factors; resist turning the sample into an IDE

### v0.4 Authoring UX

Status: **first comprehensive documentation + messaging pass delivered** (ongoing polish can continue as incremental work)

What is already solid:

- author-facing wording for same-effect uniform binding and **Hint** lines for common raw Kotlin mistakes
- `fn` body vs declared return type validation at compile time
- **Hub doc** [v0.4-authoring-ux.md](v0.4-authoring-ux.md) plus expanded [agsl-vs-redbytefx.md](agsl-vs-redbytefx.md) (including `sample` vs `sampleUv` and naming collisions)
- [cookbook-patterns.md](cookbook-patterns.md) as a maintained companion with a compact porting checklist and multiple end-to-end rewrites
- README / backlog / checklist cross-links for the authoring path

What is still open (by design or for later milestones):

- diagnostics remain **exception-shaped**, not a structured IDE-style API
- cookbook can always grow; deeper **v0.6** naming and canonical surface work still applies to stdlib

### v0.5 Runtime quality

Status: **first baseline + docs milestone closed** (see [v0.5-runtime-quality.md](v0.5-runtime-quality.md))

What is already solid:

- audit + measurement docs, reproducible `Radar` / `Circuit` path, physical-device snapshot in [runtime-audit-v0.5.md](runtime-audit-v0.5.md)
- runtime API semantics (`FxInstance` boolean setters, controller invalidation)

What can still grow incrementally:

- more device classes / resize passes when runtime code changes

### v0.6 Library shaping

Status: **first guidance milestone closed** (see [v0.6-library-shaping.md](v0.6-library-shaping.md))

What is already solid:

- canonical vs exploratory in stdlib KDoc, README family map, sample path signals and follow-ups

What can still grow incrementally:

- per-helper naming and defaults as usage grows; publication remains out of scope

## What is intentionally still out of scope

- `maven-publish`
- signing
- artifact pipeline
- release contract / compatibility promises

## Recommended next step

With `v0.3`–`v0.6` first passes documented, prefer **incremental** work: selective stdlib shaping, runtime follow-ups when code changes, and keeping CI (`qualityCheck`: tests + sample compile + Detekt) green.
