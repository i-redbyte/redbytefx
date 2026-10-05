package ru.redbyte.redbytefx.gl

internal fun rgbaByteCount(width: Int, height: Int): Int {
    require(width > 0 && height > 0) { "RGBA size must be positive, was ${width}x$height" }
    val pixels = width.toLong() * height.toLong()
    require(pixels <= Int.MAX_VALUE / 4) { "RGBA image of ${width}x$height is too large" }
    return (pixels * 4L).toInt()
}

internal fun readFramebufferPixels(device: GlDevice, width: Int, height: Int, into: ByteArray) {
    val bytes = rgbaByteCount(width, height)
    require(into.size >= bytes) { "Pixel read needs $bytes bytes, was ${into.size}" }
    device.readPixelsRgba(0, 0, width, height, into)
}

internal fun readColorTargetPixels(device: GlDevice, bound: Int, target: GlColorTarget, into: ByteArray) {
    val bytes = rgbaByteCount(target.width, target.height)
    require(into.size >= bytes) { "Pixel read needs $bytes bytes, was ${into.size}" }
    val restore = bound != target.framebuffer
    if (restore) device.bindFramebuffer(target.framebuffer)
    try {
        device.readPixelsRgba(0, 0, target.width, target.height, into)
    } finally {
        if (restore) device.bindFramebuffer(bound)
    }
}
