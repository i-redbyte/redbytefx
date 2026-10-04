package ru.redbyte.redbytefx.gl

internal fun rgbaByteCount(width: Int, height: Int): Int {
    require(width > 0 && height > 0) { "Pixel read size must be positive, was ${width}x$height" }
    val bytes = width.toLong() * height.toLong() * 4L
    require(bytes <= Int.MAX_VALUE) { "Pixel read of ${width}x$height is too large" }
    return bytes.toInt()
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
