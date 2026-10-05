# Changelog

All notable changes to the published Maven artifacts (`io.github.i-redbyte:redbytefx-*`) are listed here.

Русская версия: [changelog.ru.md](changelog.ru.md).

## 1.1.0 — 2026-04-04

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
- SDF extras: `sdHexagon`, `sdRhombus`, `sdEquilateralTriangle`, `sdCylinder`, `sdOctahedron`, `opRound`, `opOnion`, `sdfSmoothSubtract`, `sdfSmoothIntersect`, `rotate2d`.
- Lighting/color extras: `fresnel`, `phong`, `hueShift`, `filmicTonemap`, `hash22`, `voronoi`.

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

### Compose

- **`redbyteFx`**: runtime invalidation uses `invalidateDraw()` instead of `View.postInvalidateOnAnimation()`.
- GLES **`redbyteFx(controller)`**: fragment-only effect host with [screenMesh] and auto `uResolution`. `GlFrame`'s public constructor accepts pixel size; the present program also receives `setResolution`.
- KDoc uses **program** consistently (not “effect”) for `ShaderProgram` ownership.

### Sample app

- GLES **Lit mesh** demo (`litTexturedMesh`, `ortho`, `setLitModel`).
- GLES **Effect** demo: fragment-only `redbyteFx`, `resolution`, stdlib SDF (`sdHexagon`, `rotate2d`).
- Gallery **photo picker** on Planet with runtime permissions and system photo picker on API 33+.
- Additional API showcases: `remap`, `sdSegment`, `bindInt`, negative `scale`.
