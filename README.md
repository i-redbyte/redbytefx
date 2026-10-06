**English** · [Русский](README.ru.md)

# RedByteFX

**RedByteFX** is a Kotlin DSL with two surfaces on one typed shader algebra. An **effect** is a fragment shader: Android AGSL on API 33+, or GLES from API 24, both on the short `redbyteFx` path. A **scene** is a mesh on OpenGL ES from API 24: vertex and fragment stages, buffers, textures, and draws. The same algebra also compiles to OpenGL ES 3.1 compute and OpenGL ES 3.2 geometry and tessellation.

Authoring is Kotlin, not a shader string. The compiler emits the text the platform actually runs:

`shader(target) { ... } -> ShaderProgram -> AGSL RuntimeShader, a GLES 3.0 program, a GLES 3.1 compute program, or a GLES 3.2 program`

**Platform:** library `minSdk` is **24**. **AGSL** (`ShaderTarget.Agsl`, `rememberFxController`, `Modifier.redbyteFx`) needs **API 33+** (`RuntimeShader`); below that, runtime calls throw `AgslNotSupportedException` and Android Studio warns via `@RequiresApi`. **OpenGL ES** works from API 24 through `redbytefx-gl` and `redbytefx-gl-compose`: scenes use `GlSurface`; fragment-only effects use GLES `redbyteFx`. GLES output is GLSL ES 3.00, 3.10 compute, or 3.20 with geometry and tessellation. API reference: [GitHub Pages](https://i-redbyte.github.io/redbytefx/).

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

The sine of `x` moves the sampling point up and down. `frequency` sets how often the wave repeats; `amplitude` sets its height.

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

Each vertex is multiplied by the view and projection matrices to put it on screen. The fragment stage gives every visible pixel the same orange color; this short snippet does not calculate lighting yet.

A GLES **scene** needs both stages. Varyings written in the vertex stage are read in the fragment stage. Inspect them with `vertexSource()` and `fragmentSource()`.

A GLES **effect** may omit `vertex { }`. The compiler injects a fullscreen triangle whose attribute is spelled `a_corner` (the same layout as `screenMesh`). Pair that program with GLES `redbyteFx`. Geometry or tessellation still needs an explicit vertex stage.

```kotlin
val pulse = shader(ShaderTarget.Gles30) {
    val time = uniformTime()
    fragment {
        val wave = sin(time.expr)
        vec4(0.5f.lit + wave * 0.5f.lit, 0.15f.lit, 0.85f.lit, 1f.lit)
    }
}
```

`sin(time)` moves between −1 and 1; multiplying by 0.5 and adding 0.5 turns it into a red-channel value between 0 and 1.

OpenGL ES 3.1 is a compute program. It has no vertex or fragment stage. Each `storageBlock` is std430, and `compute(localSizeX)` writes its fields. Several storage blocks are legal; the binding point is the declaration order starting at 0, and one block stays at 0. `compute(localSizeX, localSizeY, localSizeZ)` spells all three local sizes; `compute(n)` still spells only `local_size_x`. Inspect the text with `computeSource()`. The EGL context for that program is OpenGL ES 3.1. Inside compute, `globalId`, `localId`, and `workGroupId` are `ivec3` values spelled from `gl_GlobalInvocationID`, `gl_LocalInvocationID`, and `gl_WorkGroupID`. A storage block can also hold `floatArray`, `vec2Array`, `vec3Array`, and `vec4Array`. A sized array keeps its count. The last field may omit the count and is spelled `type name[]`; an unsized field anywhere else is rejected when the block is finished. Index an element with an `int` expression, such as `values[globalId.x]`, then `store` it. `packStd430` takes logical components, so a `vec3` contributes three floats and the packer inserts the std430 zero padding that makes the element stride 16 bytes. `GlProgramRuntime.dispatch(x, y, z)` runs the linked compute program. `shared` arrays and `barrier()` are legal only in compute. A `uniformBlock` can be read from compute; AGSL still rejects a uniform block. Several uniform blocks are legal on one program. The binding point is the declaration order. GLSL ES 3.10 and 3.20 spell `layout(std140, binding = N)`. GLSL ES 3.00 spells `layout(std140)` and the runtime sets the binding, because that language rejects `binding`. Its fields may be sized `floatArray`, `vec2Array`, `vec3Array`, `vec4Array`, `mat2`, `mat3`, and `mat4`. `packStd140` and `unpackStd140` use logical floats and skip std140 padding. An unsized array stays on `storageBlock`.

```kotlin
val cells = shader(ShaderTarget.Gles31) {
    storageBlock("cells") {
        val value = vec4("value")
        compute(64) { value.store(value) }
    }
}
```

This minimal compute example writes each value back unchanged. It demonstrates storage access without applying a mathematical transformation.

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

The tessellation levels are all 1, so this patch is not subdivided. The later stages pass through one input position; the fragment stage paints it red.

A varying written by the vertex and read by the fragment is declared on every stage between them: vertex, tessellation control, tessellation evaluation, geometry, then fragment. Missing stages are skipped. At each boundary the previous stage's `out` matches the next stage's `in` by name, type, and precision. When geometry or tessellation is present, those declarations are members of one interface block named `rb_pipe`. Every stage of that program lists the same members: varyings the vertex wrote and that the fragment or an intermediate stage reads, in declaration order. A varying that is declared and never written stays out of the block. Tessellation control copies `tc_in[gl_InvocationID]` to `tc_out[gl_InvocationID]`. Geometry copies the input vertex just written to `gl_Position`: one `gl_in[k]` in that position, including a `repeat` index, becomes `gs_in[k]` immediately before that `EmitVertex`. The same copy applies when that `gl_Position` was written before the `repeat` or `whenTrue` that emits, and it is not reused after an emit that already consumed it. Several indices, or a position that does not read `gl_in` while the varying is still unwritten, is an error whose message asks for `varying.set` before that emit. A `varying.set` between that `gl_Position` and the emit replaces the copy of that varying for that emit only. Tessellation evaluation interpolates with `gl_TessCoord` when the stage does not write the varying. One input vertex is copied from `te_in[0]`. Triangles weight the three patch vertices by `gl_TessCoord`. Isolines `mix` the two vertices by `gl_TessCoord.x`. Quads use a bilinear `mix` of corners `(0,0)`, `(1,0)`, `(1,1)`, and `(0,1)` by `gl_TessCoord.xy`. An explicit write replaces that interpolation. A stage that has `rb_pipe` and writes `gl_Position` redeclares `gl_PerVertex` with only `vec4 gl_Position` before the block. A uniform read in geometry or tessellation is declared in that stage. `glIn(index)` and `varying.at(index)` accept dynamic `int` expressions; only compile-time constant indices are bounds-checked against the input patch.

`Modifier.redbyteFx` applies an AGSL `RenderEffect`. It does not run a GLES program. GLES `redbyteFx` is a different symbol in `ru.redbyte.redbytefx.gl.compose`: import one per file. The sample app opens AGSL examples and OpenGL examples from separate screens. Each OpenGL scene is a `GLSurfaceView` that links a `ShaderProgram`. The OpenGL list covers a fragment-only **Effect** (`redbyteFx`, stdlib SDF), buffers and indexed draws, textures and mipmaps, lighting, instancing, render-to-texture, a **Lit mesh** scene (`litTexturedMesh`, `setLitModel`), geometry, tessellation, and small games. **Planet** can use a gallery photo (runtime read permission on API 28 and below; system photo picker on API 33+). The sample UI is English or Russian from the device locale. Geometry and tessellation scenes need an OpenGL ES 3.2 context.

## Math behind the sample examples

Shaders work with coordinates: a pixel has a position, and a mesh has vertices. **UV** means a position scaled to roughly 0–1 across the image. Sampling reads the color at a UV position. A **mask** is a number from 0 to 1 that decides where an effect appears; `mix` uses such a number to blend two colors. A **normal** points away from a surface and helps calculate lighting. Many examples animate a value by adding time to a sine wave.

### AGSL effects

| Example | What the math does |
| --- | --- |
| Flip | Replaces a sampling coordinate with its distance from the opposite edge (`width - x` or `height - y`) to turn the image over. |
| Mirror | Reflects coordinates on one side of the center, so both halves sample the same half of the image. |
| Rotate | Moves coordinates around the center with sine and cosine, then samples the rotated position. |
| Scale | Measures each coordinate from the center and divides that distance by the scale before sampling. |
| Offset | Adds a two-dimensional offset to the sampling position, shifting the picture. |
| Wave | Adds a sine wave to the vertical sampling coordinate; nearby columns shift by different amounts. |
| Pulse | Rounds UVs onto a pixel grid and uses time, rows, and a moving column to light selected cells. |
| Signal | Repeats coordinates into a grid; thresholds and smooth edges turn parts of it into scan lines. |
| Posterize | Rounds colors to fewer levels, then blends that result with the original image. |
| Film | Adds time-varying grain and darkens pixels near the edges with a vignette mask. |
| Grade | Changes color strength and blends tinted versions of the original with standard color blend formulas. |
| Warp | Uses layered noise to displace UVs, then samples the image at those bent coordinates. |
| Prism | Samples color channels at slightly different positions and adds a repeating color palette. |
| Spotlight | Measures distance from a chosen center; soft shape masks keep the center bright and the outside dim. |
| Beacon | Moves a spotlight back and forth with time; easing slows it near the ends of its path. |
| Composite | Uses masks as blend weights to combine the source with other colors or layers. |
| Frame | Measures distance to the image edges and lights a narrow band to draw a frame. |
| Corner | Combines small masks near the corners with a moving sweep to draw HUD brackets. |
| Reveal | Compares pixel position with an animated cutoff; a soft boundary gradually shows a recolored version of the image. |
| Sweep | Projects position along a chosen direction and makes a soft band that travels across the image. |
| Glitch | Shifts selected horizontal bands and adds signal-like stripes to mimic a broken display. |
| Radar | Converts position around a center to distance and angle, then draws arcs and a rotating scan sector. |
| Halo | Measures distance from the center in aspect-corrected coordinates to brighten a ring and central glow. |
| Circuit | Measures distance to line segments and circles; timed pulses travel along the chosen paths. |
| Sigil | Uses signed distance to circles and boxes: negative is inside, and values near zero make soft outlines. |
| Duotone | Computes brightness from the source color and uses it to blend between two chosen colors. |
| Aurora | Layers a ring, a rotating angular sweep, a changing palette, and slightly separated color samples. |
| Liquid Glass | Warps sampling coordinates for a flowing refraction effect, separates color channels at the edge, and brightens a rim. |
| Animated Gradient | Uses sine waves over UV and time to change the red, green, and blue channels smoothly. |
| Physics Bubble | Compose moves the bubble with drag and spring motion; the shader bends the background and colors the rim like a thin film. |
| Touch Ripple | Uses distance from the touch point and elapsed time to draw expanding colored rings over the image. |
| Metaballs | Computes distance to three moving circles and smoothly joins their fields so they merge into one blob. |
| CRT Terminal | Curves sampling coordinates, offsets red and blue near the edge, and modulates brightness in thin scan lines. |

### OpenGL ES scenes

| Example | What the math does |
| --- | --- |
| Triangle | Sends three vertex positions to the screen; pixels inside their triangle receive a color. |
| Effect | Draws a full-screen triangle; distance to a rotating hexagon gives it a soft edge, and time changes its color. |
| Spheres | Updates each ball's position and velocity; boundary and ball collisions change its direction. Lighting uses the direction of each sphere's surface. |
| Flag | Adds time-based sine waves to cloth vertices; their height and position set the folds. |
| Neon floor | Uses perspective so distant grid cells shrink; repeated coordinates draw lines and time scrolls them toward the camera. |
| Lamp | Rotates vertices and surface normals; the angle between a normal and the light controls brightness. |
| City | Places textured blocks in 3D and moves the camera around them with sine and cosine. |
| Lit crate | View and projection matrices place a textured box in the scene; the normal–light angle brightens faces turned toward the light. |
| Planet | Sphere coordinates place the surface and orbiting moons; light direction, color mixing, and drag control their appearance. |
| Slice | Uses one indexed mesh but draws only the first part of its index list, so a slider reveals more squares. |
| Stamp | Converts a touch position to texture coordinates and changes a small rectangle of texture pixels. |
| Sky | Uses a direction from the sphere to choose which of six cube-map faces supplies a color. |
| Mirror | Renders a triangle into a texture, then maps that texture onto a second rectangle. |
| Mips | Shows a checker texture with and without smaller precomputed copies; the smaller copies smooth distant detail. |
| Glass orb | Solves where a viewing ray meets a sphere; the rim grows brighter when the surface faces away from the viewer. |
| Iso bands | Adds moving sine waves into one field and compares it with thresholds to make colored bands. |
| Palette | Maps height and time to a repeating rainbow; sine and cosine position the 3D arch. |
| Hedgehog | Finds each triangle's center and outward normal, then adds a point along that normal to make a spike. |
| Ocean | Interpolates positions inside a patch, then adds two sine waves to raise and lower its surface. |
| Wireframe | Turns each triangle edge into a thin strip, making mesh edges visible. |
| Electric sea | Combines thin sine-shaped lightning paths with a grid of flickering stars. |
| red_byte | Updates letter positions over time: they fall, then the completed word changes color and rolls sideways. |
| Tunnel | Perspective makes corridor rings approach; comparing the ship's position with a ring's opening detects a clean pass. |
| Maze | Updates the ball from board tilt and checks it against box walls; lighting makes the ball look round. |
| Breakout | Changes the ball's direction when it touches walls, the paddle, or a brick; hit bricks disappear. |
| Raid | Moves the camera forward and tests shots against enemies at different depths; hits add a brief flash. |
| Descent | Perspective places gates along the slope; comparing skier and gate positions decides whether a pass counts. |
| Lit mesh | Rotates a textured sphere and uses the normal–light angle for diffuse brightness. |

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
| `redbytefx-gl-compose` | `GlSurface`, `GlController`, `GlFrame`, mesh helpers, and GLES `redbyteFx` for a fragment-only effect |
| `redbytefx-compose` | `rememberFxController`, `FxController`, `Modifier.redbyteFx` for AGSL |
| `redbytefx-stdlib` | Effect recipes: coordinates, masks, compositing, 2D/3D SDF, lighting, color, noise |
| `redbytefx-3d` | CPU scene data for meshes and cameras (`lookAt`, `perspective`, `ortho`). No OpenGL driver |

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

`rememberFxController` owns one AGSL runtime. Use one controller per render target. `Modifier.redbyteFx` writes `uResolution` from the draw size. `ShaderProgram.resolution` is always bound on AGSL. `RuntimeShader` is touched on the UI thread.

A GLES effect uses the same fragment recipe from API 24. Omit `vertex { }`; the compiler injects a fullscreen triangle. `ru.redbyte.redbytefx.gl.compose.redbyteFx` draws it and writes `resolution`. It does not sample Compose content (`sample()` and `sampleUv()` stay AGSL-only).

```kotlin
val pulse = shader(ShaderTarget.Gles30) {
    val time = uniformTime()
    fragment {
        val uv = aspectCenteredUv(normalizedUv(), resolution)
        val fill = softFill(sdHexagon(uv, 0.3f))
        vec4(fill, fill, fill, 1f.lit)
    }
}

@Composable
fun Pulse(program: ShaderProgram, time: HighFloatUniform) {
    val fx = rememberGlController(program)
    fx.bindTime(time)
    redbyteFx(fx, Modifier.fillMaxSize())
}
```

## Authoring

Stages are a small state machine. Uniforms, samplers, and varyings are declared on the program. `fragment { }` and `vertex { }` emit shader code. `vertex` exists for `ShaderTarget.Gles30` and `ShaderTarget.Gles32`. `geometry`, `tessControl`, and `tessEval` exist for `ShaderTarget.Gles32`. `compute` exists for `ShaderTarget.Gles31`.

- `fragCoord` is a fragment input in pixels, including inside a fragment `fn`. AGSL spells `fragCoord`. GLES spells `gl_FragCoord.xy`. `resolution` is `uResolution` on AGSL and GLES; Compose writes it from the draw size. The name `uResolution` is reserved: declare `resolution` by reading `FragmentDsl.resolution`, not with `uniform("uResolution", …)`.
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

Scalar floats of one precision support `+`, `-`, `*`, `/`. Vectors support vector-vector and vector-scalar math of the same precision. `mat2`, `mat3`, and `mat4` multiply a matrix of the same size or a vector with that many lanes; both spell `*`. Float and int scalars compare with `gt`, `lt`, `ge`, `le`, `eq`, and `ne`. `gte` is another spelling of `ge`. `ifElse(condition, ifTrue, ifFalse)` is the ternary. `choose(selector) { on(id) { value }; otherwise { value } }` nests that ternary for a float selector. Boolean scalars support `and`, `or`, and `not`.

Math that spells the same call in both languages includes `sin`, `cos`, `tan`, `sign`, `abs`, `floor`, `ceil`, `fract`, `sqrt`, `min`, `max`, `mod`, `pow`, `mix`, `clamp`, `smoothstep`, `step`, `saturate`, `dot`, `length`, `reflect`, `transpose`, and `distance` for `vec2` and `vec3`. `sqrt` is the GLSL call and does not replace a negative argument.

`redbytefx-stdlib` adds coordinate and compositing recipes on the same `fragment` receiver: `normalizedUv`, `sampleUv` (AGSL only), `centeredUv`, `aspectCenteredUv`, `rotate2d`, masks, reveals, blend helpers, and SDF helpers such as `sdCircle`, `sdHexagon`, `sdRhombus`, `sdEquilateralTriangle`, and `softFill`. 3D distances include `sdCylinder` and `sdOctahedron`; lighting/color extras include `fresnel`, `phong`, `hueShift`, and `filmicTonemap`. `softFill` and `stroke` still take the pen width from the caller. `softFillScreen(distance)` and `strokeScreen(distance, width)` take the edge width from `fwidth(distance)` and are legal only in a fragment.

## Runtime

`ShaderProgram` is immutable and readable from any thread. The expression graph and the generated strings exist at compile time.

AGSL playback is `program.newAgslInstance()`. An unchanged uniform does not call the GPU. `set` writes highp and mediump floats and vectors, and `uniformInt` through `setIntUniform`. GLES playback is `GlProgramRuntime` in `redbytefx-gl`: create it, link it, and destroy it on the thread that owns the EGL context. `destroy` unbinds the program before deleting it. `set` writes those floats, `uniform1i` for `uniformInt` and for `uniformBool` as 0 or 1, and column-major `uniformMatrix*fv` for `mat2`, `mat3`, and `mat4` with transpose false. Link uploads the defaults, including an identity matrix when none was passed, and the same value does not call `glUniform` again. `bind` of `sampler2D` uses `GL_TEXTURE_2D`; `bind` of `samplerCube` uses `GL_TEXTURE_CUBE_MAP`. Both share `GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS`. `dispatch(x, y, z)` runs a linked GLES 3.1 program and then calls `glMemoryBarrier` with `GL_SHADER_STORAGE_BARRIER_BIT`. `runtime.set(storageBlock, floats)` packs logical std430 values and skips the upload when those bytes are unchanged. `runtime.read(storageBlock, into)` copies those logical floats back, without std430 padding, after a `GL_BUFFER_UPDATE_BARRIER_BIT` so a compute write is visible. `set` and `read` take that block. Several uniform or storage blocks bind in declaration order from 0, and a block from another shader is rejected. A binding at or above `GL_MAX_UNIFORM_BUFFER_BINDINGS` or `GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS` is rejected when the program is linked. `GlController.set` of a uniform or storage block keeps the latest floats and writes them again when the EGL context is recreated. `dispatch` and `read` are queued on the GL thread; `read` returns immediately and fills the caller's buffer from that queue. `GlCompute` hosts a GLES 3.1 program with no mesh and no draw. Link runs on the GL thread. `GlSurface` is unchanged for ordinary scenes. `readFramebuffer` and `readColorTarget` copy RGBA8 into a buffer the caller allocated. The byte count is checked before `glReadPixels`. Reading a color target leaves the previously bound framebuffer bound. A draw may pass a `GlPipeline`: blend factor and equation, scissor, color mask, depth mask, and `cullFace`. The default draw still has blend, scissor, and culling off, writes every color channel, and writes depth. The depth test is unchanged. The next draw replaces that state. `draw(mesh, model, material)` copies one model matrix into the instance attributes and writes `material` on `GlFrame.material` just before that draw. Extra meshes skip a re-upload when the vertex and index array references are unchanged. Scene helpers include `identity`, `multiply`, `translation`, `rotationX`/`Y`/`Z`, `scale`, `disc`, `extrudePolygon`, `tubeAlong`, `transform`, `merge`, `tagUv`, and `instanceModel()`. A default draw does not call `glGetError`. `strictErrors` on `GlProgramRuntime` and `GlSurfaceConfig` turns a driver error after draw, dispatch, texture upload, and read into `GlException` with `GlCode.DriverError`. A strict link still drains errors after link. The `set` upload fixes the byte size, including an unsized tail. A later set whose packed size differs allocates the buffer again. `StorageBlock.byteSize` is the std430 size of a fully sized block. A block with an unsized array reports that size through `byteSize(valueCount)`, which matches `packStd430`. `uniformMedium`, `uniformMediumVec2`, `uniformMediumVec3`, and `uniformMediumVec4` declare the mediump forms; on AGSL they still go through `setFloatUniform`. `FxController` binds float uniforms and `uniformInt` for Compose.

## Not in this version

The compiler does not emit GLES 2.0 or desktop GL. It does not emit `while`, recursion, nested functions, or a loop bound that is not a constant from 1 to 64. `uniformBool`, `uniformMat2`, `uniformMat3`, `uniformMat4`, and `samplerCube` are rejected on AGSL.

## Releases

Published artifacts: `io.github.i-redbyte:redbytefx-*`. Version **1.1.0** is defined in [gradle.properties](gradle.properties). See [docs/changelog.md](docs/changelog.md) ([Русский](docs/changelog.ru.md)).

To publish to Maven Central (maintainers):

```bash
./gradlew publishToMavenCentral
```

Requires Sonatype Central Portal credentials (`mavenCentralUsername`, `mavenCentralPassword`) and a configured GPG signing key (`signingInMemoryKey` or `signing.gnupg.*` in Gradle properties). The build fails fast if either is missing.

## Contributing

Run `./gradlew qualityCheck` before a PR. That gate is unit tests, sample compilation, and detekt with JetBrains official Kotlin style (`kotlin.code.style=official`, `detekt-formatting`). Reformat with `./gradlew detekt -PdetektAutoCorrect=true`. A pre-commit hook in `githooks/` runs `./gradlew detekt` on every commit (installed into `.git/hooks` by Gradle, or `./gradlew installGitHooks`). Device GLES tests are `./gradlew :redbytefx-gl:connectedDebugAndroidTest` and are not part of `qualityCheck`.

API site locally: `./gradlew dokkaHtmlSite` (`build/docs/site/index.html`). CI publishes to `https://i-redbyte.github.io/redbytefx/` on push to **`master`/`main`**; other branches only verify the build. See [docs/github-pages.md](docs/github-pages.md).

## License

[MIT](LICENSE)
