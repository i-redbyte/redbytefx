package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.redbyte.redbytefx.sample.ui.CyberBadge
import ru.redbyte.redbytefx.sample.ui.CyberPanel

enum class GlExample(
    val title: String,
    val summary: String,
) {
    Triangle(
        title = "Triangle",
        summary = "One triangle, a vertex stage, and a fragment stage on an OpenGL ES 3.0 surface.",
    ),
    Balls(
        title = "Spheres",
        summary = "Colored balls stay round, bounce off the screen, and trade trajectories on impact. A slider adds or removes them.",
    ),
}

@Composable
fun GlExampleList(onOpen: (GlExample) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Vertex and fragment stages. The surface owns the EGL context.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        GlExample.entries.forEach { example ->
            CyberPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .clickable { onOpen(example) },
            ) {
                CyberBadge(text = "GLES 3.0")
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
