**English** · [Русский](README.ru.md)

# RedByteFX

**RedByteFX** is a Kotlin DSL with two surfaces on one typed shader algebra. An **effect** is a fragment shader: Android AGSL on API 31+, the short path through `redbyteFx`. A **scene** is a mesh on OpenGL ES from API 24: vertex and fragment stages, buffers, textures, and draws. The same algebra also compiles to OpenGL ES 3.1 compute and OpenGL ES 3.2 geometry and tessellation.

Authoring is Kotlin, not a shader string. The compiler emits the text the platform actually runs:

`shader(target) { ... } -> ShaderProgram -> AGSL RuntimeShader, a GLES 3.0 program, a GLES 3.1 compute program, or a GLES 3.2 program`

**Platform:** library `minSdk` is **24**. **AGSL** (`ShaderTarget.Agsl`, `rememberFxController`, `redbyteFx`) needs **API 31+** (`RuntimeShader`); below that, runtime calls throw `AgslNotSupportedException` and Android Studio warns via `@RequiresApi`. **OpenGL ES** scenes work from API 24 through `redbytefx-gl` and `redbytefx-gl-compose` (`GlSurface`). GLES output is GLSL ES 3.00, 3.10 compute, or 3.20 with geometry and tessellation. API reference: [GitHub Pages](https://i-redbyte.github.io/redbytefx/).

## What you write

One carrier, `Expr<T>`. Rank is nominal (`Vec2`, `Vec3`, `Vec4`, and the matrix types). Precision is a parameter: `Flt<High>` is a highp float, `Flt<Med>` is a mediump float. Color is a `Vec4` of mediump floats, produced by `color(...)`. There is no separate color type. Write the common cases as `HighFloat`, `HighVec2`, `HighVec3`, `HighVec4`, `MedFloat`, `MedVec2`, `MedVec3`, and `MedVec4`. Uniform handles of those shapes are `HighFloatUniform`, `HighVec2Uniform`, `HighVec3Uniform`, and `HighVec4Uniform`.

`Uniform<T>` is a handle owned by one `ShaderProgram`. Inside a stage you read `uniform.expr`. The handle itself is what Compose and the GLES runtime write. A handle from one program is not valid on another.

```kotlin
val wave = shader(ShaderTarget.Agsl) {
    val amplitude = uniform("wave_amplitude", 0f)
    val frequency = uniform("wave_frequency", 0.08f)
    fragment {
        val offset = float2(0f, sin(fragCoord.x * frequency.expr) * amplitude.expr)
        sample(fragCoord + offset)
    }
}
```

AGSL is fragment-only. The generated entry stays `half4 main(float2 fragCoord)`, which `RuntimeShader` requires. Inspect it with `wave.agslSource()`.

A scene is the other surface. This lit box is OpenGL ES only: `shader(ShaderTarget.Agsl)` rejects `uniformMat4`, so AGSL cannot compile it.

```kotlin
val crate = shader(ShaderTarget.Gles30) {
    val view = uniformMat4("view")
    val projection = uniformMat4("projection")
    vertex {
        val position = attributeVec3("position")
        val clip = projection.expr * (view.expr * vec4(position.x, position.y, position.z, 1f.lit))
        glPosition(clip)
    }
    fragment {
        vec4(0.85f.lit, 0.45f.lit, 0.2f.lit, 1f.lit)
    }
}
```

OpenGL ES 3.0 needs both stages. Varyings written in the vertex stage are read in the fragment stage. Inspect them with `vertexSource()` and `fragmentSource()`.

```kotlin
val pulse = shader(ShaderTarget.Gles30) {
    val time = uniformTime()
    vertex {
        val position = attributeVec2("position")
        glPosition(vec4(position.x, position.y, 0f.lit, 1f.lit))
    }
    fragment {
        val wave = sin(time.expr)
        vec4(0.5f.lit + wave * 0.5f.lit, 0.15f.lit, 0.85f.lit, 1f.lit)
    }
}
```

OpenGL ES 3.1 is a compute program. It has no vertex or fragment stage. Each `storageBlock` is std430, and `compute(localSizeX)` writes its fields. Several storage blocks are legal; the binding point is the declaration order starting at 0, and one block stays at 0. `compute(localSizeX, localSizeY, localSizeZ)` spells all three local sizes; `compute(n)` still spells only `local_size_x`. Inspect the text with `computeSource()`. The EGL context for that program is OpenGL ES 3.1. Inside compute, `globalId`, `localId`, and `workGroupId` are `ivec3` values spelled from `gl_GlobalInvocationID`, `gl_LocalInvocationID`, and `gl_WorkGroupID`. A storage block can also hold `floatArray`, `vec2Array`, `vec3Array`, and `vec4Array`. A sized array keeps its count. The last field may omit the count and is spelled `type name[]`; an unsized field anywhere else is rejected when the block is finished. Index an element with an `int` expression, such as `values[globalId.x]`, then `store` it. `packStd430` takes logical components, so a `vec3` contributes three floats and the packer inserts the std430 zero padding that makes the element stride 16 bytes. `GlProgramRuntime.dispatch(x, y, z)` runs the linked compute program. `shared` arrays and `barrier()` are legal only in compute. A `uniformBlock` can be read from compute; AGSL still rejects a uniform block. Several uniform blocks are legal on one program, spelled `layout(std140, binding = N)` in declaration order. Its fields may be sized `floatArray`, `vec2Array`, `vec3Array`, `vec4Array`, `mat2`, `mat3`, and `mat4`. `packStd140` and `unpackStd140` use logical floats and skip std140 padding. An unsized array stays on `storageBlock`.

```kotlin
val cells = shader(ShaderTarget.Gles31) {
    storageBlock("cells") {
        val value = vec4("value")
        compute(64) { value.store(value) }
    }
}
```

OpenGL ES 3.2 keeps the vertex and fragment stages and adds optional geometry and tessellation. Tessellation is a control stage and an evaluation stage together. The EGL context for that program is OpenGL ES 3.2. Inspect the extra stages with `geometrySource()`, `tessControlSource()`, and `tessEvalSource()`.

```kotlin
val patch = shader(ShaderTarget.Gles32) {
    vertex { glPosition(attributeVec4("position")) }
    tessControl(3) {
        tessLevelOuter(0, 1f.lit)
        tessLevelOuter(1, 1f.lit)
        tessLevelOuter(2, 1f.lit)
        tessLevelInner(0, 1f.lit)
        passPosition()
    }
    tessEval(TessPrimitive.Triangles) { glPosition(glIn(0)) }
    geometry(GeometryInput.Triangles, GeometryOutput.TriangleStrip, 3) {
        glPosition(glIn(0))
        emitVertex()
        endPrimitive()
    }
    fragment { vec4(1f.lit, 0f.lit, 0f.lit, 1f.lit) }
}
```

A varying written by the vertex and read by the fragment is declared on every stage between them: vertex, tessellation control, tessellation evaluation, geometry, then fragment. Missing stages are skipped. At each boundary the previous stage's `out` matches the next stage's `in` by name, type, and precision. When geometry or tessellation is present, those declarations are members of one interface block named `rb_pipe`. Every stage of that program lists the same members: varyings the vertex wrote and that the fragment or an intermediate stage reads, in declaration order. A varying that is declared and never written stays out of the block. Tessellation control copies `tc_in[gl_InvocationID]` to `tc_out[gl_InvocationID]`. Geometry copies the input vertex just written to `gl_Position`: one `gl_in[k]` in that position, including a `repeat` index, becomes `gs_in[k]` immediately before that `EmitVertex`. The same copy applies when that `gl_Position` was written before the `repeat` or `whenTrue` that emits, and it is not reused after an emit that already consumed it. Several indices, or a position that does not read `gl_in` while the varying is still unwritten, is an error whose message asks for `varying.set` before that emit. A `varying.set` between that `gl_Position` and the emit replaces the copy of that varying for that emit only. Tessellation evaluation interpolates with `gl_TessCoord` when the stage does not write the varying. One input vertex is copied from `te_in[0]`. Triangles weight the three patch vertices by `gl_TessCoord`. Isolines `mix` the two vertices by `gl_TessCoord.x`. Quads use a bilinear `mix` of corners `(0,0)`, `(1,0)`, `(1,1)`, and `(0,1)` by `gl_TessCoord.xy`. An explicit write replaces that interpolation. A stage that has `rb_pipe` and writes `gl_Position` redeclares `gl_PerVertex` with only `vec4 gl_Position` before the block. A uniform read in geometry or tessellation is declared in that stage. `glIn(index)` and `varying.at(index)` accept dynamic `int` expressions; only compile-time constant indices are bounds-checked against the input patch.

`Modifier.redbyteFx` applies an AGSL `RenderEffect`. It does not run a GLES program. The sample app opens AGSL examples and OpenGL examples from separate screens. Each OpenGL scene is a `GLSurfaceView` that links a `ShaderProgram`. The OpenGL list covers buffers and indexed draws, textures and mipmaps, lighting, instancing, render-to-texture, geometry, tessellation, and small games. The sample UI stays in English unless the device language is Russian. Geometry and tessellation scenes need an OpenGL ES 3.2 context.

## Install

```kotlin
dependencies {
    implementation("io.github.i-redbyte:redbytefx-core:1.1.0")
    implementation("io.github.i-redbyte:redbytefx-gl:1.1.0")
    implementation("io.github.i-redbyte:redbytefx-gl-compose:1.1.0")
    implementation("io.github.i-redbyte:redbytefx-compose:1.1.0")
    implementation("io.github.i-redbyte:redbytefx-stdlib:1.1.0")
    implementation("io.github.i-redbyte:redbytefx-3d:1.1.0")
}
```

| Artifact | Role |
|----------|------|
| `redbytefx-core` | `shader`, `Expr`, uniforms, AGSL, GLSL ES 3.00, GLSL ES 3.10 compute, and GLSL ES 3.20 spelling |
| `redbytefx-gl` | GLES 3.0, 3.1, and 3.2 link, uniform and matrix writes, cube and 2D binds, compute `dispatch`, and storage uploads, bound to the EGL thread |
| `redbytefx-gl-compose` | `GlSurface`, `GlController`, `GlFrame`, and mesh helpers for a GLES scene in Compose |
| `redbytefx-compose` | `rememberFxController`, `FxController`, `Modifier.redbyteFx` for AGSL |
| `redbytefx-stdlib` | Effect recipes: coordinates, masks, compositing, SDF, and `lambert` |
| `redbytefx-3d` | CPU scene data for meshes and cameras. No OpenGL driver |

## Compose

Uniforms are declared on the program, then bound by the same handle.

```kotlin
val program = shader(ShaderTarget.Agsl) {
    val amplitude = uniform("wave_amplitude", 0f)
    val frequency = uniform("wave_frequency", 0.08f)
    fragment {
        val offset = float2(0f, sin(fragCoord.x * frequency.expr) * amplitude.expr)
        sample(fragCoord + offset)
    }
}

@Composable
fun WaveLabel(program: ShaderProgram, amplitude: HighFloatUniform, frequency: HighFloatUniform) {
    val fx = rememberFxController(program)
    fx.bindFloat(amplitude, 12f)
    fx.bindFloat(frequency, 0.08f)
    Text("RedByteFX", modifier = Modifier.redbyteFx(fx))
}
```

`rememberFxController` owns one AGSL runtime. Use one controller per render target. `redbyteFx` writes the resolution from the draw size. `RuntimeShader` is touched on the UI thread.

## Authoring

Stages are a small state machine. Uniforms, samplers, and varyings are declared on the program. `fragment { }` and `vertex { }` emit shader code. `vertex` exists for `ShaderTarget.Gles30` and `ShaderTarget.Gles32`. `geometry`, `tessControl`, and `tessEval` exist for `ShaderTarget.Gles32`. `compute` exists for `ShaderTarget.Gles31`.

- `fragCoord` is a fragment input in pixels, including inside a fragment `fn`. AGSL spells `fragCoord`. GLES spells `gl_FragCoord.xy`. `resolution` is an AGSL fragment input in pixels; GLES rejects it.
- `sample()` reads the child shader. It is legal in an AGSL fragment, including inside a fragment `fn` via `this@fragment.sample()`. Other stages reject it.
- `texture(sampler, uv)` is legal in a GLES fragment, including inside a fragment `fn` via `this@fragment.texture()`. Other stages reject it.
- `attributeVec2`, `attributeVec3`, `attributeVec4`, and `glPosition` are legal in a GLES vertex. `glPosition` is also legal in geometry and tessellation evaluation.
- `varyingFloat`, `varyingVec2`, `varyingVec3`, and `varyingVec4` are highp. The fragment can read a varying only when the vertex wrote it.
- `let(expr, "name")` names a local in the generated shader.
- `local(initializer, "name")` declares a float, a float vector, or an int before the stage commands and assigns it as a statement. `set` updates it, including inside `repeat` and `whenTrue`. `local` inside `repeat` is rejected. The value can be returned from the fragment or written to `gl_Position`.
- `whenTrue(condition) { }` places commands in `if (condition)`. There is no `else`.
- `repeat(count) { i -> ... }` is a counted loop, `for (int i = 0; i < count; ++i)`, with `count` from 1 to 64. The body can nest another `repeat`.
- `discard()` and `discard(condition)` are legal only in a fragment, including inside a fragment `fn`. The conditional form is `if (condition) discard`. `dFdx`, `dFdy`, and `fwidth` are fragment-only as well.
- `fn` takes 0 to 8 arguments. A witness value supplies the erased shape: `fn(0f.lit, 0f.lit, "name") { p0, p1 -> ... }`. Parameters are `p0` through `p7`. Functions do not nest, they cannot declare uniforms, and recursion is rejected. The same rules apply in compute, geometry, and tessellation. A call from the wrong stage is rejected.
- `uniformInt` is a scalar int on AGSL (`setIntUniform`) and GLES (`uniform1i`). `uniformBool`, `uniformMat2`, `uniformMat3`, and `uniformMat4` are GLES only; AGSL rejects them. Matrices upload column-major with `transpose` false, and the link default is the identity. `samplerCube` and `textureCube` are GLES fragment only.

```kotlin
shader(ShaderTarget.Agsl) {
    fragment {
        val gain = fn(0f.lit, "gain") { p0 -> saturate(p0) }
        val tone = gain(sample().r.toHigh()).toMed()
        color(tone, tone, tone, sample().a)
    }
}
```

Scalar floats of one precision support `+`, `-`, `*`, `/`. Vectors support vector-vector and vector-scalar math of the same precision. `mat2`, `mat3`, and `mat4` multiply a matrix of the same size or a vector with that many lanes; both spell `*`. Float and int scalars compare with `gt`, `lt`, `ge`, `le`, `eq`, and `ne`. `gte` is another spelling of `ge`. `ifElse(condition, ifTrue, ifFalse)` is the ternary. Boolean scalars support `and`, `or`, and `not`.

Math that spells the same call in both languages includes `sin`, `cos`, `tan`, `sign`, `abs`, `floor`, `ceil`, `fract`, `sqrt`, `min`, `max`, `mod`, `pow`, `mix`, `clamp`, `smoothstep`, `step`, `saturate`, `dot`, `length`, `reflect`, `transpose`, and `distance` for `vec2` and `vec3`. `sqrt` is the GLSL call and does not replace a negative argument.

`redbytefx-stdlib` adds coordinate and compositing recipes on the same `fragment` receiver: `normalizedUv`, `sampleUv`, `centeredUv`, masks, reveals, blend helpers, and SDF helpers such as `sdCircle` and `softFill`. `softFill` and `stroke` still take the pen width from the caller. `softFillScreen(distance)` and `strokeScreen(distance, width)` take the edge width from `fwidth(distance)` and are legal only in a fragment.

## Runtime

`ShaderProgram` is immutable and readable from any thread. The expression graph and the generated strings exist at compile time.

AGSL playback is `program.newAgslInstance()`. An unchanged uniform does not call the GPU. `set` writes highp and mediump floats and vectors, and `uniformInt` through `setIntUniform`. GLES playback is `GlProgramRuntime` in `redbytefx-gl`: create it, link it, and destroy it on the thread that owns the EGL context. `destroy` unbinds the program before deleting it. `set` writes those floats, `uniform1i` for `uniformInt` and for `uniformBool` as 0 or 1, and column-major `uniformMatrix*fv` for `mat2`, `mat3`, and `mat4` with transpose false. Link uploads the defaults, including an identity matrix when none was passed, and the same value does not call `glUniform` again. `bind` of `sampler2D` uses `GL_TEXTURE_2D`; `bind` of `samplerCube` uses `GL_TEXTURE_CUBE_MAP`. Both share `GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS`. `dispatch(x, y, z)` runs a linked GLES 3.1 program and then calls `glMemoryBarrier` with `GL_SHADER_STORAGE_BARRIER_BIT`. `runtime.set(storageBlock, floats)` packs logical std430 values and skips the upload when those bytes are unchanged. `runtime.read(storageBlock, into)` copies those logical floats back, without std430 padding, after a `GL_BUFFER_UPDATE_BARRIER_BIT` so a compute write is visible. `set` and `read` take that block. Several uniform or storage blocks bind in declaration order from 0, and a block from another shader is rejected. A binding at or above `GL_MAX_UNIFORM_BUFFER_BINDINGS` or `GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS` is rejected when the program is linked. `GlController.set` of a uniform or storage block keeps the latest floats and writes them again when the EGL context is recreated. `dispatch` and `read` are queued on the GL thread; `read` returns immediately and fills the caller's buffer from that queue. `GlCompute` hosts a GLES 3.1 program with no mesh and no draw. Link runs on the GL thread. `GlSurface` is unchanged for ordinary scenes. The `set` upload fixes the byte size, including an unsized tail. A later set whose packed size differs allocates the buffer again. `StorageBlock.byteSize` is the std430 size of a fully sized block. A block with an unsized array reports that size through `byteSize(valueCount)`, which matches `packStd430`. `uniformMedium`, `uniformMediumVec2`, `uniformMediumVec3`, and `uniformMediumVec4` declare the mediump forms; on AGSL they still go through `setFloatUniform`. `FxController` binds float uniforms and `uniformInt` for Compose.

## Not in this version

The compiler does not emit GLES 2.0 or desktop GL. It does not emit `while`, recursion, nested functions, or a loop bound that is not a constant from 1 to 64. `uniformBool`, `uniformMat2`, `uniformMat3`, `uniformMat4`, and `samplerCube` are rejected on AGSL.

## Contributing

Run `./gradlew qualityCheck` before a PR. That gate is unit tests, sample compilation, and detekt. Device GLES tests are `./gradlew :redbytefx-gl:connectedDebugAndroidTest` and are not part of `qualityCheck`.

API site locally: `./gradlew dokkaHtmlSite` (`build/docs/site/index.html`). CI publishes to `https://i-redbyte.github.io/redbytefx/` on push to **`master`/`main`**; other branches only verify the build. See [docs/github-pages.md](docs/github-pages.md).

## License

[MIT](LICENSE)
