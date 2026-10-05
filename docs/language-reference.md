# Language reference (summary)

**English narrative:** [README.md](https://github.com/i-redbyte/redbytefx/blob/master/README.md) · **Русский:** [README.ru.md](https://github.com/i-redbyte/redbytefx/blob/master/README.ru.md)

## Entry

- `shader(ShaderTarget, block)` returns [ShaderProgram](https://github.com/i-redbyte/redbytefx/blob/master/redbytefx-core/src/main/java/ru/redbyte/redbytefx/ShaderDsl.kt).
- AGSL: API 33+ (`RedByteFxApis.AGSL_MIN_SDK`), `newAgslInstance()`, Compose `rememberFxController` / `redbyteFx`.
- GLES: API 24+, `GlProgramRuntime` in `redbytefx-gl`, or `GlSurface` / `GlCompute` / `redbyteFx` in `redbytefx-gl-compose`.

## Two surfaces (English)

**Effect.** Fragment only, `redbytefx-stdlib` recipes, `redbyteFx`. AGSL (`ShaderTarget.Agsl`) is API 33+ on the UI thread. GLES (`ShaderTarget.Gles30`) is API 24+ on the EGL thread: omit `vertex { }` and the compiler injects a fullscreen triangle (`a_corner`). `resolution` is `uResolution` on both. GLES `redbyteFx` does not sample Compose content (`sample()` / `sampleUv()` stay AGSL-only). “3D” here is `sdSphere` / `rayMarch` in the fragment — no mesh.

**Scene.** Vertex and fragment, buffers, textures, draws. OpenGL ES on the EGL thread, API 24+. Real meshes live here; AGSL cannot compile `uniformMat4` scene programs.

## Две поверхности

**Эффект.** Только фрагмент, рецепты `redbytefx-stdlib`, `redbyteFx`. AGSL (`ShaderTarget.Agsl`) — API 33+, поток UI. GLES (`ShaderTarget.Gles30`) — API 24+, поток EGL: без `vertex { }` компилятор подставляет полноэкранный треугольник (`a_corner`). `resolution` — это `uResolution` на обоих. GLES `redbyteFx` не сэмплирует Compose-контент (`sample()` / `sampleUv()` остаются только у AGSL). Трёхмерность здесь — это `sdSphere` и `rayMarch`: луч считается во фрагменте, сетки нет.

**Сцена.** Вершина и фрагмент, буфер, текстура, draw. OpenGL ES, поток EGL, API 24+. Трёхмерность здесь — это меш. AGSL эту программу не собирает: `uniformMat4` на эффекте отвергается. `uniformBlock` на GLES принимает размерные массивы float/vec и `mat2`/`mat3`/`mat4`; `packStd140` и `unpackStd140` работают логическими float и пропускают padding. Несколько `uniformBlock` и `storageBlock` получают binding по порядку объявления с нуля. GLSL ES 3.00 пишет uniform-блок как `layout(std140)` без `binding`, точку ставит рантайм. GLSL ES 3.10 и 3.20 пишут `binding`. `GlController` хранит последний `set` такого блока и пишет его снова на новом контексте. `dispatch` и `read` идут в очередь GL-потока; `read` не блокирует вызывающего. `GlCompute` — хост GLES 3.1 без меша и без растеризации; `GlSurface` для обычной сцены остаётся прежним. `readFramebuffer` и `readColorTarget` читают RGBA8 в готовый буфер; размер проверяется до `glReadPixels`, а привязка framebuffer после чтения цели та же. `GlPipeline` на `draw` задаёт blend, scissor, маску цвета, маску глубины и `cullFace`; следующий draw подставляет своё, по умолчанию как раньше. Отсечение граней по умолчанию выключено. `draw(model, material)` задаёт матрицу экземпляра и per-draw material uniform. Обычный draw не вызывает `glGetError`. `strictErrors` превращает ошибку драйвера после draw, dispatch, загрузки текстуры, uniform/storage block upload, mipmap generation и чтения в `GlException`. Неразмерный массив остаётся у `storageBlock`.

## Stages

| Stage | Targets |
|-------|---------|
| `fragment { }` | Agsl, Gles30, Gles32 |
| `vertex { }` | Gles30, Gles32 (optional for a fragment-only effect) |
| `compute { }` | Gles31 |
| `geometry { }` / `tessControl` / `tessEval` | Gles32 |

On `Gles30`, and on `Gles32` without geometry or tessellation, omitting `vertex { }` injects a fullscreen triangle. The attribute is spelled `a_corner` even if a user `fn` reuses that identifier. Pair the program with `screenMesh` or GLES `redbyteFx`. Geometry/tessellation still need an explicit vertex stage.

На `Gles30` и на `Gles32` без geometry/tessellation отсутствие `vertex { }` подставляет полноэкранный треугольник. Атрибут пишется `a_corner`, даже если пользовательский `fn` занимает этот идентификатор. Программу рисуют `screenMesh` или GLES `redbyteFx`. Geometry и tessellation по-прежнему требуют явную вершину.

## Reserved names / зарезервированные имена

- `uResolution` is reserved. Read `FragmentDsl.resolution`; do not declare `uniform("uResolution", …)`. AGSL always binds `ShaderProgram.resolution` (the generated source already declares it). GLES binds it when the fragment (including an unused fragment `fn`) reads `resolution`.
- `a_corner` is the injected screen-triangle attribute. A user `fn("a_corner")` is renamed; the attribute keeps `a_corner`.
- `sample()` / `sampleUv()` are AGSL-only. GLES `redbyteFx` does not sample Compose content.

- `uResolution` зарезервировано. Читайте `FragmentDsl.resolution`, не объявляйте `uniform("uResolution", …)`. На AGSL `ShaderProgram.resolution` всегда привязан. На GLES uniform появляется, если фрагмент (в том числе невызываемый fragment-`fn`) читает `resolution`.
- `a_corner` — атрибут подставленного треугольника. Пользовательский `fn("a_corner")` переименовывается; атрибут остаётся `a_corner`.
- `sample()` / `sampleUv()` — только AGSL. GLES `redbyteFx` не сэмплирует Compose-контент.

## Statements

- `repeat(1..64) { i -> }` - counted `for` loop.
- `local(init)` / `local(init, "name")` / `set` - `local` is not legal inside `repeat`.
- `whenTrue(cond) { }` - `if` without `else`.
- `fn(witness..., "name") { p0, ... -> expr }` - no nesting, no recursion.
- `discard()` / `discard(cond)` - fragment only.

## Literals and types

- `float(1f)`, `float2(…)`, `float3(…)`, `float4(…)` — GLSL-style constructors; `1f.lit` / `1f.med` extensions do the same for scalars.
- `color(…)` — mediump RGBA for fragment color.
- `HighVec4`, `MedVec4`, … — typealiases over `Expr<…>` (see `Aliases.kt`).

## User functions (`fn`)

Stage DSLs expose `fn { … }` and `fn(witness) { p -> … }`. The return type is `Fn0` … `Fn8` (parameter count). Call that handle with `invoke` / `fn(args)` to emit a user function in shader source. No nesting, recursion, or cross-stage calls. The body receiver is `FnDsl`, so `sample()`, `texture()`, `fragCoord`, and `resolution` are not in scope there. A fragment `fn` may capture them with `this@fragment.sample()`, `this@fragment.texture()`, `this@fragment.fragCoord`, and `this@fragment.resolution`. A vertex or compute `fn` cannot capture them.

## Comparisons

Infix `gt`, `lt`, `ge`, `le`, `eq`, `ne` on floats; `gte` alias in `Sugar.kt`. Vector compares yield `bvec*`; use `any` / `all`. `ifElse(cond, a, b)` for ternaries. `choose(selector) { on(id) { … }; otherwise { … } }` nests that ternary on `abs(selector - id) < width`.

## Swizzles

Vector expressions use GLSL-style property names: `.xy`, `.rgb`, `.rgba`, and single-letter lanes
`.x` / `.r`, `.a` (alpha on `vec4`), and so on. Names match generated shader masks; see KDoc on
[SwizzleAccess.kt](https://github.com/i-redbyte/redbytefx/blob/master/redbytefx-core/src/main/java/ru/redbyte/redbytefx/SwizzleAccess.kt).

## Math (portable)

`sin`, `cos`, `abs`, `floor`, `ceil`, `fract`, `sqrt`, `min`, `max`, `mod`, `pow`, `mix`, `clamp`, `smoothstep`, `step`, `saturate`, `dot`, `length`, `normalize`, `distance`, `cross`, `dFdx`, `dFdy`, `fwidth` (derivatives: fragment only).

## Modules

| Artifact | Role |
|----------|------|
| redbytefx-core | Shader language |
| redbytefx-gl | GLES resources and draws |
| redbytefx-gl-compose | Compose host: GLES scene (`GlSurface`) or fragment-only effect (`redbyteFx`) |
| redbytefx-compose | AGSL |
| redbytefx-stdlib | Effect recipes, including 2D/3D SDF, lighting, and color |
| redbytefx-3d | CPU scene data, no driver |

Mesh primitives (`triangle`, `quad`, `box`, `sphere`, `torus`) are CPU data in `redbytefx-3d`. `redbytefx-gl-compose` adapts them to `GlMesh` with the same names and indices. The layout is `a_position` (vec3), `a_normal` (vec3), and `a_uv` (vec2, 0..1 on each face), spelled by `attributeVec3("position")`, `attributeVec3("normal")`, and `attributeVec2("uv")`. `lookAt`, `perspective`, and `ortho` return a column-major `FloatArray(16)` in the same order as `glUniformMatrix4fv` with transpose false. Clip z is the OpenGL ES range −1..1. `ortho` requires `far > near`.

`litTexturedMesh` multiplies vertices by a model matrix (identity when omitted). Normals use `normalMatrix`, the upper 3×3 inverse-transpose computed on the CPU. When the model changes at runtime, upload both matrices or call `GlProgramRuntime.setLitModel` in `redbytefx-gl-compose`. The fragment multiplies Lambert shading by the albedo sample, including texture alpha.

`GlProgramRuntime.uploadRgba` creates an RGBA8 texture on the GLES context thread (linear filter, repeat wrap, no mip chain). Bind the returned name with `bind` on a `sampler2D`. The City sample uploads a window grid and an asphalt image from that callback. The sample **Lit mesh** scene demonstrates `litTexturedMesh` and `setLitModel`; **Planet** can wrap a user photo (gallery permissions on older API levels; system photo picker on API 33+).

## Sample app / тестовое приложение

The sample module is not published to Maven. It ships AGSL demos (`Modifier.redbyteFx`) and GLES scenes (`GlSurface`), including a fragment-only GLES **Effect**. UI strings follow the device locale (English or Russian). See the repository `sample/` tree and [docs/changelog.md](changelog.md) / [docs/changelog.ru.md](changelog.ru.md) for release notes.
