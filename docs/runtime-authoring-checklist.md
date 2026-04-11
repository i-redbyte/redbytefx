# Runtime authoring checklist

Short version of the practical RedByteFX flow:

`compile -> inspect -> bind -> apply`

Use this together with [agsl-vs-redbytefx.md](agsl-vs-redbytefx.md) and
[cookbook-patterns.md](cookbook-patterns.md) when a shader is technically compiling but still does
not behave the way you expect.

## 1. Compile once

Start from one compiled effect:

```kotlin
val effect = redbytefx {
    val amount by autoUniformFloat(0.6f)
    val base = let(sample(), "base")
    mix(base, grayscale(base), amount)
}
```

Keep the first version close to raw AGSL shape:

- explicit `fragCoord` / `resolution` when the source shader uses them directly
- obvious locals through `let(...)`
- named helper extraction through `fn(...)` only when it really improves readability
- stdlib helpers only after the generated shape still makes sense

## 2. Inspect generated AGSL before debugging runtime

Check `effect.agslSource()` first.

Good first scan order:

1. `uniform ...` declarations
2. generated local variables from `let(...)`
3. user helper declarations from `fn(...)`
4. final sampling path in `main(...)`

Most early authoring mistakes show up here first:

- wrong uniform naming
- wrong coordinate/sampling space
- local naming that hides the actual data flow
- helper extraction that made the generated shader harder to read

## 3. Keep param ownership intact

Uniform handles are effect-specific.

Good:

```kotlin
val setup = run {
    var amountParam: FxParam.Float? = null
    val effect = redbytefx {
        val amount by autoUniformFloat(0.6f)
        amountParam = amount
        mix(sample(), grayscale(sample()), amount)
    }
    effect to amountParam!!
}
```

Bad:

```kotlin
val effectA = redbytefx { ... }
val effectB = redbytefx { ... }
val amountFromA: FxParam.Float = ...

val controller = rememberFxController(effectB)
controller.bindFloat(amountFromA, 0.6f) // wrong effect
```

If runtime binding looks suspicious, verify that:

- the controller came from the same compiled effect
- the `FxParam` handle also came from that same compiled effect

Matching names do not make handles interchangeable.

## 4. Use one controller per render target

Good:

```kotlin
val fx = rememberFxController(effect)
fx.bindFloat(amountParam, amount)

Box(Modifier.redbyteFx(fx))
```

Rule of thumb:

- one controller = one mutable runtime shader instance
- one render target should normally own one controller
- if the same effect appears in two places or at two sizes, prefer two controllers

Avoid sharing one controller across unrelated surfaces, because resolution and runtime state then
stop matching one specific draw target.

## 5. Let Compose own resolution sync

When you use `Modifier.redbyteFx(fx)`, the draw path already keeps the shader resolution current.

That means most Compose code should:

- bind author-controlled uniforms only
- use `bindTime(...)`, `bindFloat(...)`, `bindFloat2(...)`, `bindFloat3(...)`, `bindFloat4(...)`
- avoid manually calling `setResolution(...)` unless you are deliberately driving the runtime
  outside the normal Compose render path

## 6. Check sampling space explicitly

Before blaming runtime or Compose, verify the sampling space.

- use `sample(...)` for pixel/sample-space reads
- use `normalizedUv() + sampleUv(...)` for normalized UV resampling
- if UV is only used for masks or gradients, the base content read may still correctly stay on
  `sample()`

Typical mistake:

```kotlin
val uv = normalizedUv()
sample(uv) // wrong: uv is not pixel-space
```

Correct:

```kotlin
val uv = normalizedUv()
sampleUv(uv)
```

## 7. If something still looks wrong, debug in this order

1. `agslSource()` shape
2. uniform ownership
3. sampling space
4. controller-per-target rule
5. runtime measurement / platform behavior

This order matters because most issues are still authoring-shape problems long before they become
true runtime bugs.
