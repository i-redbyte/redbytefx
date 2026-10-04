# Error codes

Compile-time authoring failures throw `AuthoringException` with `AuthoringCode`. Shader graph failures throw `ProgramException` with `ProgramCode`. GLES runtime failures throw `GlException` with `GlCode`.

See the generated Dokka pages for `AuthoringCode`, `ProgramCode`, and `GlCode` enum entries. Typical fixes:

| Area | Symptom | Direction |
|------|---------|-----------|
| Authoring | uniform inside `fn` | Move uniforms to program scope |
| Program | recursive `fn` / `recur` | Use `repeat` or acyclic calls only |
| Program | `local` inside `repeat` | Use `local` before the loop and `set` inside |
| GLES link | `MissingUniformLocation` in strict mode | Spelled uniform inactive in linked program |
| AGSL runtime | `AgslNotSupportedException` | API 33+ or use GLES Compose |

Platform matrix: [RedByteFxApis](https://i-redbyte.github.io/redbytefx/) in API reference.
