[English](README.md) · **Русский**

# RedByteFX

**RedByteFX** - это Kotlin DSL, который компилирует одну типизированную алгебру шейдера в Android AGSL, OpenGL ES 3.0, compute OpenGL ES 3.1 и geometry с tessellation OpenGL ES 3.2.

Пишется Kotlin, а не строка шейдера. Компилятор выпускает текст, который реально исполняет платформа:

`shader(target) { ... } -> ShaderProgram -> AGSL RuntimeShader, программа GLES 3.0, compute-программа GLES 3.1 или программа GLES 3.2`

**Платформа:** `minSdk` библиотеки **24**. **AGSL** (`ShaderTarget.Agsl`, `rememberFxController`, `redbyteFx`) требует **API 31+** (`RuntimeShader`); ниже - `AgslNotSupportedException` и предупреждение IDE через `@RequiresApi`. **OpenGL ES** - с API 24 через `redbytefx-gl` и `redbytefx-gl-compose` (`GlSurface`). GLES: GLSL ES 3.00, 3.10 compute, 3.20 geometry/tessellation. Справочник API: [GitHub Pages](https://i-redbyte.github.io/redbytefx/).

## Что вы пишете

Один носитель значения, `Expr<T>`. Ранг именной (`Vec2`, `Vec3`, `Vec4` и матрицы). Точность - параметр: `Flt<High>` это highp float, `Flt<Med>` это mediump float. Цвет - `Vec4` из mediump-чисел, его даёт `color(...)`. Отдельного типа цвета нет. Частые случаи пишутся как `HighFloat`, `HighVec2`, `HighVec3`, `HighVec4`, `MedFloat`, `MedVec2`, `MedVec3` и `MedVec4`. Рукояти этих форм - `HighFloatUniform`, `HighVec2Uniform`, `HighVec3Uniform` и `HighVec4Uniform`.

`Uniform<T>` - рукоять одной программы `ShaderProgram`. Внутри стадии читается `uniform.expr`. Саму рукоять пишут Compose и GLES-рантайм. Рукоять одной программы недействительна для другой.

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

AGSL - только фрагмент. Сгенерированный вход остаётся `half4 main(float2 fragCoord)`: этого требует `RuntimeShader`. Смотреть текст: `wave.agslSource()`.

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

OpenGL ES 3.1 - это compute-программа. В ней нет вершины и фрагмента. Один `storageBlock` использует std430, а `compute(localSizeX)` пишет его поля. `compute(localSizeX, localSizeY, localSizeZ)` задаёт все три размера группы; `compute(n)` по-прежнему пишет только `local_size_x`. Текст: `computeSource()`. EGL-контекст этой программы - OpenGL ES 3.1. В compute `globalId`, `localId` и `workGroupId` - это `ivec3`, в тексте `ivec3(gl_GlobalInvocationID)`, `ivec3(gl_LocalInvocationID)` и `ivec3(gl_WorkGroupID)`. В storage-блоке есть `floatArray`, `vec2Array`, `vec3Array` и `vec4Array`. Массив с числом сохраняет размер. Последнее поле может быть без числа и пишется как `type name[]`; неразмерное поле не в конце блока отвергается при закрытии блока. Элемент индексируется выражением `int`, например `values[globalId.x]`, и пишется через `store`. `packStd430` принимает логические компоненты: у `vec3` три числа, а дырку до шага 16 байт нулями дописывает packer. `GlProgramRuntime.dispatch(x, y, z)` запускает уже слинкованную compute-программу. `shared`-массивы и `barrier()` законны только в compute. `uniformBlock` можно читать из compute; AGSL по-прежнему отвергает uniform-блок.

```kotlin
val cells = shader(ShaderTarget.Gles31) {
    storageBlock("cells") {
        val value = vec4("value")
        compute(64) { value.store(value) }
    }
}
```

OpenGL ES 3.2 сохраняет вершину и фрагмент и добавляет необязательные geometry и tessellation. Tessellation - это вместе control и evaluation. EGL-контекст этой программы - OpenGL ES 3.2. Дополнительные тексты: `geometrySource()`, `tessControlSource()` и `tessEvalSource()`.

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

Varying, который пишет вершина и читает фрагмент, объявляется на каждой стадии между ними: вершина, tessellation control, tessellation evaluation, geometry, затем фрагмент. Отсутствующие стадии пропускаются. На каждой границе `out` предыдущей стадии совпадает с `in` следующей по имени, типу и precision. Если в программе есть geometry или tessellation, эти объявления - члены одного блока `rb_pipe`. На всех стадиях программы один и тот же набор: varying, которые записала вершина и которые читает фрагмент или промежуточная стадия, в порядке объявления. Объявленный и нигде не записанный varying в блок не входит. Tessellation control копирует `tc_in[gl_InvocationID]` в `tc_out[gl_InvocationID]`. Geometry берёт индекс той входной вершины, которую только что записали в `gl_Position`: один `gl_in[k]`, в том числе индекс `repeat`, становится `gs_in[k]` непосредственно перед этим `EmitVertex`. Та же прокидка работает, когда этот `gl_Position` записан до `repeat` или `whenTrue`, который эмитит, и не используется повторно после emit, который её уже забрал. Несколько индексов или позиция без `gl_in`, пока varying ещё не записан, - ошибка, и текст просит явный `varying.set` перед этим emit. `varying.set` между этим `gl_Position` и emit заменяет прокидку только этого varying и только этого emit. Tessellation evaluation интерполирует `gl_TessCoord`, если стадия этот varying не писала. Одна входная вершина копируется из `te_in[0]`. Triangles взвешивают три вершины патча через `gl_TessCoord`. Isolines делают `mix` двух вершин по `gl_TessCoord.x`. Quads делают билинейный `mix` углов `(0,0)`, `(1,0)`, `(1,1)` и `(0,1)` по `gl_TessCoord.xy`. Явная запись заменяет эту интерполяцию. Стадия, у которой есть `rb_pipe` и которая пишет `gl_Position`, переобъявляет `gl_PerVertex` только с `vec4 gl_Position` до пользовательского блока. Uniform, прочитанный в geometry или tessellation, объявляется в исходнике этой стадии. `glIn(index)` и `varying.at(index)` допускают динамический `int`; проверка границ патча на этапе компиляции есть только для константных индексов.

Тестовое приложение открывает примеры AGSL и OpenGL с разных экранов. Каждая OpenGL-сцена - это `GLSurfaceView`, который линкует `ShaderProgram`. В списке есть треугольник, экранные сферы, красный флаг, освещённое тело, один std140-блок камеры, сравнения векторов, радужная арка общей функцией, шипы и каркас geometry-стадии, океан на tessellation, звёздное небо с молниями, объёмная надпись и пять небольших игр: туннель, лабиринт, арканоид, налёт и спуск. Интерфейс примеров остаётся английским, пока язык устройства не русский. Geometry и tessellation требуют контекст OpenGL ES 3.2. `Modifier.redbyteFx` остаётся на AGSL и программу GLES не запускает.

## Установка

```kotlin
dependencies {
    implementation("io.github.i-redbyte:redbytefx-core:1.1.0")
    implementation("io.github.i-redbyte:redbytefx-gl:1.1.0")
    implementation("io.github.i-redbyte:redbytefx-compose:1.1.0")
    implementation("io.github.i-redbyte:redbytefx-stdlib:1.1.0")
}
```

| Артефакт | Роль |
|----------|------|
| `redbytefx-core` | `shader`, `Expr`, uniform-ы, спеллинг AGSL, GLSL ES 3.00, compute GLSL ES 3.10 и GLSL ES 3.20 |
| `redbytefx-gl` | Линковка GLES 3.0, 3.1 и 3.2, запись uniform и матриц, привязка 2D и куба, compute `dispatch` и загрузка storage на потоке EGL |
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

Стадии - небольшой автомат. Uniform, sampler и varying объявляются на программе. Код шейдера пишут `fragment { }` и `vertex { }`. `vertex` есть у `ShaderTarget.Gles30` и `ShaderTarget.Gles32`. `geometry`, `tessControl` и `tessEval` есть у `ShaderTarget.Gles32`. `compute` есть у `ShaderTarget.Gles31`.

- `fragCoord` и `resolution` - входы AGSL-фрагмента, в пикселях.
- `sample()` читает дочерний шейдер. Это законно только в AGSL-фрагменте и не внутри `fn`.
- `texture(sampler, uv)` законен только в GLES-фрагменте.
- `attributeVec2`, `attributeVec3`, `attributeVec4` и `glPosition` законны в GLES-вершине. `glPosition` также законен в geometry и tessellation evaluation.
- `varyingFloat`, `varyingVec2`, `varyingVec3` и `varyingVec4` имеют precision highp. Фрагмент читает varying только если его писала вершина.
- `let(expr, "name")` даёт локальной переменной имя в сгенерированном шейдере.
- `local(initializer, "name")` объявляет float, float-вектор или int до команд стадии, а присваивание пишет оператором. `set` обновляет её, в том числе внутри `repeat` и `whenTrue`. `local` внутри `repeat` отвергается. Значение можно вернуть из фрагмента или записать в `gl_Position`.
- `whenTrue(condition) { }` ставит команды в `if (condition)`. Ветки `else` нет.
- `repeat(count) { i -> ... }` - цикл со счётчиком, `for (int i = 0; i < count; ++i)`, где `count` от 1 до 64. В теле можно вложить ещё один `repeat`.
- `discard()` и `discard(condition)` законны только во фрагменте, в том числе внутри фрагментной `fn`. Условная форма - `if (condition) discard`. `dFdx`, `dFdy` и `fwidth` тоже только во фрагменте.
- `fn` принимает от 0 до 8 аргументов. Свидетель задаёт стёртую форму: `fn(0f.lit, 0f.lit, "name") { p0, p1 -> ... }`. Параметры называются `p0`…`p7`. Функции не вкладываются, не объявляют uniform, и рекурсия отвергается. Те же правила действуют в compute, geometry и tessellation. Вызов из чужой стадии отвергается.
- `uniformInt` - скалярный int и на AGSL (`setIntUniform`), и на GLES (`uniform1i`). `uniformBool`, `uniformMat2`, `uniformMat3` и `uniformMat4` есть только на GLES; AGSL их отвергает. Матрицы заливаются column-major с `transpose` false, а дефолт на link - единичная. `samplerCube` и `textureCube` законны только в GLES-фрагменте.

```kotlin
shader(ShaderTarget.Agsl) {
    fragment {
        val gain = fn(0f.lit, "gain") { p0 -> saturate(p0) }
        val tone = gain(sample().r.toHigh()).toMed()
        color(tone, tone, tone, sample().a)
    }
}
```

Скалярные float одной точности поддерживают `+`, `-`, `*`, `/`. Векторы поддерживают вектор-вектор и вектор-скаляр той же точности. Сравнения: `gt`, `lt`, `ge`, `le`, `eq`, `ne`. `gte` - другое написание `ge`. `ifElse(condition, ifTrue, ifFalse)` - это тернарный выбор. Булевы скаляры поддерживают `and`, `or` и `not`.

Математика с одним и тем же вызовом в обоих языках: `sin`, `cos`, `abs`, `floor`, `ceil`, `fract`, `sqrt`, `min`, `max`, `mod`, `pow`, `mix`, `clamp`, `smoothstep`, `step`, `saturate`, `dot`, `length`. `sqrt` - это вызов GLSL, отрицательный аргумент не заменяется.

`redbytefx-stdlib` добавляет рецепты координат и композитинга на том же receiver `fragment`: `normalizedUv`, `sampleUv`, `centeredUv`, маски, reveal, смешивание и SDF вроде `sdCircle` и `softFill`. У `softFill` и `stroke` ширина пера по-прежнему аргумент вызывающего. `softFillScreen(distance)` и `strokeScreen(distance, width)` берут ширину края из `fwidth(distance)` и законны только во фрагменте.

## Рантайм

`ShaderProgram` неизменяем и читается с любого потока. Граф выражений и сгенерированные строки существуют на компиляции.

Воспроизведение AGSL - `program.newAgslInstance()`. Неизменившийся uniform не идёт на GPU. `set` пишет float и векторы highp и mediump, а `uniformInt` уходит через `setIntUniform`. Воспроизведение GLES - `GlProgramRuntime` в `redbytefx-gl`: создавать, линковать и уничтожать его на потоке EGL-контекста. `destroy` отвязывает программу до удаления. `set` пишет эти float, `uniform1i` для `uniformInt` и для `uniformBool` как 0 или 1, и column-major `uniformMatrix*fv` для `mat2`, `mat3` и `mat4` с transpose false. При линковке заливаются дефолты, в том числе единичная матрица, если свою не передали, а то же значение не вызывает `glUniform` повторно. `bind` для `sampler2D` использует `GL_TEXTURE_2D`, для `samplerCube` - `GL_TEXTURE_CUBE_MAP`. Оба делят `GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS`. `dispatch(x, y, z)` запускает слинкованную программу GLES 3.1 и затем вызывает `glMemoryBarrier` с `GL_SHADER_STORAGE_BARRIER_BIT`. `runtime.set(storageBlock, floats)` упаковывает логические значения std430 и пропускает загрузку, когда эти байты не изменились. Если размер упакованного буфера изменился, буфер выделяется заново. `StorageBlock.byteSize` - это размер std430 полностью размерного блока. Блок с неразмерным массивом сообщает размер через `byteSize(valueCount)`, и этот размер совпадает с `packStd430`. `uniformMedium`, `uniformMediumVec2`, `uniformMediumVec3` и `uniformMediumVec4` объявляют формы mediump; на AGSL они по-прежнему идут через `setFloatUniform`. `FxController` привязывает float-uniform и `uniformInt` для Compose.

## Чего в этой версии нет

Компилятор не выпускает GLES 2.0 и desktop GL. Он не выпускает `while`, рекурсию, вложенные функции и границу цикла, которая не константа от 1 до 64. `uniformBool`, `uniformMat2`, `uniformMat3`, `uniformMat4` и `samplerCube` на AGSL отвергаются.

## Участие

Перед PR запускайте `./gradlew qualityCheck`. Эти ворота - модульные тесты, сборка sample и detekt. Тесты GLES на устройстве - `./gradlew :redbytefx-gl:connectedDebugAndroidTest`, в `qualityCheck` они не входят.

## Лицензия

[MIT](LICENSE)
