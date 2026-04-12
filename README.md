**English** · [Русский](README.ru.md)

# RedByteFX

**AGSL is powerful but awkward to write by hand** — string shaders, easy-to-miss uniforms, and a lot of ceremony. **RedByteFX** gives you the same pipeline (**Kotlin → AGSL → `RuntimeShader` / `RenderEffect`**) through a **small, typed DSL**: real expressions, `let`/`fn`, compile-time checks, and **Jetpack Compose** bindings so UI state becomes shader uniforms without glue code. Less plumbing, more of what you actually want: **readable shader logic that still maps to predictable AGSL** when you peek at `agslSource()`.

---

## Add to your project

**Version `1.0.0`**

```kotlin
dependencies {
    implementation("com.github.i-redbyte:redbytefx-core:1.0.0")
    implementation("com.github.i-redbyte:redbytefx-compose:1.0.0")
    implementation("com.github.i-redbyte:redbytefx-stdlib:1.0.0")
}
```

Use `google()` + `mavenCentral()` in `settings.gradle.kts` as usual. If Gradle cannot resolve these coordinates yet, the artifacts may be on **GitHub Packages** for this repo — see the [Gradle registry docs](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry) and the **Packages** section of the GitHub repository (one-time repository + auth setup).

| Artifact | Role |
|----------|------|
| `redbytefx-core` | DSL, compiler, uniforms, `redbytefx { }` |
| `redbytefx-compose` | `FxController`, `Modifier.redbyteFx`, bindings |
| `redbytefx-stdlib` | Extra helpers (masks, ramps, noise, SDF, …) |

**Requirements:** Android **API 33+**, Kotlin; Jetpack Compose when you use `redbytefx-compose`.

---

## Raw AGSL vs RedByteFX (same idea, less noise)

**AGSL** — you juggle strings, watch naming, and repeat boilerplate:

```glsl
uniform shader rb_input;
uniform float2 rb_resolution;
uniform float u_amount;

half4 main(float2 fragCoord) {
  half4 base = rb_sample(fragCoord);
  float l = dot(base.rgb, float3(0.299, 0.587, 0.114));
  half3 g = half3(l, l, l);
  return half4(mix(g, base.rgb, u_amount), base.a);
}
```

**RedByteFX** — same structure, typed, with a uniform you bind from Kotlin:

```kotlin
val effect = redbytefx {
    val amount by autoUniformFloat(1f)
    val base = let(sample(), "base")
    val gray = grayscale(base)
    mix(gray, base, amount)
}
```

No manual `rb_sample` / uniform declarations for the standard input — the DSL and compiler handle the boring parts. You focus on the math.

---

## Example: wave + Compose

```kotlin
val waveEffect = run {
    var amp: FxParam.Float? = null
    var freq: FxParam.Float? = null
    val effect = redbytefx {
        val a = uniformFloat(0f, "wave_amp")
        val f = uniformFloat(0.08f, "wave_freq")
        amp = a
        freq = f
        val offset = float2(0f, sin(fragCoord.x * f) * a)
        sample(fragCoord + offset)
    }
    Triple(effect, amp!!, freq!!)
}

@Composable
fun WaveLabel(amp: Float, freq: Float) {
    val fx = rememberFxController(waveEffect.first)
    fx.bindFloat(waveEffect.second, amp)
    fx.bindFloat(waveEffect.third, freq)
    Text("RedByteFX", modifier = Modifier.redbyteFx(fx))
}
```

---

## Example: grayscale dial (stdlib + core)

```kotlin
val grayscaleDemo = run {
    var amount: FxParam.Float? = null
    val effect = redbytefx {
        val amountUniform by autoUniformFloat(0f)
        amount = amountUniform
        val base = let(sample(), "base")
        mix(base, grayscale(base), amountUniform)
    }
    effect to amount!!
}
```

---

## Modules in this repository

| Module | Role |
|--------|------|
| `:redbytefx-core` | Language + compiler + runtime bridge |
| `:redbytefx-compose` | Compose integration |
| `:redbytefx-stdlib` | Higher-level helpers |
| `:sample` | Demo app (not a published artifact) |

---

## Contributing

Pull requests and issues are welcome. Small, focused changes are easiest to merge; if you’re planning something big, a short issue first helps everyone. Thanks for improving RedByteFX.

---

## License

[MIT](LICENSE)
