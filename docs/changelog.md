# Changelog

All notable changes to the published Maven artifacts (`io.github.i-redbyte:redbytefx-*`) are listed here.

Русская версия: [changelog.ru.md](changelog.ru.md).

## 1.2.0 - unreleased

### Shader DSL and compilation

- A subexpression that one statement reads more than once is spelled once and reused. Layered helpers no longer grow exponentially: `fbm(domainWarp(uv))` went from 3.4 MB of GLSL to a few kilobytes, and deeply shared graphs no longer run the compiler out of memory.
- Generated identifiers are always valid ASCII. Cyrillic names are transliterated (`яркость` becomes `u_yarkost`), and names that clash with GLSL ES 3.20 or AGSL keywords, types, or builtins get a suffix. Prefixed names (`u_`, `a_`, `v_`) keep their 1.1.1 spelling, and `gl_`, `sk_`, and `__` never appear.
- A `repeat { }` whose body records no shader statement now fails with `ProgramCode.EmptyRepeat` and points to `local(...).set(...)` or `kotlin.repeat`, instead of silently emitting an empty loop.
- Looking up a uniform by an author name shared by several uniforms throws and lists the spelled names; spelled names still resolve directly.
- `Expr` caches its hash, so a graph with reused subexpressions hashes in linear time.
- Comparing two equal expression graphs built separately takes linear time instead of exponential.
- A function that reads a stage-only value only inside a local assignment can no longer be called from another stage: it fails with `ProgramCode.FunctionWrongStage` instead of producing a shader that fails to link. Each function is classified once, when it is defined.
- `ShaderTarget.Agsl` no longer requires API 33: compiling AGSL works on any API level. Running it still needs Android 13, and `AgslInstance` now carries `@RequiresApi(33)`. New `RedByteFxPlatform.isAgslSupported()` checks support without throwing.
- New `FrameClock` accumulates frame time in whole nanoseconds, skips paused gaps, and caps a single step; both `bindTime` helpers use it, so time no longer drifts after hours.

### GLES runtime

- Uniform setters bind their program before every write, and `drawRange` makes its own program current, so `A.use(); B.set(...); A.drawRange(...)` draws with `A`. The shared `GlTextureUnits` also tracks the bound framebuffer, which color targets now restore correctly.
- Deleting a uniform or storage block buffer no longer clears a binding point another program may be using.
- `glGetError` draining stops after 16 reads, so a lost robust context cannot hang the GL thread.
- Uniform and storage block updates, the native upload scratch, and `setLitModel` no longer allocate on steady updates.

### Compose hosts

- An exception on the GL thread (a queued write, a `runOnGl` block, `onFrame`, or a non-link setup error) no longer crashes the app. Rendering stops, the error is published as `GlController.failure`, and the new `failureOverlay` parameter of `GlSurface` and `GlCompute` shows it. Matrix and block arrays are validated when `set` is called.
- GL views keep their EGL context across pauses and start paused unless the host lifecycle is resumed. `GlFrame.seconds` keeps counting across a new context without jumping.
- A new `mesh` passed to `GlSurface` is uploaded on the existing context instead of recreating the view.
- A recorded draw that no longer fits a mesh shrunk from another thread is skipped instead of failing.
- `GlFrame.replace(vertices)` and `GlMesh.replace(vertices)` check kept indices against their cached highest index instead of scanning them on every call.
- A `GlController` no longer keeps a released GL view, and its `Context`, reachable until the next view attaches.
- `GlCompute` lays out at least 1 dp with a transparent surface, so a zero-size host still links.
- `GlController` writes allocate nothing and never hold its lock during a GL call; the AGSL modifier invalidates through one path; the scene renderer avoids per-frame allocations and per-draw state calls.
- The 1.1.1 JVM signatures of `GlSurface` and `GlCompute` are kept, so binaries compiled against 1.1.1 keep working.
- `redbytefx-gl-compose` exposes `redbytefx-3d` as an API dependency, because `setLitModel` takes a `LitMesh`.

### Standard library

- Alpha guards in medium precision use 1/1024 instead of 1e-6, which half floats flush to zero, and `polarAngle01` is defined at its center.
- `sdfNormal` samples the scene four times (tetrahedral differences) instead of six, so the scene appears in the shader two fewer times. Normals can differ from 1.1.1 in the last bits.

### Sample and checks

- The sample runs on Android 7 to 9 (the window theme had no parent below API 29) and closes the AGSL examples below Android 13 instead of crashing.
- `qualityCheck` now also runs Android Lint and checks the public API against committed `api/*.api` dumps.

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
