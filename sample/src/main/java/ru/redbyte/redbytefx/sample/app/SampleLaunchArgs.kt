package ru.redbyte.redbytefx.sample.app

import android.content.Intent
import ru.redbyte.redbytefx.RedByteFxPlatform
import ru.redbyte.redbytefx.sample.model.DemoId

public const val EXTRA_START_DEMO: String = "ru.redbyte.redbytefx.sample.extra.START_DEMO"

public fun Intent.startDemoIdOrNull(): DemoId? =
    startDemoFrom(getStringExtra(EXTRA_START_DEMO), RedByteFxPlatform.isAgslSupported())

/** The AGSL demo named by [raw], or null when it is unknown or this device cannot run AGSL. */
internal fun startDemoFrom(raw: String?, agslSupported: Boolean): DemoId? {
    if (!agslSupported) return null
    val name = raw?.trim().orEmpty()
    if (name.isEmpty()) return null
    return DemoId.entries.firstOrNull { demo -> demo.name.equals(name, ignoreCase = true) }
}
