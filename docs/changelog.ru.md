# Журнал изменений

Заметные изменения опубликованных артефактов Maven (`io.github.i-redbyte:redbytefx-*`).

English: [changelog.md](changelog.md).

## 1.1.0 — 2026-10-06

### Платформа и документация

- **Минимум AGSL — API 33** (`RedByteFxApis.AGSL_MIN_SDK`), как у `RuntimeShader` / `RenderEffect.createRuntimeShaderEffect`.
- README, справочник языка и таблицы кодов ошибок обновлены (английский и русский).

### Компилятор и stdlib

- **`scale`**: отрицательный масштаб сохраняет знак; околонулевые значения безопасно зажимаются.
- **Reveal-маски** (`horizontalReveal`, `verticalReveal`): progress 0 и 1 полностью прячут/показывают контент.
- **Экранный SDF** (`softFillScreen`, `strokeScreen`): защита, когда `fwidth` схлопывается в ноль.
- **KDoc polar** уточняет направление угла в UV-пространстве фрагмента.
- **`choose(selector) { on(id) { … }; otherwise { … } }`**: вложенный переносимый тернарный выбор по float-id.
- **`cosinePalette`**: насыщает RGB в `[0, 1]`.
- **`grain`**: складывает время через `fract` до хеша.
- **`wrapLambert`**: Ламберт с полом ambient.
- **`resolution`**: законно на GLES как `uResolution`; Compose пишет размер кадра. На AGSL `ShaderProgram.resolution` всегда привязан (исходник уже объявлял uniform). Имя `uResolution` зарезервировано для авторского uniform.
- **GLES только из фрагмента**: без `vertex { }` компилятор подставляет полноэкранный треугольник `a_corner`. Атрибут сохраняет написание `a_corner`, даже если пользовательский `fn` занимает этот идентификатор. Невызываемые fragment-функции, которые читают `resolution`, всё равно биндят `uResolution`.
- **Стадийные `fn`**: uniform, члены `uniformBlock` и varying, на которые ссылаются только невызываемые `fn` стадии, всё равно объявляются в GLSL этой стадии (тела всех owned-`fn` попадают в исходник).
- Дополнительный SDF: `sdHexagon`, `sdRhombus`, `sdEquilateralTriangle`, `sdCylinder`, `sdOctahedron`, `opRound`, `opOnion`, `sdfSmoothSubtract`, `sdfSmoothIntersect`, `rotate2d`.
- Освещение и цвет: `fresnel`, `phong`, `hueShift`, `filmicTonemap`, `hash22`, `voronoi`.
- **Premultiplied alpha**: `premultiply`, `unpremultiply`, premultiplied `blendMultiply` / `blendScreen` / `blendOverlay`; `alphaMaskStraight` для straight RGB. Нелинейные операции (`filmicTonemap`, `posterize`) сохраняют premultiplied alpha.
- **`topLeftUv`**: один хелпер для AGSL (origin сверху) и GLES (origin снизу).

### GLES и 3D

- **Линковка ES 3.0** больше не спрашивает `GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS` (неверный enum 0x500 на goldfish/GFXSTREAM).
- **Detekt** форматирует официальным стилем Kotlin от JetBrains (`detekt-formatting`, ktlint `android: false`).
- **`litTexturedMesh`**: выход фрагмента учитывает альфу текстуры; KDoc объясняет обновление model/normal matrix.
- **`GlProgramRuntime.setLitModel`**: заливает model и согласованный `normalMatrix` вместе.
- **`strictErrors`**: также проверяет драйвер после записи uniform/storage-блока и `generateMipmap2D`.
- Кэш матричных uniform всегда копирует массивы вызывающего (чтобы случайно не мутировать их).
- **`GlCompute`**: освобождает GPU-ресурсы при dispose, как `GlSurface`.
- **`GlFrame.draw(model, material)`**: одна матрица модели на draw (копируется) и per-draw material uniform.
- **`GlPipeline.cullFace`**: необязательное отсечение граней; по умолчанию выключено.
- Дополнительные меши не перезаливаются, пока ссылки на массивы вершин и индексов те же.
- Матрицы сцены: `identity` / `IDENTITY`, `multiply`, `translation`, `rotationX`/`Y`/`Z`, `scale`.
- Меши сцены: `disc`, `extrudePolygon`, `tubeAlong`, `transform`, `merge`, `tagUv`.
- **`instanceModel()`**: `mat4` из атрибутов экземпляра `a_model0`…`a_model3`.
- **`rayMarch`**: сцена считается один раз на активный шаг; направление нормализуется один раз; проверяются `steps` / `epsilon` / `far`; неактивные шаги пропускаются. **`sdOctahedron`**: корректные расстояния до рёбер и вершин.
- **Укрепление рантайма**: безопаснее очередь GL-потока; освобождение GPU-юнитов при ошибке; отказ от нулевых handle драйвера. **`GlFrame`**: обновление буферов после `replace`, актуальные границы меша для записанных draw, сброс разросшихся пулов draw/mesh.
- **CPU-меши сцены**: winding полигонов и аллокации геометрии.

### Инструменты

- Опциональный **pre-commit detekt** (`githooks/`, `./gradlew installGitHooks`).

### Compose

- **`redbyteFx`**: инвалидация рантайма через `invalidateDraw()`, а не `View.postInvalidateOnAnimation()`.
- GLES **`redbyteFx(controller)`**: хост эффекта только из фрагмента с [screenMesh] и автоматическим `uResolution`. Публичный конструктор `GlFrame` принимает размер в пикселях; present-программа тоже получает `setResolution`.
- В KDoc для владения `ShaderProgram` везде **program** (не «effect»).

### Тестовое приложение

- GLES-демо **Lit mesh** (`litTexturedMesh`, `ortho`, `setLitModel`).
- GLES-демо **Effect**: только фрагмент, `redbyteFx`, `resolution`, SDF из stdlib (`sdHexagon`, `rotate2d`).
- **Photo picker** на Planet: runtime-разрешения и системный выбор фото на API 33+.
- Дополнительные витрины API: `remap`, `sdSegment`, `bindInt`, отрицательный `scale`.
