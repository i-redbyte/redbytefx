[English](README.md) · **Русский**

# RedByteFX

[![JitPack](https://jitpack.io/v/i-redbyte/redbytefx.svg)](https://jitpack.io/#i-redbyte/redbytefx)

**RedByteFX** — это Kotlin-first DSL для Android AGSL, который избавляет от необходимости писать большие строковые шейдеры вручную.

AGSL сам по себе мощный, но в обычной Kotlin-разработке он ощущается довольно чужеродно:

- логика шейдера живёт внутри строк, а не в коде
- `uniform`-ы объявляются руками и в них легко ошибиться
- даже простые эффекты быстро обрастают boilerplate
- для Compose обычно нужен дополнительный glue code
- отладка часто начинается с вопроса: "в какой строке я сейчас всё сломал?"

RedByteFX сохраняет тот же реальный Android-пайплайн:

`Kotlin DSL -> сгенерированный AGSL -> RuntimeShader / RenderEffect`

То есть рантайм остаётся нативным и предсказуемым, но сам процесс написания шейдера становится гораздо приятнее: типизированные выражения, читаемые локальные переменные через `let(...)`, переиспользуемые функции через `fn(...)`, удобные привязки к Compose и возможность в любой момент посмотреть итоговый AGSL через `agslSource()`.

Коротко: **меньше боли от строкового AGSL, меньше клея, никакого второго рантайма и никакой потери производительности или гибкости ради удобства.**

## Чистый AGSL vs RedByteFX

Одна и та же идея, одна и та же математика, но очень разный опыт разработки.

### Написано на AGSL вручную

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

Математика та же, но теперь это обычный Kotlin-код: типы, автодополнение IDE, нормальные имена, рефакторинг без боли и параметры, которые можно привязать из рантайма без плясок с сырой AGSL-строкой.

## Пример уровня sample

Ниже реальный кусок из demo-приложения. На чистом AGSL такие эффекты довольно быстро разрастаются в длинный блок со служебными `uniform`, временными переменными и повторяющимся композитингом. В RedByteFX он остаётся читаемым:

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

То есть контроль над шейдером никуда не девается, но код наконец выглядит так, будто его писал Kotlin-разработчик, а не человек, которого заперли внутри строкового литерала.

## Подключение

Публичные релизы доступны через JitPack.

Добавьте репозиторий в `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io") {
            content {
                includeGroup("com.github.i-redbyte.redbytefx")
            }
        }
    }
}
```

После этого подключите нужные модули:

```kotlin
dependencies {
    implementation("com.github.i-redbyte.redbytefx:redbytefx-core:1.0.0")
    implementation("com.github.i-redbyte.redbytefx:redbytefx-compose:1.0.0")
    implementation("com.github.i-redbyte.redbytefx:redbytefx-stdlib:1.0.0")
}
```

| Артефакт | Что даёт |
|----------|----------|
| `redbytefx-core` | DSL, компилятор, uniform-ы, runtime bridge, `redbytefx { }` |
| `redbytefx-compose` | `FxController`, `rememberFxController`, `Modifier.redbyteFx`, привязки к Compose |
| `redbytefx-stdlib` | Готовые шейдерные хелперы: маски, градиенты, blends, noise, SDF, routing, lighting и другое |

**Требования:** Android **API 33+**. Модуль `redbytefx-compose` нужен только если хотите Compose-обвязку.

## Как с этим обычно работают

1. Пишете эффект через `redbytefx { ... }`.
2. Если нужно увидеть реальную форму шейдера, смотрите `effect.agslSource()`.
3. Создаёте или `remember`-ите runtime instance/controller.
4. Привязываете `uniform`-ы к состоянию, времени или анимациям.
5. Применяете эффект через `Modifier.redbyteFx(...)` или через низкоуровневый runtime API.

Пример с Compose:

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

## Ключевые принципы библиотеки

- **Сначала Kotlin.** Шейдерный код должен ощущаться нативно для Kotlin, а не как шаблонизация строк.
- **Типизированные выражения и uniform-ы.** Ошибок меньше, рефакторинг спокойнее.
- **Читаемый результат.** `let(...)`, `fn(...)` и `agslSource()` помогают не терять связь с финальным AGSL.
- **Никакого своего рендера.** На выходе всё равно используется стандартный Android `RuntimeShader` / `RenderEffect`.
- **Слои по ролям.** `core` даёт язык, `compose` даёт runtime bindings, `stdlib` даёт переиспользуемые высокоуровневые рецепты.

## Модули репозитория

| Модуль | Роль |
|--------|------|
| `:redbytefx-core` | DSL для авторинга, компилятор, мост к рантайму |
| `:redbytefx-compose` | Интеграция с Compose |
| `:redbytefx-stdlib` | Переиспользуемые высокоуровневые шейдерные хелперы |
| `:sample` | Демо-приложение с реальными эффектами и предпросмотром сгенерированного AGSL |

## Участие в проекте

Issues, идеи, PR'ы, эксперименты и улучшения в духе "сделал AGSL чуть менее ворчливым" очень приветствуются.

Небольшие сфокусированные изменения проще ревьюить, но и большие идеи тоже welcome. Если замах планируется серьёзный, лучше сначала открыть issue и синхронизироваться по направлению, чтобы потом не героически переписывать полмира шейдеров в два часа ночи.

## Лицензия

Проект распространяется по лицензии [MIT](LICENSE).
