package ru.redbyte.redbytefx.sample.model

enum class DemoId {
    Flip,
    Mirror,
    Rotate,
    Scale,
    Offset
}

data class DemoInfo(
    val id: DemoId,
    val title: String,
    val subtitle: String
)

val DemoCatalog: List<DemoInfo> = listOf(
    DemoInfo(DemoId.Flip, "Flip", "Flip X/Y (classic reflections)"),
    DemoInfo(DemoId.Mirror, "Mirror", "Symmetry X/Y using left/right or top/bottom half"),
    DemoInfo(DemoId.Rotate, "Rotate", "Rotation around center pivot"),
    DemoInfo(DemoId.Scale, "Scale", "Uniforms: float2 (sx, sy)"),
    DemoInfo(DemoId.Offset, "Offset", "Uniforms: float2 (dx, dy)")
)