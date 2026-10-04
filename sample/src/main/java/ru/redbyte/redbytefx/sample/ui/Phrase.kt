package ru.redbyte.redbytefx.sample.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import ru.redbyte.redbytefx.sample.model.DemoInfo
import ru.redbyte.redbytefx.sample.model.DemoRussian
import ru.redbyte.redbytefx.sample.model.Phrase

@Composable
fun Phrase.show(): String = say(en, ru)

@Composable
fun Phrase.showUpper(): String = show().uppercase()

@Composable
fun say(en: String, ru: String): String = sayLocalized(LocalConfiguration.current.locales[0].language, en, ru)

/** Same strings as [say], safe from callbacks and background work. */
fun say(context: Context, en: String, ru: String): String =
    sayLocalized(context.resources.configuration.locales[0].language, en, ru)

private fun sayLocalized(language: String, en: String, ru: String): String =
    if (language == "ru") ru else en

@Composable
fun russian(): Boolean = LocalConfiguration.current.locales[0].language == "ru"

@Composable
fun DemoInfo.shownTitle(): String = say(title, DemoRussian.title(id))

@Composable
fun DemoInfo.shownSubtitle(): String = say(subtitle, DemoRussian.subtitle(id))

@Composable
fun DemoInfo.shownFocus(): String = say(focus, DemoRussian.focus(id))

@Composable
fun shownFamily(key: String): String = DemoRussian.family(key).show()

@Composable
fun shownTag(tag: String): String = say(tag, DemoRussian.tag(tag))

@Composable
fun ui(en: String): String = say(en, ControlRu[en] ?: en)

fun ruCount(count: Int, one: String, few: String, many: String): String {
    val mod100 = count % 100
    val mod10 = count % 10
    val word = when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
    return "$count $word"
}

private val ControlRu = mapOf(
    "Enabled" to "Включено",
    "Play" to "Играть",
    "Amount" to "Доля",
    "Speed" to "Скорость",
    "Angle" to "Угол",
    "Amplitude" to "Амплитуда",
    "Frequency" to "Частота",
    "Density" to "Плотность",
    "Line Width" to "Толщина линии",
    "Warp" to "Искажение",
    "Refraction" to "Преломление",
    "Flow" to "Течение",
    "Chroma px" to "Сдвиг цвета",
    "Edge RGB" to "Цвет края",
    "Ice edge" to "Холодная кромка",
    "Radius" to "Радиус",
    "Levels" to "Уровни",
    "Warmth" to "Тепло",
    "Glow" to "Свечение",
    "Spread" to "Разброс",
    "Shift" to "Сдвиг",
    "Scale" to "Масштаб",
    "Drift" to "Снос",
    "Offset X" to "Сдвиг X",
    "Offset Y" to "Сдвиг Y",
    "Blend width" to "Ширина слияния",
    "Strength" to "Сила",
    "Corner Size" to "Размер угла",
    "Thickness" to "Толщина",
    "Scale X" to "Масштаб X",
    "Scale Y" to "Масштаб Y",
    "Barrel" to "Бочка",
    "RGB split" to "Сдвиг RGB",
    "Scanlines" to "Строки",
    "Grain" to "Зерно",
    "Vignette" to "Виньетка",
    "Center X" to "Центр X",
    "Center Y" to "Центр Y",
    "Panel Width" to "Ширина панели",
    "Width" to "Ширина",
    "Grid" to "Сетка",
    "Mix" to "Смешение",
    "Spectral" to "Спектр",
    "Flip X" to "Отразить X",
    "Flip Y" to "Отразить Y",
    "Right" to "Справа",
    "Left" to "Слева",
    "Bottom" to "Снизу",
    "Top" to "Сверху",
    "Horizontal" to "По горизонтали",
    "Vertical" to "По вертикали",
    "Radial" to "По кругу",
    "Source" to "Источник",
    "Oscillator" to "Генератор",
    "Processor" to "Процессор",
    "Capacitor" to "Конденсатор",
    "Resistor" to "Резистор",
    "Output" to "Выход",
    "Axis" to "Ось",
    "From" to "Откуда",
    "Mode" to "Режим",
    "Node" to "Узел",
    "Active Node" to "Активный узел",
    "PREV" to "Назад",
    "NEXT" to "Дальше",
    "COPY" to "Копировать",
    "COPIED" to "Скопировано",
    "SHARE" to "Поделиться",
    "COLLAPSE" to "Свернуть",
    "EXPAND" to "Развернуть",
    "CLOSE" to "Закрыть",
    "DSL" to "Описание",
    "AGSL" to "AGSL",
    "OpenGL ES" to "OpenGL ES",
    "OPENGL ES" to "OPENGL ES",
    "DSL snippet" to "Фрагмент описания",
    "Generated AGSL" to "Получившийся AGSL",
    "X" to "X",
    "Y" to "Y",
    "RedByteFX" to "RedByteFX",
    "Sigil//SDF" to "Знак",
    "Halo//Light" to "Ореол",
    "Frame//Shell" to "Рамка",
    "Sweep//Track" to "Проход света",
    "Radar//Polar" to "Локатор",
    "Corner//HUD" to "Углы",
    "Aurora//Showcase" to "Сияние",
    "Signal//Ghost" to "Помехи",
)
