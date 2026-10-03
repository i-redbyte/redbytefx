package ru.redbyte.redbytefx.sample.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun LabHome(
    onAgsl: () -> Unit,
    onGl: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = say(
                "Two runtimes, one algebra.",
                "Два способа запуска, одна алгебра.",
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LabCard(
            modifier = Modifier.weight(1f),
            badge = "AGSL",
            title = say("AGSL examples", "Примеры AGSL"),
            summary = say(
                "The cookbook as it is now: transforms, motion, color, compositing, and procedural effects on RuntimeShader.",
                "Сборник как он есть: преобразования, движение, цвет, сборка и процедурные эффекты на RuntimeShader.",
            ),
            onClick = onAgsl,
        )
        LabCard(
            modifier = Modifier.weight(1f),
            badge = "OPENGL ES 3.0",
            title = say("OpenGL examples", "Примеры OpenGL"),
            summary = say(
                "Meshes, light, and five small games. The code chip opens the description beside the GLSL that OpenGL ES compiles.",
                "Сетки, свет и пять небольших игр. Кнопка кода открывает описание рядом с GLSL, который собирает OpenGL ES.",
            ),
            onClick = onGl,
        )
    }
}

@Composable
private fun LabCard(
    badge: String,
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CyberPanel(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onClick),
    ) {
        CyberBadge(text = badge)
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 14.dp),
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
