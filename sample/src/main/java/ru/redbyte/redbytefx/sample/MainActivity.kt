package ru.redbyte.redbytefx.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ru.redbyte.redbytefx.sample.app.RedByteFxSampleApp
import ru.redbyte.redbytefx.sample.ui.theme.RedByteFxSampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RedByteFxSampleTheme {
                RedByteFxSampleApp()
            }
        }
    }
}