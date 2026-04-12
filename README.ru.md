[English](README.md) · **Русский**

# RedByteFX

**AGSL мощный, но неудобен вручную** — строки шейдеров, легко ошибиться с uniform-ами, много рутины. **RedByteFX** даёт тот же путь (**Kotlin → AGSL → `RuntimeShader` / `RenderEffect`**) через **компактный типизированный DSL**: выражения, `let`/`fn`, проверки на этапе компиляции и привязки к **Jetpack Compose**, чтобы состояние UI стало uniform-ами без лишнего клея. Меньше шаблонного кода — больше **понятной шейдерной логики**, при этом сгенерированный AGSL по-прежнему можно посмотреть через `agslSource()`.

---

## Подключение

**Версия `1.0.0`**

```kotlin
dependencies {
    implementation("com.github.i-redbyte:redbytefx-core:1.0.0")
    implementation("com.github.i-redbyte:redbytefx-compose:1.0.0")
    implementation("com.github.i-redbyte:redbytefx-stdlib:1.0.0")
}
```

В `settings.gradle.kts` как обычно: `google()` и `mavenCentral()`. Если Gradle пока не находит эти координаты, артефакты могут быть в **GitHub Packages** у этого репозитория — см. [документацию Gradle Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry) и раздел **Packages** на GitHub (однократная настройка репозитория и доступа).

| Артефакт | Назначение |
|----------|------------|
| `redbytefx-core` | DSL, компилятор, uniform-ы, `redbytefx { }` |
| `redbytefx-compose` | `FxController`, `Modifier.redbyteFx`, привязки |
| `redbytefx-stdlib` | Доп. хелперы (маски, градиенты, шум, SDF, …) |

**Требования:** Android **API 33+**, Kotlin; Jetpack Compose — если используете `redbytefx-compose`.

---

## Сырой AGSL и RedByteFX (та же идея — меньше шума)

**AGSL** — строки, имена, повторяющийся шаблон:

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

**RedByteFX** — та же структура, типизированно, uniform привязывается из Kotlin:

```kotlin
val effect = redbytefx {
    val amount by autoUniformFloat(1f)
    val base = let(sample(), "base")
    val gray = grayscale(base)
    mix(gray, base, amount)
}
```

Стандартный ввод и объявления вокруг `rb_sample` DSL берёт на себя — вы описываете математику.

---

## Пример: волна + Compose

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

## Пример: градации серого

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

## Модули в репозитории

| Модуль | Роль |
|--------|------|
| `:redbytefx-core` | Язык, компилятор, мост к рантайму |
| `:redbytefx-compose` | Интеграция с Compose |
| `:redbytefx-stdlib` | Высокоуровневые хелперы |
| `:sample` | Демо-приложение (не артефакт Maven) |

---

## Участие

Issues и pull request’ы приветствуются. Небольшие, сфокусированные изменения проще принять; если задумали крупное — короткий issue заранее всем облегчит жизнь. Спасибо, что улучшаете RedByteFX.

---

## Лицензия

[MIT](LICENSE)
