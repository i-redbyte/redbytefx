# Changelog

All notable changes to the published Maven artifacts (`io.github.i-redbyte:redbytefx-*`) are listed here.

Русская версия: [changelog.ru.md](changelog.ru.md).

## 1.1.1 - 2026-10-08

### Shader DSL and math

- AGSL and GLSL emission now keeps local values in the correct scope and emits commands before expressions that use them. This fixes shaders with nested conditions and local variables; fragment command dependencies are also included in generated GLSL.
- Shared expression graphs are traversed once during compilation, and unused AGSL functions are omitted from generated source.
- Added shader `inverse` and `matrixCompMult` for 2x2, 3x3, and 4x4 matrices. The 3D module now also provides CPU-side `transpose`, `inverse`, `transformPoint`, and `transformDirection`.
- Uniform and storage block layouts validate their sizes more carefully and reuse packing buffers to reduce allocations.

### Runtime and rendering

- AGSL uniform defaults and float payloads are preserved. Runtime handles are validated, effect refresh can retry after a failure, and Compose listeners are released with their host.
- GLES and Compose handle failed uploads, sampler binds, frame passes, and EGL context changes more safely. Mesh replacement uses current index bounds, and frame rendering avoids repeated GL state changes.
- Reused scalar uniform caches and block upload buffers; coalesced Compose writes are now drained in linear time instead of repeatedly scanning the queue.

### Sample and checks

- Circuit avoids much of its per-pixel pulse work, Neon floor animates vertices on the GPU, and several sample scenes avoid temporary allocations. Physics Bubble drag updates are processed in order.
- `qualityCheck` now includes sample unit tests; a device test covers AGSL code emission with nested local values.

## 1.1.0 — 2026-10-06

### Platform and docs

- **AGSL minimum API is 33** (`RedByteFxApis.AGSL_MIN_SDK`), matching `RuntimeShader` / `RenderEffect.createRuntimeShaderEffect`.
- README, language reference, and error-code tables updated (English and Russian).

### Compiler and stdlib

- **`scale`**: negative scale factors keep their sign; near-zero values are clamped safely.
- **Reveal masks** (`horizontalReveal`, `verticalReveal`): progress 0 and 1 fully hide/show content.
- **Screen SDF** (`softFillScreen`, `strokeScreen`): guard `fwidth` when it collapses to zero.
- **Polar KDoc** clarifies angle direction in fragment UV space.
- **`choose(selector) { on(id) { … }; otherwise { … } }`**: nested portable ternary on a float id.
- **`cosinePalette`**: saturates RGB to `[0, 1]`.
- **`grain`**: folds time with `fract` before hashing.
- **`wrapLambert`**: Lambert with an ambient floor.
- **`resolution`**: legal on GLES as `uResolution`; Compose writes the view size. AGSL programs always bind `ShaderProgram.resolution` (the generated source already declared it). `uResolution` is reserved as an author uniform name.
- **GLES fragment-only**: omit `vertex { }` and the compiler injects a fullscreen `a_corner` triangle. That attribute keeps the `a_corner` spelling even if a user `fn` reuses the identifier. Unused fragment functions that read `resolution` still bind `uResolution`.
- **Stage-owned `fn`**: uniforms, `uniformBlock` members, and varyings referenced only in uncalled stage functions are still declared in that stage’s GLSL (all owned `fn` bodies are spelled).
- SDF extras: `sdHexagon`, `sdRhombus`, `sdEquilateralTriangle`, `sdCylinder`, `sdOctahedron`, `opRound`, `opOnion`, `sdfSmoothSubtract`, `sdfSmoothIntersect`, `rotate2d`.
- Lighting/color extras: `fresnel`, `phong`, `hueShift`, `filmicTonemap`, `hash22`, `voronoi`.
- **Premultiplied alpha**: `premultiply`, `unpremultiply`, and premultiplied `blendMultiply` / `blendScreen` / `blendOverlay`; `alphaMaskStraight` for straight RGB authoring. Nonlinear ops (`filmicTonemap`, `posterize`) keep premultiplied alpha.
- **`topLeftUv`**: one helper for AGSL top-left vs GLES bottom-left fragment origin.

### GLES and 3D

- **ES 3.0 link** no longer queries `GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS` (invalid enum 0x500 on goldfish/GFXSTREAM).
- **Detekt** formats with JetBrains official Kotlin style (`detekt-formatting`, ktlint `android: false`).
- **`litTexturedMesh`**: fragment output respects texture alpha; KDoc explains model/normal matrix updates.
- **`GlProgramRuntime.setLitModel`**: uploads model and matching `normalMatrix` together.
- **`strictErrors`**: also checks the driver after uniform/storage block writes and `generateMipmap2D`.
- Matrix uniform cache always copies caller arrays (avoids accidental mutation).
- **`GlCompute`**: releases GPU resources on dispose like `GlSurface`.
- **`GlFrame.draw(model, material)`**: one model matrix per draw (copied) and a per-draw material uniform.
- **`GlPipeline.cullFace`**: optional face culling; default stays off.
- Extra meshes skip GPU upload when vertex/index array references are unchanged.
- Scene matrices: `identity` / `IDENTITY`, `multiply`, `translation`, `rotationX`/`Y`/`Z`, `scale`.
- Scene meshes: `disc`, `extrudePolygon`, `tubeAlong`, `transform`, `merge`, `tagUv`.
- **`instanceModel()`**: `mat4` from instance attributes `a_model0`…`a_model3`.
- **`rayMarch`**: evaluate the scene once per active step; normalize direction once; validate `steps` / `epsilon` / `far`; skip inactive steps. **`sdOctahedron`**: correct edge and vertex distances.
- **Runtime hardening**: safer GL-thread task dispatch; release GPU units on failure; reject zero driver handles. **`GlFrame`**: refresh surface buffers after `replace`, use latest mesh bounds for recorded draws, release oversized draw/mesh pools.
- **Scene CPU meshes**: polygon winding and geometry allocation fixes.

### Tooling

- Optional **detekt pre-commit** hook (`githooks/`, `./gradlew installGitHooks`).

### Compose

- **`redbyteFx`**: runtime invalidation uses `invalidateDraw()` instead of `View.postInvalidateOnAnimation()`.
- GLES **`redbyteFx(controller)`**: fragment-only effect host with [screenMesh] and auto `uResolution`. `GlFrame`'s public constructor accepts pixel size; the present program also receives `setResolution`.
- KDoc uses **program** consistently (not “effect”) for `ShaderProgram` ownership.

### Sample app

- GLES **Lit mesh** demo (`litTexturedMesh`, `ortho`, `setLitModel`).
- GLES **Effect** demo: fragment-only `redbyteFx`, `resolution`, stdlib SDF (`sdHexagon`, `rotate2d`).
- Gallery **photo picker** on Planet with runtime permissions and system photo picker on API 33+.
- Additional API showcases: `remap`, `sdSegment`, `bindInt`, negative `scale`.
