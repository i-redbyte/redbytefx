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
import ru.redbyte.redbytefx.sample.ui.demos.DemoGles

enum class GlExample(
    val title: String,
    val summary: String,
    val api: String,
) {
    Triangle(
        title = "Triangle",
        summary = "One triangle, a vertex stage, and a fragment stage on an OpenGL ES 3.0 surface.",
        api = "GLES 3.0",
    ),
    Balls(
        title = "Spheres",
        summary = "Colored balls stay round, bounce off the screen, and trade trajectories on impact. A slider adds or removes them.",
        api = "GLES 3.0",
    ),
    Flag(
        title = "Flag",
        summary = "A red cloth. One function folds it, and C++ sits in the upper left.",
        api = "GLES 3.0",
    ),
    Floor(
        title = "Neon floor",
        summary = "A ground grid scrolls toward the camera. Perspective is the vertex w divide.",
        api = "GLES 3.0",
    ),
    Lamp(
        title = "Lamp",
        summary = "A spinning gem. The vertex stage turns the normal, and the fragment stage lights it.",
        api = "GLES 3.0",
    ),
    City(
        title = "City",
        summary = "Colored blocks and a moving camera live in one std140 block, written once per frame.",
        api = "GLES 3.0",
    ),
    Orb(
        title = "Glass orb",
        summary = "One closed ray-sphere hit, a Fresnel rim, and a sky behind it.",
        api = "GLES 3.0",
    ),
    Bands(
        title = "Iso bands",
        summary = "A moving field split by vector comparisons. any() lights the hot rim.",
        api = "GLES 3.0",
    ),
    Palette(
        title = "Palette",
        summary = "A 3D arch. A four-argument function paints a rainbow that flows from the crown down to the feet.",
        api = "GLES 3.0",
    ),
    Hedgehog(
        title = "Hedgehog",
        summary = "A geometry stage pulls a spike out of every triangle.",
        api = "GLES 3.2",
    ),
    Ocean(
        title = "Ocean",
        summary = "A tessellated patch. The evaluation stage raises waves from the patch corners.",
        api = "GLES 3.2",
    ),
    Wire(
        title = "Wireframe",
        summary = "A geometry stage turns each triangle edge into a thin ribbon.",
        api = "GLES 3.2",
    ),
    Storm(
        title = "Electric sea",
        summary = "A star field. A touch drops a jagged bolt from the sky, and random bolts strike on their own.",
        api = "GLES 3.0",
    ),
    Word(
        title = "red_byte",
        summary = "The letters of red_byte drop from the top in two rows. After the last one lands, the word shifts color and rolls left to right.",
        api = "GLES 3.0",
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
                text = "Vertex, fragment, geometry, and tessellation stages. The surface owns the EGL context.",
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
                    text = example.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = example.summary,
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
        GlExample.Orb -> DemoOrb()
        GlExample.Bands -> DemoBands()
        GlExample.Palette -> DemoPalette()
        GlExample.Hedgehog -> DemoHedgehog()
        GlExample.Ocean -> DemoOcean()
        GlExample.Wire -> DemoWire()
        GlExample.Storm -> DemoStorm()
        GlExample.Word -> DemoWord()
    }
}
