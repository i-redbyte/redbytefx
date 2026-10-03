/**
 * Core authoring package for RedByteFX.
 *
 * One expression carrier, [Expr], is spelled to AGSL, OpenGL ES 3.0, 3.1 compute, or 3.2
 * geometry and tessellation through [shader].
 * Uniform handles stay separate from the shader value: runtime code writes the handle, and the
 * shader reads [Uniform.expr].
 */
package ru.redbyte.redbytefx
