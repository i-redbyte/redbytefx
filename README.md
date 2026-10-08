**English** · [Русский](README.ru.md)

# RedByteFX

**RedByteFX** lets you create visual effects and scenes for Android in Kotlin. Instead of keeping a shader in a long AGSL or GLSL string, you describe coordinates, colors, time, textures, and shapes with a typed DSL. The library turns that description into shader code for the GPU. You can inspect the generated code with `agslSource()`, `vertexSource()`, or `fragmentSource()`.

There are two paths. **Effects** change an image that is already on screen: a wave, glow, color treatment, or transition. Use AGSL to apply one to Android content, or OpenGL ES to draw a full-screen effect. **Scenes** draw their own objects with vertices, a camera, textures, and lighting through OpenGL ES.

The math stays visible. A wave shifts the point where a pixel reads its color using a sine function; a soft glow can start with the distance to a chosen point. The optional standard library provides reusable masks, gradients, noise, transforms, and color blending, and you can combine them with your own formulas.

AGSL effects require **Android 13 / API 33**. OpenGL ES scenes run from **Android 7 / API 24**. The library's `minSdk` is 24.

<table>
  <tr>
    <td align="center"><img src="docs/media/aurora.gif" width="280" alt="Aurora"><br>Aurora</td>
    <td align="center"><img src="docs/media/liquid-glass.gif" width="280" alt="Liquid Glass"><br>Liquid Glass</td>
    <td align="center"><img src="docs/media/metaballs.gif" width="280" alt="Metaballs"><br>Metaballs</td>
  </tr>
  <tr>
    <td align="center"><img src="docs/media/crt.gif" width="280" alt="CRT terminal"><br>CRT</td>
    <td align="center"><img src="docs/media/gradient.gif" width="280" alt="Animated gradient"><br>Gradient</td>
    <td align="center"><img src="docs/media/radar.gif" width="280" alt="Radar"><br>Radar</td>
  </tr>
</table>

These clips come from the [sample app](sample/). You can also [download the sample APK](https://github.com/i-redbyte/redbytefx/releases/download/v1.1.0/redbytefx-sample-1.1.0.apk).

## Add it to a new project

Make sure your project uses `mavenCentral()`. Add the modules you need to your app's `build.gradle.kts` (current version: **1.1.0**):

```kotlin
dependencies {
    implementation("io.github.i-redbyte:redbytefx-core:1.1.0")
    implementation("io.github.i-redbyte:redbytefx-compose:1.1.0")    // AGSL in Compose
    implementation("io.github.i-redbyte:redbytefx-gl:1.1.0")         // OpenGL ES runtime
    implementation("io.github.i-redbyte:redbytefx-gl-compose:1.1.0") // OpenGL ES in Compose
    implementation("io.github.i-redbyte:redbytefx-stdlib:1.1.0")     // Optional effect helpers
}
```

For an AGSL effect on a regular Android `View`, `redbytefx-core` is enough. For an AGSL effect in Compose, add `redbytefx-compose`. For an OpenGL scene, add `redbytefx-gl` and, when using the ready-made Compose surface, `redbytefx-gl-compose`. `redbytefx-3d` is optional if you want mesh and camera helpers.

## Your first AGSL effect

This effect shifts the point where each pixel reads its color. The sine wave makes neighboring columns move by different amounts. The same `ShaderProgram` works with Compose and a regular Android `View`.

```kotlin
import ru.redbyte.redbytefx.*

fun waveProgram(): ShaderProgram = shader(ShaderTarget.Agsl) {
    fragment {
        val offset = float2(0f, sin(fragCoord.x * 0.08f) * 12f)
        sample(fragCoord + offset)
    }
}
```

**Compose screen:** remember the program and apply the effect to a composable.

```kotlin
@RequiresApi(33)
@Composable
fun WaveText() {
    val program = remember { waveProgram() }
    val controller = rememberFxController(program)
    Text("RedByteFX", modifier = Modifier.redbyteFx(controller))
}
```

Use `setContent { WaveText() }` from your Activity on API 33+. Imports for this snippet: `androidx.annotation.RequiresApi`, `androidx.compose.runtime.*`, `androidx.compose.material3.Text`, `androidx.compose.ui.Modifier`, and `ru.redbyte.redbytefx.compose.*`.

**View screen:** attach an AGSL instance to a `TextView`. Update its resolution when the view size changes; the input image is the view's own drawing.

```kotlin
@RequiresApi(33)
fun waveTextView(context: Context): TextView {
    val instance = waveProgram().newAgslInstance()
    return TextView(context).apply {
        text = "RedByteFX"
        addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            instance.setResolution(view.width.toFloat(), view.height.toFloat())
            view.setRenderEffect(instance.renderEffect())
        }
    }
}
```

Use `setContentView(waveTextView(this))` from an Activity on API 33+. Imports: `android.content.Context`, `android.widget.TextView`, `androidx.annotation.RequiresApi`, and `ru.redbyte.redbytefx.*`. If you change a uniform later, call `setRenderEffect(instance.renderEffect())` again after the change.

For a longer, beginner-friendly AGSL walkthrough, see [“Маяк в пустыне: Kotlin DSL для Android-шейдеров”](https://habr.com/ru/articles/1022546/) (in Russian). It uses an earlier version of the API, so use the snippets here for version 1.1.0.

## Your first OpenGL ES scene

Here three vertices make a triangle. The vertex stage places them on screen; the fragment stage colors the pixels inside it. Unlike the AGSL effect, this scene draws its own image.

```kotlin
import ru.redbyte.redbytefx.*

fun triangleProgram(): ShaderProgram = shader(ShaderTarget.Gles30) {
    vertex {
        val position = attributeVec2("position")
        glPosition(vec4(position.x, position.y, 0f.lit, 1f.lit))
    }
    fragment { vec4(0.85f.lit, 0.35f.lit, 0.2f.lit, 1f.lit) }
}
```

**Compose screen:** `GlSurface` creates the OpenGL view, uploads the vertices, and draws the scene. Vertex positions range from −1 to 1 across the screen.

```kotlin
@Composable
fun TriangleScene() {
    val program = remember { triangleProgram() }
    val mesh = remember {
        GlMesh(
            vertices = floatArrayOf(-0.6f, -0.5f, 0.6f, -0.5f, 0f, 0.7f),
            stride = 2,
            attribs = listOf(GlAttrib("a_position", 2, 0)),
        )
    }
    GlSurface(
        controller = rememberGlController(program),
        mesh = mesh,
        modifier = Modifier.fillMaxSize(),
        onFrame = {},
    )
}
```

Use `setContent { TriangleScene() }` from your Activity. Imports: `androidx.compose.runtime.*`, `androidx.compose.foundation.layout.fillMaxSize`, `androidx.compose.ui.Modifier`, and `ru.redbyte.redbytefx.gl.compose.*`.

**View-based screen:** place the same scene in a `ComposeView`. This keeps the rest of your Activity or Fragment in the View system while `GlSurface` manages the OpenGL context and drawing.

```kotlin
val triangleView = ComposeView(this).apply {
    setContent { TriangleScene() }
}
setContentView(triangleView)
```

Import `androidx.compose.ui.platform.ComposeView`. A `ComponentActivity` supplies the lifecycle needed by `GlSurface`. For a pure `GLSurfaceView.Renderer`, `redbytefx-gl` also exposes `GlProgramRuntime`; that route requires you to manage the EGL context and vertex buffers yourself.

## Explore the examples

The [sample app](sample/) has AGSL effects and OpenGL ES scenes. Start with these, then try the full [AGSL catalog](sample/src/main/java/ru/redbyte/redbytefx/sample/model/Demo.kt) and [OpenGL catalog](sample/src/main/java/ru/redbyte/redbytefx/sample/ui/gl/GlExampleList.kt):

| Example | The idea behind the picture |
| --- | --- |
| Wave | A sine wave shifts the point where each pixel reads its color. |
| Aurora | A moving light ring, a rotating sweep, and small offsets between color channels create iridescence. |
| Liquid Glass | Bent sampling coordinates and a bright rim make the content look like moving glass. |
| Metaballs | Distances to moving circles are blended so nearby circles merge into one shape. |
| CRT Terminal | Curved coordinates, scan lines, and separated colors imitate an old display. |
| Triangle (OpenGL ES) | Three vertices define an area; the GPU fills its pixels with color. |
| Lamp (OpenGL ES) | A surface is brighter when its normal points toward the light. |
| Ocean (OpenGL ES 3.2) | A patch is split into smaller pieces, and sine waves raise its vertices. |

UV means a position on an image, usually from 0 to 1 in each direction. A *mask* is a number from 0 to 1 that says where an effect appears. A *normal* points away from a surface and helps calculate lighting.

<details>
<summary>All 61 sample examples, one line each</summary>

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

</details>

## Documentation

- [API reference](https://i-redbyte.github.io/redbytefx/) - public classes and functions.
- [Shader language reference](docs/language-reference.md) - expressions, types, uniforms, and stages.
- [Changelog](docs/changelog.md) - releases and changes.

## License

[MIT](LICENSE)

## Contributing

Want to try an effect, improve an example, or fix a rough edge? Issues and pull requests are welcome, including reports from real devices. Before opening a pull request, run `./gradlew qualityCheck` to run unit tests, compile the sample, and check Kotlin style. If you are new to shaders, a small clear example is a great contribution.
