package ru.redbyte.redbytefx.sample.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ru.redbyte.redbytefx.sample.R
import ru.redbyte.redbytefx.sample.model.DemoCatalog
import ru.redbyte.redbytefx.sample.model.DemoId
import ru.redbyte.redbytefx.sample.ui.DemoScreen
import ru.redbyte.redbytefx.sample.ui.HomeScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedByteFxSampleApp() {
    var currentDemo: DemoId? by rememberSaveable { mutableStateOf(null) }
    val appName = stringResource(id = R.string.app_name)

    val title = currentDemo?.let { id ->
        DemoCatalog.firstOrNull { it.id == id }?.title ?: "Demo"
    } ?: appName

    BackHandler(enabled = currentDemo != null) {
        currentDemo = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    if (currentDemo != null) {
                        TextButton(onClick = { currentDemo = null }) { Text(text = "Back") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            val id = currentDemo
            if (id == null) {
                HomeScreen(
                    demos = DemoCatalog,
                    onOpen = { demoId -> currentDemo = demoId }
                )
            } else {
                DemoScreen(id = id)
            }
        }
    }
}
