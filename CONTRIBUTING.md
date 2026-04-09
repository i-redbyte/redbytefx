# Contributing

## Library Principles

- `redbytefx` is a library-first project. Public API should be intentionally small, predictable, and easy to evolve.
- Be strict about implementation quality. Always review code for unnecessary complexity, useless abstraction, memory retention risks, and performance churn.
- Never drift away from the core idea of the library. Discipline, quality, and user ergonomics take priority over novelty.
- Prefer a functional core and an imperative shell: keep the DSL, expressions, and compiler pure and compositional, while confining mutable runtime behavior to thin integration layers.
- Do not turn `redbytefx` into an academic FP framework. Functional style is welcome only when it makes shader authoring clearer, smaller, and more predictable for users.
- Every public API declaration must have KotlinDoc.
- When public API changes, the KotlinDoc for that API must be updated in the same change.
- Before the first public release, API cleanup is preferred over premature compatibility constraints.
- After the first public release, backward compatibility becomes a core library rule and must be treated as a top priority.
- Publication is intentionally deferred until the library surface, docs, tests, and sample app are all ready.

## API Rules

- Library modules use Kotlin explicit API mode.
- Prefer `internal` by default. Only expose symbols that are part of the intended authoring experience.
- User-facing API changes should usually update tests, sample code, and README examples together.
- Keep Compose integration thin and focused on runtime ergonomics, not on hiding the underlying effect model.
- Keep the `v0.1` core stdlib limited to essential shader building blocks: constructors, branching, component access, operators, and fundamental math/color helpers.
- Recipe-style helpers, visual presets, noise generators, and effect packs should stay out of `core` until they justify a separate module or a clearly stable extension layer.

## Quality Bar

- Public API should have clear naming and typed behavior.
- DSL additions should be validated by generated AGSL tests when possible.
- New user-facing features should be demonstrated in the sample app when they materially affect authoring workflow.
- Avoid carrying legacy or convenience API into the public surface unless it supports the long-term direction of the library.
