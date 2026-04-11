# redbytefx roadmap

This roadmap is intentionally pre-publication.

The library is still raw. The near-term goal is to make the authoring model, tooling, and docs genuinely strong before any artifact-publishing push starts.

## Current snapshot

Current level: `v0.2 closed / v0.3 Tooling closed / v0.4 Authoring UX first pass delivered`

That split is important:

- the library surface has now reached a curated-enough `v0.2 Shader stdlib` close-out point
- the sample app has completed the first **v0.3 Tooling** milestone ([v0.3-tooling.md](v0.3-tooling.md)): shared demo layout, catalog, copy/share on code panels, wide layouts

At the same time, some later-roadmap groundwork is already visible:

- `v0.4` is no longer only an idea; diagnostics, AGSL-vs-DSL docs, and cookbook rewrites now exist in first-pass form
- `v0.4` guidance now also includes a short runtime-authoring checklist that connects `core` authoring with `compose` binding/application
- `v0.5` has a real runtime audit note and reproducible emulator/physical-device measurement flow
- `v0.6` is no longer just backlog text; package docs, sample family maps, demo path guidance, and follow-up routing now start to shape a canonical teaching surface

That does **not** mean those later milestones are complete. It means the project already contains some realistic preparation work for them while the main active level shifts toward **runtime quality (`v0.5`)** and **library shaping (`v0.6`)**, with incremental sample polish as needed.

## Application audit

The sample app is no longer a throwaway sandbox.

### What is already there

- `sample/model/Demo.kt` defines a real catalog: `26` demos across `5` sections with per-demo title, subtitle, focus, layer, animation flag, and snippet metadata
- `sample/ui/HomeScreen.kt` already provides search, section jumps, starter routes, quick filtering, grouped sections, top-level metrics, and a canonical family map that mirrors the current `stdlib` teaching surface
- the catalog now also has an explicit canonical/start-here signal, so the sample can teach the recommended path instead of only listing everything equally
- `sample/ui/DemoComponents.kt` already delivers the core tooling stack:
  - visual preview
  - live controls
  - DSL snippet
  - generated AGSL panel
  - copy actions for DSL / AGSL inspection
  - per-demo focus text
  - per-demo focus tags
  - explicit debug checklist / comparison flow
  - previous/next demo navigation
  - related-demo recommendations from the current screen
- the current phone layout now keeps preview and live controls first, pushes the heavier inspection panels lower, and uses a more compact chip/text scale on narrow devices
- the current demo layout already scales into a more useful large-screen inspection flow instead of forcing the same narrow vertical stack everywhere
- `sample/ui/DemoScreen.kt` routes the catalog into the concrete demo implementations cleanly
- `sample/app/RedByteFxSampleApp.kt` gives the app a stable shell with safe-area handling, animated navigation, per-demo header state, and `START_DEMO` retargeting for an already running activity
- `sample/ui/CircuitDemo.kt` proves the stack can handle a more structured, data-driven showcase scene instead of only one-screen parameter toys

### What that means

The sample demonstrates the core `v0.3` thesis:

`controls -> DSL snippet -> generated AGSL -> visual result`

The first **v0.3** closure rounds that out with consistent **DemoLayout**, **COPY/SHARE** on code panels, and deliberate wide layouts ([v0.3-tooling.md](v0.3-tooling.md)). Further work here is incremental polish, not a blank-slate tooling project.

## Version roadmap

## v0.2 Shader stdlib

### Goal

Build a small but very strong standard library of primitives plus the first wave of recipe-level helpers.

### Current level

Closed enough to stop treating stdlib curation as the main front.

### What is already done

- broad helper coverage across the main shader-authoring families
- compiler/runtime tests and AGSL goldens, including representative stdlib snapshots
- sample coverage that exercises the helper surface in real demos

### What still happens after closure

- small selective surface cleanup can continue when it materially improves predictability
- broader shaping now belongs under `v0.6`, not under "keep v0.2 open"
- helper-surface expansion should no longer be the default answer to missing patterns

## v0.3 Tooling

### Goal

Turn the sample into a strong adoption tool, where every important demo clearly shows:

- controls
- DSL snippet
- generated AGSL
- visual result

### Current level

**Closed for the first complete tooling pass:** shared `DemoLayout`, catalog + inspection UX, wide breakpoint (≥ ~840dp) two-column preview/controls, **COPY + SHARE** on DSL/AGSL panels, compact **COMPARE CODE** dialog, and per-demo debug checklist. See [v0.3-tooling.md](v0.3-tooling.md).

### What can still evolve (without reopening the milestone)

- further polish of presentation on new form factors
- small catalog/discoverability tweaks as the demo list grows
- optional extras (e.g. export formats) only if they stay lightweight

## v0.4 Authoring UX

### Goal

Make writing RedByteFX feel teachable, debuggable, and approachable to someone coming from raw AGSL or Shadertoy.

### Main deliverables

- better compiler/runtime error messages
- cleaner naming and wording across the public surface
- documentation: `AGSL vs redbytefx`
- concise runtime authoring checklist for `compile -> inspect -> bind -> apply`
- cookbook: "how to rewrite a shader from Shadertoy/AGSL into the DSL"
- guidance for common translation patterns:
  - coordinates
  - uniforms
  - helper extraction
  - masks
  - animation/time
  - compositing

**Snapshot:** the first **end-to-end v0.4 authoring pass** (hub doc, expanded guides, cookbook porting checklist, cross-links, key KDoc) is in [v0.4-authoring-ux.md](v0.4-authoring-ux.md). Further work is incremental (more examples, deeper messages), not a reopening of the milestone from zero.

## v0.5 Runtime quality

### Goal

Make the runtime and sample behavior boring in the best possible way: predictable, stable, and easy to trust.

### Main deliverables

- perf pass on runtime invalidation and sample rendering
- leak/lifecycle audit around controllers and demo navigation
- stronger smoke/screenshot coverage for key demos
- more confidence around generated AGSL stability and regression detection
- cleanup of runtime edge cases that feel acceptable in demos but not in a serious library
- reproducible runtime measurement flow that can be run against `Radar` / `Circuit` on emulator first and physical devices next

**Snapshot:** first closure is documented in [v0.5-runtime-quality.md](v0.5-runtime-quality.md) with links to audit and measurement scenarios. Further work is breadth and follow-up when code changes.

## v0.6 Library shaping

### Goal

Reach the point where the library is still pre-publication, but clearly converging toward a stable identity.

### Current level

First **guidance + curation** milestone documented in [v0.6-library-shaping.md](v0.6-library-shaping.md). The public surface can still evolve; the mental model is no longer “only backlog text.”

### What is already visible

- package-level guidance now points authors to the canonical `core` and `stdlib` starter path first
- the sample home screen exposes a canonical family map instead of only listing demos
- demo inspection flow now shows `START HERE` / `CANONICAL` / `EXPLORATORY` path guidance directly inside the detail view
- follow-up recommendations now try to route exploratory demos back toward the curated path instead of only showing nearby neighbors

### What still needs to happen

- tighten the actual public surface so the same canonical vs exploratory split is obvious in naming, defaults, and helper selection
- keep aligning cookbook/examples with the same family map
- document limitations and supported patterns in one place without turning that into a publication contract

### Main deliverables

- final pass on public surface clarity
- stronger separation between canonical APIs and experimental helpers
- mature cookbook/examples layer
- clearer statement of supported patterns and current limitations
- confidence that the project has a stable mental model, not just a growing set of capabilities
- explicit criteria for what becomes part of the canonical teaching surface versus what remains exploratory

## What is explicitly not part of this roadmap yet

- Maven publishing
- signing
- production artifact pipeline
- compatibility guarantees as a hard release contract

Those only make sense after the library, tooling, and authoring experience stop feeling raw.
