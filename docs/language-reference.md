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
