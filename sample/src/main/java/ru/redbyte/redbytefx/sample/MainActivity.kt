package ru.redbyte.redbytefx.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.redbyte.redbytefx.FxEffect
import ru.redbyte.redbytefx.FxParam
import ru.redbyte.redbytefx.MirrorXFrom
import ru.redbyte.redbytefx.MirrorXParams
import ru.redbyte.redbytefx.compose.redbyteFx
import ru.redbyte.redbytefx.compose.rememberFxController
import ru.redbyte.redbytefx.redbytefx

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SampleScreen() }
    }
}

private data class FxSetup(
    val effect: FxEffect,
    val flipX: FxParam.Float,
    val flipY: FxParam.Float,
    val mirrorX: MirrorXParams,
    val angle: FxParam.Float
)

@androidx.compose.runtime.Composable
private fun SampleScreen() {
    var flipXEnabled by rememberSaveable { mutableStateOf(false) }
    var flipYEnabled by rememberSaveable { mutableStateOf(false) }

    var mirrorEnabled by rememberSaveable { mutableStateOf(false) }
    var mirrorFrom by rememberSaveable { mutableStateOf(MirrorXFrom.Right) }

    var angle by rememberSaveable { mutableFloatStateOf(0f) }

    val setup = remember {
        var pFlipX: FxParam.Float? = null
        var pFlipY: FxParam.Float? = null
        var pMirror: MirrorXParams? = null
        var pAngle: FxParam.Float? = null

        val effect = redbytefx {
            pFlipX = flipX(0f)
            pFlipY = flipY(0f)
            pMirror = mirrorX(enabled = 0f, from = MirrorXFrom.Right)
            pAngle = rotate(0f)
        }

        FxSetup(effect, pFlipX!!, pFlipY!!, pMirror!!, pAngle!!)
    }

    val fx = rememberFxController(setup.effect)

    fun applyMirror() {
        fx.setFloat(setup.mirrorX.enabled, if (mirrorEnabled) 1f else 0f)
        fx.setFloat(setup.mirrorX.from, mirrorFrom.v)
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .redbyteFx(fx)
                        .background(Color.Red),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "RedByteFX", style = MaterialTheme.typography.displayLarge, color = Color.Black)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Flip X")
                    Switch(
                        checked = flipXEnabled,
                        onCheckedChange = {
                            flipXEnabled = it
                            fx.setFloat(setup.flipX, if (it) 1f else 0f)
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Flip Y")
                    Switch(
                        checked = flipYEnabled,
                        onCheckedChange = {
                            flipYEnabled = it
                            fx.setFloat(setup.flipY, if (it) 1f else 0f)
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Mirror X")
                    Switch(
                        checked = mirrorEnabled,
                        onCheckedChange = {
                            mirrorEnabled = it
                            applyMirror()
                        }
                    )
                }

                if (mirrorEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = mirrorFrom == MirrorXFrom.Right,
                                onClick = {
                                    mirrorFrom = MirrorXFrom.Right
                                    applyMirror()
                                }
                            )
                            Text(text = "From Right")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = mirrorFrom == MirrorXFrom.Left,
                                onClick = {
                                    mirrorFrom = MirrorXFrom.Left
                                    applyMirror()
                                }
                            )
                            Text(text = "From Left")
                        }
                    }
                }

                Text(text = "Rotate: ${angle.toInt()}°")
                Slider(
                    value = angle,
                    onValueChange = {
                        angle = it
                        fx.setFloat(setup.angle, it)
                    },
                    valueRange = 0f..360f
                )
            }
        }
    }
}