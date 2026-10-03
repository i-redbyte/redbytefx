package ru.redbyte.redbytefx.sample.ui.gl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.Mat4
import ru.redbyte.redbytefx.Sampler2D
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.Vec3
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.gl.compose.GL_LINK_FALLBACK
import ru.redbyte.redbytefx.gl.compose.GlFrame
import ru.redbyte.redbytefx.gl.compose.GlLinkState
import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.gl.compose.GlSurface
import ru.redbyte.redbytefx.gl.compose.GlSurfaceConfig
import ru.redbyte.redbytefx.gl.compose.rememberGlController
import ru.redbyte.redbytefx.gl.compose.sphere
import ru.redbyte.redbytefx.gl.compose.torus
import ru.redbyte.redbytefx.gt
import ru.redbyte.redbytefx.ifElse
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.mix
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.sample.ui.CyberPanel
import ru.redbyte.redbytefx.sample.ui.say
import ru.redbyte.redbytefx.saturate
import ru.redbyte.redbytefx.scene.lookAt
import ru.redbyte.redbytefx.scene.perspective
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.stdlib.lambert
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.vec3
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.w
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

internal const val PLANET_MOON_LIMIT: Int = 5

internal const val PLANET_COLOR: Int = 0

internal const val PLANET_GRADIENT: Int = 1

internal const val PLANET_PHOTO: Int = 2

internal const val PLANET_BODY: Float = 0f

internal const val PLANET_MOON: Float = 1f

internal const val PLANET_RING: Float = 2f

internal const val PLANET_MOON_SCALE: Float = 0.2f

private const val PLANET_TAP_TRAVEL: Float = 36f

private const val PLANET_TAP_RADIUS: Float = 0.34f

private const val PLANET_PHOTO_EDGE: Int = 256

private val planetWhite = byteArrayOf(-1, -1, -1, -1)

internal class PlanetScene(
    val program: ShaderProgram,
    val mesh: GlMesh,
    val rings: GlMesh,
    val view: Uniform<Mat4>,
    val projection: Uniform<Mat4>,
    val light: Uniform<Vec3<Flt<High>>>,
    val heat: Uniform<Flt<High>>,
    val mode: Uniform<Flt<High>>,
    val albedo: Uniform<Sampler2D>,
)

internal class PlanetBody(
    val x: Float,
    val y: Float,
    val z: Float,
    val scale: Float,
    val kind: Float,
)

internal class PlanetOrbit(
    val radius: Float,
    val tilt: Float,
    val spin: Float,
    val speed: Float,
)

internal class PlanetPhoto(
    val width: Int,
    val height: Int,
    val rgba: ByteArray,
    val generation: Int,
)

internal class PlanetRig {
    val camera = CameraMatrices()

    @Volatile var px: Float = 0f

    @Volatile var py: Float = 0f

    @Volatile var photo: PlanetPhoto? = null

    val moons = AtomicInteger(3)

    val mode = AtomicInteger(PLANET_COLOR)

    var yaw: Float = 0.35f

    var pitch: Float = 0.15f

    var heat: Float = 0.12f

    private val ticks = AtomicInteger(0)

    private val anchors = AtomicInteger(0)

    private var seenSerial: Int = -1

    private var seenAnchor: Int = 0

    private var lastX: Float = 0f

    private var lastY: Float = 0f

    private var lastSeconds: Float = 0f

    var boundRuntime: GlProgramRuntime? = null

    var photoTexture: Int = 0

    var fallback: Int = 0

    var uploadedGeneration: Int = -1

    fun begin(x: Float, y: Float) {
        px = x
        py = y
        anchors.incrementAndGet()
    }

    fun mark(x: Float, y: Float) {
        px = x
        py = y
        ticks.incrementAndGet()
    }

    fun advance(seconds: Float) {
        val dt = if (lastSeconds == 0f) 0f else (seconds - lastSeconds).coerceIn(0f, 0.05f)
        lastSeconds = seconds
        val anchorStamp = anchors.get()
        val stamp = ticks.get()
        var drag = 0f
        if (seenSerial < 0 || anchorStamp != seenAnchor) {
            lastX = px
            lastY = py
            seenSerial = stamp
            seenAnchor = anchorStamp
        } else if (stamp != seenSerial) {
            val dx = px - lastX
            val dy = py - lastY
            yaw += dx * 2.4f
            pitch = (pitch - dy * 1.1f).coerceIn(-0.85f, 1.1f)
            drag = abs(dx) + abs(dy)
            lastX = px
            lastY = py
            seenSerial = stamp
        }
        heat = nextPlanetHeat(heat, drag, dt)
    }

    fun syncTextures(runtime: GlProgramRuntime, albedo: Uniform<Sampler2D>) {
        if (boundRuntime !== runtime) {
            boundRuntime = runtime
            photoTexture = 0
            uploadedGeneration = -1
            fallback = runtime.uploadRgba(1, 1, planetWhite)
        }
        val latest = photo
        if (latest != null && latest.generation != uploadedGeneration) {
            if (photoTexture != 0) runtime.deleteTexture(photoTexture)
            photoTexture = runtime.uploadRgba(latest.width, latest.height, latest.rgba)
            uploadedGeneration = latest.generation
        }
        val name = if (mode.get() == PLANET_PHOTO && photoTexture != 0) photoTexture else fallback
        runtime.bind(albedo, name)
    }
}

internal fun planetScene(): PlanetScene {
    lateinit var view: Uniform<Mat4>
    lateinit var projection: Uniform<Mat4>
    lateinit var light: Uniform<Vec3<Flt<High>>>
    lateinit var heat: Uniform<Flt<High>>
    lateinit var mode: Uniform<Flt<High>>
    lateinit var albedo: Uniform<Sampler2D>
    val program = shader(ShaderTarget.Gles30) {
        view = uniformMat4("view")
        projection = uniformMat4("projection")
        light = uniformVec3("light", 0.4f, 0.8f, 0.5f)
        heat = uniform("heat", 0.1f)
        mode = uniform("mode", 0f)
        albedo = sampler2D("albedo")
        val uv = varyingVec2("uv")
        val facing = varyingVec3("normal")
        val kind = varyingFloat("kind")
        val place = varyingVec3("place")
        vertex {
            val position = attributeVec3("position")
            val normal = attributeVec3("normal")
            val texcoord = attributeVec2("uv")
            val column0 = attributeVec4("model0")
            val column1 = attributeVec4("model1")
            val column2 = attributeVec4("model2")
            val column3 = attributeVec4("model3")
            uv.set(texcoord)
            facing.set(normal)
            kind.set(column0.w)
            place.set(vec3(column3.x, column3.y, column3.z))
            val axis = vec4(column0.x, column0.y, column0.z, 0f.lit)
            val world = axis * position.x + column1 * position.y + column2 * position.z + column3
            glPosition(projection.expr * (view.expr * world))
        }
        fragment {
            val ring = kind.expr gt 1.5f
            val moon = kind.expr gt 0.5f
            val solid = vec3(0.18f.lit, 0.42f.lit, 0.82f.lit)
            val land = vec3(0.16f.lit, 0.46f.lit, 0.28f.lit)
            val ice = vec3(0.75f.lit, 0.88f.lit, 0.96f.lit)
            val gradient = mix(land, ice, uv.expr.y)
            val texel = texture(albedo, uv.expr)
            val photo = vec3(texel.x, texel.y, texel.z)
            val chosen = ifElse(mode.expr gt 1.5f, photo, ifElse(mode.expr gt 0.5f, gradient, solid))
            val moonColor = vec3(
                saturate(0.55f.lit + place.expr.y * 0.2f.lit),
                saturate(0.6f.lit + place.expr.z * 0.1f.lit),
                saturate(0.72f.lit + place.expr.x * 0.12f.lit),
            )
            val hot = vec3(1f.lit, 0.36f.lit, 0.06f.lit)
            val warmth = ifElse(moon, 0.04f.lit, heat.expr)
            val surface = mix(ifElse(moon, moonColor, chosen), hot, warmth)
            val shade = lambert(facing.expr, light.expr)
            val lift = shade + 0.18f.lit + ifElse(moon, 0f.lit, heat.expr * 0.28f.lit)
            val lit = vec3(surface.x * lift, surface.y * lift, surface.z * lift)
            val ringColor = vec3(0.45f.lit, 0.62f.lit, 0.78f.lit) * (0.55f.lit + shade * 0.35f.lit)
            val rgb = ifElse(ring, ringColor, lit)
            vec4(rgb.x, rgb.y, rgb.z, 1f.lit)
        }
    }
    return PlanetScene(
        program,
        planetMesh(),
        planetRingMesh(),
        view,
        projection,
        light,
        heat,
        mode,
        albedo,
    )
}

internal fun planetMesh(): GlMesh {
    val source = sphere(1f, 20, 32)
    return GlMesh(
        vertices = source.vertices,
        stride = source.stride,
        attribs = source.attribs,
        depth = true,
        clearR = 0.008f,
        clearG = 0.01f,
        clearB = 0.02f,
        indices = source.indices,
    )
}

internal fun planetRingMesh(): GlMesh {
    val source = torus(1f, 0.012f, 64, 8)
    return GlMesh(
        vertices = source.vertices,
        stride = source.stride,
        attribs = source.attribs,
        depth = true,
        indices = source.indices,
    )
}

internal fun planetBodies(moons: Int, seconds: Float, yaw: Float): List<PlanetBody> {
    val count = moons.coerceIn(0, PLANET_MOON_LIMIT)
    val bodies = ArrayList<PlanetBody>(count + 1)
    bodies.add(PlanetBody(0f, 0f, 0f, 1f, PLANET_BODY))
    for (index in 0 until count) {
        val place = moonPosition(index, seconds, yaw)
        bodies.add(PlanetBody(place[0], place[1], place[2], PLANET_MOON_SCALE, PLANET_MOON))
    }
    return bodies
}

internal fun planetInstances(moons: Int, seconds: Float, yaw: Float): FloatArray {
    val bodies = planetBodies(moons, seconds, yaw)
    val packed = FloatArray(bodies.size * 16)
    bodies.forEachIndexed { index, body -> writeBody(packed, index, body) }
    return packed
}

internal fun planetOrbitInstances(moons: Int, yaw: Float): FloatArray {
    val count = moons.coerceIn(0, PLANET_MOON_LIMIT)
    if (count == 0) return FloatArray(0)
    val packed = FloatArray(count * 16)
    for (index in 0 until count) writeOrbit(packed, index, planetOrbit(index, yaw))
    return packed
}

internal fun planetOrbitMatrix(index: Int, yaw: Float): FloatArray {
    val packed = FloatArray(16)
    writeOrbit(packed, 0, planetOrbit(index, yaw))
    return packed
}

internal fun planetOrbit(index: Int, yaw: Float): PlanetOrbit = PlanetOrbit(
    radius = 1.42f + index * 0.26f,
    tilt = 0.22f + index * 0.27f,
    spin = index * 0.9f + yaw * (0.22f + index * 0.11f),
    speed = 0.4f + index * 0.18f,
)

internal fun orbitPoint(orbit: PlanetOrbit, cosine: Float, sine: Float): FloatArray {
    val tiltCos = cos(orbit.tilt)
    val tiltSin = sin(orbit.tilt)
    val yawCos = cos(orbit.spin)
    val yawSin = sin(orbit.spin)
    return floatArrayOf(
        orbit.radius * (cosine * yawCos - sine * tiltCos * yawSin),
        orbit.radius * (sine * tiltSin),
        orbit.radius * (cosine * yawSin + sine * tiltCos * yawCos),
    )
}

internal fun moonPosition(index: Int, seconds: Float, yaw: Float): FloatArray {
    val orbit = planetOrbit(index, yaw)
    val phase = seconds * orbit.speed + yaw * (0.65f + index * 0.4f)
    return orbitPoint(orbit, cos(phase), sin(phase))
}

internal fun nextPlanetHeat(heat: Float, drag: Float, dt: Float): Float =
    (heat + drag * 1.8f - dt * 0.18f).coerceIn(0.04f, 1f)

internal fun planetLight(yaw: Float, pitch: Float, heat: Float): FloatArray {
    val span = 0.7f + heat * 0.45f
    return floatArrayOf(
        sin(yaw + 0.6f) * span,
        0.25f + pitch * 0.9f + heat * 0.55f,
        cos(yaw + 0.6f),
    )
}

internal fun planetEye(yaw: Float, pitch: Float): FloatArray = floatArrayOf(
    sin(yaw * 0.35f) * 0.85f,
    0.42f + pitch * 0.4f,
    3.25f,
)

internal fun planetTapped(x: Float, y: Float, travel: Float): Boolean =
    travel < PLANET_TAP_TRAVEL && x * x + y * y < PLANET_TAP_RADIUS * PLANET_TAP_RADIUS

internal fun decodePlanetPhoto(context: Context, uri: Uri, generation: Int): PlanetPhoto? {
    val decoded = openPlanetBitmap(context, uri) ?: return null
    val scaled = scalePlanetBitmap(decoded, PLANET_PHOTO_EDGE)
    val photo = PlanetPhoto(scaled.width, scaled.height, bitmapToRgba(scaled), generation)
    if (scaled !== decoded) scaled.recycle()
    decoded.recycle()
    return photo
}

@Composable
internal fun DemoPlanet() {
    val scene = remember { planetScene() }
    val rig = remember { PlanetRig() }
    val controller = rememberGlController(scene.program, GlSurfaceConfig(depth = true))
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val generation = remember { AtomicInteger(0) }
    var moons by remember { mutableIntStateOf(rig.moons.get()) }
    var mode by remember { mutableIntStateOf(rig.mode.get()) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val stamp = generation.incrementAndGet()
        scope.launch {
            val photo = withContext(Dispatchers.Default) { decodePlanetPhoto(context, uri, stamp) }
            if (photo != null && stamp == generation.get()) rig.photo = photo
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            key(scene.program) {
                GlSurface(
                    controller = controller,
                    mesh = scene.mesh,
                    modifier = Modifier
                        .fillMaxSize()
                        .planetGesture(rig) {
                            val next = (rig.mode.get() + 1) % 3
                            mode = next
                            rig.mode.set(next)
                            if (next == PLANET_PHOTO && rig.photo == null) picker.launch("image/*")
                        },
                    onFrame = { frame -> rig.render(scene, frame) },
                    overlay = { state -> PlanetLinkOverlay(state) },
                )
            }
            GlCodeCompare(
                program = scene.program,
                dsl = planetDsl,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
        PlanetBar(
            moons = moons,
            mode = mode,
            onMoons = { next ->
                moons = next
                rig.moons.set(next)
            },
            onColor = {
                mode = PLANET_COLOR
                rig.mode.set(PLANET_COLOR)
            },
            onGradient = {
                mode = PLANET_GRADIENT
                rig.mode.set(PLANET_GRADIENT)
            },
            onPhoto = {
                mode = PLANET_PHOTO
                rig.mode.set(PLANET_PHOTO)
                picker.launch("image/*")
            },
        )
    }
}

private fun PlanetRig.render(scene: PlanetScene, frame: GlFrame) {
    advance(frame.seconds)
    val eye = planetEye(yaw, pitch)
    frame.runtime.set(scene.view, lookAt(eye[0], eye[1], eye[2], 0f, 0f, 0f, 0f, 1f, 0f, camera.view))
    frame.runtime.set(scene.projection, perspective(0.9f, frame.aspect, 0.08f, 40f, camera.projection))
    val light = planetLight(yaw, pitch, heat)
    frame.runtime.set(scene.light, light[0], light[1], light[2])
    frame.runtime.set(scene.heat, heat)
    frame.runtime.set(scene.mode, mode.get().toFloat())
    syncTextures(frame.runtime, scene.albedo)
    val count = moons.get().coerceIn(0, PLANET_MOON_LIMIT)
    frame.draw(scene.mesh, instances = planetInstances(count, frame.seconds, yaw))
    val orbits = planetOrbitInstances(count, yaw)
    if (orbits.isNotEmpty()) frame.draw(scene.rings, instances = orbits)
}

@Composable
private fun PlanetBar(
    moons: Int,
    mode: Int,
    onMoons: (Int) -> Unit,
    onColor: () -> Unit,
    onGradient: () -> Unit,
    onPhoto: () -> Unit,
) {
    CyberPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            text = say("Moons $moons", "Спутники: $moons"),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Slider(
            value = moons.toFloat(),
            onValueChange = { value ->
                onMoons(value.roundToInt().coerceIn(0, PLANET_MOON_LIMIT))
            },
            valueRange = 0f..PLANET_MOON_LIMIT.toFloat(),
            steps = PLANET_MOON_LIMIT - 1,
        )
        Text(
            text = say(
                "Drag heats the planet and sends the moons onto new orbits. Tap the planet to change its surface.",
                "Вращение греет планету и уводит спутники на другие орбиты. Нажатие меняет поверхность.",
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PlanetChoice(say("Color", "Цвет"), mode == PLANET_COLOR, onColor)
            PlanetChoice(say("Gradient", "Градиент"), mode == PLANET_GRADIENT, onGradient)
            PlanetChoice(say("Photo", "Фото"), mode == PLANET_PHOTO, onPhoto)
        }
    }
}

@Composable
private fun PlanetChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            text = label,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun PlanetLinkOverlay(state: GlLinkState) {
    if (state is GlLinkState.Failed) {
        val text = if (state.message == GL_LINK_FALLBACK) {
            say(GL_LINK_FALLBACK, "Это устройство не может собрать шейдер.")
        } else {
            state.message
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

private fun Modifier.planetGesture(rig: PlanetRig, onTap: () -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        var travel = 0f
        var previousX = down.position.x
        var previousY = down.position.y
        rig.begin(planetNdcX(previousX, size.width), planetNdcY(previousY, size.height))
        down.consume()
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) {
                val x = planetNdcX(change.position.x, size.width)
                val y = planetNdcY(change.position.y, size.height)
                if (planetTapped(x, y, travel)) onTap()
                break
            }
            travel += hypot(change.position.x - previousX, change.position.y - previousY)
            previousX = change.position.x
            previousY = change.position.y
            rig.mark(planetNdcX(previousX, size.width), planetNdcY(previousY, size.height))
            change.consume()
        }
    }
}

private fun planetNdcX(x: Float, width: Int): Float = x / width.coerceAtLeast(1) * 2f - 1f

private fun planetNdcY(y: Float, height: Int): Float = 1f - y / height.coerceAtLeast(1) * 2f

private fun writeBody(out: FloatArray, index: Int, body: PlanetBody) {
    val base = index * 16
    out[base] = body.scale
    out[base + 1] = 0f
    out[base + 2] = 0f
    out[base + 3] = body.kind
    out[base + 5] = body.scale
    out[base + 10] = body.scale
    out[base + 12] = body.x
    out[base + 13] = body.y
    out[base + 14] = body.z
    out[base + 15] = 1f
}

private fun writeOrbit(out: FloatArray, slot: Int, orbit: PlanetOrbit) {
    val tiltCos = cos(orbit.tilt)
    val tiltSin = sin(orbit.tilt)
    val yawCos = cos(orbit.spin)
    val yawSin = sin(orbit.spin)
    val radius = orbit.radius
    val base = slot * 16
    out[base] = radius * yawCos
    out[base + 1] = 0f
    out[base + 2] = radius * yawSin
    out[base + 3] = PLANET_RING
    out[base + 4] = radius * tiltSin * yawSin
    out[base + 5] = radius * tiltCos
    out[base + 6] = -radius * tiltSin * yawCos
    out[base + 8] = -radius * tiltCos * yawSin
    out[base + 9] = radius * tiltSin
    out[base + 10] = radius * tiltCos * yawCos
    out[base + 15] = 1f
}

private fun openPlanetBitmap(context: Context, uri: Uri): Bitmap? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    try {
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
    } catch (_: IOException) {
        return null
    } catch (_: SecurityException) {
        return null
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    val options = BitmapFactory.Options().apply {
        inSampleSize = planetSampleSize(bounds.outWidth, bounds.outHeight, PLANET_PHOTO_EDGE)
        inPreferredConfig = Bitmap.Config.ARGB_8888
        inPremultiplied = false
    }
    return try {
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }
}

private fun planetSampleSize(width: Int, height: Int, edge: Int): Int {
    var sample = 1
    while (width / sample > edge * 2 || height / sample > edge * 2) sample *= 2
    return sample
}

private fun scalePlanetBitmap(bitmap: Bitmap, edge: Int): Bitmap {
    val largest = maxOf(bitmap.width, bitmap.height)
    if (largest <= edge) return bitmap
    val scale = edge.toFloat() / largest
    val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
    val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, width, height, true)
}

private fun bitmapToRgba(bitmap: Bitmap): ByteArray {
    val count = bitmap.width * bitmap.height
    val pixels = IntArray(count)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val rgba = ByteArray(count * 4)
    var index = 0
    for (pixel in pixels) {
        rgba[index] = ((pixel shr 16) and 0xff).toByte()
        rgba[index + 1] = ((pixel shr 8) and 0xff).toByte()
        rgba[index + 2] = (pixel and 0xff).toByte()
        rgba[index + 3] = ((pixel ushr 24) and 0xff).toByte()
        index += 4
    }
    return rgba
}
