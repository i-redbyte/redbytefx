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
    agslAvailable: Boolean,
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
            summary = if (agslAvailable) {
                say(
                    "The cookbook as it is now: transforms, motion, color, compositing, and procedural effects on RuntimeShader.",
                    "Сборник как он есть: преобразования, движение, цвет, сборка и процедурные эффекты на RuntimeShader.",
                )
            } else {
                agslUnavailableText()
            },
            onClick = onAgsl,
            enabled = agslAvailable,
        )
        LabCard(
            modifier = Modifier.weight(1f),
            badge = "OPENGL ES 3.x",
            title = say("OpenGL examples", "Примеры OpenGL"),
            summary = say(
                "Meshes, textures, light, and small games, from ES 3.0 through geometry and tessellation. The code chip opens the description beside the GLSL that OpenGL ES compiles.",
                "Сетки, текстуры, свет и небольшие игры, от ES 3.0 до geometry и tessellation. Кнопка кода открывает описание рядом с GLSL, который собирает OpenGL ES.",
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
    enabled: Boolean = true,
) {
    CyberPanel(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .clickable(enabled = enabled, onClick = onClick),
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

/** Why AGSL examples are closed on this device. */
@Composable
internal fun agslUnavailableText(): String = say(
    "AGSL needs Android 13 (API 33). This device can still run every OpenGL example.",
    "Для AGSL нужен Android 13 (API 33). Все примеры OpenGL на этом устройстве работают.",
)

/** Shown instead of the AGSL lab when RuntimeShader is unavailable. */
@Composable
fun AgslUnavailable() {
    Text(
        text = agslUnavailableText(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp),
    )
}
