package ru.redbyte.redbytefx.scene

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val TWO_PI: Float = (2.0 * PI).toFloat()

/**
 * Scene. Filled disc in XY, facing ±Z, with a short rim of thickness `2 * [halfZ]`.
 * [segments] is the rim tessellation (at least 8).
 */
public fun disc(radius: Float, halfZ: Float, segments: Int = 48): SceneMesh {
    require(radius.isFinite() && halfZ.isFinite() && radius > 0f && halfZ > 0f) {
        "disc radius and halfZ must be finite and positive"
    }
    require(segments >= 8) { "disc segments must be at least 8, was $segments" }
    val vertexCount = checkedVertexCount(segments.toLong() * 6 + 2)
    val indexCount = checkedIndexCount(segments.toLong() * 12)
    val writer = MeshWriter(vertexCount, indexCount)
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
 * Scene. Prism along Z from a simple XY [outline] of at least three points.
 * Concave and clockwise outlines are supported. The last point may repeat the first.
 * [halfZ] is half the thickness. Faces point outward.
 */
public fun extrudePolygon(outline: List<Pair<Float, Float>>, halfZ: Float): SceneMesh {
    require(halfZ.isFinite() && halfZ > 0f) { "extrudePolygon halfZ must be finite and positive" }
    val corners = preparedOutline(outline)
    val count = corners.size
    val vertexCount = checkedVertexCount(count.toLong() * 6)
    val indexCount = checkedIndexCount(count.toLong() * 12 - 12)
    val caps = triangulatePolygon(corners)
    val minX = corners.minOf { it.first }
    val maxX = corners.maxOf { it.first }
    val minY = corners.minOf { it.second }
    val maxY = corners.maxOf { it.second }
    val writer = MeshWriter(vertexCount, indexCount)
    val front = IntArray(count)
    val back = IntArray(count)
    for (index in 0 until count) {
        val x = corners[index].first
        val y = corners[index].second
        val u = (x - minX) / (maxX - minX)
        val v = (y - minY) / (maxY - minY)
        front[index] = writer.vertex(x, y, halfZ, 0f, 0f, 1f, u, v)
        back[index] = writer.vertex(x, y, -halfZ, 0f, 0f, -1f, u, v)
    }
    for (index in caps.indices step 3) {
        val a = caps[index]
        val b = caps[index + 1]
        val c = caps[index + 2]
        writer.tri(front[a], front[b], front[c])
        writer.tri(back[a], back[c], back[b])
    }
    for (index in 0 until count) {
        val next = (index + 1) % count
        val ax = corners[index].first
        val ay = corners[index].second
        val bx = corners[next].first
        val by = corners[next].second
        val nx = by.toDouble() - ay
        val ny = ax.toDouble() - bx
        val length = hypot(nx, ny)
        val unitX = (nx / length).toFloat()
        val unitY = (ny / length).toFloat()
        val a = writer.vertex(ax, ay, halfZ, unitX, unitY, 0f, 0f, 0f)
        val b = writer.vertex(bx, by, halfZ, unitX, unitY, 0f, 1f, 0f)
        val c = writer.vertex(bx, by, -halfZ, unitX, unitY, 0f, 1f, 1f)
        val d = writer.vertex(ax, ay, -halfZ, unitX, unitY, 0f, 0f, 1f)
        writer.tri(a, d, c)
        writer.tri(a, c, b)
    }
    return writer.mesh()
}

private fun preparedOutline(outline: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
    require(outline.size >= 3) { "extrudePolygon needs at least 3 points, was ${outline.size}" }
    val closed = if (outline.first() == outline.last()) outline.dropLast(1) else outline
    require(closed.size >= 3) { "extrudePolygon needs at least 3 distinct points" }
    require(closed.all { it.first.isFinite() && it.second.isFinite() }) {
        "extrudePolygon points must be finite"
    }
    for (index in closed.indices) {
        require(closed[index] != closed[(index + 1) % closed.size]) {
            "extrudePolygon adjacent points must differ"
        }
    }
    require(simpleOutline(closed)) { "extrudePolygon outline must be a simple polygon" }
    var signedArea = 0.0
    for (index in closed.indices) {
        val a = closed[index]
        val b = closed[(index + 1) % closed.size]
        signedArea += a.first.toDouble() * b.second - b.first.toDouble() * a.second
    }
    require(signedArea.isFinite() && signedArea != 0.0) { "extrudePolygon outline must have area" }
    val oriented = if (signedArea < 0.0) closed.reversed() else closed
    val withoutCollinear = oriented.toMutableList()
    var index = 0
    while (withoutCollinear.size > 3 && index < withoutCollinear.size) {
        val count = withoutCollinear.size
        val previous = withoutCollinear[(index - 1 + count) % count]
        val current = withoutCollinear[index]
        val next = withoutCollinear[(index + 1) % count]
        if (cross2(previous, current, next) == 0.0) {
            withoutCollinear.removeAt(index)
            if (index > 0) index--
        } else {
            index++
        }
    }
    return withoutCollinear
}

private fun simpleOutline(outline: List<Pair<Float, Float>>): Boolean {
    val count = outline.size
    for (first in 0 until count) {
        val nextFirst = (first + 1) % count
        for (second in first + 1 until count) {
            val nextSecond = (second + 1) % count
            if (nextFirst == second || nextSecond == first) continue
            if (segmentsIntersect(outline[first], outline[nextFirst], outline[second], outline[nextSecond])) {
                return false
            }
        }
    }
    return true
}

private fun segmentsIntersect(
    a: Pair<Float, Float>,
    b: Pair<Float, Float>,
    c: Pair<Float, Float>,
    d: Pair<Float, Float>,
): Boolean {
    val ac = cross2(a, b, c)
    val ad = cross2(a, b, d)
    val ca = cross2(c, d, a)
    val cb = cross2(c, d, b)
    if (ac == 0.0 && pointOnSegment(c, a, b)) return true
    if (ad == 0.0 && pointOnSegment(d, a, b)) return true
    if (ca == 0.0 && pointOnSegment(a, c, d)) return true
    if (cb == 0.0 && pointOnSegment(b, c, d)) return true
    return (ac > 0.0) != (ad > 0.0) && (ca > 0.0) != (cb > 0.0)
}

private fun pointOnSegment(
    point: Pair<Float, Float>,
    a: Pair<Float, Float>,
    b: Pair<Float, Float>,
): Boolean = point.first in minOf(a.first, b.first)..maxOf(a.first, b.first) &&
    point.second in minOf(a.second, b.second)..maxOf(a.second, b.second)

private fun triangulatePolygon(outline: List<Pair<Float, Float>>): IntArray {
    val size = outline.size
    val next = IntArray(size) { (it + 1) % size }
    val previous = IntArray(size) { (it - 1 + size) % size }
    val alive = BooleanArray(size) { true }
    val ears = BooleanArray(size) { isPolygonEar(it, previous, next, alive, outline) }
    val triangles = IntArray((size - 2) * 3)
    var count = outline.size
    var written = 0
    var cursor = 0
    var searched = 0
    while (count > 3) {
        if (!ears[cursor]) {
            cursor = next[cursor]
            searched++
            if (searched == count) {
                // Removing an ear can expose an ear elsewhere if the removed point was the
                // only point inside that candidate triangle. Refresh only on this rare path.
                require(refreshPolygonEars(ears, previous, next, alive, outline)) {
                    "extrudePolygon outline must be a simple polygon"
                }
                searched = 0
            }
            continue
        }
        val a = previous[cursor]
        val c = next[cursor]
        triangles[written++] = a
        triangles[written++] = cursor
        triangles[written++] = c
        alive[cursor] = false
        next[a] = c
        previous[c] = a
        count--
        if (count > 3) {
            ears[a] = isPolygonEar(a, previous, next, alive, outline)
            ears[c] = isPolygonEar(c, previous, next, alive, outline)
        }
        cursor = c
        searched = 0
    }
    val a = cursor
    val b = next[a]
    val c = next[b]
    require(cross2(outline[a], outline[b], outline[c]) > 0.0) {
        "extrudePolygon outline must be a simple polygon"
    }
    triangles[written++] = a
    triangles[written++] = b
    triangles[written] = c
    return triangles
}

private fun refreshPolygonEars(
    ears: BooleanArray,
    previous: IntArray,
    next: IntArray,
    alive: BooleanArray,
    outline: List<Pair<Float, Float>>,
): Boolean {
    var anyEar = false
    for (candidate in outline.indices) {
        if (!alive[candidate]) continue
        ears[candidate] = isPolygonEar(candidate, previous, next, alive, outline)
        anyEar = anyEar || ears[candidate]
    }
    return anyEar
}

private fun isPolygonEar(
    index: Int,
    previous: IntArray,
    next: IntArray,
    alive: BooleanArray,
    outline: List<Pair<Float, Float>>,
): Boolean {
    val a = previous[index]
    val b = index
    val c = next[index]
    if (cross2(outline[a], outline[b], outline[c]) <= 0.0) return false
    for (other in outline.indices) {
        if (alive[other] && other != a && other != b && other != c &&
            insideTriangle(outline[other], outline[a], outline[b], outline[c])
        ) {
            return false
        }
    }
    return true
}

private fun insideTriangle(
    point: Pair<Float, Float>,
    a: Pair<Float, Float>,
    b: Pair<Float, Float>,
    c: Pair<Float, Float>,
): Boolean = cross2(a, b, point) >= 0.0 &&
    cross2(b, c, point) >= 0.0 &&
    cross2(c, a, point) >= 0.0

private fun cross2(a: Pair<Float, Float>, b: Pair<Float, Float>, c: Pair<Float, Float>): Double =
    (b.first.toDouble() - a.first) * (c.second.toDouble() - a.second) -
        (b.second.toDouble() - a.second) * (c.first.toDouble() - a.first)

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
    val points = if (path.size > 1 && path.first() == path.last()) path.dropLast(1) else path
    require(points.size >= 8) { "tubeAlong path needs at least 8 points, was ${points.size}" }
    require(rings >= 4) { "tubeAlong rings must be at least 4, was $rings" }
    require(radius.isFinite() && radius > 0f && z.isFinite()) {
        "tubeAlong radius must be finite and positive, and z must be finite"
    }
    require(points.all { it.first.isFinite() && it.second.isFinite() }) { "tubeAlong path points must be finite" }
    val count = points.size
    require(points.indices.all { points[it] != points[(it + 1) % count] }) {
        "tubeAlong adjacent path points must differ"
    }
    val vertexCount = checkedVertexCount(count.toLong() * rings)
    val indexCount = checkedIndexCount(count.toLong() * rings * 6)
    val writer = MeshWriter(vertexCount, indexCount)
    val ids = IntArray(count * rings)
    for (index in 0 until count) {
        val previous = points[(index - 1 + count) % count]
        val current = points[index]
        val next = points[(index + 1) % count]
        val dx = next.first.toDouble() - previous.first
        val dy = next.second.toDouble() - previous.second
        val tangent = hypot(dx, dy)
        require(tangent.isFinite() && tangent > 0.0) {
            "tubeAlong path has an undefined tangent at point $index"
        }
        val bx = (-dy / tangent).toFloat()
        val by = (dx / tangent).toFloat()
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
