package ru.redbyte.redbytefx

/**
 * Readable names for common [Expr] and [Uniform] shapes.
 *
 * These are **typealiases** only (`HighVec4` = `Expr<Vec4<Flt<High>>>`). They do not change code
 * generation; use them in app code and KDoc for clarity.
 */

/** Highp float expression. Same type as [Expr] of [Flt] with [High]. */
public typealias HighFloat = Expr<Flt<High>>

/** Mediump float expression. Same type as [Expr] of [Flt] with [Med]. */
public typealias MedFloat = Expr<Flt<Med>>

/** Highp vec2 expression. */
public typealias HighVec2 = Expr<Vec2<Flt<High>>>

/** Highp vec3 expression. */
public typealias HighVec3 = Expr<Vec3<Flt<High>>>

/** Highp vec4 expression. */
public typealias HighVec4 = Expr<Vec4<Flt<High>>>

/** Mediump vec2 expression. */
public typealias MedVec2 = Expr<Vec2<Flt<Med>>>

/** Mediump vec3 expression. */
public typealias MedVec3 = Expr<Vec3<Flt<Med>>>

/** Mediump vec4 expression. Color is this type. */
public typealias MedVec4 = Expr<Vec4<Flt<Med>>>

/** Uniform handle whose shader value is a highp float. */
public typealias HighFloatUniform = Uniform<Flt<High>>

/** Uniform handle whose shader value is a mediump float. */
public typealias MedFloatUniform = Uniform<Flt<Med>>

/** Uniform handle whose shader value is a highp vec2. */
public typealias HighVec2Uniform = Uniform<Vec2<Flt<High>>>

/** Uniform handle whose shader value is a highp vec3. */
public typealias HighVec3Uniform = Uniform<Vec3<Flt<High>>>

/** Uniform handle whose shader value is a highp vec4. */
public typealias HighVec4Uniform = Uniform<Vec4<Flt<High>>>

/** Uniform handle whose shader value is a mediump vec2. */
public typealias MedVec2Uniform = Uniform<Vec2<Flt<Med>>>

/** Uniform handle whose shader value is a mediump vec3. */
public typealias MedVec3Uniform = Uniform<Vec3<Flt<Med>>>

/** Uniform handle whose shader value is a mediump vec4. */
public typealias MedVec4Uniform = Uniform<Vec4<Flt<Med>>>
