@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ru.redbyte.redbytefx.sample.ui.gl

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.sample.ui.CyberBadge
import ru.redbyte.redbytefx.sample.ui.CyberCodeAction
import ru.redbyte.redbytefx.sample.ui.CyberCodeBlock
import ru.redbyte.redbytefx.sample.ui.CyberPanel
import ru.redbyte.redbytefx.sample.ui.say
import ru.redbyte.redbytefx.sample.ui.ui

internal fun glesListing(program: ShaderProgram): String = buildString {
    append("// vertex\n")
    append(program.vertexSource().trim())
    append("\n\n// fragment\n")
    append(program.fragmentSource().trim())
    if (program.hasTessellation()) {
        append("\n\n// tessellation control\n")
        append(program.tessControlSource().trim())
        append("\n\n// tessellation evaluation\n")
        append(program.tessEvalSource().trim())
    }
    if (program.hasGeometry()) {
        append("\n\n// geometry\n")
        append(program.geometrySource().trim())
    }
    append('\n')
}

@Composable
internal fun GlCodeCompare(
    program: ShaderProgram,
    dsl: String,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val glsl = remember(program) { glesListing(program) }
    CyberBadge(
        text = say("DSL + GLES", "Код и GLSL"),
        modifier = modifier
            .padding(12.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { open = true },
        accent = MaterialTheme.colorScheme.primary,
        fill = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
    )
    if (open) {
        GlCodeDialog(dsl = dsl, glsl = glsl, onDismiss = { open = false })
    }
}

@Composable
private fun GlCodeDialog(
    dsl: String,
    glsl: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 14.dp),
        ) {
            CyberPanel(
                modifier = Modifier.fillMaxSize(),
                accent = MaterialTheme.colorScheme.primary,
                contentPadding = PaddingValues(12.dp),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CyberBadge(text = say("CODE COMPARE", "СРАВНЕНИЕ КОДА"), accent = MaterialTheme.colorScheme.primary)
                    CyberBadge(text = say("DSL", "ОПИСАНИЕ"), accent = MaterialTheme.colorScheme.secondary)
                    CyberBadge(text = say("OPENGL ES", "OPENGL ES"), accent = MaterialTheme.colorScheme.tertiary)
                    CyberBadge(
                        text = say("CLOSE", "ЗАКРЫТЬ"),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onDismiss),
                        accent = MaterialTheme.colorScheme.tertiary,
                        fill = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f),
                    )
                }
                Text(
                    text = say(
                        "The DSL is the program you write. OpenGL ES is the GLSL the driver compiles from it.",
                        "Описание - это программа, которую вы пишете. OpenGL ES - это GLSL, который из неё собирает драйвер.",
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Column(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SourcePanel(title = say("DSL", "Описание"), text = dsl)
                    SourcePanel(title = "OpenGL ES", text = glsl)
                }
            }
        }
    }
}

@Composable
private fun SourcePanel(title: String, text: String) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val shareWord = ui("SHARE")
    SelectionContainer {
        CyberCodeBlock(
            title = title,
            text = text,
            maxLines = Int.MAX_VALUE,
            meta = say(
                "${text.lineSequence().count()} lines",
                "${text.lineSequence().count()} строк",
            ),
            actions = listOf(
                CyberCodeAction(
                    label = "COPY",
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(
                                ClipData.newPlainText("redbytefx-$title", text).toClipEntry(),
                            )
                        }
                    },
                ),
                CyberCodeAction(
                    label = "SHARE",
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                            putExtra(Intent.EXTRA_SUBJECT, "RedByteFX - $title")
                        }
                        context.startActivity(Intent.createChooser(send, "$shareWord $title"))
                    },
                ),
            ),
        )
    }
}
