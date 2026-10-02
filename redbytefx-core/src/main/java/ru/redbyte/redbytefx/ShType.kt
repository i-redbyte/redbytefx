package ru.redbyte.redbytefx

/**
 * Precision of a floating-point shader value.
 *
 * [High] is spelled `float` / `highp`. [Med] is spelled `half` / `mediump`.
 */
public sealed interface Prec

/** Full shader float precision. */
public interface High : Prec

/** Medium shader float precision. */
public interface Med : Prec

/** Closed set of shader types carried by [Expr]. */
public sealed interface ShType

/** Floating scalar of precision [P]. */
public interface Flt<P : Prec> : ShType

/** Integer scalar. Arithmetic for this type is not part of the float algebra. */
public interface IntS : ShType

/** Boolean scalar. */
public interface BoolS : ShType

/** Two-component boolean vector. This is not [Vec2] of [BoolS]. */
public interface BVec2 : ShType

/** Three-component boolean vector. */
public interface BVec3 : ShType

/** Four-component boolean vector. */
public interface BVec4 : ShType

/** Two-component vector. Rank stays nominal so operators can return [Vec2]. */
public interface Vec2<S : ShType> : ShType

/** Three-component vector. */
public interface Vec3<S : ShType> : ShType

/** Four-component vector. Color is [Vec4] of [Flt] with [Med] precision, not a separate type. */
public interface Vec4<S : ShType> : ShType

/** Square 2×2 float matrix. */
public interface Mat2 : ShType

/** Square 3×3 float matrix. */
public interface Mat3 : ShType

/** Square 4×4 float matrix. */
public interface Mat4 : ShType

/** GLES `sampler2D`. */
public interface Sampler2D : ShType

/** AGSL child shader input. This is not a GLES sampler. */
public interface ChildShader : ShType
