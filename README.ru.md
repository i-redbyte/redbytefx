[English](README.md) · **Русский**

# RedByteFX

**RedByteFX** — это Kotlin DSL, который компилирует одну типизированную алгебру шейдера в Android AGSL, OpenGL ES 3.0, compute OpenGL ES 3.1 и geometry с tessellation OpenGL ES 3.2.

Пишется Kotlin, а не строка шейдера. Компилятор выпускает текст, который реально исполняет платформа:

`shader(target) { ... } -> ShaderProgram -> AGSL RuntimeShader, программа GLES 3.0, compute-программа GLES 3.1 или программа GLES 3.2`

**Платформа:** Android API 33+. AGSL требует `RuntimeShader`. Выход OpenGL ES — это GLSL ES 3.00, вершина и фрагмент, GLSL ES 3.10 compute или GLSL ES 3.20 с geometry и tessellation.

## Что вы пишете

Один носитель значения, `Expr<T>`. Ранг именной (`Vec2`, `Vec3`, `Vec4` и матрицы). Точность — параметр: `Flt<High>` это highp float, `Flt<Med>` это mediump float. Цвет — `Vec4` из mediump-чисел, его даёт `color(...)`. Отдельного типа цвета нет. Частые случаи пишутся как `HighFloat`, `HighVec2`, `HighVec3`, `HighVec4`, `MedFloat`, `MedVec2`, `MedVec3` и `MedVec4`. Рукояти этих форм — `HighFloatUniform`, `HighVec2Uniform`, `HighVec3Uniform` и `HighVec4Uniform`.

`Uniform<T>` — рукоять одной программы `ShaderProgram`. Внутри стадии читается `uniform.expr`. Саму рукоять пишут Compose и GLES-рантайм. Рукоять одной программы недействительна для другой.

```kotlin
val wave = shader(ShaderTarget.Agsl) {
    val amplitude = uniform("wave_amplitude", 0f)
    val frequency = uniform("wave_frequency", 0.08f)
    fragment {
        val offset = float2(0f, sin(fragCoord.x * frequency.expr) * amplitude.expr)
        sample(fragCoord + offset)
    }
}
```

AGSL — только фрагмент. Сгенерированный вход остаётся `half4 main(float2 fragCoord)`: этого требует `RuntimeShader`. Смотреть текст: `wave.agslSource()`.

OpenGL ES 3.0 требует обе стадии. Varying, записанный в вершине, читается во фрагменте. Тексты: `vertexSource()` и `fragmentSource()`.

```kotlin
val pulse = shader(ShaderTarget.Gles30) {
    val time = uniformTime()
    vertex {
        val position = attributeVec2("position")
        glPosition(vec4(position.x, position.y, 0f.lit, 1f.lit))
    }
    fragment {
        val wave = sin(time.expr)
        vec4(0.5f.lit + wave * 0.5f.lit, 0.15f.lit, 0.85f.lit, 1f.lit)
    }
}
```

OpenGL ES 3.1 — это compute-программа. В ней нет вершины и фрагмента. Один `storageBlock` использует std430, а `compute(localSizeX)` пишет его поля. Текст: `computeSource()`. EGL-контекст этой программы — OpenGL ES 3.1.

```kotlin
val cells = shader(ShaderTarget.Gles31) {
    storageBlock("cells") {
        val value = vec4("value")
        compute(64) { value.store(value) }
    }
}
```

OpenGL ES 3.2 сохраняет вершину и фрагмент и добавляет необязательные geometry и tessellation. Tessellation — это вместе control и evaluation. EGL-контекст этой программы — OpenGL ES 3.2. Дополнительные тексты: `geometrySource()`, `tessControlSource()` и `tessEvalSource()`.

```kotlin
val patch = shader(ShaderTarget.Gles32) {
    vertex { glPosition(attributeVec4("position")) }
    tessControl(3) {
        tessLevelOuter(0, 1f.lit)
        tessLevelOuter(1, 1f.lit)
        tessLevelOuter(2, 1f.lit)
        tessLevelInner(0, 1f.lit)
        passPosition()
    }
    tessEval(TessPrimitive.Triangles) { glPosition(glIn(0)) }
    geometry(GeometryInput.Triangles, GeometryOutput.TriangleStrip, 3) {
        glPosition(glIn(0))
        emitVertex()
        endPrimitive()
    }
    fragment { vec4(1f.lit, 0f.lit, 0f.lit, 1f.lit) }
}
```

`Modifier.redbyteFx` накладывает AGSL `RenderEffect`. Программу GLES он не запускает. Тестовое приложение открывает примеры AGSL и OpenGL с разных экранов. В списке OpenGL есть треугольник и сцена Spheres на своём `GLSurfaceView`.

## Установка

```kotlin
dependencies {
    implementation("io.github.i-redbyte:redbytefx-core:1.0.0")
    implementation("io.github.i-redbyte:redbytefx-gl:1.0.0")
    implementation("io.github.i-redbyte:redbytefx-compose:1.0.0")
    implementation("io.github.i-redbyte:redbytefx-stdlib:1.0.0")
}
```

| Артефакт | Роль |
|----------|------|
| `redbytefx-core` | `shader`, `Expr`, uniform-ы, спеллинг AGSL, GLSL ES 3.00, compute GLSL ES 3.10 и GLSL ES 3.20 |
| `redbytefx-gl` | Линковка GLES 3.0, 3.1 и 3.2, запись uniform-ов и загрузка storage на потоке EGL |
| `redbytefx-compose` | `rememberFxController`, `FxController`, `Modifier.redbyteFx` для AGSL |
| `redbytefx-stdlib` | Координаты, маски, композитинг, SDF и родственные помощники поверх того же DSL |

## Compose

Uniform объявляется на программе и привязывается той же рукоятью.

```kotlin
val program = shader(ShaderTarget.Agsl) {
    val amplitude = uniform("wave_amplitude", 0f)
    val frequency = uniform("wave_frequency", 0.08f)
    fragment {
        val offset = float2(0f, sin(fragCoord.x * frequency.expr) * amplitude.expr)
        sample(fragCoord + offset)
    }
}

@Composable
fun WaveLabel(program: ShaderProgram, amplitude: HighFloatUniform, frequency: HighFloatUniform) {
    val fx = rememberFxController(program)
    fx.bindFloat(amplitude, 12f)
    fx.bindFloat(frequency, 0.08f)
    Text("RedByteFX", modifier = Modifier.redbyteFx(fx))
}
```

`rememberFxController` владеет одним AGSL-рантаймом. На каждую поверхность рисования нужен свой контроллер. `redbyteFx` пишет resolution из размера отрисовки. `RuntimeShader` трогается только с UI-потока.

## Авторство

Стадии — небольшой автомат. Uniform, sampler и varying объявляются на программе. Код шейдера пишут `fragment { }` и `vertex { }`. `vertex` есть у `ShaderTarget.Gles30` и `ShaderTarget.Gles32`. `geometry`, `tessControl` и `tessEval` есть у `ShaderTarget.Gles32`. `compute` есть у `ShaderTarget.Gles31`.

- `fragCoord` и `resolution` — входы AGSL-фрагмента, в пикселях.
- `sample()` читает дочерний шейдер. Это законно только в AGSL-фрагменте и не внутри `fn`.
- `texture(sampler, uv)` законен только в GLES-фрагменте.
- `attributeVec2` и `glPosition` законны только в GLES-вершине.
- `let(expr, "name")` даёт локальной переменной имя в сгенерированном шейдере.
- `fn` принимает 0, 1 или 2 аргумента. Свидетель задаёт стёртую форму: `fn(0f.lit, 0f.lit, "name") { p0, p1 -> ... }`. Параметры называются `p0` и `p1`. Функции не вкладываются и не объявляют uniform.

```kotlin
shader(ShaderTarget.Agsl) {
    fragment {
        val gain = fn(0f.lit, "gain") { p0 -> saturate(p0) }
        val tone = gain(sample().r.toHigh()).toMed()
        color(tone, tone, tone, sample().a)
    }
}
```

Скалярные float одной точности поддерживают `+`, `-`, `*`, `/`. Векторы поддерживают вектор-вектор и вектор-скаляр той же точности. Сравнения: `gt`, `lt`, `ge`, `le`, `eq`, `ne`. `gte` — другое написание `ge`. `ifElse(condition, ifTrue, ifFalse)` — это тернарный выбор. Булевы скаляры поддерживают `and`, `or` и `not`.

Математика с одним и тем же вызовом в обоих языках: `sin`, `cos`, `abs`, `floor`, `ceil`, `fract`, `min`, `max`, `mod`, `pow`, `mix`, `clamp`, `smoothstep`, `step`, `saturate`, `dot`, `length`. Встроенного `sqrt` нет: неотрицательный корень — это `pow(max(x, 0f), 0.5f)`.

`redbytefx-stdlib` добавляет рецепты координат и композитинга на том же receiver `fragment`: `normalizedUv`, `sampleUv`, `centeredUv`, маски, reveal, смешивание и SDF вроде `sdCircle` и `softFill`.

## Рантайм

`ShaderProgram` неизменяем и читается с любого потока. Граф выражений и сгенерированные строки существуют на компиляции.

Воспроизведение AGSL — `program.newAgslInstance()`. Неизменившийся uniform не идёт на GPU. Воспроизведение GLES — `GlProgramRuntime` в `redbytefx-gl`: создавать, линковать и уничтожать его на потоке EGL-контекста. `destroy` отвязывает программу до удаления. `runtime.set(storageBlock, floats)` упаковывает std430 и пропускает загрузку, когда эти байты не изменились.

## Чего в этой версии нет

Компилятор не выпускает GLES 2.0.

## Участие

Перед PR запускайте `./gradlew qualityCheck`. Эти ворота — модульные тесты, сборка sample и detekt. Тесты GLES на устройстве — `./gradlew :redbytefx-gl:connectedDebugAndroidTest`, в `qualityCheck` они не входят.

## Лицензия

[MIT](LICENSE)
