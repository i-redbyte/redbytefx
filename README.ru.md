[English](README.md) · **Русский**

# RedByteFX

**RedByteFX** — это DSL для Android AGSL, ориентированный на Kotlin, который позволяет писать шейдерную логику как обычный Kotlin-код, а не как длинные строковые литералы с AGSL.

AGSL сам по себе мощный, но из Kotlin он часто ощущается неудобно:

- код шейдера живёт внутри строк
- `uniform`-ы приходится объявлять вручную, и в них легко ошибиться
- даже небольшие эффекты быстро обрастают boilerplate
- для Compose обычно требуется дополнительная runtime-обвязка
- отладка часто начинается с вопроса: «в какой строке я сейчас всё сломал?»

RedByteFX сохраняет реальный Android-пайплайн без замены рантайма:

`Kotlin DSL -> сгенерированный AGSL -> RuntimeShader / RenderEffect`

То есть исполнение остаётся нативным и предсказуемым, а сам процесс написания становится намного удобнее: типизированные выражения, понятные локальные переменные через `let(...)`, переиспользуемые функции через `fn(...)`, uniform-handles, привязки к Compose и возможность посмотреть итоговый AGSL через `agslSource()`.

**Требование по платформе:** библиотека работает только на **Android API 33+**. RedByteFX опирается на современный стек AGSL / `RuntimeShader` / `RenderEffect` и не предназначен для более низких API.

## Зачем нужна эта библиотека

RedByteFX нужен для типичной ситуации, когда raw AGSL технически подходит идеально, но как поверхность для написания шейдера оказывается слишком неудобным.

Библиотека помогает, если вам нужны:

- шейдеры, которые ощущаются нативно для Kotlin
- типизированные uniform-ы и выражения вместо строковой «магии»
- удобный перенос AGSL или Shadertoy-подобной математики в Kotlin
- прозрачный переход от DSL к реальному AGSL через `agslSource()`
- интеграция с Compose без отдельного рендер-движка
- удобство без потери производительности и гибкости

## Чистый AGSL vs RedByteFX

Одна и та же идея, одна и та же математика, но совершенно разный опыт разработки.

### AGSL вручную

```glsl
uniform shader content;
uniform float wave_amplitude;
uniform float wave_frequency;

half4 main(float2 fragCoord) {
  float2 offset = float2(0.0, sin(fragCoord.x * wave_frequency) * wave_amplitude);
  return content.eval(fragCoord + offset);
}
```

### То же самое на RedByteFX

```kotlin
val wave = redbytefx {
    val amplitude = uniformFloat(0f, "wave_amplitude")
    val frequency = uniformFloat(0.08f, "wave_frequency")
    val offset = float2(0f, sin(fragCoord.x * frequency) * amplitude)
    sample(fragCoord + offset)
}
```

Шейдер остаётся таким же явным, но теперь это уже нормальный Kotlin-код: типы, автодополнение IDE, безопасный рефакторинг и runtime-параметры без ручного управления сырыми AGSL-строками.

## Пример из sample-приложения

Ниже реальный фрагмент эффекта из demo-приложения. На чистом AGSL такие вещи быстро становятся шумными, а в RedByteFX остаются читаемыми за счёт typed expressions, локальных переменных и готовых compositing-хелперов:

```kotlin
val effect = redbytefx {
    val amount by autoUniformFloat(0.82f)
    val warmth by autoUniformFloat(0.58f)
    val glow by autoUniformFloat(0.38f)

    val base = let(sample(), "base")
    val saturated = let(
        adjustSaturation(base, mix(0.9f, 1.55f, amount)),
        "saturated"
    )
    val tint = let(
        color(
            mix(0.26f, 0.94f, warmth),
            mix(0.48f, 0.72f, warmth),
            mix(0.92f, 0.38f, warmth),
            base.a
        ),
        "tint"
    )
    val multiplied = let(blendMultiply(saturated, tint, 0.25f), "multiplied")
    val screened = let(blendScreen(multiplied, tint, glow), "screened")

    blendOverlay(base, screened, amount)
}
```

## Подключение

RedByteFX публикуется в **Maven Central**, поэтому для потребителя достаточно обычного набора репозиториев:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
```

Подключите нужные модули:

```kotlin
dependencies {
    implementation("io.github.i-redbyte:redbytefx-core:1.0.0")
    implementation("io.github.i-redbyte:redbytefx-compose:1.0.0")
    implementation("io.github.i-redbyte:redbytefx-stdlib:1.0.0")
}
```

### Version Catalog / TOML

`gradle/libs.versions.toml`:

```toml
[versions]
redbytefx = "1.0.0"

[libraries]
redbytefx-core = { module = "io.github.i-redbyte:redbytefx-core", version.ref = "redbytefx" }
redbytefx-compose = { module = "io.github.i-redbyte:redbytefx-compose", version.ref = "redbytefx" }
redbytefx-stdlib = { module = "io.github.i-redbyte:redbytefx-stdlib", version.ref = "redbytefx" }
```

`build.gradle.kts` модуля:

```kotlin
dependencies {
    implementation(libs.redbytefx.core)
    implementation(libs.redbytefx.compose)
    implementation(libs.redbytefx.stdlib)
}
```

## Модули

| Артефакт | Что даёт |
|----------|----------|
| `redbytefx-core` | DSL для написания эффектов, компилятор, uniform-ы, мост к рантайму, `redbytefx { }` |
| `redbytefx-compose` | `FxController`, `rememberFxController`, `Modifier.redbyteFx`, привязки к Compose |
| `redbytefx-stdlib` | высокоуровневые shader-хелперы для координат, масок, compositing, routing, lighting, SDF и другого |

## Быстрый старт

```kotlin
val waveEffect = run {
    var amplitude: FxParam.Float? = null
    var frequency: FxParam.Float? = null
    val effect = redbytefx {
        val amp = uniformFloat(0f, "wave_amplitude")
        val freq = uniformFloat(0.08f, "wave_frequency")
        amplitude = amp
        frequency = freq
        val offset = float2(0f, sin(fragCoord.x * freq) * amp)
        sample(fragCoord + offset)
    }
    Triple(effect, amplitude!!, frequency!!)
}

@Composable
fun WaveLabel(amp: Float, freq: Float) {
    val fx = rememberFxController(waveEffect.first)
    fx.bindFloat(waveEffect.second, amp)
    fx.bindFloat(waveEffect.third, freq)
    Text("RedByteFX", modifier = Modifier.redbyteFx(fx))
}
```

Типовой рабочий цикл:

1. Один раз компилируете эффект через `redbytefx { ... }`.
2. Если нужно проверить форму реального шейдера, смотрите `effect.agslSource()`.
3. Сохраняете `FxParam` handles для runtime-привязки.
4. Создаёте `FxInstance` или `FxController`.
5. Привязываете значения и применяете эффект.

## Минимально-полная документация по DSL

### Координаты и sampling

- `fragCoord` — текущая координата фрагмента в пиксельном пространстве.
- `resolution` — текущий размер области рендера в пикселях.
- `sample()` читает входной контент в пиксельном пространстве и по умолчанию использует `fragCoord`.
- `sample(coord)` читает вход в другой точке пиксельного пространства.
- `sampleUnclamped(...)` читает без ограничения координат и нужен в основном для осознанных экспериментов на границах.
- В `stdlib` функция `normalizedUv()` переводит координаты из пиксельного пространства в нормализованный UV `[0, 1]²`.
- В `stdlib` функция `sampleUv(uv)` делает обратное: превращает UV в реальный sampled read.
- В `stdlib` функции `centeredUv(...)` и `aspectCenteredUv(...)` удобно использовать как стартовую точку для масок, lighting, SDF и локальной scene-математики.

### Uniform-ы и runtime handles

- `uniformFloat`, `uniformFloat2`, `uniformFloat3`, `uniformFloat4` объявляют uniform-ы эффекта.
- `uniformTime()` — это специализированный float-uniform для времени.
- `autoUniformFloat`, `autoUniformTime`, `autoUniformFloat2/3/4` автоматически выводят читаемое имя из имени delegated property в Kotlin.
- Возвращаемые `FxParam.*` — это одновременно и выражения внутри DSL, и handles для runtime-привязки.
- Uniform-handle всегда привязан к конкретному эффекту: параметр из одного `redbytefx { ... }` нельзя безопасно использовать с другим эффектом.

Пример:

```kotlin
val effect = redbytefx {
    val amount by autoUniformFloat(0.5f)
    val shift = uniformFloat2(0f, 0f, "shift")
    sample(fragCoord + shift * amount)
}
```

### Типы выражений

- `FloatExpr` — скалярное float-выражение.
- `BoolExpr` — логическое выражение.
- `Float2Expr`, `Float3Expr`, `Float4Expr` — векторные выражения.
- `ColorExpr` — цветовой результат, который маппится в AGSL `half4`.
- Это не runtime-значения, а типизированные узлы выражений, которые компилятор превращает в AGSL.

### Конструкторы значений

- `float(...)` — явное scalar-выражение.
- `float2(...)`, `float3(...)`, `float4(...)` — конструкторы векторов.
- `color(r, g, b, a)` и `color(rgb, a)` — конструкторы цветового результата.

Пример:

```kotlin
val uv = fragCoord / resolution
val tint = color(0.2f, 0.8f, 1f, 1f)
val offset = float2(0f, sin(uv.x * 12f) * 6f)
sample(fragCoord + offset) * tint.a
```

### Операторы и условия

- Для scalar-выражений доступны `+`, `-`, `*`, `/`.
- Для векторов доступны вектор-вектор и вектор-скаляр операции там, где это имеет смысл.
- Целые литералы вроде `1 - amount` и `2 * uv` поддерживаются специально для удобного переноса AGSL-стиля в Kotlin.
- Сравнения выполняются через `lt`, `lte`, `gt`, `gte`, `eq`, `neq`.
- `ifElse(condition, ifTrue, ifFalse)` выбирает между выражениями внутри шейдера.
- Для `BoolExpr` есть `and`, `or` и `!`.

Пример:

```kotlin
val mask = ifElse(amount gt 0.5f, 1f, 0f)
```

### Базовые math/color builtins

В `core` уже есть привычные базовые блоки, близкие по смыслу к AGSL:

- `mix`, `clamp`, `smoothstep`, `step`, `saturate`
- `sin`, `cos`, `atan`, `pow`, `sqrt`, `abs`, `floor`, `ceil`, `fract`
- `min`, `max`, `mod`
- `dot`, `length`, `distance`, `normalize`
- `luminance`, `grayscale`

Идея в том, чтобы сохранять близость к AGSL-лексике, но при этом получить типизированные и более удобные Kotlin-вызовы.

### Локальные переменные через `let(...)`

- `let(...)` сохраняет выражение в локальную переменную внутри сгенерированного AGSL.
- Это один из главных инструментов, чтобы большие эффекты оставались читаемыми.
- Если передать имя, RedByteFX нормализует его в безопасный AGSL identifier.

Пример:

```kotlin
val base = let(sample(), "base")
val luma = let(luminance(base), "luma")
val mono = let(grayscale(base), "mono")
mix(base, mono, luma)
```

### Переиспользуемые функции через `fn(...)` и `fnN(...)`

- `fn(...)` создаёт переиспользуемые AGSL helper-функции с arity от 0 до 4.
- `fnN(...)` покрывает кейсы с пятью и более параметрами.
- Это удобно, когда вы хотите, чтобы в итоговом AGSL сохранялась граница функции, а не всё сливалось в одно большое дерево выражений.

Пример:

```kotlin
val palette = fn(
    name = "palette_rgb",
    arg1 = FloatType,
    arg2 = FloatType,
    returns = Float3Type
) { tone, warmth ->
    val phase = let(tone * 6.2831855f, "phase")
    float3(
        0.24f + 0.45f * sin(phase + warmth * 0.90f + 0.10f),
        0.30f + 0.42f * sin(phase + warmth * 1.50f + 2.10f),
        0.42f + 0.36f * sin(phase + warmth * 2.10f + 4.20f)
    )
}
```

### Трансформации в `core`

Даже в `core` уже есть полезные хелперы для написания эффектов, которые остаются близкими к исходной AGSL-модели:

- `flipX`, `flipY`
- `mirrorX`, `mirrorY`
- базовые преобразования координат для прямого переноса логики из raw AGSL

Когда задача — буквально перевести существующий AGSL в Kotlin DSL, лучше начинать именно отсюда.

### Высокоуровневые хелперы в `stdlib`

`redbytefx-stdlib` добавляет готовые рецепты поверх базового DSL. Полезные стартовые группы:

- координаты: `normalizedUv`, `sampleUv`, `centeredUv`, `aspectCenteredUv`
- маски и reveal: `circleMask`, `rectMask`, `ringMask`, `horizontalReveal`, `verticalReveal`, `radialReveal`
- compositing: `maskedMix`, `alphaMask`, `maskedScreen`, `maskedOverlay`, `blendMultiply`, `blendScreen`, `blendOverlay`
- lighting и shaping: `rimLight`, `centerGlow`, SDF-хелперы, routing-хелперы, noise и warp-хелперы

Хорошее практическое правило:

1. Оставайтесь в `core`, если переносите raw AGSL почти один в один.
2. Переходите в `stdlib`, когда в шейдере начинают повторяться coordinate-, mask-, compositing- или shaping-паттерны.

### Runtime-поверхность

- `FxEffect` — неизменяемое описание уже скомпилированного эффекта.
- `FxEffect.agslSource()` возвращает сгенерированный AGSL для отладки, тестов и документации.
- `FxInstance` — низкоуровневый изменяемый runtime API для императивных хостов.
- `FxController` из `redbytefx-compose` — Compose-friendly обёртка над одним runtime instance с удобной логикой привязки значений и инвалидирования.
- На один render target нужен один controller или один runtime instance.

## Участие в проекте

Проект открыт для внешнего контрибьюта.

Пожалуйста, по возможности сохраняйте существующую стилистику проекта:

- поддерживайте Kotlin-first характер DSL
- не жертвуйте читаемостью ради «хитрых» сокращений
- сохраняйте понятную структуру generated AGSL
- придерживайтесь текущих принципов именования, документации и примеров из sample

### Как сделать PR

1. Сделайте fork и создайте отдельную ветку под одно конкретное изменение.
2. Постарайтесь держать PR небольшим, но завершённым по смыслу.
3. Прогоните `./gradlew qualityCheck` и нужные тесты модулей.
4. Если меняется публичный API или поведение, обновите документацию и при необходимости sample.
5. В описании PR коротко укажите: какая была проблема, что изменено и как вы это проверили.

## Лицензия

Проект распространяется по лицензии [MIT](LICENSE).
