package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.Expr
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.FragmentDsl
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.Vec3
import ru.redbyte.redbytefx.abs
import ru.redbyte.redbytefx.and
import ru.redbyte.redbytefx.clamp
import ru.redbyte.redbytefx.cross
import ru.redbyte.redbytefx.div
import ru.redbyte.redbytefx.dot
import ru.redbyte.redbytefx.float
import ru.redbyte.redbytefx.float2
import ru.redbyte.redbytefx.float3
import ru.redbyte.redbytefx.gt
import ru.redbyte.redbytefx.ifElse
import ru.redbyte.redbytefx.le
import ru.redbyte.redbytefx.length
import ru.redbyte.redbytefx.lt
import ru.redbyte.redbytefx.max
import ru.redbyte.redbytefx.min
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.normalize
import ru.redbyte.redbytefx.or
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.sqrt
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.unaryMinus
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.xz
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

/**
 * Largest fixed step count for [rayMarch].
 * The language has no `while`; [FragmentDsl.repeat] accepts the same 1..64 range.
 */
public const val MAX_RAY_STEPS: Int = 64

/**
 * Signed distance from [point] to a sphere at the origin.
 * Negative is inside.
 */
public fun sdSphere(point: Expr<Vec3<Flt<High>>>, radius: Expr<Flt<High>>): Expr<Flt<High>> =
    length(point) - max(radius, 0f)

/** Signed distance from [point] to a sphere at the origin with a literal [radius]. */
public fun sdSphere(point: Expr<Vec3<Flt<High>>>, radius: Float): Expr<Flt<High>> =
    sdSphere(point, float(radius))

/**
 * Signed distance from [point] to an axis-aligned box at the origin.
 * [halfSize] is the half extent on each axis. This is the 3D form; the 2D form is [sdBox].
 */
public fun sdBox3(point: Expr<Vec3<Flt<High>>>, halfSize: Expr<Vec3<Flt<High>>>): Expr<Flt<High>> {
    val q = abs(point) - float3(max(halfSize.x, 0f), max(halfSize.y, 0f), max(halfSize.z, 0f))
    val outside = length(max(q, float(0f)))
    val inside = min(max(q.x, max(q.y, q.z)), 0f)
    return outside + inside
}

/**
 * Signed distance from [point] to a plane.
 * [normal] should be unit length. [offset] shifts the plane along that normal:
 * the distance is `dot(point, normal) + offset`.
 */
public fun sdPlane(
    point: Expr<Vec3<Flt<High>>>,
    normal: Expr<Vec3<Flt<High>>>,
    offset: Expr<Flt<High>>,
): Expr<Flt<High>> = dot(point, normal) + offset

/** Signed distance from [point] to a plane with a literal [offset]. */
public fun sdPlane(
    point: Expr<Vec3<Flt<High>>>,
    normal: Expr<Vec3<Flt<High>>>,
    offset: Float,
): Expr<Flt<High>> = sdPlane(point, normal, float(offset))

/**
 * Signed distance from [point] to a capsule from [start] to [end] with [radius].
 */
public fun sdCapsule(
    point: Expr<Vec3<Flt<High>>>,
    start: Expr<Vec3<Flt<High>>>,
    end: Expr<Vec3<Flt<High>>>,
    radius: Expr<Flt<High>>,
): Expr<Flt<High>> {
    val along = point - start
    val span = end - start
    val t = clamp(dot(along, span) / max(dot(span, span), 0.0001f), 0f, 1f)
    return length(along - span * t) - max(radius, 0f)
}

/** Signed distance from [point] to a capsule with a literal [radius]. */
public fun sdCapsule(
    point: Expr<Vec3<Flt<High>>>,
    start: Expr<Vec3<Flt<High>>>,
    end: Expr<Vec3<Flt<High>>>,
    radius: Float,
): Expr<Flt<High>> = sdCapsule(point, start, end, float(radius))

/**
 * Signed distance from [point] to a torus at the origin.
 * The major circle lies in the XZ plane. [major] is the ring radius and [minor] is the tube radius.
 */
public fun sdTorus(
    point: Expr<Vec3<Flt<High>>>,
    major: Expr<Flt<High>>,
    minor: Expr<Flt<High>>,
): Expr<Flt<High>> {
    val q = float2(length(point.xz) - major, point.y)
    return length(q) - max(minor, 0f)
}

/** Signed distance from [point] to a torus with literal radii. */
public fun sdTorus(
    point: Expr<Vec3<Flt<High>>>,
    major: Float,
    minor: Float,
): Expr<Flt<High>> = sdTorus(point, float(major), float(minor))

/**
 * Unsigned distance from [point] to the triangle [a], [b], [c].
 * The result is always >= 0, so it is not a signed field: [sdfUnion] and [sdfSubtract]
 * do not treat the interior as negative.
 */
public fun sdTriangle(
    point: Expr<Vec3<Flt<High>>>,
    a: Expr<Vec3<Flt<High>>>,
    b: Expr<Vec3<Flt<High>>>,
    c: Expr<Vec3<Flt<High>>>,
): Expr<Flt<High>> {
    val ba = b - a
    val pa = point - a
    val cb = c - b
    val pb = point - b
    val ac = a - c
    val pc = point - c
    val nor = cross(ba, ac)
    val inside = signOf(dot(cross(ba, nor), pa)) +
        signOf(dot(cross(cb, nor), pb)) +
        signOf(dot(cross(ac, nor), pc))
    val nearestA = ba * clamp(dot(ba, pa) / max(dot(ba, ba), 0.0001f), 0f, 1f) - pa
    val nearestB = cb * clamp(dot(cb, pb) / max(dot(cb, cb), 0.0001f), 0f, 1f) - pb
    val nearestC = ac * clamp(dot(ac, pc) / max(dot(ac, ac), 0.0001f), 0f, 1f) - pc
    val edge = min(min(dot(nearestA, nearestA), dot(nearestB, nearestB)), dot(nearestC, nearestC))
    val face = dot(nor, pa)
    val plane = face * face / max(dot(nor, nor), 0.0001f)
    return sqrt(ifElse(inside lt 2f, edge, plane))
}

/** Closer of two signed distances. */
public fun sdfUnion(a: Expr<Flt<High>>, b: Expr<Flt<High>>): Expr<Flt<High>> = min(a, b)

/** Overlap of two signed distances. */
public fun sdfIntersect(a: Expr<Flt<High>>, b: Expr<Flt<High>>): Expr<Flt<High>> = max(a, b)

/**
 * [shape] with [cut] removed.
 * [cut] is the volume that punches the hole; [shape] is what remains.
 */
public fun sdfSubtract(shape: Expr<Flt<High>>, cut: Expr<Flt<High>>): Expr<Flt<High>> = max(shape, -cut)

/**
 * Polynomial smooth minimum of two signed distances.
 * [k] is the blend radius. Values at or below zero fall back to a tiny positive width.
 */
public fun smoothMin(
    a: Expr<Flt<High>>,
    b: Expr<Flt<High>>,
    k: Expr<Flt<High>>,
): Expr<Flt<High>> {
    val safeK = max(k, 0.0001f)
    val h = max(safeK - abs(a - b), 0f) / safeK
    return min(a, b) - h * h * safeK * 0.25f
}

/** Polynomial smooth minimum with a literal blend radius [k]. */
public fun smoothMin(a: Expr<Flt<High>>, b: Expr<Flt<High>>, k: Float): Expr<Flt<High>> =
    smoothMin(a, b, float(k))

/**
 * Sphere-traces [scene] from [origin] along [direction].
 *
 * [steps] is a constant in `1..[MAX_RAY_STEPS]` because the march is a fixed [FragmentDsl.repeat],
 * not a `while`. The direction is normalized inside the march.
 * The result is the distance traveled to a hit closer than [epsilon], or [far] on a miss.
 * Call this from a fragment stage: it declares locals, which cannot live inside `fn` unless
 * the fragment receiver is qualified.
 */
public fun FragmentDsl.rayMarch(
    origin: Expr<Vec3<Flt<High>>>,
    direction: Expr<Vec3<Flt<High>>>,
    steps: Int = 48,
    epsilon: Float = 0.001f,
    far: Float = 20f,
    scene: (Expr<Vec3<Flt<High>>>) -> Expr<Flt<High>>,
): Expr<Flt<High>> {
    require(steps in 1..MAX_RAY_STEPS) {
        "rayMarch steps must be 1..$MAX_RAY_STEPS, was $steps"
    }
    val start = local(origin)
    val dir = local(normalize(direction))
    val traveled = local(float(0f))
    val hit = local(float(far))
    val near = float(epsilon)
    val horizon = float(far)
    repeat(steps) {
        val point = start.expr + dir.expr * traveled.expr
        val dist = scene(point)
        val close = (dist lt near) and (traveled.expr le horizon)
        val stop = close or (traveled.expr gt horizon)
        hit.set(ifElse(close, traveled.expr, hit.expr))
        traveled.set(ifElse(stop, traveled.expr, traveled.expr + dist))
    }
    return hit.expr
}

/**
 * Outward normal of a signed [scene] at [point], from central differences of width [epsilon].
 * This is an expression: it does not declare locals.
 */
public fun sdfNormal(
    point: Expr<Vec3<Flt<High>>>,
    epsilon: Float = 0.001f,
    scene: (Expr<Vec3<Flt<High>>>) -> Expr<Flt<High>>,
): Expr<Vec3<Flt<High>>> {
    val step = float(epsilon)
    val zero = float(0f)
    val dx = scene(point + float3(step, zero, zero)) - scene(point - float3(step, zero, zero))
    val dy = scene(point + float3(zero, step, zero)) - scene(point - float3(zero, step, zero))
    val dz = scene(point + float3(zero, zero, step)) - scene(point - float3(zero, zero, step))
    return normalize(float3(dx, dy, dz))
}

private fun signOf(value: Expr<Flt<High>>): Expr<Flt<High>> =
    ifElse(value gt 0f, float(1f), ifElse(value lt 0f, float(-1f), float(0f)))
