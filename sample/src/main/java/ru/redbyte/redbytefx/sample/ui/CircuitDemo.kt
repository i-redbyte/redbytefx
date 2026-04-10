package ru.redbyte.redbytefx.sample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.redbyte.redbytefx.*
import ru.redbyte.redbytefx.compose.bindFloat
import ru.redbyte.redbytefx.compose.bindTime
import ru.redbyte.redbytefx.compose.redbyteFx
import ru.redbyte.redbytefx.compose.rememberFxController
import ru.redbyte.redbytefx.stdlib.aspectCenteredUv
import ru.redbyte.redbytefx.stdlib.maskedMix
import ru.redbyte.redbytefx.stdlib.maskedOverlay
import ru.redbyte.redbytefx.stdlib.maskedScreen
import ru.redbyte.redbytefx.stdlib.sdBox
import ru.redbyte.redbytefx.stdlib.sdCircle
import ru.redbyte.redbytefx.stdlib.sdRoundedBox
import ru.redbyte.redbytefx.stdlib.segmentMask
import ru.redbyte.redbytefx.stdlib.segmentProgress
import ru.redbyte.redbytefx.stdlib.softFill

private enum class CircuitNode { Source, Oscillator, Processor, Capacitor, Resistor, Output }
private enum class CircuitNodeShape { Pad, Chip, Module, Capacitor }

private data class CircuitPoint(
    val x: Float,
    val y: Float
)

private data class CircuitNodeSpec(
    val id: CircuitNode,
    val shaderIndex: Float,
    val title: String,
    val shortLabel: String,
    val position: CircuitPoint,
    val shape: CircuitNodeShape,
    val halfSize: CircuitPoint = CircuitPoint(0.10f, 0.07f),
    val radius: Float = 0.07f,
    val cornerRadius: Float = 0.03f,
    val feather: Float = 0.016f
)

private data class CircuitSegmentSpec(
    val id: String,
    val start: CircuitPoint,
    val end: CircuitPoint,
    val thickness: Float = 0.04f,
    val feather: Float = 0.016f,
    val bandWidth: Float = 0.22f,
    val bandFeather: Float = 0.08f,
    val pulseSpeed: Float = 0.36f,
    val pulseOffset: Float = 0f
)

private data class CircuitConnectionSpec(
    val from: CircuitNode,
    val to: CircuitNode,
    val segmentIds: List<String>
)

private data class CircuitActivationSpec(
    val nodeIds: Set<CircuitNode>,
    val segmentIds: Set<String>
)

private data class CircuitBoardSpec(
    val nodes: List<CircuitNodeSpec>,
    val segments: List<CircuitSegmentSpec>,
    val connections: List<CircuitConnectionSpec>,
    val viaPoints: List<CircuitPoint>,
    val activations: Map<CircuitNode, CircuitActivationSpec>
)

private data class CircuitSetup(
    val effect: FxEffect,
    val time: FxParam.Float,
    val route: FxParam.Float,
    val amount: FxParam.Float
)

private const val CIRCUIT_BOARD_HALF_WIDTH = 0.86f
private const val CIRCUIT_BOARD_HALF_HEIGHT = 0.48f
private const val CIRCUIT_TRACE_THICKNESS_SCALE = 0.25f
private const val CIRCUIT_TRACE_FEATHER_SCALE = 0.42f
private const val CIRCUIT_TRACE_MIN_THICKNESS = 0.0032f
private const val CIRCUIT_TRACE_MIN_FEATHER = 0.0026f
private const val CIRCUIT_PULSE_THICKNESS_EXTRA = 0.0008f

private val circuitBoardSpec: CircuitBoardSpec = buildCircuitBoardSpec()

private fun buildCircuitBoardSpec(): CircuitBoardSpec {
    val nodes = listOf(
        CircuitNodeSpec(
            id = CircuitNode.Source,
            shaderIndex = 0f,
            title = "Source",
            shortLabel = "VIN",
            position = CircuitPoint(-0.66f, -0.12f),
            shape = CircuitNodeShape.Pad,
            radius = 0.078f
        ),
        CircuitNodeSpec(
            id = CircuitNode.Oscillator,
            shaderIndex = 1f,
            title = "Oscillator",
            shortLabel = "OSC",
            position = CircuitPoint(-0.38f, 0.20f),
            shape = CircuitNodeShape.Module,
            halfSize = CircuitPoint(0.11f, 0.06f),
            cornerRadius = 0.028f
        ),
        CircuitNodeSpec(
            id = CircuitNode.Processor,
            shaderIndex = 2f,
            title = "Processor",
            shortLabel = "MCU",
            position = CircuitPoint(0.02f, 0.00f),
            shape = CircuitNodeShape.Chip,
            halfSize = CircuitPoint(0.21f, 0.12f),
            cornerRadius = 0.036f
        ),
        CircuitNodeSpec(
            id = CircuitNode.Capacitor,
            shaderIndex = 3f,
            title = "Capacitor",
            shortLabel = "C1",
            position = CircuitPoint(0.32f, -0.28f),
            shape = CircuitNodeShape.Capacitor,
            halfSize = CircuitPoint(0.055f, 0.105f),
            cornerRadius = 0.014f
        ),
        CircuitNodeSpec(
            id = CircuitNode.Resistor,
            shaderIndex = 4f,
            title = "Resistor",
            shortLabel = "R1",
            position = CircuitPoint(0.42f, 0.18f),
            shape = CircuitNodeShape.Module,
            halfSize = CircuitPoint(0.12f, 0.05f),
            cornerRadius = 0.026f
        ),
        CircuitNodeSpec(
            id = CircuitNode.Output,
            shaderIndex = 5f,
            title = "Output",
            shortLabel = "OUT",
            position = CircuitPoint(0.68f, 0.02f),
            shape = CircuitNodeShape.Module,
            halfSize = CircuitPoint(0.12f, 0.085f),
            cornerRadius = 0.028f
        )
    )

    val segments = listOf(
        CircuitSegmentSpec(
            id = "source_pin",
            start = CircuitPoint(-0.58f, -0.12f),
            end = CircuitPoint(-0.48f, -0.12f),
            thickness = 0.014f,
            feather = 0.010f,
            bandWidth = 0.12f,
            bandFeather = 0.04f,
            pulseSpeed = 0.34f,
            pulseOffset = 0.00f
        ),
        CircuitSegmentSpec(
            id = "vin_a",
            start = CircuitPoint(-0.48f, -0.12f),
            end = CircuitPoint(-0.24f, -0.12f),
            thickness = 0.018f,
            feather = 0.010f,
            bandWidth = 0.13f,
            bandFeather = 0.04f,
            pulseSpeed = 0.34f,
            pulseOffset = 0.04f
        ),
        CircuitSegmentSpec(
            id = "vin_b",
            start = CircuitPoint(-0.24f, -0.12f),
            end = CircuitPoint(-0.24f, 0.00f),
            thickness = 0.018f,
            feather = 0.010f,
            bandWidth = 0.13f,
            bandFeather = 0.04f,
            pulseSpeed = 0.34f,
            pulseOffset = 0.12f
        ),
        CircuitSegmentSpec(
            id = "cpu_in_pin",
            start = CircuitPoint(-0.24f, 0.00f),
            end = CircuitPoint(-0.19f, 0.00f),
            thickness = 0.014f,
            feather = 0.010f,
            bandWidth = 0.12f,
            bandFeather = 0.04f,
            pulseSpeed = 0.34f,
            pulseOffset = 0.18f
        ),
        CircuitSegmentSpec(
            id = "osc_pin",
            start = CircuitPoint(-0.27f, 0.20f),
            end = CircuitPoint(-0.18f, 0.20f),
            thickness = 0.013f,
            feather = 0.010f,
            bandWidth = 0.12f,
            bandFeather = 0.04f,
            pulseSpeed = 0.42f,
            pulseOffset = 0.10f
        ),
        CircuitSegmentSpec(
            id = "osc_a",
            start = CircuitPoint(-0.18f, 0.20f),
            end = CircuitPoint(-0.08f, 0.20f),
            thickness = 0.016f,
            feather = 0.010f,
            bandWidth = 0.13f,
            bandFeather = 0.04f,
            pulseSpeed = 0.42f,
            pulseOffset = 0.18f
        ),
        CircuitSegmentSpec(
            id = "osc_b",
            start = CircuitPoint(-0.08f, 0.20f),
            end = CircuitPoint(-0.08f, 0.14f),
            thickness = 0.016f,
            feather = 0.010f,
            bandWidth = 0.13f,
            bandFeather = 0.04f,
            pulseSpeed = 0.42f,
            pulseOffset = 0.26f
        ),
        CircuitSegmentSpec(
            id = "osc_c",
            start = CircuitPoint(-0.08f, 0.14f),
            end = CircuitPoint(0.02f, 0.14f),
            thickness = 0.016f,
            feather = 0.010f,
            bandWidth = 0.13f,
            bandFeather = 0.04f,
            pulseSpeed = 0.42f,
            pulseOffset = 0.34f
        ),
        CircuitSegmentSpec(
            id = "cpu_clk_pin",
            start = CircuitPoint(0.02f, 0.14f),
            end = CircuitPoint(0.02f, 0.12f),
            thickness = 0.013f,
            feather = 0.010f,
            bandWidth = 0.12f,
            bandFeather = 0.04f,
            pulseSpeed = 0.42f,
            pulseOffset = 0.40f
        ),
        CircuitSegmentSpec(
            id = "cpu_cap_pin",
            start = CircuitPoint(0.08f, -0.12f),
            end = CircuitPoint(0.08f, -0.14f),
            thickness = 0.013f,
            feather = 0.010f,
            bandWidth = 0.12f,
            bandFeather = 0.04f,
            pulseSpeed = 0.30f,
            pulseOffset = 0.18f
        ),
        CircuitSegmentSpec(
            id = "cap_a",
            start = CircuitPoint(0.08f, -0.14f),
            end = CircuitPoint(0.32f, -0.14f),
            thickness = 0.017f,
            feather = 0.010f,
            bandWidth = 0.13f,
            bandFeather = 0.04f,
            pulseSpeed = 0.30f,
            pulseOffset = 0.22f
        ),
        CircuitSegmentSpec(
            id = "cap_b",
            start = CircuitPoint(0.32f, -0.14f),
            end = CircuitPoint(0.32f, -0.18f),
            thickness = 0.017f,
            feather = 0.010f,
            bandWidth = 0.13f,
            bandFeather = 0.04f,
            pulseSpeed = 0.30f,
            pulseOffset = 0.32f
        ),
        CircuitSegmentSpec(
            id = "cpu_res_pin",
            start = CircuitPoint(0.16f, 0.12f),
            end = CircuitPoint(0.16f, 0.18f),
            thickness = 0.013f,
            feather = 0.010f,
            bandWidth = 0.12f,
            bandFeather = 0.04f,
            pulseSpeed = 0.38f,
            pulseOffset = 0.10f
        ),
        CircuitSegmentSpec(
            id = "res_a",
            start = CircuitPoint(0.16f, 0.18f),
            end = CircuitPoint(0.30f, 0.18f),
            thickness = 0.016f,
            feather = 0.010f,
            bandWidth = 0.13f,
            bandFeather = 0.04f,
            pulseSpeed = 0.38f,
            pulseOffset = 0.16f
        ),
        CircuitSegmentSpec(
            id = "out_a",
            start = CircuitPoint(0.23f, 0.02f),
            end = CircuitPoint(0.52f, 0.02f),
            thickness = 0.018f,
            feather = 0.010f,
            bandWidth = 0.13f,
            bandFeather = 0.04f,
            pulseSpeed = 0.40f,
            pulseOffset = 0.08f
        ),
        CircuitSegmentSpec(
            id = "res_out_a",
            start = CircuitPoint(0.54f, 0.18f),
            end = CircuitPoint(0.58f, 0.18f),
            thickness = 0.015f,
            feather = 0.010f,
            bandWidth = 0.12f,
            bandFeather = 0.04f,
            pulseSpeed = 0.44f,
            pulseOffset = 0.28f
        ),
        CircuitSegmentSpec(
            id = "res_out_b",
            start = CircuitPoint(0.58f, 0.18f),
            end = CircuitPoint(0.58f, 0.02f),
            thickness = 0.015f,
            feather = 0.010f,
            bandWidth = 0.12f,
            bandFeather = 0.04f,
            pulseSpeed = 0.44f,
            pulseOffset = 0.38f
        ),
        CircuitSegmentSpec(
            id = "res_out_c",
            start = CircuitPoint(0.58f, 0.02f),
            end = CircuitPoint(0.52f, 0.02f),
            thickness = 0.015f,
            feather = 0.010f,
            bandWidth = 0.12f,
            bandFeather = 0.04f,
            pulseSpeed = 0.44f,
            pulseOffset = 0.46f
        ),
        CircuitSegmentSpec(
            id = "out_pin",
            start = CircuitPoint(0.52f, 0.02f),
            end = CircuitPoint(0.56f, 0.02f),
            thickness = 0.014f,
            feather = 0.010f,
            bandWidth = 0.12f,
            bandFeather = 0.04f,
            pulseSpeed = 0.40f,
            pulseOffset = 0.18f
        )
    )

    val connections = listOf(
        CircuitConnectionSpec(
            from = CircuitNode.Source,
            to = CircuitNode.Processor,
            segmentIds = listOf("source_pin", "vin_a", "vin_b", "cpu_in_pin")
        ),
        CircuitConnectionSpec(
            from = CircuitNode.Oscillator,
            to = CircuitNode.Processor,
            segmentIds = listOf("osc_pin", "osc_a", "osc_b", "osc_c", "cpu_clk_pin")
        ),
        CircuitConnectionSpec(
            from = CircuitNode.Processor,
            to = CircuitNode.Capacitor,
            segmentIds = listOf("cpu_cap_pin", "cap_a", "cap_b")
        ),
        CircuitConnectionSpec(
            from = CircuitNode.Processor,
            to = CircuitNode.Resistor,
            segmentIds = listOf("cpu_res_pin", "res_a")
        ),
        CircuitConnectionSpec(
            from = CircuitNode.Processor,
            to = CircuitNode.Output,
            segmentIds = listOf("out_a", "out_pin")
        ),
        CircuitConnectionSpec(
            from = CircuitNode.Resistor,
            to = CircuitNode.Output,
            segmentIds = listOf("res_out_a", "res_out_b", "res_out_c", "out_pin")
        )
    )

    val nodeCenters = nodes.map { it.position }.toSet()
    val viaPoints = segments
        .flatMap { listOf(it.start, it.end) }
        .groupingBy { it }
        .eachCount()
        .filter { (point, count) -> count > 1 && point !in nodeCenters }
        .keys
        .toList()

    val activations = nodes.associate { node ->
        val incident = connections.filter { it.from == node.id || it.to == node.id }
        node.id to CircuitActivationSpec(
            nodeIds = incident.flatMap { listOf(it.from, it.to) }.toSet() + node.id,
            segmentIds = incident.flatMap { it.segmentIds }.toSet()
        )
    }

    return CircuitBoardSpec(
        nodes = nodes,
        segments = segments,
        connections = connections,
        viaPoints = viaPoints,
        activations = activations
    )
}

private fun CircuitPoint.toExpr(): Float2Expr = float2(x, y)

private fun circuitUnionMask(expressions: Iterable<FloatExpr>): FloatExpr {
    val items = expressions.toList()
    if (items.isEmpty()) return float(0f)

    fun merge(start: Int, endExclusive: Int): FloatExpr {
        val count = endExclusive - start
        return when {
            count <= 0 -> float(0f)
            count == 1 -> items[start]
            else -> {
                val middle = start + count / 2
                max(
                    merge(start, middle),
                    merge(middle, endExclusive)
                )
            }
        }
    }

    return merge(0, items.size)
}

private fun circuitSelection(route: FloatExpr, node: CircuitNodeSpec): BoolExpr =
    abs(route - node.shaderIndex) lt 0.25f

private fun circuitRoundedMask(
    local: Float2Expr,
    centerX: Float = 0f,
    centerY: Float = 0f,
    halfWidth: Float,
    halfHeight: Float,
    radius: Float,
    feather: Float
): FloatExpr = softFill(
    distance = sdRoundedBox(
        point = local - float2(centerX, centerY),
        halfSize = float2(halfWidth, halfHeight),
        radius = radius
    ),
    feather = feather
)

private fun circuitNodeMask(
    board: Float2Expr,
    node: CircuitNodeSpec
): FloatExpr {
    val local = board - node.position.toExpr()
    return when (node.shape) {
        CircuitNodeShape.Pad -> {
            if (node.id == CircuitNode.Source) {
                circuitUnionMask(
                    listOf(
                        softFill(
                            distance = sdCircle(local, radius = node.radius * 0.90f),
                            feather = node.feather * 0.82f
                        ),
                        circuitRoundedMask(
                            local = local,
                            centerX = node.radius * 0.42f,
                            halfWidth = 0.018f,
                            halfHeight = 0.014f,
                            radius = 0.008f,
                            feather = node.feather * 0.56f
                        )
                    )
                )
            } else {
                softFill(
                    distance = sdCircle(local, radius = node.radius),
                    feather = node.feather
                )
            }
        }

        CircuitNodeShape.Chip,
        CircuitNodeShape.Module -> {
            when (node.id) {
                CircuitNode.Oscillator -> circuitUnionMask(
                    listOf(
                        circuitRoundedMask(
                            local = local,
                            halfWidth = 0.086f,
                            halfHeight = 0.050f,
                            radius = 0.024f,
                            feather = node.feather * 0.78f
                        ),
                        circuitRoundedMask(
                            local = local,
                            centerX = 0.088f,
                            halfWidth = 0.018f,
                            halfHeight = 0.016f,
                            radius = 0.009f,
                            feather = node.feather * 0.56f
                        )
                    )
                )

                CircuitNode.Resistor -> circuitUnionMask(
                    listOf(
                        circuitRoundedMask(
                            local = local,
                            halfWidth = 0.064f,
                            halfHeight = 0.026f,
                            radius = 0.015f,
                            feather = node.feather * 0.76f
                        ),
                        circuitRoundedMask(
                            local = local,
                            centerX = -0.086f,
                            halfWidth = 0.024f,
                            halfHeight = 0.018f,
                            radius = 0.010f,
                            feather = node.feather * 0.56f
                        ),
                        circuitRoundedMask(
                            local = local,
                            centerX = 0.086f,
                            halfWidth = 0.024f,
                            halfHeight = 0.018f,
                            radius = 0.010f,
                            feather = node.feather * 0.56f
                        )
                    )
                )

                CircuitNode.Output -> circuitUnionMask(
                    listOf(
                        circuitRoundedMask(
                            local = local,
                            halfWidth = 0.094f,
                            halfHeight = 0.068f,
                            radius = 0.024f,
                            feather = node.feather * 0.80f
                        ),
                        circuitRoundedMask(
                            local = local,
                            centerX = -0.090f,
                            halfWidth = 0.022f,
                            halfHeight = 0.022f,
                            radius = 0.010f,
                            feather = node.feather * 0.54f
                        )
                    )
                )

                else -> softFill(
                    distance = sdRoundedBox(
                        point = local,
                        halfSize = node.halfSize.toExpr(),
                        radius = node.cornerRadius
                    ),
                    feather = node.feather
                )
            }
        }

        CircuitNodeShape.Capacitor -> circuitUnionMask(
            listOf(
                circuitRoundedMask(
                    local = local,
                    halfWidth = 0.042f,
                    halfHeight = 0.086f,
                    radius = 0.019f,
                    feather = node.feather * 0.76f
                ),
                circuitRoundedMask(
                    local = local,
                    centerY = 0.086f,
                    halfWidth = 0.024f,
                    halfHeight = 0.018f,
                    radius = 0.010f,
                    feather = node.feather * 0.56f
                )
            )
        )
    }
}

private fun circuitSegmentMask(
    board: Float2Expr,
    segment: CircuitSegmentSpec
): FloatExpr = segmentMask(
    point = board,
    start = segment.start.toExpr(),
    end = segment.end.toExpr(),
    thickness = if (segment.thickness * CIRCUIT_TRACE_THICKNESS_SCALE > CIRCUIT_TRACE_MIN_THICKNESS) {
        segment.thickness * CIRCUIT_TRACE_THICKNESS_SCALE
    } else {
        CIRCUIT_TRACE_MIN_THICKNESS
    },
    feather = if (segment.feather * CIRCUIT_TRACE_FEATHER_SCALE > CIRCUIT_TRACE_MIN_FEATHER) {
        segment.feather * CIRCUIT_TRACE_FEATHER_SCALE
    } else {
        CIRCUIT_TRACE_MIN_FEATHER
    }
)

private fun circuitSegmentPulse(
    board: Float2Expr,
    time: FloatExpr,
    segment: CircuitSegmentSpec
): FloatExpr {
    val start = segment.start.toExpr()
    val end = segment.end.toExpr()
    val phase = fract(time * segment.pulseSpeed + segment.pulseOffset)
    val progress = segmentProgress(point = board, start = start, end = end)
    val delta = fract(phase - progress + 1f)
    val pulseThickness = if (segment.thickness * CIRCUIT_TRACE_THICKNESS_SCALE + CIRCUIT_PULSE_THICKNESS_EXTRA > CIRCUIT_TRACE_MIN_THICKNESS) {
        segment.thickness * CIRCUIT_TRACE_THICKNESS_SCALE + CIRCUIT_PULSE_THICKNESS_EXTRA
    } else {
        CIRCUIT_TRACE_MIN_THICKNESS
    }
    val pulseFeather = if (segment.feather * CIRCUIT_TRACE_FEATHER_SCALE > CIRCUIT_TRACE_MIN_FEATHER) {
        segment.feather * CIRCUIT_TRACE_FEATHER_SCALE
    } else {
        CIRCUIT_TRACE_MIN_FEATHER
    }
    val headLength = if (segment.bandWidth * 0.20f > 0.018f) {
        segment.bandWidth * 0.20f
    } else {
        0.018f
    }
    val tailLength = if (segment.bandWidth * 0.72f > headLength + 0.02f) {
        segment.bandWidth * 0.72f
    } else {
        headLength + 0.02f
    }
    val head = 1f - smoothstep(0f, headLength + segment.bandFeather * 0.45f, delta)
    val tail = (1f - smoothstep(headLength, tailLength + segment.bandFeather, delta)) * 0.42f
    val profile = max(head, tail)
    return profile * segmentMask(
        point = board,
        start = start,
        end = end,
        thickness = pulseThickness,
        feather = pulseFeather
    )
}

private fun circuitViaMask(
    board: Float2Expr,
    point: CircuitPoint
): FloatExpr = softFill(
    distance = sdCircle(board - point.toExpr(), radius = 0.0042f),
    feather = 0.0030f
)

private fun CircuitNodeSpec.uiXFraction(aspect: Float): Float =
    position.x / aspect + 0.5f

private fun CircuitNodeSpec.uiYFraction(): Float =
    position.y + 0.5f

private fun CircuitNodeSpec.uiWidth(maxWidth: Dp, aspect: Float): Dp {
    val boardWidth = when (shape) {
        CircuitNodeShape.Pad -> radius * 2f
        CircuitNodeShape.Chip,
        CircuitNodeShape.Module,
        CircuitNodeShape.Capacitor -> halfSize.x * 2f
    }
    return maxWidth * (boardWidth / aspect)
}

private fun CircuitNodeSpec.uiHeight(maxHeight: Dp): Dp {
    val boardHeight = when (shape) {
        CircuitNodeShape.Pad -> radius * 2f
        CircuitNodeShape.Chip,
        CircuitNodeShape.Module,
        CircuitNodeShape.Capacitor -> halfSize.y * 2f
    }
    return maxHeight * boardHeight
}

@Composable
private fun CircuitPreviewStage(
    modifier: Modifier = Modifier,
    board: CircuitBoardSpec,
    selected: CircuitNode,
    onSelect: (CircuitNode) -> Unit
) {
    val shape = RoundedCornerShape(24.dp)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(244.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.96f),
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.94f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.26f),
                shape = shape
            )
    ) {
        val previewAspect = remember(maxWidth, maxHeight) {
            (maxWidth.value / maxHeight.value).coerceAtLeast(1f)
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .then(modifier)
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 1f),
                                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 1f)
                            )
                        )
                )
            )
        }

        board.nodes.forEach { node ->
            val targetWidth = node.uiWidth(maxWidth, previewAspect)
            val targetHeight = node.uiHeight(maxHeight)
            CircuitTapTarget(
                text = node.shortLabel,
                selected = selected == node.id,
                width = targetWidth,
                height = targetHeight,
                modifier = Modifier.offset(
                    x = maxWidth * node.uiXFraction(previewAspect) - targetWidth / 2f,
                    y = maxHeight * node.uiYFraction() - targetHeight / 2f
                ),
                onClick = { onSelect(node.id) }
            )
        }
    }
}

@Composable
fun DemoCircuit() {
    val boardSpec = circuitBoardSpec
    val nodesById = remember { boardSpec.nodes.associateBy { it.id } }
    var playing by rememberSaveable { mutableStateOf(true) }
    var selected by rememberSaveable { mutableStateOf(CircuitNode.Source) }
    var amountUi by rememberSaveable { mutableFloatStateOf(90f) }

    val setup = remember {
        var timeParam: FxParam.Float? = null
        var routeParam: FxParam.Float? = null
        var amountParam: FxParam.Float? = null
        val effect = redbytefx {
            val time by autoUniformTime()
            val route by autoUniformFloat(0f)
            val amount by autoUniformFloat(0.9f)
            timeParam = time
            routeParam = route
            amountParam = amount

            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val board = let(aspectCenteredUv(uv, resolution), "board")

            val boardMask = let(
                softFill(
                    distance = sdRoundedBox(
                        point = board,
                        halfSize = float2(CIRCUIT_BOARD_HALF_WIDTH, CIRCUIT_BOARD_HALF_HEIGHT),
                        radius = 0.06f
                    ),
                    feather = 0.03f
                ),
                "board_mask"
            )

            val nodeMasks = boardSpec.nodes.associate { node ->
                node.id to let(
                    circuitNodeMask(board = board, node = node),
                    "${node.id.name.lowercase()}_mask"
                )
            }
            val traceMasks = boardSpec.segments.associate { segment ->
                segment.id to let(
                    circuitSegmentMask(board = board, segment = segment),
                    "${segment.id}_trace"
                )
            }
            val pulseMasks = boardSpec.segments.associate { segment ->
                segment.id to let(
                    circuitSegmentPulse(board = board, time = time, segment = segment),
                    "${segment.id}_pulse"
                )
            }
            val viaMask = let(
                circuitUnionMask(boardSpec.viaPoints.map { circuitViaMask(board = board, point = it) }),
                "via_mask"
            )

            val passiveCopper = let(
                circuitUnionMask(traceMasks.values) +
                    circuitUnionMask(nodeMasks.values) * 0.72f +
                    viaMask * 0.54f,
                "passive_copper"
            )
            val activeNodeMask = let(
                circuitUnionMask(
                    boardSpec.activations.map { (nodeId, activation) ->
                        ifElse(
                            circuitSelection(route, nodesById.getValue(nodeId)),
                            circuitUnionMask(activation.nodeIds.map { activeNodeId ->
                                nodeMasks.getValue(activeNodeId)
                            }),
                            float(0f)
                        )
                    }
                ),
                "active_node_mask"
            )
            val activeTraceMask = let(
                circuitUnionMask(
                    boardSpec.activations.map { (nodeId, activation) ->
                        ifElse(
                            circuitSelection(route, nodesById.getValue(nodeId)),
                            circuitUnionMask(activation.segmentIds.map { segmentId ->
                                traceMasks.getValue(segmentId)
                            }),
                            float(0f)
                        )
                    }
                ),
                "active_trace_mask"
            )
            val activePulseMask = let(
                circuitUnionMask(
                    boardSpec.activations.map { (nodeId, activation) ->
                        ifElse(
                            circuitSelection(route, nodesById.getValue(nodeId)),
                            circuitUnionMask(activation.segmentIds.map { segmentId ->
                                pulseMasks.getValue(segmentId)
                            }),
                            float(0f)
                        )
                    }
                ),
                "active_pulse_mask"
            )
            val selectedNodeMask = let(
                circuitUnionMask(
                    boardSpec.nodes.map { node ->
                        ifElse(
                            circuitSelection(route, node),
                            nodeMasks.getValue(node.id),
                            float(0f)
                        )
                    }
                ),
                "selected_node_mask"
            )
            val relatedNodeMask = let(
                max(activeNodeMask - selectedNodeMask * 0.55f, 0f),
                "related_node_mask"
            )

            val boardTint = let(color(float3(0.02f, 0.12f, 0.08f), 1f), "board_tint")
            val copperTint = let(color(float3(0.12f, 0.48f, 0.28f), 1f), "copper_tint")
            val glowTint = let(color(float3(0.05f, 0.36f, 0.56f), 1f), "glow_tint")
            val signalTint = let(color(float3(0.34f, 0.86f, 1f), 1f), "signal_tint")
            val sparkTint = let(color(float3(0.98f, 0.86f, 0.18f), 1f), "spark_tint")
            val hotTint = let(color(float3(1f, 0.96f, 0.82f), 1f), "hot_tint")

            val substrate = let(maskedMix(base, boardTint, boardMask, 1f), "substrate")
            val copper = let(
                maskedMix(
                    base = substrate,
                    revealed = copperTint,
                    mask = passiveCopper,
                    amount = 0.74f
                ),
                "copper"
            )
            val energized = let(
                maskedMix(
                    base = copper,
                    revealed = glowTint,
                    mask = activeTraceMask + relatedNodeMask * 0.24f,
                    amount = amount * 0.44f
                ),
                "energized"
            )

            val pulsed = let(
                maskedMix(
                    base = energized,
                    revealed = signalTint,
                    mask = activePulseMask + activeTraceMask * 0.08f,
                    amount = amount * 0.92f
                ),
                "pulsed"
            )

            val sparked = let(
                maskedMix(
                    base = pulsed,
                    revealed = sparkTint,
                    mask = activePulseMask * 0.74f,
                    amount = amount * 0.58f
                ),
                "sparked"
            )

            maskedMix(
                base = sparked,
                revealed = hotTint,
                mask = selectedNodeMask + activePulseMask * 0.14f,
                amount = amount * 0.34f
            )
        }
        CircuitSetup(
            effect = effect,
            time = timeParam!!,
            route = routeParam!!,
            amount = amountParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(
        setup.route,
        nodesById.getValue(selected).shaderIndex
    )
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            CircuitPreviewStage(
                modifier = Modifier.redbyteFx(fx),
                board = boardSpec,
                selected = selected,
                onSelect = { selected = it }
            )
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            Text(text = "Active Node", style = MaterialTheme.typography.titleMedium)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                boardSpec.nodes.chunked(3).forEach { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        chunk.forEach { node ->
                            RadioRow(node.title, selected = selected == node.id) {
                                selected = node.id
                            }
                        }
                    }
                }
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}

@Composable
private fun CircuitTapTarget(
    text: String,
    selected: Boolean,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val textColor = if (selected) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.96f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f)
    }
    val fontSize = if (width < 34.dp) {
        10.sp
    } else {
        11.sp
    }
    val letterSpacing = if (width < 34.dp) {
        0.4.sp
    } else {
        0.9.sp
    }

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize,
                letterSpacing = letterSpacing
            ),
            textAlign = TextAlign.Center,
            softWrap = false,
            color = textColor
        )
    }
}

@Composable
private fun rememberGeneratedAgsl(effect: FxEffect): String =
    remember(effect) { effect.agslSource() }
