[English](README.md) · **Русский**

# RedByteFX

**RedByteFX** - это Kotlin DSL с двумя поверхностями на одной типизированной алгебре шейдера. **Эффект** - фрагментный шейдер: Android AGSL на API 33+ или GLES с API 24, оба на коротком пути `redbyteFx`. **Сцена** - меш на OpenGL ES с API 24: вершина и фрагмент, буферы, текстуры и вызовы отрисовки. Та же алгебра собирается в compute OpenGL ES 3.1 и в geometry с tessellation OpenGL ES 3.2.

Пишется Kotlin, а не строка шейдера. Компилятор выпускает текст, который реально исполняет платформа:

`shader(target) { ... } -> ShaderProgram -> AGSL RuntimeShader, программа GLES 3.0, compute-программа GLES 3.1 или программа GLES 3.2`

**Платформа:** `minSdk` библиотеки **24**. **AGSL** (`ShaderTarget.Agsl`, `rememberFxController`, `Modifier.redbyteFx`) требует **API 33+** (`RuntimeShader`); ниже - `AgslNotSupportedException` и предупреждение IDE через `@RequiresApi`. **OpenGL ES** - с API 24 через `redbytefx-gl` и `redbytefx-gl-compose`: сцены через `GlSurface`, эффект только из фрагмента через GLES `redbyteFx`. GLES: GLSL ES 3.00, 3.10 compute, 3.20 geometry/tessellation. Справочник API: [GitHub Pages](https://i-redbyte.github.io/redbytefx/).

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

Синус от `x` сдвигает точку выборки вверх и вниз. `frequency` задаёт частоту волны, а `amplitude` — её высоту.

AGSL - только фрагмент. Сгенерированный вход остаётся `half4 main(float2 fragCoord)`: этого требует `RuntimeShader`. Смотреть текст: `wave.agslSource()`.

Сцена - вторая поверхность. Освещённый бокс ниже только для OpenGL ES: `shader(ShaderTarget.Agsl)` отвергает `uniformMat4`, поэтому AGSL его не соберёт.

```kotlin
val crate = shader(ShaderTarget.Gles30) {
    val view = uniformMat4("view")
    val projection = uniformMat4("projection")
    vertex {
        val position = attributeVec3("position")
        val clip = projection.expr * (view.expr * vec4(position.x, position.y, position.z, 1f.lit))
        glPosition(clip)
    }
    fragment {
        vec4(0.85f.lit, 0.45f.lit, 0.2f.lit, 1f.lit)
    }
}
```

Координаты каждой вершины умножаются на матрицы камеры и проекции, чтобы попасть на экран. Фрагментный этап красит все видимые пиксели одинаковым оранжевым цветом; в этом коротком примере освещение ещё не вычисляется.

GLES-**сцена** требует обе стадии. Varying, записанный в вершине, читается во фрагменте. Тексты: `vertexSource()` и `fragmentSource()`.

GLES-**эффект** может обойтись без `vertex { }`. Компилятор подставляет полноэкранный треугольник с атрибутом `a_corner` (та же раскладка, что у `screenMesh`). Такую программу рисует GLES `redbyteFx`. Geometry и tessellation по-прежнему требуют явную вершину.

```kotlin
val pulse = shader(ShaderTarget.Gles30) {
    val time = uniformTime()
    fragment {
        val wave = sin(time.expr)
        vec4(0.5f.lit + wave * 0.5f.lit, 0.15f.lit, 0.85f.lit, 1f.lit)
    }
}
```

`sin(time)` меняется от −1 до 1. Умножение на 0.5 и прибавление 0.5 превращает его в значение красного канала от 0 до 1.

OpenGL ES 3.1 - это compute-программа. В ней нет вершины и фрагмента. Каждый `storageBlock` использует std430, а `compute(localSizeX)` пишет его поля. Несколько storage-блоков законны; точка привязки — порядок объявления с нуля, и один блок остаётся на 0. `compute(localSizeX, localSizeY, localSizeZ)` задаёт все три размера группы; `compute(n)` по-прежнему пишет только `local_size_x`. Текст: `computeSource()`. EGL-контекст этой программы - OpenGL ES 3.1. В compute `globalId`, `localId` и `workGroupId` - это `ivec3`, в тексте `ivec3(gl_GlobalInvocationID)`, `ivec3(gl_LocalInvocationID)` и `ivec3(gl_WorkGroupID)`. В storage-блоке есть `floatArray`, `vec2Array`, `vec3Array` и `vec4Array`. Массив с числом сохраняет размер. Последнее поле может быть без числа и пишется как `type name[]`; неразмерное поле не в конце блока отвергается при закрытии блока. Элемент индексируется выражением `int`, например `values[globalId.x]`, и пишется через `store`. `packStd430` принимает логические компоненты: у `vec3` три числа, а дырку до шага 16 байт нулями дописывает packer. `GlProgramRuntime.dispatch(x, y, z)` запускает уже слинкованную compute-программу. `shared`-массивы и `barrier()` законны только в compute. `uniformBlock` можно читать из compute; AGSL по-прежнему отвергает uniform-блок. Несколько uniform-блоков законны в одной программе. Точка привязки — порядок объявления. GLSL ES 3.10 и 3.20 пишут `layout(std140, binding = N)`. GLSL ES 3.00 пишет `layout(std140)`, а точку ставит рантайм: это слово язык отвергает. В его полях бывают размерные `floatArray`, `vec2Array`, `vec3Array`, `vec4Array`, `mat2`, `mat3` и `mat4`. `packStd140` и `unpackStd140` берут логические float и пропускают padding std140. Неразмерный массив остаётся только у `storageBlock`.

```kotlin
val cells = shader(ShaderTarget.Gles31) {
    storageBlock("cells") {
        val value = vec4("value")
        compute(64) { value.store(value) }
    }
}
```

Этот минимальный compute-пример записывает значение обратно без изменений. Он показывает работу с памятью, но пока не преобразует данные математически.

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

Все уровни тесселяции равны 1, поэтому участок не дробится. Следующие этапы передают входную позицию дальше, а фрагментный этап красит её в красный цвет.

Varying, который пишет вершина и читает фрагмент, объявляется на каждой стадии между ними: вершина, tessellation control, tessellation evaluation, geometry, затем фрагмент. Отсутствующие стадии пропускаются. На каждой границе `out` предыдущей стадии совпадает с `in` следующей по имени, типу и precision. Если в программе есть geometry или tessellation, эти объявления - члены одного блока `rb_pipe`. На всех стадиях программы один и тот же набор: varying, которые записала вершина и которые читает фрагмент или промежуточная стадия, в порядке объявления. Объявленный и нигде не записанный varying в блок не входит. Tessellation control копирует `tc_in[gl_InvocationID]` в `tc_out[gl_InvocationID]`. Geometry берёт индекс той входной вершины, которую только что записали в `gl_Position`: один `gl_in[k]`, в том числе индекс `repeat`, становится `gs_in[k]` непосредственно перед этим `EmitVertex`. Та же прокидка работает, когда этот `gl_Position` записан до `repeat` или `whenTrue`, который эмитит, и не используется повторно после emit, который её уже забрал. Несколько индексов или позиция без `gl_in`, пока varying ещё не записан, - ошибка, и текст просит явный `varying.set` перед этим emit. `varying.set` между этим `gl_Position` и emit заменяет прокидку только этого varying и только этого emit. Tessellation evaluation интерполирует `gl_TessCoord`, если стадия этот varying не писала. Одна входная вершина копируется из `te_in[0]`. Triangles взвешивают три вершины патча через `gl_TessCoord`. Isolines делают `mix` двух вершин по `gl_TessCoord.x`. Quads делают билинейный `mix` углов `(0,0)`, `(1,0)`, `(1,1)` и `(0,1)` по `gl_TessCoord.xy`. Явная запись заменяет эту интерполяцию. Стадия, у которой есть `rb_pipe` и которая пишет `gl_Position`, переобъявляет `gl_PerVertex` только с `vec4 gl_Position` до пользовательского блока. Uniform, прочитанный в geometry или tessellation, объявляется в исходнике этой стадии. `glIn(index)` и `varying.at(index)` допускают динамический `int`; проверка границ патча на этапе компиляции есть только для константных индексов.

Тестовое приложение открывает примеры AGSL и OpenGL с разных экранов. Каждая OpenGL-сцена - это `GLSurfaceView`, который линкует `ShaderProgram`. Список OpenGL покрывает фрагментный **Effect** (`redbyteFx`, SDF из stdlib), буферы и индексные вызовы, текстуры и мип-уровни, освещение, инстансинг, рисование в текстуру, сцену **Lit mesh** (`litTexturedMesh`, `setLitModel`), geometry, tessellation и небольшие игры. В **Planet** можно обернуть планету своим фото (runtime-доступ к галерее на API 28 и ниже; системный photo picker на API 33+). Интерфейс примеров - английский или русский по языку устройства. Geometry и tessellation требуют контекст OpenGL ES 3.2. `Modifier.redbyteFx` остаётся на AGSL и программу GLES не запускает. GLES `redbyteFx` - другой символ в `ru.redbyte.redbytefx.gl.compose`: в одном файле импортируйте один из них.

## Математика примеров простыми словами

Шейдер работает с координатами: у пикселя есть положение, а у меша — вершины. **UV** — это координаты, приведённые примерно к диапазону от 0 до 1 по ширине и высоте картинки. Выборка получает цвет в указанной точке UV. **Маска** — число от 0 до 1, которое указывает, где виден эффект; `mix` смешивает по нему два цвета. **Нормаль** показывает направление от поверхности и помогает считать освещение. Во многих примерах время добавляется к синусоиде, чтобы картинка двигалась.

### Эффекты AGSL

| Пример | Как работает математика |
| --- | --- |
| Flip | Заменяет координату выборки расстоянием до противоположного края (`width - x` или `height - y`), переворачивая изображение. |
| Mirror | Отражает координаты с одной стороны центра: обе половины берут цвет из одной и той же половины картинки. |
| Rotate | Переносит координаты вокруг центра с помощью синуса и косинуса, затем берёт цвет в повёрнутой точке. |
| Scale | Считает расстояние от координаты до центра и делит его на масштаб перед выборкой цвета. |
| Offset | Прибавляет двумерное смещение к координатам выборки, сдвигая картинку. |
| Wave | Прибавляет синусоиду к вертикальной координате выборки: соседние столбцы сдвигаются по-разному. |
| Pulse | Округляет UV до сетки пикселей, а время, номер строки и бегущий столбец подсвечивают отдельные ячейки. |
| Signal | Повторяет координаты в виде сетки; пороги и плавные границы превращают её части в строки развёртки. |
| Posterize | Округляет цвета до меньшего числа уровней и смешивает результат с исходной картинкой. |
| Film | Добавляет меняющееся зерно и затемняет пиксели ближе к краям с помощью маски виньетки. |
| Grade | Меняет насыщенность и смешивает тонированные версии исходного цвета по формулам смешивания. |
| Warp | Сдвигает UV с помощью нескольких слоёв шума и берёт цвет в искривлённых точках. |
| Prism | Берёт цветовые каналы в чуть разных точках и добавляет повторяющуюся цветовую палитру. |
| Spotlight | Считает расстояние до заданного центра; мягкие маски оставляют центр ярким, а края затемняют. |
| Beacon | Двигает световое пятно туда и обратно по времени; сглаживание замедляет его у концов пути. |
| Composite | Использует маски как веса при смешивании исходной картинки с другими цветами или слоями. |
| Frame | Считает расстояние до краёв картинки и подсвечивает узкую полосу, рисуя рамку. |
| Corner | Соединяет небольшие маски у углов с движущейся полосой света, рисуя скобки интерфейса. |
| Reveal | Сравнивает координату пикселя с подвижной границей; мягкий переход постепенно показывает перекрашенную версию картинки. |
| Sweep | Считает положение вдоль выбранного направления и проводит по картинке мягкую световую полосу. |
| Glitch | Сдвигает отдельные горизонтальные полосы и добавляет помехи, похожие на сбой экрана. |
| Radar | Переводит положение относительно центра в расстояние и угол, затем рисует дуги и вращающийся сектор сканирования. |
| Halo | Считает расстояние от центра с учётом пропорций экрана, подсвечивая кольцо и середину. |
| Circuit | Считает расстояние до отрезков и кругов; импульсы бегут по выбранным дорожкам. |
| Sigil | Считает расстояние со знаком до кругов и прямоугольников: внутри знак отрицательный, возле нуля получается мягкий контур. |
| Duotone | Вычисляет яркость исходного цвета и по ней смешивает два выбранных цвета. |
| Aurora | Складывает светящееся кольцо, вращающийся сектор, меняющуюся палитру и чуть смещённые цветовые каналы. |
| Liquid Glass | Искривляет координаты выборки, будто свет проходит сквозь текучее стекло; разделяет цвета у края и подсвечивает ободок. |
| Animated Gradient | Синусоиды по UV и времени плавно меняют красный, зелёный и синий каналы. |
| Physics Bubble | Compose двигает пузырь при перетаскивании и возвращает его пружиной; шейдер искривляет фон и окрашивает тонкую плёнку у края. |
| Touch Ripple | Расстояние от точки касания и прошедшее время задают расширяющиеся цветные кольца поверх картинки. |
| Metaballs | Считает расстояние до трёх движущихся кругов и плавно объединяет их поля, чтобы круги сливались. |
| CRT Terminal | Искривляет координаты выборки, сдвигает красный и синий у края и меняет яркость тонкими строками. |

### Сцены OpenGL ES

| Пример | Как работает математика |
| --- | --- |
| Triangle | Передаёт три вершины на экран; пиксели внутри полученного треугольника окрашиваются. |
| Effect | Рисует треугольник на весь экран; расстояние до вращающегося шестиугольника даёт мягкий край, а время меняет цвет. |
| Spheres | Обновляет положение и скорость шаров; столкновения с краем и друг с другом меняют направление. Освещение зависит от направления поверхности шара. |
| Flag | Добавляет к вершинам полотна синусоиды, зависящие от времени; высота и положение задают складки. |
| Neon floor | Перспектива уменьшает дальние клетки; повторяющиеся координаты рисуют линии, а время двигает их к камере. |
| Lamp | Поворачивает вершины и нормали поверхности; угол между нормалью и направлением света задаёт яркость. |
| City | Расставляет текстурированные блоки в 3D и двигает камеру вокруг них с помощью синуса и косинуса. |
| Lit crate | Матрицы камеры и проекции показывают ящик с текстурами; угол между нормалью и светом осветляет повёрнутые к нему грани. |
| Planet | Координаты сферы задают поверхность и орбиты спутников; направление света, смешивание цветов и жесты меняют вид сцены. |
| Slice | Использует один индексированный меш, но рисует только начало списка индексов: ползунок открывает новые квадраты. |
| Stamp | Переводит касание в координаты текстуры и меняет небольшой прямоугольник её пикселей. |
| Sky | По направлению от сферы выбирает одну из шести граней кубической текстуры и берёт её цвет. |
| Mirror | Сначала рисует треугольник в текстуру, затем накладывает эту текстуру на другой прямоугольник. |
| Mips | Показывает клетчатую текстуру с уменьшенными копиями и без них; копии сглаживают далёкие мелкие детали. |
| Glass orb | Вычисляет точку пересечения луча со сферой; ободок ярче там, где поверхность отвернулась от зрителя. |
| Iso bands | Складывает движущиеся синусоиды в одно поле и сравнивает его с порогами, получая цветные полосы. |
| Palette | Переводит высоту и время в повторяющуюся радугу; синус и косинус задают форму объёмной арки. |
| Hedgehog | Находит центр и внешнее направление каждого треугольника, затем добавляет точку в этом направлении — получается шип. |
| Ocean | Находит точки внутри участка между его углами и поднимает их двумя синусоидами, создавая волны. |
| Wireframe | Превращает каждое ребро треугольника в тонкую полосу, показывая каркас меша. |
| Electric sea | Складывает тонкие извилистые линии молний и сетку мерцающих звёзд. |
| red_byte | Обновляет положение букв по времени: они падают, после чего слово меняет цвет и перекатывается в сторону. |
| Tunnel | Перспектива приближает кольца коридора; сравнение положения корабля с отверстием кольца засчитывает точный пролёт. |
| Maze | Двигает шар по наклону поля и проверяет столкновения с коробками-стенами; свет делает шар объёмным. |
| Breakout | Меняет направление мяча при касании стен, ракетки и кирпичей; задетые кирпичи исчезают. |
| Raid | Двигает камеру вперёд и проверяет попадания по врагам на разной глубине; попадание даёт короткую вспышку. |
| Descent | Перспектива расставляет ворота на склоне; сравнение положений лыжника и ворот решает, засчитать ли проезд. |
| Lit mesh | Вращает текстурированную сферу; угол между нормалью и светом задаёт её рассеянную яркость. |

## Установка

```kotlin
dependencies {
    implementation("io.github.i-redbyte:redbytefx-core:1.1.0")
    implementation("io.github.i-redbyte:redbytefx-gl:1.1.0")
    implementation("io.github.i-redbyte:redbytefx-gl-compose:1.1.0")
    implementation("io.github.i-redbyte:redbytefx-compose:1.1.0")
    implementation("io.github.i-redbyte:redbytefx-stdlib:1.1.0")
    implementation("io.github.i-redbyte:redbytefx-3d:1.1.0")
}
```

| Артефакт | Роль |
|----------|------|
| `redbytefx-core` | `shader`, `Expr`, uniform-ы, спеллинг AGSL, GLSL ES 3.00, compute GLSL ES 3.10 и GLSL ES 3.20 |
| `redbytefx-gl` | Линковка GLES 3.0, 3.1 и 3.2, запись uniform и матриц, привязка 2D и куба, compute `dispatch` и загрузка storage на потоке EGL |
| `redbytefx-gl-compose` | `GlSurface`, `GlController`, `GlFrame`, помощники мешей и GLES `redbyteFx` для эффекта только из фрагмента |
| `redbytefx-compose` | `rememberFxController`, `FxController`, `Modifier.redbyteFx` для AGSL |
| `redbytefx-stdlib` | Рецепты эффекта: координаты, маски, композитинг, 2D/3D SDF, освещение, цвет, шум |
| `redbytefx-3d` | Данные сцены на CPU: меши и камера (`lookAt`, `perspective`, `ortho`). Драйвера OpenGL нет |

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

`rememberFxController` владеет одним AGSL-рантаймом. На каждую поверхность рисования нужен свой контроллер. `Modifier.redbyteFx` пишет `uResolution` из размера отрисовки. На AGSL `ShaderProgram.resolution` всегда привязан. `RuntimeShader` трогается только с UI-потока.

GLES-эффект с API 24 пишет тот же фрагмент. Без `vertex { }` компилятор подставляет полноэкранный треугольник. `ru.redbyte.redbytefx.gl.compose.redbyteFx` рисует его и пишет `resolution`. Compose-контент он не сэмплирует (`sample()` и `sampleUv()` остаются только у AGSL).

```kotlin
val pulse = shader(ShaderTarget.Gles30) {
    val time = uniformTime()
    fragment {
        val uv = aspectCenteredUv(normalizedUv(), resolution)
        val fill = softFill(sdHexagon(uv, 0.3f))
        vec4(fill, fill, fill, 1f.lit)
    }
}

@Composable
fun Pulse(program: ShaderProgram, time: HighFloatUniform) {
    val fx = rememberGlController(program)
    fx.bindTime(time)
    redbyteFx(fx, Modifier.fillMaxSize())
}
```

## Составление

Стадии - небольшой автомат. Uniform, sampler и varying объявляются на программе. Код шейдера пишут `fragment { }` и `vertex { }`. `vertex` есть у `ShaderTarget.Gles30` и `ShaderTarget.Gles32`. `geometry`, `tessControl` и `tessEval` есть у `ShaderTarget.Gles32`. `compute` есть у `ShaderTarget.Gles31`.

- `fragCoord` - вход фрагмента в пикселях, в том числе внутри фрагментной `fn`. AGSL пишет `fragCoord`. GLES пишет `gl_FragCoord.xy`. `resolution` - это `uResolution` на AGSL и GLES; Compose пишет его из размера кадра. Имя `uResolution` зарезервировано: объявляйте его чтением `FragmentDsl.resolution`, а не через `uniform("uResolution", …)`.
- `sample()` читает дочерний шейдер. Это законно в AGSL-фрагменте, в том числе внутри фрагментной `fn` через `this@fragment.sample()`. Другие стадии отвергают вызов.
- `texture(sampler, uv)` законен в GLES-фрагменте, в том числе внутри фрагментной `fn` через `this@fragment.texture()`. Другие стадии отвергают вызов.
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

Скалярные float одной точности поддерживают `+`, `-`, `*`, `/`. Векторы поддерживают вектор-вектор и вектор-скаляр той же точности. `mat2`, `mat3` и `mat4` умножаются на матрицу того же размера или на вектор с таким же числом компонент; оба языка пишут `*`. Сравнения скаляров float и int: `gt`, `lt`, `ge`, `le`, `eq`, `ne`. `gte` - другое написание `ge`. `ifElse(condition, ifTrue, ifFalse)` - это тернарный выбор. `choose(selector) { on(id) { value }; otherwise { value } }` вкладывает этот тернарный выбор для float-селектора. Булевы скаляры поддерживают `and`, `or` и `not`.

Математика с одним и тем же вызовом в обоих языках: `sin`, `cos`, `tan`, `sign`, `abs`, `floor`, `ceil`, `fract`, `sqrt`, `min`, `max`, `mod`, `pow`, `mix`, `clamp`, `smoothstep`, `step`, `saturate`, `dot`, `length`, `reflect`, `transpose` и `distance` для `vec2` и `vec3`. `sqrt` - это вызов GLSL, отрицательный аргумент не заменяется.

`redbytefx-stdlib` добавляет рецепты координат и композитинга на том же receiver `fragment`: `normalizedUv`, `sampleUv` (только AGSL), `centeredUv`, `aspectCenteredUv`, `rotate2d`, маски, reveal, смешивание и SDF вроде `sdCircle`, `sdHexagon`, `sdRhombus`, `sdEquilateralTriangle` и `softFill`. Трёхмерные дистанции: `sdCylinder` и `sdOctahedron`; освещение и цвет: `fresnel`, `phong`, `hueShift`, `filmicTonemap`. У `softFill` и `stroke` ширина пера по-прежнему аргумент вызывающего. `softFillScreen(distance)` и `strokeScreen(distance, width)` берут ширину края из `fwidth(distance)` и законны только во фрагменте.

## Рантайм

`ShaderProgram` неизменяем и читается с любого потока. Граф выражений и сгенерированные строки существуют на компиляции.

Воспроизведение AGSL - `program.newAgslInstance()`. Неизменившийся uniform не идёт на GPU. `set` пишет float и векторы highp и mediump, а `uniformInt` уходит через `setIntUniform`. Воспроизведение GLES - `GlProgramRuntime` в `redbytefx-gl`: создавать, линковать и уничтожать его на потоке EGL-контекста. `destroy` отвязывает программу до удаления. `set` пишет эти float, `uniform1i` для `uniformInt` и для `uniformBool` как 0 или 1, и column-major `uniformMatrix*fv` для `mat2`, `mat3` и `mat4` с transpose false. При линковке заливаются дефолты, в том числе единичная матрица, если свою не передали, а то же значение не вызывает `glUniform` повторно. `bind` для `sampler2D` использует `GL_TEXTURE_2D`, для `samplerCube` - `GL_TEXTURE_CUBE_MAP`. Оба делят `GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS`. `dispatch(x, y, z)` запускает слинкованную программу GLES 3.1 и затем вызывает `glMemoryBarrier` с `GL_SHADER_STORAGE_BARRIER_BIT`. `runtime.set(storageBlock, floats)` упаковывает логические значения std430 и пропускает загрузку, когда эти байты не изменились. `runtime.read(storageBlock, into)` копирует эти логические float обратно, без выравнивания std430, после `GL_BUFFER_UPDATE_BARRIER_BIT`, чтобы запись compute была видна. `set` и `read` принимают именно этот блок. Несколько uniform- или storage-блоков привязываются в порядке объявления с нуля, а блок из другого шейдера отвергается. Если номер привязки не меньше `GL_MAX_UNIFORM_BUFFER_BINDINGS` или `GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS`, линковка отвергает программу. `GlController.set` для uniform- или storage-блока хранит последние числа и пишет их снова, когда EGL-контекст создаётся заново. `dispatch` и `read` ставятся в очередь GL-потока; `read` возвращается сразу и заполняет буфер вызывающего из этой очереди. `GlCompute` держит программу GLES 3.1 без меша и без растеризации. Линковка идёт на GL-потоке. `GlSurface` для обычных сцен не меняется. `readFramebuffer` и `readColorTarget` копируют RGBA8 в буфер, который выделил вызывающий. Число байт проверяется до `glReadPixels`. Чтение цветовой цели оставляет привязанным тот framebuffer, который был привязан до вызова. В `draw` можно передать `GlPipeline`: фактор и уравнение смешивания, ножницы, маску цвета, маску глубины и `cullFace`. Обычный draw по-прежнему без смешивания, ножниц и отсечения граней, пишет все каналы цвета и глубину. Тест глубины не меняется. Следующий draw подставляет своё состояние. `draw(mesh, model, material)` копирует одну матрицу модели в атрибуты экземпляра и пишет `material` в `GlFrame.material` прямо перед этим draw. Дополнительные меши не перезаливаются, пока ссылки на массивы вершин и индексов те же. Хелперы сцены: `identity`, `multiply`, `translation`, `rotationX`/`Y`/`Z`, `scale`, `disc`, `extrudePolygon`, `tubeAlong`, `transform`, `merge`, `tagUv` и `instanceModel()`. Обычный draw не вызывает `glGetError`. `strictErrors` у `GlProgramRuntime` и `GlSurfaceConfig` превращает ошибку драйвера после draw, dispatch, загрузки текстуры и чтения в `GlException` с `GlCode.DriverError`. Строгая линковка по-прежнему снимает ошибки после link. Загрузка через `set` фиксирует размер в байтах, включая неразмерный хвост. Если размер упакованного буфера изменился, буфер выделяется заново. `StorageBlock.byteSize` - это размер std430 полностью размерного блока. Блок с неразмерным массивом сообщает размер через `byteSize(valueCount)`, и этот размер совпадает с `packStd430`. `uniformMedium`, `uniformMediumVec2`, `uniformMediumVec3` и `uniformMediumVec4` объявляют формы mediump; на AGSL они по-прежнему идут через `setFloatUniform`. `FxController` привязывает float-uniform и `uniformInt` для Compose.

## Чего в этой версии нет

Компилятор не выпускает GLES 2.0 и desktop GL. Он не выпускает `while`, рекурсию, вложенные функции и границу цикла, которая не константа от 1 до 64. `uniformBool`, `uniformMat2`, `uniformMat3`, `uniformMat4` и `samplerCube` на AGSL отвергаются.

## Релизы

Артефакты Maven: `io.github.i-redbyte:redbytefx-*`. Версия **1.1.0** задаётся в [gradle.properties](gradle.properties). Список изменений - [docs/changelog.ru.md](docs/changelog.ru.md) ([English](docs/changelog.md)).

Публикация в Maven Central (для сопровождающих):

```bash
./gradlew publishToMavenCentral
```

Нужны учётные данные Sonatype Central Portal (`mavenCentralUsername`, `mavenCentralPassword`) и настроенный GPG-ключ (`signingInMemoryKey` или `signing.gnupg.*` в свойствах Gradle). Сборка останавливается, если чего-то не хватает.

## Участие

Перед отправкой PR запустите `./gradlew qualityCheck`. Команда выполнит модульные тесты, проверит компиляцию приложения-примера и стиль Kotlin-кода.

Чтобы автоматически поправить форматирование, запустите `./gradlew detekt -PdetektAutoCorrect=true` и просмотрите изменения. Перед каждым коммитом стиль тоже проверяется. Если Git-хук не установился, добавьте его командой `./gradlew installGitHooks`.

Документацию API можно [читать онлайн](https://i-redbyte.github.io/redbytefx/) или собрать локально командой `./gradlew dokkaHtmlSite`, а затем открыть `build/docs/site/index.html`. Онлайн-версия обновляется после изменений в `master` или `main`. Подробности — в [инструкции по публикации](docs/github-pages.md).

## Лицензия

[MIT](LICENSE)
