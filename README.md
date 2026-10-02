**English** · [Русский](README.ru.md)

# RedByteFX

**RedByteFX** is a Kotlin DSL that compiles one typed shader algebra to Android AGSL, OpenGL ES 3.0, OpenGL ES 3.1 compute, and OpenGL ES 3.2 geometry and tessellation.

Authoring is Kotlin, not a shader string. The compiler emits the text the platform actually runs:

`shader(target) { ... } -> ShaderProgram -> AGSL RuntimeShader, a GLES 3.0 program, a GLES 3.1 compute program, or a GLES 3.2 program`

**Platform:** Android API 33+. AGSL needs `RuntimeShader`. OpenGL ES output is GLSL ES 3.00 vertex and fragment, GLSL ES 3.10 compute, or GLSL ES 3.20 with geometry and tessellation.

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

OpenGL ES 3.1 is a compute program. It has no vertex or fragment stage. One `storageBlock` is std430, and `compute(localSizeX)` writes its fields. Inspect the text with `computeSource()`. The EGL context for that program is OpenGL ES 3.1.

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

`Modifier.redbyteFx` applies an AGSL `RenderEffect`. It does not run a GLES program. The sample app opens AGSL examples and OpenGL examples from separate screens. The OpenGL list includes a triangle and the Spheres scene, which draws on its own `GLSurfaceView`.

## Install

```kotlin
dependencies {
    implementation("io.github.i-redbyte:redbytefx-core:1.0.0")
    implementation("io.github.i-redbyte:redbytefx-gl:1.0.0")
    implementation("io.github.i-redbyte:redbytefx-compose:1.0.0")
    implementation("io.github.i-redbyte:redbytefx-stdlib:1.0.0")
}
```

| Artifact | Role |
|----------|------|
| `redbytefx-core` | `shader`, `Expr`, uniforms, AGSL, GLSL ES 3.00, GLSL ES 3.10 compute, and GLSL ES 3.20 spelling |
| `redbytefx-gl` | GLES 3.0, 3.1, and 3.2 link, uniform writes, and storage uploads, bound to the EGL thread |
| `redbytefx-compose` | `rememberFxController`, `FxController`, `Modifier.redbyteFx` for AGSL |
| `redbytefx-stdlib` | Coordinates, masks, compositing, SDF, and related helpers on top of the same DSL |

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

- `fragCoord` and `resolution` are AGSL fragment inputs, in pixels.
- `sample()` reads the child shader. It is legal only in an AGSL fragment, and not inside `fn`.
- `texture(sampler, uv)` is legal only in a GLES fragment.
- `attributeVec2` and `glPosition` are legal only in a GLES vertex.
- `let(expr, "name")` names a local in the generated shader.
- `fn` takes 0, 1, or 2 arguments. A witness value supplies the erased shape: `fn(0f.lit, 0f.lit, "name") { p0, p1 -> ... }`. Parameters are `p0` and `p1`. Functions do not nest, and they cannot declare uniforms.

```kotlin
shader(ShaderTarget.Agsl) {
    fragment {
        val gain = fn(0f.lit, "gain") { p0 -> saturate(p0) }
        val tone = gain(sample().r.toHigh()).toMed()
        color(tone, tone, tone, sample().a)
    }
}
```

Scalar floats of one precision support `+`, `-`, `*`, `/`. Vectors support vector-vector and vector-scalar math of the same precision. Comparisons are `gt`, `lt`, `ge`, `le`, `eq`, `ne`. `gte` is another spelling of `ge`. `ifElse(condition, ifTrue, ifFalse)` is the ternary. Boolean scalars support `and`, `or`, and `not`.

Math that spells the same call in both languages includes `sin`, `cos`, `abs`, `floor`, `ceil`, `fract`, `min`, `max`, `mod`, `pow`, `mix`, `clamp`, `smoothstep`, `step`, `saturate`, `dot`, and `length`. There is no `sqrt` builtin: a non-negative root is `pow(max(x, 0f), 0.5f)`.

`redbytefx-stdlib` adds coordinate and compositing recipes on the same `fragment` receiver: `normalizedUv`, `sampleUv`, `centeredUv`, masks, reveals, blend helpers, and SDF helpers such as `sdCircle` and `softFill`.

## Runtime

`ShaderProgram` is immutable and readable from any thread. The expression graph and the generated strings exist at compile time.

AGSL playback is `program.newAgslInstance()`. An unchanged uniform does not call the GPU. GLES playback is `GlProgramRuntime` in `redbytefx-gl`: create it, link it, and destroy it on the thread that owns the EGL context. `destroy` unbinds the program before deleting it. `runtime.set(storageBlock, floats)` packs std430 and skips the upload when those bytes are unchanged.

## Not in this version

The compiler does not emit GLES 2.0.

## Contributing

Run `./gradlew qualityCheck` before a PR. That gate is unit tests, sample compilation, and detekt. Device GLES tests are `./gradlew :redbytefx-gl:connectedDebugAndroidTest` and are not part of `qualityCheck`.

## License

[MIT](LICENSE)
