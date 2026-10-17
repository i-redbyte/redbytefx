package ru.redbyte.redbytefx.gl.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
public fun GlLinkErrorOverlay(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(24.dp),
    )
}

/** Text for [GlController.failure]: the exception message, or its type when it has none. */
internal fun glFailureMessage(error: Throwable): String =
    error.message?.takeIf { it.isNotBlank() } ?: error::class.java.simpleName
