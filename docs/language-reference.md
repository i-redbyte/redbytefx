# Language reference (summary)

Full narrative: [README.md](https://github.com/i-redbyte/redbytefx/blob/main/README.md).

## Entry

- `shader(ShaderTarget, block)` returns [ShaderProgram](https://github.com/i-redbyte/redbytefx/blob/main/redbytefx-core/src/main/java/ru/redbyte/redbytefx/ShaderDsl.kt).
- AGSL: API 31+, `newAgslInstance()`, Compose `rememberFxController` / `redbyteFx`.
- GLES: API 24+, `GlProgramRuntime` or `GlSurface` in `redbytefx-gl-compose`.

## Stages

| Stage | Targets |
|-------|---------|
| `fragment { }` | Agsl, Gles30, Gles32 |
| `vertex { }` | Gles30, Gles32 |
| `compute { }` | Gles31 |
| `geometry { }` / `tessControl` / `tessEval` | Gles32 |

## Statements

- `repeat(1..64) { i -> }` - counted `for` loop.
- `local(init) { }` / `set` - not inside `repeat`.
- `whenTrue(cond) { }` - `if` without `else`.
- `fn(witness..., "name") { p0, ... -> expr }` - no nesting, no recursion.
- `discard()` / `discard(cond)` - fragment only.

## Literals and types

- `float(1f)`, `float2(…)`, `float3(…)`, `float4(…)` — GLSL-style constructors; `1f.lit` / `1f.med` extensions do the same for scalars.
- `color(…)` — mediump RGBA for fragment color.
- `HighVec4`, `MedVec4`, … — typealiases over `Expr<…>` (see `Aliases.kt`).

## User functions (`fn`)

Stage DSLs expose `fn { … }` and `fn(witness) { p -> … }`. The return type is `Fn0` … `Fn8` (parameter count). Call with `invoke()` or `fnHandle(arg)` to emit a user function in shader source. No nesting, recursion, or cross-stage calls.

## Comparisons

Infix `gt`, `lt`, `ge`, `le`, `eq`, `ne` on floats; `gte` alias in `Sugar.kt`. Vector compares yield `bvec*`; use `any` / `all`. `ifElse(cond, a, b)` for ternaries.

## Swizzles

Vector expressions use GLSL-style property names: `.xy`, `.rgb`, `.rgba`, and single-letter lanes
`.x` / `.r`, `.a` (alpha on `vec4`), and so on. Names match generated shader masks; see KDoc on
[SwizzleAccess.kt](https://github.com/i-redbyte/redbytefx/blob/support-opengl/redbytefx-core/src/main/java/ru/redbyte/redbytefx/SwizzleAccess.kt).

## Math (portable)

`sin`, `cos`, `abs`, `floor`, `ceil`, `fract`, `sqrt`, `min`, `max`, `mod`, `pow`, `mix`, `clamp`, `smoothstep`, `step`, `saturate`, `dot`, `length`, `normalize`, `distance`, `cross`, `dFdx`, `dFdy`, `fwidth` (derivatives: fragment only).

## Modules

| Artifact | Role |
|----------|------|
| redbytefx-core | DSL + compiler |
| redbytefx-gl | GLES runtime |
| redbytefx-gl-compose | Compose GLES |
| redbytefx-compose | AGSL Compose |
| redbytefx-stdlib | Fragment recipes |
