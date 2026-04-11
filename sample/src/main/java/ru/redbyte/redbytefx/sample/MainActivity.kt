package ru.redbyte.redbytefx.sample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import ru.redbyte.redbytefx.sample.app.RedByteFxSampleApp
import ru.redbyte.redbytefx.sample.app.startDemoIdOrNull
import ru.redbyte.redbytefx.sample.model.DemoId
import ru.redbyte.redbytefx.sample.ui.theme.RedByteFxSampleTheme

class MainActivity : ComponentActivity() {
    private val launchDemo = mutableStateOf<DemoId?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchDemo.value = intent.startDemoIdOrNull()
        setContent {
            RedByteFxSampleTheme {
                RedByteFxSampleApp(
                    initialDemo = launchDemo.value,
                    launchDemoRequest = launchDemo.value
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.startDemoIdOrNull()?.let { requestedDemo ->
            launchDemo.value = requestedDemo
        }
    }
}
