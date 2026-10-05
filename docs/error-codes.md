# Error codes / Коды ошибок

Compile-time authoring failures throw `AuthoringException` with `AuthoringCode`. Shader graph failures throw `ProgramException` with `ProgramCode`. GLES runtime failures throw `GlException` with `GlCode`.

Ошибки составления на компиляции бросают `AuthoringException` с `AuthoringCode`. Ошибки графа шейдера — `ProgramException` с `ProgramCode`. Ошибки GLES-рантайма — `GlException` с `GlCode`.

See the generated Dokka pages for `AuthoringCode`, `ProgramCode`, and `GlCode` enum entries. Typical fixes:

Сгенерированные страницы Dokka перечисляют значения `AuthoringCode`, `ProgramCode` и `GlCode`. Типичные исправления:

| Area / область | Symptom / симптом | Direction / куда смотреть |
|------|---------|-----------|
| Authoring | uniform inside `fn` / uniform внутри `fn` | Move uniforms to program scope / объявляйте uniform на программе |
| Authoring | `resolution` outside fragment / `resolution` вне фрагмента | Use `resolution` only in a fragment or a fragment `fn` via `this@fragment` / только во фрагменте или во fragment-`fn` через `this@fragment` |
| Authoring | `uniform("uResolution", …)` | Reserved name; read `FragmentDsl.resolution` / имя зарезервировано; читайте `FragmentDsl.resolution` |
| Program | recursive `fn` / `recur` | Use `repeat` or acyclic calls only / только `repeat` или ациклические вызовы |
| Program | `local` inside `repeat` / `local` внутри `repeat` | Use `local` before the loop and `set` inside / `local` до цикла, `set` внутри |
| GLES link | `MissingUniformLocation` in strict mode | Spelled uniform inactive in linked program / spelled-uniform неактивен в слинкованной программе |
| AGSL runtime | `AgslNotSupportedException` | API 33+ or use GLES Compose (`redbyteFx` in `gl.compose`) / API 33+ либо GLES Compose |
| Compose | two `redbyteFx` symbols / два символа `redbyteFx` | Import the AGSL modifier **or** the GLES host, not both in one file / импортируйте AGSL-модификатор **или** GLES-хост, не оба в одном файле |

Platform matrix: [RedByteFxApis](https://i-redbyte.github.io/redbytefx/) in API reference.

Матрица платформ: [RedByteFxApis](https://i-redbyte.github.io/redbytefx/) в справочнике API.
