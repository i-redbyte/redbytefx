# Changelog

All notable changes to the published Maven artifacts (`io.github.i-redbyte:redbytefx-*`) are listed here.

## 1.1.0 — 2026-04-04

### Platform and docs

- **AGSL minimum API is 33** (`RedByteFxApis.AGSL_MIN_SDK`), matching `RuntimeShader` / `RenderEffect.createRuntimeShaderEffect`.
- README, language reference, and error-code tables updated (English and Russian).

### Compiler and stdlib

- **`scale`**: negative scale factors keep their sign; near-zero values are clamped safely.
- **Reveal masks** (`horizontalReveal`, `verticalReveal`): progress 0 and 1 fully hide/show content.
- **Screen SDF** (`softFillScreen`, `strokeScreen`): guard `fwidth` when it collapses to zero.
- **Polar KDoc** clarifies angle direction in fragment UV space.

### GLES and 3D

- **`litTexturedMesh`**: fragment output respects texture alpha; KDoc explains model/normal matrix updates.
- **`GlProgramRuntime.setLitModel`**: uploads model and matching `normalMatrix` together.
- **`strictErrors`**: also checks the driver after uniform/storage block writes and `generateMipmap2D`.
- Matrix uniform cache always copies caller arrays (avoids accidental mutation).
- **`GlCompute`**: releases GPU resources on dispose like `GlSurface`.

### Compose

- **`redbyteFx`**: runtime invalidation uses `invalidateDraw()` instead of `View.postInvalidateOnAnimation()`.
- KDoc uses **program** consistently (not “effect”) for `ShaderProgram` ownership.

### Sample app

- GLES **Lit mesh** demo (`litTexturedMesh`, `ortho`, `setLitModel`).
- Gallery **photo picker** on Planet with runtime permissions and system photo picker on API 33+.
- Additional API showcases: `remap`, `sdSegment`, `bindInt`, negative `scale`.
