/**
 * Core authoring package for RedByteFX.
 *
 * One expression carrier, [Expr], is spelled to AGSL or OpenGL ES 3.0 through [shader].
 * Uniform handles stay separate from the shader value: runtime code writes the handle, and the
 * shader reads [Uniform.expr].
 */
package ru.redbyte.redbytefx
