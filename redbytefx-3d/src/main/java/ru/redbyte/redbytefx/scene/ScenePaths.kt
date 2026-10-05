package ru.redbyte.redbytefx.scene

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val TWO_PI: Float = (2.0 * PI).toFloat()
private const val MIN_LENGTH: Float = 1.0e-5f

/**
 * Scene. Filled disc in XY, facing ±Z, with a short rim of thickness `2 * [halfZ]`.
 * [segments] is the rim tessellation (at least 8).
 */
public fun disc(radius: Float, halfZ: Float, segments: Int = 48): SceneMesh {
    require(radius > 0f && halfZ > 0f) { "disc radius and halfZ must be positive" }
    require(segments >= 8) { "disc segments must be at least 8, was $segments" }
    val writer = MeshWriter()
    val frontCenter = writer.vertex(0f, 0f, halfZ, 0f, 0f, 1f, 0.5f, 0.5f)
    val backCenter = writer.vertex(0f, 0f, -halfZ, 0f, 0f, -1f, 0.5f, 0.5f)
    val frontRim = IntArray(segments)
    val backRim = IntArray(segments)
    for (index in 0 until segments) {
        val angle = index / segments.toFloat() * TWO_PI
        val x = cos(angle) * radius
        val y = sin(angle) * radius
        val u = 0.5f + cos(angle) * 0.5f
        val v = 0.5f + sin(angle) * 0.5f
        frontRim[index] = writer.vertex(x, y, halfZ, 0f, 0f, 1f, u, v)
        backRim[index] = writer.vertex(x, y, -halfZ, 0f, 0f, -1f, u, v)
    }
    for (index in 0 until segments) {
        val next = (index + 1) % segments
        writer.tri(frontCenter, frontRim[index], frontRim[next])
        writer.tri(backCenter, backRim[next], backRim[index])
        val nx = cos((index + 0.5f) / segments * TWO_PI)
        val ny = sin((index + 0.5f) / segments * TWO_PI)
        val a = writer.vertex(cos(index / segments.toFloat() * TWO_PI) * radius, sin(index / segments.toFloat() * TWO_PI) * radius, halfZ, nx, ny, 0f, 0f, 0f)
        val b = writer.vertex(cos(next / segments.toFloat() * TWO_PI) * radius, sin(next / segments.toFloat() * TWO_PI) * radius, halfZ, nx, ny, 0f, 1f, 0f)
        val c = writer.vertex(cos(next / segments.toFloat() * TWO_PI) * radius, sin(next / segments.toFloat() * TWO_PI) * radius, -halfZ, nx, ny, 0f, 1f, 1f)
        val d = writer.vertex(cos(index / segments.toFloat() * TWO_PI) * radius, sin(index / segments.toFloat() * TWO_PI) * radius, -halfZ, nx, ny, 0f, 0f, 1f)
        writer.tri(a, d, c)
        writer.tri(a, c, b)
    }
    return writer.mesh()
}

/**
 * Scene. Prism along Z from a closed XY [outline] of at least three points.
 * [halfZ] is half the thickness. Faces point outward.
 */
public fun extrudePolygon(outline: List<Pair<Float, Float>>, halfZ: Float): SceneMesh {
    require(outline.size >= 3) { "extrudePolygon needs at least 3 points, was ${outline.size}" }
    require(halfZ > 0f) { "extrudePolygon halfZ must be positive, was $halfZ" }
    val count = outline.size
    val centerX = outline.sumOf { it.first.toDouble() }.toFloat() / count
    val centerY = outline.sumOf { it.second.toDouble() }.toFloat() / count
    val writer = MeshWriter()
    val frontCenter = writer.vertex(centerX, centerY, halfZ, 0f, 0f, 1f, 0.5f, 0.5f)
    val backCenter = writer.vertex(centerX, centerY, -halfZ, 0f, 0f, -1f, 0.5f, 0.5f)
    val front = IntArray(count)
    val back = IntArray(count)
    for (index in 0 until count) {
        val x = outline[index].first
        val y = outline[index].second
        front[index] = writer.vertex(x, y, halfZ, 0f, 0f, 1f, 0.5f, 0.5f)
        back[index] = writer.vertex(x, y, -halfZ, 0f, 0f, -1f, 0.5f, 0.5f)
    }
    for (index in 0 until count) {
        val next = (index + 1) % count
        writer.tri(frontCenter, front[index], front[next])
        writer.tri(backCenter, back[next], back[index])
        val ax = outline[index].first
        val ay = outline[index].second
        val bx = outline[next].first
        val by = outline[next].second
        var nx = by - ay
        var ny = ax - bx
        val length = sqrt(nx * nx + ny * ny).coerceAtLeast(MIN_LENGTH)
        nx /= length
        ny /= length
        val a = writer.vertex(ax, ay, halfZ, nx, ny, 0f, 0f, 0f)
        val b = writer.vertex(bx, by, halfZ, nx, ny, 0f, 1f, 0f)
        val c = writer.vertex(bx, by, -halfZ, nx, ny, 0f, 1f, 1f)
        val d = writer.vertex(ax, ay, -halfZ, nx, ny, 0f, 0f, 1f)
        writer.tri(a, d, c)
        writer.tri(a, c, b)
    }
    return writer.mesh()
}

/**
 * Scene. Tube along a closed XY [path]. The tube radius is [radius], [rings] around the
 * tangent, offset by [z] from the path plane. [path] needs at least 8 points and [rings] at least 4.
 */
public fun tubeAlong(
    path: List<Pair<Float, Float>>,
    radius: Float,
    rings: Int = 8,
    z: Float = 0f,
): SceneMesh {
    require(path.size >= 8) { "tubeAlong path needs at least 8 points, was ${path.size}" }
    require(rings >= 4) { "tubeAlong rings must be at least 4, was $rings" }
    require(radius > 0f) { "tubeAlong radius must be positive, was $radius" }
    val count = path.size
    val writer = MeshWriter()
    val ids = IntArray(count * rings)
    for (index in 0 until count) {
        val previous = path[(index - 1 + count) % count]
        val current = path[index]
        val next = path[(index + 1) % count]
        var tx = next.first - previous.first
        var ty = next.second - previous.second
        val tangent = sqrt(tx * tx + ty * ty).coerceAtLeast(MIN_LENGTH)
        tx /= tangent
        ty /= tangent
        var bx = -ty
        var by = tx
        val binormal = sqrt(bx * bx + by * by).coerceAtLeast(MIN_LENGTH)
        bx /= binormal
        by /= binormal
        for (ring in 0 until rings) {
            val angle = ring / rings.toFloat() * TWO_PI
            val cosine = cos(angle)
            val sine = sin(angle)
            val nx = bx * cosine
            val ny = by * cosine
            val nz = sine
            ids[index * rings + ring] = writer.vertex(
                current.first + nx * radius,
                current.second + ny * radius,
                z + nz * radius,
                nx,
                ny,
                nz,
                ring / rings.toFloat(),
                index / count.toFloat(),
            )
        }
    }
    for (index in 0 until count) {
        val next = (index + 1) % count
        for (ring in 0 until rings) {
            val ringNext = (ring + 1) % rings
            val a = ids[index * rings + ring]
            val b = ids[index * rings + ringNext]
            val c = ids[next * rings + ringNext]
            val d = ids[next * rings + ring]
            writer.tri(a, d, c)
            writer.tri(a, c, b)
        }
    }
    return writer.mesh()
}
