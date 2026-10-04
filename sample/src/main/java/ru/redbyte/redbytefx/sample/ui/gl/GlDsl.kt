package ru.redbyte.redbytefx.sample.ui.gl

internal val triangleDsl = """
shader(ShaderTarget.Gles30) {
    time = uniformTime()
    vertex {
        val position = attributeVec2("position")
        glPosition(vec4(position.x, position.y, 0f.lit, 1f.lit))
    }
    fragment {
        val wave = sin(time.expr)
        vec4(0.5f.lit + wave * 0.5f.lit, 0.15f.lit, 0.85f.lit, 1f.lit)
    }
}
""".trimIndent()

internal val flagDsl = """
shader(ShaderTarget.Gles30) {
    time = uniformTime()
    aspect = uniform("aspect", 1f)
    val cloth = varyingVec2("cloth")
    vertex {
        fold = fn(0f.lit, 0f.lit, 0f.lit, "fold") { u, v, t ->
            sin(u * 6.2f.lit + t * 1.7f.lit) * (0.08f.lit + v * 0.22f.lit) +
                sin(v * 4.4f.lit - t * 1.15f.lit) * 0.045f.lit
        }
        val uv = attributeVec2("uv")
        val height = fold(uv.x, uv.y, time.expr)
        cloth.set(uv)
        glPosition(vec4((uv.x * 1.65f.lit - 0.82f.lit) / aspect.expr, uv.y * 0.95f.lit - 0.42f.lit, height, height + 2.15f.lit))
    }
    fragment {
        val shade = fold(cloth.expr.x, cloth.expr.y, time.expr)
        val mark = cppMark(cloth.expr.x, cloth.expr.y)
        val rgb = mix(vec3(0.82f.lit, 0.05f.lit, 0.08f.lit), vec3(0.96f.lit, 0.94f.lit, 0.9f.lit), mark) *
            (0.7f.lit + shade * 1.8f.lit)
        vec4(rgb.x, rgb.y, rgb.z, 1f.lit)
    }
}

fun FragmentDsl.cppMark(u: Expr<Flt<High>>, v: Expr<Flt<High>>): Expr<Flt<High>> {
    val letter = slab(u, v, 0.04f, 0.78f, 0.058f, 0.96f) +
        slab(u, v, 0.04f, 0.935f, 0.115f, 0.96f) +
        slab(u, v, 0.04f, 0.78f, 0.115f, 0.805f)
    val first = slab(u, v, 0.15f, 0.8f, 0.168f, 0.94f) + slab(u, v, 0.128f, 0.855f, 0.19f, 0.885f)
    val second = slab(u, v, 0.22f, 0.8f, 0.238f, 0.94f) + slab(u, v, 0.198f, 0.855f, 0.26f, 0.885f)
    return saturate(letter + first + second)
}
""".trimIndent()

internal val floorDsl = """
shader(ShaderTarget.Gles30) {
    time = uniformTime()
    aspect = uniform("aspect", 1f)
    val world = varyingVec2("world")
    vertex {
        val xz = attributeVec2("xz")
        world.set(vec2(xz.x * 2.6f.lit, xz.y))
        glPosition(vec4(xz.x * 1.25f.lit / aspect.expr, (-0.5f).lit, 0f.lit, xz.y))
    }
    fragment {
        val edge = min(abs(fract(world.expr.x) - 0.5f.lit), abs(fract(world.expr.y * 1.4f.lit) - 0.5f.lit))
        val neon = 1f.lit - smoothstep(0.0f.lit, 0.045f.lit, edge)
        val pulse = 0.65f.lit + 0.35f.lit * sin(time.expr * 2f.lit + world.expr.y)
        val fog = saturate((world.expr.y - 0.4f.lit) / 6.5f.lit)
        val glow = vec3(0.15f.lit, 0.9f.lit, 0.95f.lit) * neon * pulse
        val rgb = mix(vec3(0.02f.lit, 0.035f.lit, 0.07f.lit) + glow, vec3(0.01f.lit, 0.015f.lit, 0.03f.lit), fog)
        vec4(rgb.x, rgb.y, rgb.z, 1f.lit)
    }
}
""".trimIndent()

internal val lampDsl = """
shader(ShaderTarget.Gles30) {
    time = uniformTime()
    aspect = uniform("aspect", 1f)
    val normalXy = varyingVec2("normalXy")
    val normalZ = varyingVec2("normalZ")
    vertex {
        val position = attributeVec3("position")
        val normal = attributeVec3("normal")
        val angle = time.expr * 0.65f.lit
        val turn = cos(angle)
        val lift = sin(angle)
        val x = position.x * turn + position.z * lift
        val z = position.z * turn - position.x * lift + 1.7f.lit
        val nx = normal.x * turn + normal.z * lift
        val nz = normal.z * turn - normal.x * lift
        normalXy.set(vec2(nx, normal.y))
        normalZ.set(vec2(nz, 1f.lit))
        glPosition(vec4(x / aspect.expr, position.y, z * 0.25f.lit - 0.35f.lit, z))
    }
    fragment {
        val nx = normalXy.expr.x
        val ny = normalXy.expr.y
        val nz = normalZ.expr.x
        val inv = 1f.lit / max(length(vec3(nx, ny, nz)), 0.0001f.lit)
        val light = vec3((-0.35f).lit, 0.8f.lit, (-0.45f).lit)
        val diffuse = max((nx * light.x + ny * light.y + nz * light.z) * inv, 0f.lit)
        val spec = pow(diffuse, 28f)
        val warm = vec3(1f.lit, 0.62f.lit, 0.22f.lit) * (0.16f.lit + diffuse * 0.9f.lit)
        val rgb = warm + vec3(spec, spec * 0.85f.lit, spec * 0.45f.lit)
        vec4(rgb.x, rgb.y, rgb.z, 1f.lit)
    }
}
""".trimIndent()

internal val cityDsl = """
shader(ShaderTarget.Gles30) {
    aspect = uniform("aspect", 1f)
    facade = sampler2D("facade")
    ground = sampler2D("ground")
    val uv = varyingVec2("uv")
    val facing = varyingVec3("normal")
    val height = varyingFloat("height")
    frame = uniformBlock("frame") {
        val time = float("time")
        val eyeX = float("eyeX")
        val eyeY = float("eyeY")
        val eyeZ = float("eyeZ")
        val rightX = float("rightX")
        val rightZ = float("rightZ")
        val forwardX = float("forwardX")
        val forwardZ = float("forwardZ")
        vertex {
            val position = attributeVec3("position")
            val normal = attributeVec3("normal")
            val texcoord = attributeVec2("uv")
            val dx = position.x - eyeX
            val dy = position.y - eyeY
            val dz = position.z - eyeZ
            uv.set(texcoord)
            facing.set(normal)
            height.set(position.y)
            glPosition(vec4((dx * rightX + dz * rightZ) / aspect.expr, dy, (dx * forwardX + dz * forwardZ) * 0.2f.lit - 0.5f.lit, dx * forwardX + dz * forwardZ))
        }
        fragment {
            val street = (height.expr lt 0.02f.lit) and (facing.expr.y gt 0.85f.lit)
            val texel = ifElse(street, texture(ground, uv.expr * float2(4f, 4f)), texture(facade, uv.expr * float2(2f, 3f)))
            val light = saturate(facing.expr.y * 0.35f.lit + facing.expr.z * 0.2f.lit + 0.55f.lit) * (0.94f.lit + 0.06f.lit * abs(sin(time * 1.4f.lit)))
            vec4(texel.x * light, texel.y * light, texel.z * light, 1f.lit)
        }
    }
}
""".trimIndent()

internal val crateDsl = """
shader(ShaderTarget.Gles30) {
    view = uniformMat4("view")
    projection = uniformMat4("projection")
    light = uniformVec3("light", 0.35f, 0.85f, 0.4f)
    facade = sampler2D("facade")
    ground = sampler2D("ground")
    val uv = varyingVec2("uv")
    val facing = varyingVec3("normal")
    val height = varyingFloat("height")
    vertex {
        val position = attributeVec3("position")
        val normal = attributeVec3("normal")
        val texcoord = attributeVec2("uv")
        uv.set(texcoord)
        facing.set(normal)
        height.set(position.y)
        glPosition(projection.expr * (view.expr * vec4(position.x, position.y, position.z, 1f.lit)))
    }
    fragment {
        val street = (height.expr lt 0.02f.lit) and (facing.expr.y gt 0.85f.lit)
        val texel = ifElse(street, texture(ground, uv.expr * float2(4f, 4f)), texture(facade, uv.expr))
        val shade = lambert(facing.expr, light.expr)
        vec4(texel.x * shade, texel.y * shade, texel.z * shade, 1f.lit)
    }
}
""".trimIndent()

internal val planetDsl = """
shader(ShaderTarget.Gles30) {
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
""".trimIndent()

internal val orbDsl = """
shader(ShaderTarget.Gles30) {
    time = uniformTime()
    aspect = uniform("aspect", 1f)
    val uv = varyingVec2("uv")
    vertex {
        val corner = attributeVec2("corner")
        uv.set(corner)
        glPosition(vec4(corner.x, corner.y, 0f.lit, 1f.lit))
    }
    fragment {
        val px = let(uv.expr.x * aspect.expr, "px")
        val py = let(uv.expr.y, "py")
        val aim = vec3(px, py, 1f.lit)
        val inv = 1f.lit / max(length(aim), 0.0001f.lit)
        val dx = aim.x * inv
        val dy = aim.y * inv
        val dz = aim.z * inv
        val originX = sin(time.expr * 0.6f.lit) * 0.18f.lit
        val originY = 0.08f.lit
        val originZ = (-2.15f).lit
        val slope = originX * dx + originY * dy + originZ * dz
        val radius = 0.72f.lit
        val curve = originX * originX + originY * originY + originZ * originZ - radius * radius
        val disc = slope * slope - curve
        val travel = -slope - pow(max(disc, 0f.lit), 0.5f.lit)
        val nx = (originX + dx * travel) / radius
        val ny = (originY + dy * travel) / radius
        val nz = (originZ + dz * travel) / radius
        val facing = max(-(nx * dx + ny * dy + nz * dz), 0f.lit)
        val fresnel = pow(1f.lit - facing, 3f)
        val sky = mix(
            vec3(0.02f.lit, 0.03f.lit, 0.08f.lit),
            vec3(0.18f.lit, 0.28f.lit, 0.48f.lit),
            py * 0.5f.lit + 0.5f.lit,
        )
        val glass = vec3(0.35f.lit, 0.72f.lit, 0.95f.lit) * (0.25f.lit + facing) +
            vec3(1f.lit, 0.95f.lit, 0.9f.lit) * fresnel
        val rgb = ifElse(disc gt 0f.lit, glass, sky)
        vec4(rgb.x, rgb.y, rgb.z, 1f.lit)
    }
}
""".trimIndent()

internal val bandsDsl = """
shader(ShaderTarget.Gles30) {
    time = uniformTime()
    aspect = uniform("aspect", 1f)
    val uv = varyingVec2("uv")
    vertex {
        val corner = attributeVec2("corner")
        uv.set(corner)
        glPosition(vec4(corner.x, corner.y, 0f.lit, 1f.lit))
    }
    fragment {
        val field = sin(uv.expr.x * aspect.expr * 3.2f.lit + time.expr) +
            cos(uv.expr.y * 2.4f.lit - time.expr * 0.7f.lit)
        val probe = vec2(field, sin(field * 2f.lit + time.expr))
        val hot = any(probe gt vec2(1.15f.lit, 0.35f.lit))
        val cool = any(probe lt vec2((-0.85f).lit, 0f.lit))
        val band = ifElse(field gt 0.15f.lit, vec3(0.85f.lit, 0.45f.lit, 0.18f.lit), ifElse(cool, vec3(0.05f.lit, 0.06f.lit, 0.16f.lit), vec3(0.1f.lit, 0.28f.lit, 0.62f.lit)))
        val rgb = ifElse(hot, vec3(1f.lit, 0.92f.lit, 0.7f.lit), band)
        vec4(rgb.x, rgb.y, rgb.z, 1f.lit)
    }
}
""".trimIndent()

internal val paletteDsl = """
shader(ShaderTarget.Gles30) {
    time = uniformTime()
    aspect = uniform("aspect", 1f)
    val paint = varyingVec2("paint")
    vertex {
        ink = fn(0f.lit, 0f.lit, 0f.lit, 0f.lit, "ink") { down, shade, clock, shift ->
            val hue = fract(down - clock * 0.12f.lit + shift)
            val red = saturate(abs(hue * 6f.lit - 3f.lit) - 1f.lit)
            val green = saturate(2f.lit - abs(hue * 6f.lit - 2f.lit))
            val blue = saturate(2f.lit - abs(hue * 6f.lit - 4f.lit))
            vec3(red, green, blue) * (0.28f.lit + shade * 0.72f.lit)
        }
        val position = attributeVec3("position")
        val normal = attributeVec3("normal")
        val uv = attributeVec2("uv")
        val shade = saturate(normal.y * 0.5f.lit + 0.5f.lit)
        val color = ink(uv.x, shade, time.expr, 0f.lit)
        paint.set(vec2(shade, uv.x))
        glPosition(vec4(position.x / aspect.expr, position.z, position.y, 1f.lit))
    }
    fragment {
        val color = ink(paint.expr.y, paint.expr.x, time.expr, 0f.lit)
        vec4(color.x, color.y, color.z, 1f.lit)
    }
}
""".trimIndent()

internal val hedgehogDsl = """
shader(ShaderTarget.Gles32) {
    time = uniformTime()
    vertex {
        val position = attributeVec3("position")
        val turn = cos(time.expr * 0.55f.lit)
        val lift = sin(time.expr * 0.55f.lit)
        glPosition(vec4(position.x * turn + position.z * lift, position.y, position.z * turn - position.x * lift + 1.55f.lit, 1f.lit))
    }
    geometry(GeometryInput.Triangles, GeometryOutput.TriangleStrip, 12) {
        val a = glIn(0)
        val b = glIn(1)
        val c = glIn(2)
        val tip = vec4((a.x + b.x + c.x) / 3f.lit, (a.y + b.y + c.y) / 3f.lit, (a.z + b.z + c.z) / 3f.lit, 1f.lit)
        emitEye(a, b, c)
        emitEye(a, b, tip)
        emitEye(b, c, tip)
        emitEye(c, a, tip)
    }
    fragment {
        vec4(0.95f.lit, 0.32f.lit, 0.12f.lit, 1f.lit)
    }
}
""".trimIndent()

internal val oceanDsl = """
shader(ShaderTarget.Gles32) {
    time = uniformTime()
    vertex {
        val corner = attributeVec4("corner")
        glPosition(vec4(corner.x, time.expr, corner.z, 1f.lit))
    }
    tessControl(4) {
        tessLevelOuter(0, 14f.lit)
        tessLevelOuter(1, 14f.lit)
        tessLevelOuter(2, 14f.lit)
        tessLevelOuter(3, 14f.lit)
        tessLevelInner(0, 14f.lit)
        tessLevelInner(1, 14f.lit)
        passPosition()
    }
    tessEval(TessPrimitive.Quads) {
        val u = tessCoord.x
        val v = tessCoord.y
        val x = mix(mix(glIn(0).x, glIn(1).x, u), mix(glIn(3).x, glIn(2).x, u), v)
        val z = mix(mix(glIn(0).z, glIn(1).z, u), mix(glIn(3).z, glIn(2).z, u), v)
        val height = sin(x * 3.4f.lit + glIn(0).y * 1.35f.lit) * 0.16f.lit +
            sin(z * 2.3f.lit - glIn(0).y * 0.9f.lit) * 0.1f.lit
        glPosition(vec4(x * 0.62f.lit, height - 0.12f.lit, height, z + 0.85f.lit))
    }
    fragment {
        val crest = 0.5f.lit + 0.5f.lit * sin(time.expr * 1.2f.lit)
        vec4(0.03f.lit, 0.22f.lit + crest * 0.16f.lit, 0.42f.lit + crest * 0.22f.lit, 1f.lit)
    }
}
""".trimIndent()

internal val wireDsl = """
shader(ShaderTarget.Gles32) {
    time = uniformTime()
    vertex {
        val position = attributeVec3("position")
        val turn = cos(time.expr * 0.45f.lit)
        val lift = sin(time.expr * 0.45f.lit)
        glPosition(vec4(position.x * turn + position.z * lift, position.y, position.z * turn - position.x * lift + 1.55f.lit, 1f.lit))
    }
    geometry(GeometryInput.Triangles, GeometryOutput.TriangleStrip, 12) {
        emitEdge(glIn(0), glIn(1))
        emitEdge(glIn(1), glIn(2))
        emitEdge(glIn(2), glIn(0))
    }
    fragment {
        val pulse = 0.65f.lit + 0.35f.lit * sin(time.expr * 1.8f.lit)
        vec4(0.15f.lit, 0.82f.lit * pulse, 0.95f.lit, 1f.lit)
    }
}
""".trimIndent()

internal val stormDsl = """
shader(ShaderTarget.Gles30) {
    time = uniformTime()
    aspect = uniform("aspect", 1f)
    touchX = uniform("touchX", 0f)
    touchY = uniform("touchY", 2f)
    touchAge = uniform("touchAge", 4f)
    burstX = uniform("burstX", 0.2f)
    burstAge = uniform("burstAge", 4f)
    val uv = varyingVec2("uv")
    vertex {
        val corner = attributeVec2("corner")
        uv.set(corner)
        glPosition(vec4(corner.x, corner.y, 0f.lit, 1f.lit))
    }
    fragment {
        val bolt = fn(0f.lit, 0f.lit, 0f.lit, 0f.lit, "bolt") { x, y, origin, age ->
            val spine = origin + sin(y * 14f.lit) * 0.045f.lit + sin(y * 31f.lit + 1.3f.lit) * 0.022f.lit
            val core = 1f.lit - smoothstep(0f.lit, 0.007f.lit, abs(x - spine))
            val glow = 1f.lit - smoothstep(0f.lit, 0.05f.lit, abs(x - spine))
            (core * 1.7f.lit + glow * 0.5f.lit) * saturate(1f.lit - age * 0.5f.lit)
        }
        val px = let(uv.expr.x * aspect.expr, "px")
        val py = let(uv.expr.y, "py")
        val hash = fract(sin(floor(px * 26f.lit) * 127.1f.lit + floor(py * 34f.lit) * 311.7f.lit) * 43758.5f.lit)
        val spark = step(0.972f.lit, hash)
        val energy = bolt(px, py, touchX.expr, touchAge.expr) + bolt(px, py, burstX.expr, burstAge.expr)
        val rgb = vec3(0.004f.lit, 0.006f.lit, 0.02f.lit) + vec3(0.82f.lit, 0.88f.lit, 1f.lit) * spark +
            vec3(0.45f.lit, 0.7f.lit, 1f.lit) * saturate(energy)
        vec4(rgb.x, rgb.y, rgb.z, 1f.lit)
    }
}
""".trimIndent()

internal val wordDsl = """
// The letters fall, then roll, on the CPU. The shader only projects and paints.
shader(ShaderTarget.Gles30) {
    time = uniformTime()
    aspect = uniform("aspect", 1f)
    landed = uniform("landed", 0f)
    val paint = varyingVec2("paint")
    vertex {
        val position = attributeVec4("position")
        val tint = attributeVec4("tint")
        paint.set(vec2(tint.x, tint.y))
        glPosition(vec4(position.x / aspect.expr, position.y, position.z * 4f.lit, position.z + 2.5f.lit))
    }
    fragment {
        val hue = time.expr * 0.85f.lit + paint.expr.x * 0.75f.lit
        val color = vec3(
            0.5f.lit + 0.5f.lit * sin(hue),
            0.5f.lit + 0.5f.lit * sin(hue + 2.094f.lit),
            0.5f.lit + 0.5f.lit * sin(hue + 4.188f.lit),
        )
        val metal = vec3(0.72f.lit, 0.9f.lit, 1f.lit)
        val rgb = ifElse(landed.expr gt 0.5f.lit, color, metal) * (0.3f.lit + paint.expr.y * 0.75f.lit)
        vec4(rgb.x, rgb.y, rgb.z, 1f.lit)
    }
}
""".trimIndent()

internal val ballsDsl = """
shader(ShaderTarget.Gles30) {
    aspect = uniform("aspect", 1f)
    count = uniform("count", 6f)
    val uv = varyingVec2("uv")
    val xs = Array(12) { index -> uniform("c${'$'}{index}x", 0f) }
    val ys = Array(12) { index -> uniform("c${'$'}{index}y", 0f) }
    val zs = Array(12) { index -> uniform("c${'$'}{index}z", 1.2f) }
    vertex {
        val corner = attributeVec2("corner")
        uv.set(corner)
        glPosition(vec4(corner.x, corner.y, 0f.lit, 1f.lit))
    }
    fragment {
        traceSpheres(uv.expr, aspect, count, xs, ys, zs)
    }
}

fun FragmentDsl.traceSpheres(...): Expr<Vec4<Flt<High>>> {
    var color = sky
    var bestDepth = 8f.lit
    for (index in xs.indices) {
        val disc = sphereDisc(pixelX, pixelY, light, xs[index], ys[index], zs[index], index, count, bestDepth)
        color = ifElse(disc.hit, disc.color, color)
        bestDepth = ifElse(disc.hit, disc.depth, bestDepth)
    }
    return vec4(color.x, color.y, color.z, 1f.lit)
}
""".trimIndent()
