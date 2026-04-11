/**
 * Core authoring package for RedByteFX.
 *
 * This is the raw DSL "start here" surface: typed expressions, uniforms, locals, user-defined
 * helper functions, sampling, and AGSL generation. Reach for this package first when porting
 * hand-written AGSL or Shadertoy-style code line by line.
 *
 * First-pass canonical core path:
 *
 * - coordinates and sampling: [FxDsl.fragCoord], [FxDsl.resolution], [FxDsl.sample],
 *   [FxDsl.sampleUnclamped]
 * - runtime-driven inputs: [FxDsl.uniformFloat], [FxDsl.uniformTime], [FxDsl.autoUniformFloat],
 *   [FxDsl.autoUniformTime]
 * - readable generated shader structure: [FxDsl.let], [FxDsl.fn], [FxEffect.agslSource]
 * - direct math/color building blocks: [mix], [smoothstep], [sin], [color], [float2], [float3],
 *   [float4]
 *
 * A good first translation usually stays close to raw AGSL shape: keep `fragCoord`/`resolution`
 * explicit, store obvious locals with `let(...)`, sample content with [FxDsl.sample], then inspect
 * [FxEffect.agslSource]. Once the generated shader reads clearly, move repeated masks,
 * compositing, SDF, polar, or routing recipes into `:redbytefx-stdlib`.
 *
 * When that authored shader moves into runtime wiring, keep the same order of operations: compile,
 * inspect [FxEffect.agslSource], bind effect-specific params to one runtime instance/controller,
 * then apply it to one render target.
 *
 * Small authoring convenience: common whole-number math such as `1 - amount` and `2 * uv` is
 * accepted directly, so early ports do not need to turn every left-hand integer literal into `1f`
 * before the shader intent is clear.
 */
package ru.redbyte.redbytefx
