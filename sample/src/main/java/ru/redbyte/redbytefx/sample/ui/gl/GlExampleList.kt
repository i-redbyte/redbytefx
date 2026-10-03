package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.redbyte.redbytefx.sample.ui.CyberBadge
import ru.redbyte.redbytefx.sample.ui.CyberPanel
import ru.redbyte.redbytefx.sample.model.Phrase
import ru.redbyte.redbytefx.sample.ui.demos.DemoGles
import ru.redbyte.redbytefx.sample.ui.say
import ru.redbyte.redbytefx.sample.ui.show

enum class GlExample(
    val title: Phrase,
    val summary: Phrase,
    val api: String,
) {
    Triangle(
        title = Phrase(
            "Triangle",
            "Треугольник",
        ),
        summary = Phrase(
            "One triangle, a vertex stage, and a fragment stage on an OpenGL ES 3.0 surface.",
            "Один треугольник, вершинный и фрагментный этапы на поверхности OpenGL ES 3.0.",
        ),
        api = "OpenGL ES 3.0",
    ),
    Balls(
        title = Phrase(
            "Spheres",
            "Шары",
        ),
        summary = Phrase(
            "Colored balls stay round, bounce off the screen, and trade trajectories on impact. A slider adds or removes them.",
            "Цветные шары остаются круглыми, отскакивают от краёв экрана и меняют траектории при ударе. Ползунок добавляет и убирает их.",
        ),
        api = "OpenGL ES 3.0",
    ),
    Flag(
        title = Phrase(
            "Flag",
            "Флаг",
        ),
        summary = Phrase(
            "A red cloth. One function folds it, and C++ sits in the upper left.",
            "Красное полотно. Одна функция складывает складки, а в левом верхнем углу стоит C++.",
        ),
        api = "OpenGL ES 3.0",
    ),
    Floor(
        title = Phrase(
            "Neon floor",
            "Неоновый пол",
        ),
        summary = Phrase(
            "A ground grid scrolls toward the camera. Perspective is the vertex w divide.",
            "Сетка пола бежит на камеру. Глубина появляется из деления на w в вершинном этапе.",
        ),
        api = "OpenGL ES 3.0",
    ),
    Lamp(
        title = Phrase(
            "Lamp",
            "Лампа",
        ),
        summary = Phrase(
            "A spinning gem. The vertex stage turns the normal, and the fragment stage lights it.",
            "Вращающийся кристалл. Вершинный этап поворачивает нормаль, фрагментный освещает грань.",
        ),
        api = "OpenGL ES 3.0",
    ),
    City(
        title = Phrase(
            "City",
            "Город",
        ),
        summary = Phrase(
            "Textured blocks and a moving camera live in one std140 block, written once per frame.",
            "Текстурированные блоки и движущаяся камера лежат в одном блоке std140 и записываются один раз за кадр.",
        ),
        api = "OpenGL ES 3.0",
    ),
    Crate(
        title = Phrase(
            "Lit crate",
            "Освещённый ящик",
        ),
        summary = Phrase(
            "lookAt and perspective, one indexed mesh, a facade texture and a ground texture, lit by lambert.",
            "lookAt и perspective, один индексированный меш, текстура фасада и текстура земли, свет по lambert.",
        ),
        api = "OpenGL ES 3.0",
    ),
    Planet(
        title = Phrase(
            "Planet",
            "Планета",
        ),
        summary = Phrase(
            "Drag to heat the planet and move the light. Tap it for a color, a gradient, or a gallery photo. A slider sets up to five moons.",
            "Вращение пальцем греет планету и двигает свет. Нажатие задаёт цвет, градиент или фото из галереи. Ползунок ставит до пяти спутников.",
        ),
        api = "OpenGL ES 3.0",
    ),
    Slice(
        title = Phrase(
            "Slice",
            "Срез",
        ),
        summary = Phrase(
            "One indexed mesh. The slider draws the first quads and leaves the rest out of the call.",
            "Один индексированный меш. Ползунок рисует первые квадраты и не включает остальные в вызов.",
        ),
        api = "OpenGL ES 3.0",
    ),
    Stamp(
        title = Phrase(
            "Stamp",
            "Штамп",
        ),
        summary = Phrase(
            "A finger paints a rectangle into a texture that is already on the quad.",
            "Палец вписывает прямоугольник в текстуру, которая уже лежит на квадрате.",
        ),
        api = "OpenGL ES 3.0",
    ),
    Sky(
        title = Phrase(
            "Sky",
            "Небо",
        ),
        summary = Phrase(
            "Six cube faces, each a flat color, sampled on a turning sphere.",
            "Шесть граней куба, каждая своим цветом, на вращающейся сфере.",
        ),
        api = "OpenGL ES 3.0",
    ),
    Mirror(
        title = Phrase(
            "Mirror",
            "Зеркало",
        ),
        summary = Phrase(
            "A triangle is drawn into a texture by one program and shown on a quad by another.",
            "Треугольник рисуется в текстуру одной программой и показывается на квадрате другой.",
        ),
        api = "OpenGL ES 3.0",
    ),
    Mips(
        title = Phrase(
            "Mips",
            "Мипы",
        ),
        summary = Phrase(
            "The same checker, once from level 0 and once through a mip chain.",
            "Одна и та же клетка: слева только уровень 0, справа через цепочку мип-уровней.",
        ),
        api = "OpenGL ES 3.0",
    ),
    Orb(
        title = Phrase(
            "Glass orb",
            "Стеклянный шар",
        ),
        summary = Phrase(
            "One closed ray-sphere hit, a Fresnel rim, and a sky behind it.",
            "Одно пересечение луча со сферой, кант по Френелю и небо за ним.",
        ),
        api = "OpenGL ES 3.0",
    ),
    Bands(
        title = Phrase(
            "Iso bands",
            "Полосы",
        ),
        summary = Phrase(
            "A moving field split by vector comparisons. any() lights the hot rim.",
            "Движущееся поле, разрезанное сравнением векторов. any() зажигает горячий край.",
        ),
        api = "OpenGL ES 3.0",
    ),
    Palette(
        title = Phrase(
            "Palette",
            "Палитра",
        ),
        summary = Phrase(
            "A 3D arch. A four-argument function paints a rainbow that flows from the crown down to the feet.",
            "Объёмная арка. Функция с четырьмя аргументами красит радугу, которая стекает с вершины к основанию.",
        ),
        api = "OpenGL ES 3.0",
    ),
    Hedgehog(
        title = Phrase(
            "Hedgehog",
            "Ёж",
        ),
        summary = Phrase(
            "A geometry stage pulls a spike out of every triangle.",
            "Геометрический этап вытягивает шип из каждого треугольника.",
        ),
        api = "OpenGL ES 3.2",
    ),
    Ocean(
        title = Phrase(
            "Ocean",
            "Океан",
        ),
        summary = Phrase(
            "A tessellated patch. The evaluation stage raises waves from the patch corners.",
            "Участок, разбитый тесселяцией. Этап вычисления поднимает волны по углам.",
        ),
        api = "OpenGL ES 3.2",
    ),
    Wire(
        title = Phrase(
            "Wireframe",
            "Каркас",
        ),
        summary = Phrase(
            "A geometry stage turns each triangle edge into a thin ribbon.",
            "Геометрический этап превращает каждое ребро треугольника в тонкую ленту.",
        ),
        api = "OpenGL ES 3.2",
    ),
    Storm(
        title = Phrase(
            "Electric sea",
            "Электрическое море",
        ),
        summary = Phrase(
            "A star field. A touch drops a jagged bolt from the sky, and random bolts strike on their own.",
            "Звёздное поле. Касание роняет с неба изломанную молнию, а другие бьют сами по себе.",
        ),
        api = "OpenGL ES 3.0",
    ),
    Word(
        title = Phrase(
            "red_byte",
            "red_byte",
        ),
        summary = Phrase(
            "The letters of red_byte drop from the top in two rows. After the last one lands, the word shifts color and rolls left to right.",
            "Буквы red_byte падают сверху в два ряда. Когда последняя садится, слово меняет цвет и катится слева направо.",
        ),
        api = "OpenGL ES 3.0",
    ),
    Tunnel(
        title = Phrase(
            "Tunnel",
            "Туннель",
        ),
        summary = Phrase(
            "A ship flies down a neon corridor. A finger slides it sideways, and a ring counts only when the ship flies through the hole.",
            "Корабль летит по неоновому коридору. Палец сдвигает его вбок, а кольцо засчитывается, только если пролететь сквозь отверстие.",
        ),
        api = "OpenGL ES 3.0",
    ),
    Maze(
        title = Phrase(
            "Maze",
            "Лабиринт",
        ),
        summary = Phrase(
            "A ball rolls on a checkered floor. A finger tilts the board. Walls are boxes, and the ball is a lit sphere.",
            "Шар катится по клетчатому полу. Палец наклоняет поле. Стены сложены из коробок, шар - освещённая сфера.",
        ),
        api = "OpenGL ES 3.0",
    ),
    Breakout(
        title = Phrase(
            "Breakout",
            "Арканоид",
        ),
        summary = Phrase(
            "The paddle follows the finger. The ball clears rows of bricks, and a brick vanishes on the hit.",
            "Ракетка следует за пальцем. Мяч сбивает ряды кирпичей, и кирпич гаснет в момент удара.",
        ),
        api = "OpenGL ES 3.0",
    ),
    Strafe(
        title = Phrase(
            "Raid",
            "Налёт",
        ),
        summary = Phrase(
            "The camera flies forward. Enemies are spheres at different depths. A tap fires, and a hit flashes in the shader.",
            "Камера летит вперёд. Враги - сферы на разной глубине. Касание стреляет, а попадание вспыхивает в шейдере.",
        ),
        api = "OpenGL ES 3.0",
    ),
    Descent(
        title = Phrase(
            "Descent",
            "Спуск",
        ),
        summary = Phrase(
            "A skier runs down between gates. Depth works like the floor, and only a gate the skier passes inside is counted.",
            "Лыжник едет вниз между воротами. Глубина устроена как у пола, и засчитываются только ворота, в створ которых он попал.",
        ),
        api = "OpenGL ES 3.0",
    ),
}

@Composable
fun GlExampleList(onOpen: (GlExample) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = say(
                    "Each scene keeps a live picture. The code chip opens the description you write beside the GLSL that OpenGL ES compiles.",
                    "У каждой сцены живая картинка. Кнопка кода открывает описание, которое вы пишете, рядом с GLSL, который собирает OpenGL ES.",
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(GlExample.entries) { example ->
            CyberPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .clickable { onOpen(example) },
            ) {
                CyberBadge(text = example.api)
                Text(
                    text = example.title.show(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = example.summary.show(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
internal fun GlExampleScreen(example: GlExample) {
    when (example) {
        GlExample.Triangle -> DemoGles()
        GlExample.Balls -> DemoBalls()
        GlExample.Flag -> DemoFlag()
        GlExample.Floor -> DemoFloor()
        GlExample.Lamp -> DemoLamp()
        GlExample.City -> DemoCity()
        GlExample.Crate -> DemoCrate()
        GlExample.Planet -> DemoPlanet()
        GlExample.Slice -> DemoSlice()
        GlExample.Stamp -> DemoStamp()
        GlExample.Sky -> DemoSky()
        GlExample.Mirror -> DemoMirror()
        GlExample.Mips -> DemoMips()
        GlExample.Orb -> DemoOrb()
        GlExample.Bands -> DemoBands()
        GlExample.Palette -> DemoPalette()
        GlExample.Hedgehog -> DemoHedgehog()
        GlExample.Ocean -> DemoOcean()
        GlExample.Wire -> DemoWire()
        GlExample.Storm -> DemoStorm()
        GlExample.Word -> DemoWord()
        GlExample.Tunnel -> DemoTunnel()
        GlExample.Maze -> DemoMaze()
        GlExample.Breakout -> DemoBreakout()
        GlExample.Strafe -> DemoStrafe()
        GlExample.Descent -> DemoDescent()
    }
}
