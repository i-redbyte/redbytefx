package ru.redbyte.redbytefx

/**
 * Marks shader DSL receivers so a nested scope does not silently see the outer one.
 *
 * `fn { … }` uses [FnDsl]. Stage members such as [FragmentDsl.sample], [FragmentDsl.fragCoord],
 * and [FragmentDsl.resolution] stay on the stage. Reach them with `this@fragment` only when that
 * capture is intentional.
 */
@DslMarker
public annotation class RedByteFxDsl
