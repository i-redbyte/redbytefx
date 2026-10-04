# Language reference (summary)

Full narrative: [README.md](https://github.com/i-redbyte/redbytefx/blob/master/README.md).

## Entry

- `shader(ShaderTarget, block)` returns [ShaderProgram](https://github.com/i-redbyte/redbytefx/blob/master/redbytefx-core/src/main/java/ru/redbyte/redbytefx/ShaderDsl.kt).
- AGSL: API 31+, `newAgslInstance()`, Compose `rememberFxController` / `redbyteFx`.
- GLES: API 24+, `GlProgramRuntime` in `redbytefx-gl`, or `GlSurface` in `redbytefx-gl-compose`.

## Две поверхности

**Эффект.** `shader(ShaderTarget.Agsl)`, только фрагмент, рецепты `redbytefx-stdlib`, `redbyteFx`. Поток UI, API 31+. Трёхмерность здесь — это `sdSphere` и `rayMarch`: луч считается во фрагменте, сетки нет.

**Сцена.** Вершина и фрагмент, буфер, текстура, draw. OpenGL ES, поток EGL, API 24+. Трёхмерность здесь — это меш. AGSL эту программу не собирает: `uniformMat4` на эффекте отвергается.

## Stages

| Stage | Targets |
|-------|---------|
| `fragment { }` | Agsl, Gles30, Gles32 |
| `vertex { }` | Gles30, Gles32 |
| `compute { }` | Gles31 |
| `geometry { }` / `tessControl` / `tessEval` | Gles32 |

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

Stage DSLs expose `fn { … }` and `fn(witness) { p -> … }`. The return type is `Fn0` … `Fn8` (parameter count). Call that handle with `invoke` / `fn(args)` to emit a user function in shader source. No nesting, recursion, or cross-stage calls. The body receiver is `FnDsl`: `sample()`, `fragCoord`, and `resolution` are not in scope. Qualify the stage (`this@fragment.sample()`) only when that capture is intentional.

## Comparisons

Infix `gt`, `lt`, `ge`, `le`, `eq`, `ne` on floats; `gte` alias in `Sugar.kt`. Vector compares yield `bvec*`; use `any` / `all`. `ifElse(cond, a, b)` for ternaries.

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
| redbytefx-gl-compose | Compose host for a GLES scene |
| redbytefx-compose | AGSL |
| redbytefx-stdlib | Effect recipes, including 3D signed distances |
| redbytefx-3d | CPU scene data, no driver |

Mesh primitives (`triangle`, `quad`, `box`, `sphere`, `torus`) are CPU data in `redbytefx-3d`. `redbytefx-gl-compose` adapts them to `GlMesh` with the same names and indices. The layout is `a_position` (vec3), `a_normal` (vec3), and `a_uv` (vec2, 0..1 on each face), spelled by `attributeVec3("position")`, `attributeVec3("normal")`, and `attributeVec2("uv")`. `lookAt` and `perspective` return a column-major `FloatArray(16)` in the same order as `glUniformMatrix4fv` with transpose false. Clip z is the OpenGL ES range −1..1.

`GlProgramRuntime.uploadRgba` creates an RGBA8 texture on the GLES context thread (linear filter, repeat wrap, no mip chain). Bind the returned name with `bind` on a `sampler2D`. The City sample uploads a window grid and an asphalt image from that callback.
