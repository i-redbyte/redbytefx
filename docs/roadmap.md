# redbytefx roadmap

This roadmap is intentionally pre-publication.

The library is still raw. The near-term goal is to make the authoring model, tooling, and docs genuinely strong before any artifact-publishing push starts.

## Current snapshot

Current level: `late v0.2 / early v0.3`

That split is important:

- the library surface is still finishing the `v0.2 Shader stdlib` milestone
- the sample app already implements a meaningful part of the `v0.3 Tooling` vision

## Application audit

The sample app is no longer a throwaway sandbox.

### What is already there

- `sample/model/Demo.kt` defines a real catalog: `26` demos across `5` sections with per-demo title, subtitle, focus, layer, animation flag, and snippet metadata
- `sample/ui/HomeScreen.kt` already provides search, filtering, grouped sections, and top-level metrics
- `sample/ui/DemoComponents.kt` already delivers the core tooling stack:
  - visual preview
  - live controls
  - DSL snippet
  - generated AGSL panel
  - per-demo focus text
  - previous/next demo navigation
- `sample/ui/DemoScreen.kt` routes the catalog into the concrete demo implementations cleanly
- `sample/app/RedByteFxSampleApp.kt` gives the app a stable shell with safe-area handling, animated navigation, and per-demo header state
- `sample/ui/CircuitDemo.kt` proves the stack can handle a more structured, data-driven showcase scene instead of only one-screen parameter toys

### What that means

The sample already demonstrates the core `v0.3` thesis:

`controls -> DSL snippet -> generated AGSL -> visual result`

So `v0.3` should not be planned as "start tooling from zero". It should be planned as "finish and sharpen the tooling that already exists".

## Version roadmap

## v0.2 Shader stdlib

### Goal

Build a small but very strong standard library of primitives plus the first wave of recipe-level helpers.

### Current level

Late `v0.2`.

### What is already done

- broad helper coverage across the main shader-authoring families
- compiler/runtime tests and AGSL goldens
- sample coverage that exercises the helper surface in real demos

### What still needs to happen

- curate the stdlib surface
- normalize naming and parameter conventions
- document canonical helper families better
- prune overlap and noisy helpers
- finish the "what belongs in core vs stdlib" story

## v0.3 Tooling

### Goal

Turn the sample into a strong adoption tool, where every important demo clearly shows:

- controls
- DSL snippet
- generated AGSL
- visual result

### Current level

Early `v0.3` already exists.

### What still needs to happen

- make the tooling presentation more consistent across all demos
- improve large-screen layouts so preview/code/control inspection is more efficient
- strengthen catalog metadata, tags, and discoverability
- consider copy/share actions for snippets and generated AGSL
- make debugging and comparison flows feel more intentional, not just "expanded code blocks"

## v0.4 Authoring UX

### Goal

Make writing RedByteFX feel teachable, debuggable, and approachable to someone coming from raw AGSL or Shadertoy.

### Main deliverables

- better compiler/runtime error messages
- cleaner naming and wording across the public surface
- documentation: `AGSL vs redbytefx`
- cookbook: "how to rewrite a shader from Shadertoy/AGSL into the DSL"
- guidance for common translation patterns:
  - coordinates
  - uniforms
  - helper extraction
  - masks
  - animation/time
  - compositing

## v0.5 Runtime quality

### Goal

Make the runtime and sample behavior boring in the best possible way: predictable, stable, and easy to trust.

### Main deliverables

- perf pass on runtime invalidation and sample rendering
- leak/lifecycle audit around controllers and demo navigation
- stronger smoke/screenshot coverage for key demos
- more confidence around generated AGSL stability and regression detection
- cleanup of runtime edge cases that feel acceptable in demos but not in a serious library

## v0.6 Library shaping

### Goal

Reach the point where the library is still pre-publication, but clearly converging toward a stable identity.

### Main deliverables

- final pass on public surface clarity
- stronger separation between canonical APIs and experimental helpers
- mature cookbook/examples layer
- clearer statement of supported patterns and current limitations
- confidence that the project has a stable mental model, not just a growing set of capabilities

## What is explicitly not part of this roadmap yet

- Maven publishing
- signing
- production artifact pipeline
- compatibility guarantees as a hard release contract

Those only make sense after the library, tooling, and authoring experience stop feeling raw.
