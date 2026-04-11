package ru.redbyte.redbytefx.sample.app

import android.content.Intent
import ru.redbyte.redbytefx.sample.model.DemoId

public const val EXTRA_START_DEMO: String = "ru.redbyte.redbytefx.sample.extra.START_DEMO"

public fun Intent.startDemoIdOrNull(): DemoId? {
    val raw = getStringExtra(EXTRA_START_DEMO)?.trim().orEmpty()
    if (raw.isEmpty()) return null
    return DemoId.entries.firstOrNull { demo ->
        demo.name.equals(raw, ignoreCase = true)
    }
}
